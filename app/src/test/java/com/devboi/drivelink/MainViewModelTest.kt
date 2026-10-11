package com.devboi.drivelink

import com.devboi.drivelink.data.InputRepository
import com.devboi.drivelink.data.SettingsRepository
import com.devboi.drivelink.network.InputSender
import com.devboi.drivelink.ui.screens.Screens
import com.devboi.drivelink.viewmodel.MainViewModel
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.math.cos
import kotlin.math.sin

class MainViewModelTest {
    @get:Rule val files = TemporaryFolder()

    @Test fun flightTurnButtonsReleaseOnModeSettingsAndFocusChanges() {
        val inputs = InputRepository()
        val vm = MainViewModel(SettingsRepository(File(files.root, "settings.json")), inputs, InputSender(inputs))
        vm.changeFlightMode(true)
        vm.onButtonPress(4)
        vm.onButtonPress(5)
        assertEquals(48, inputs.getInputs().buttons)
        vm.onButtonRelease(4)
        assertEquals(32, inputs.getInputs().buttons)
        vm.onButtonRelease(5)
        assertEquals(0, inputs.getInputs().buttons)

        for (release in listOf<() -> Unit>(
            { vm.changeFlightMode(false) },
            { vm.changeScreen(Screens.Settings) },
            { vm.pauseInputs() }
        )) {
            vm.changeScreen(Screens.Home)
            vm.changeFlightMode(true)
            vm.onButtonPress(4)
            vm.onButtonPress(5)
            assertEquals(48, inputs.getInputs().buttons)
            release()
            assertEquals(0, inputs.getInputs().buttons)
        }
    }

    private fun rotation(axis: Int, degrees: Double): FloatArray {
        val half = Math.toRadians(degrees) / 2
        return FloatArray(4).also { it[axis] = sin(half).toFloat(); it[3] = cos(half).toFloat() }
    }

    private fun multiply(a: FloatArray, b: FloatArray) = floatArrayOf(
        a[3]*b[0]+a[0]*b[3]+a[1]*b[2]-a[2]*b[1],
        a[3]*b[1]-a[0]*b[2]+a[1]*b[3]+a[2]*b[0],
        a[3]*b[2]+a[0]*b[1]-a[1]*b[0]+a[2]*b[3],
        a[3]*b[3]-a[0]*b[0]-a[1]*b[1]-a[2]*b[2]
    )

    @Test fun collectiveOpposingHoldsCancelAndSnapshotsStayAtomic() {
        val inputs = InputRepository()
        val vm = MainViewModel(SettingsRepository(File(files.root, "settings.json")), inputs, InputSender(inputs))
        vm.changeFlightMode(true)
        vm.onCollectiveChange(true, false)
        assertEquals(1f, inputs.getInputs().throttle, 0f)
        assertEquals(0f, inputs.getInputs().flaps, 0f)
        vm.onCollectiveChange(true, true)
        assertEquals(0f, inputs.getInputs().throttle, 0f)
        assertEquals(0f, inputs.getInputs().flaps, 0f)
        vm.onCollectiveChange(false, true)
        assertEquals(0f, inputs.getInputs().throttle, 0f)
        assertEquals(1f, inputs.getInputs().flaps, 0f)
        vm.resetInputs()
        assertEquals(0f, inputs.getInputs().throttle, 0f)
        assertEquals(0f, inputs.getInputs().flaps, 0f)
        assertEquals(0f, inputs.getInputs().yaw, 0f)
    }

    @Test fun horizontalGateKeepsTouchInputsAndRecentersBothModes() {
        val inputs = InputRepository()
        val vm = MainViewModel(SettingsRepository(File(files.root, "settings.json")), inputs, InputSender(inputs))
        val upright = rotation(0, 90.0)
        vm.changeFlightMode(true)
        vm.onCollectiveChange(true, false)
        vm.onButtonPress(0)
        vm.onButtonPress(2)
        vm.onRotationChange(upright, 1)
        vm.onRotationChange(multiply(upright, rotation(1, 16.0)), 1)
        assertEquals(0.5f, inputs.getInputs().pitch, 0.001f)
        vm.onRotationChange(rotation(0, 0.0), 1)
        assertTrue(vm.motionSuspended.value)
        assertEquals(0f, inputs.getInputs().pitch, 0f)
        assertEquals(0f, inputs.getInputs().roll, 0f)
        assertEquals(1f, inputs.getInputs().throttle, 0f)
        assertEquals(5, inputs.getInputs().buttons)
        vm.calibrateFlight()
        vm.onRotationChange(rotation(0, 180.0), 1)
        vm.onRotationChange(floatArrayOf(Float.NaN, 0f, 0f, 1f), 1)
        assertTrue(vm.motionSuspended.value)
        vm.onRotationChange(upright, 1)
        assertFalse(vm.motionSuspended.value)
        assertEquals(0f, inputs.getInputs().pitch, 0f)
        vm.onRotationChange(multiply(upright, rotation(1, 16.0)), 1)
        assertEquals(0.5f, inputs.getInputs().pitch, 0.001f)

        vm.changeFlightMode(false)
        vm.onThrottleChange(0.4f)
        vm.onButtonPress(1)
        vm.onAccelerometerChange(floatArrayOf(0f, 9.81f, 0f), System.nanoTime())
        vm.onGyroscopeChange(floatArrayOf(0f, 0f, 3f), System.nanoTime())
        vm.onGyroscopeChange(floatArrayOf(0f, 0f, 3f), System.nanoTime())
        assertTrue(inputs.getInputs().steering > 0f)
        vm.onRotationChange(rotation(0, 0.0), 1)
        vm.onGyroscopeChange(floatArrayOf(0f, 0f, 999f), System.nanoTime())
        vm.onSteeringChange(0.9f)
        assertEquals(0f, inputs.getInputs().steering, 0f)
        assertEquals(0.4f, inputs.getInputs().throttle, 0f)
        assertEquals(2, inputs.getInputs().buttons)
        vm.onRotationChange(upright, 1)
        vm.onGyroscopeChange(floatArrayOf(0f, 0f, 3f), System.nanoTime())
        assertEquals(0f, inputs.getInputs().steering, 0f)
        vm.onGyroscopeChange(floatArrayOf(0f, 0f, 3f), System.nanoTime())
        assertTrue(inputs.getInputs().steering > 0f)
    }

    @Test fun gravityFallbackBlocksGyroUntilRaised() {
        val inputs = InputRepository()
        val vm = MainViewModel(SettingsRepository(File(files.root, "settings.json")), inputs, InputSender(inputs))
        for (z in listOf(9.81f, -9.81f)) {
            vm.onAccelerometerChange(floatArrayOf(0f, 0f, z), System.nanoTime())
            vm.onGyroscopeChange(floatArrayOf(0f, 0f, 999f), System.nanoTime())
            assertTrue(vm.motionSuspended.value)
            assertEquals(0f, inputs.getInputs().steering, 0f)
        }
        vm.onAccelerometerChange(floatArrayOf(Float.NaN, 0f, 9.81f), System.nanoTime())
        assertTrue(vm.motionSuspended.value)
        vm.onAccelerometerChange(floatArrayOf(0f, 9.81f, 0f), System.nanoTime())
        assertFalse(vm.motionSuspended.value)
        vm.onGyroscopeChange(floatArrayOf(0f, 0f, 3f), System.nanoTime())
        assertEquals(0f, inputs.getInputs().steering, 0f)
        vm.onGyroscopeChange(floatArrayOf(0f, 0f, 3f), System.nanoTime())
        assertTrue(inputs.getInputs().steering > 0f)
    }

    @Test fun inversionAndScalePersistAcrossPauseWithoutAffectingGroundSteering() {
        val file = File(files.root, "settings.json")
        val settings = SettingsRepository(file)
        val inputs = InputRepository()
        val vm = MainViewModel(settings, inputs, InputSender(inputs))
        assertFalse(vm.flightInvertX.value)
        assertFalse(vm.flightInvertY.value)
        vm.changeFlightMode(true)
        val upright = rotation(0, 90.0)
        val combined = multiply(upright, multiply(rotation(2, -16.0), rotation(1, 16.0)))
        vm.onRotationChange(upright, 1)
        vm.onRotationChange(combined, 1)
        vm.changeFlightInvertX(true)
        vm.changeFlightInvertY(true)
        assertEquals(-0.5f, inputs.getInputs().roll, 0.001f)
        assertEquals(-0.5f, inputs.getInputs().pitch, 0.001f)
        assertTrue(SettingsRepository(file).getFlightInvertX())
        assertTrue(SettingsRepository(file).getFlightInvertY())
        vm.changeFlightScaleX(0.5f)
        vm.changeFlightScaleY(2f)
        vm.onRotationChange(combined, 1)
        assertEquals(-0.25f, inputs.getInputs().roll, 0.001f)
        assertEquals(-1f, inputs.getInputs().pitch, 0.001f)
        assertEquals(0.5f, SettingsRepository(file).getFlightScaleX(), 0f)
        assertEquals(2f, SettingsRepository(file).getFlightScaleY(), 0f)
        vm.changeFlightScaleX(Float.NaN)
        vm.changeFlightScaleY(-1f)
        assertEquals(0.5f, vm.flightScaleX.value, 0f)
        assertEquals(2f, vm.flightScaleY.value, 0f)
        vm.pauseInputs()
        assertTrue(vm.flightInvertX.value)
        assertTrue(vm.flightInvertY.value)
        vm.resumeInputs()
        vm.onRotationChange(upright, 1)
        vm.onRotationChange(combined, 1)
        assertEquals(-0.25f, inputs.getInputs().roll, 0.001f)
        assertEquals(-1f, inputs.getInputs().pitch, 0.001f)
        vm.changeFlightMode(false)
        vm.onSteeringChange(0.3f)
        assertEquals(0.3f, inputs.getInputs().steering, 0f)
        vm.resetSettingsToDefault()
        assertFalse(vm.flightInvertX.value)
        assertFalse(vm.flightInvertY.value)
        assertFalse(SettingsRepository(file).getFlightInvertX())
        assertFalse(SettingsRepository(file).getFlightInvertY())
        assertEquals(1f, vm.flightScaleX.value, 0f)
        assertEquals(1f, vm.flightScaleY.value, 0f)
        assertEquals(1f, SettingsRepository(file).getFlightScaleX(), 0f)
        assertEquals(1f, SettingsRepository(file).getFlightScaleY(), 0f)
    }

    @Test fun staleRotationExpiresOnlyMotionAndGravityCanTakeOver() {
        var now = 1_000_000_000L
        val inputs = InputRepository { now }
        val vm = MainViewModel(SettingsRepository(File(files.root, "settings.json")), inputs, InputSender(inputs)) { now }
        val upright = rotation(0, 90.0)
        vm.changeFlightMode(true)
        vm.onRotationChange(upright, 1, now)
        now += 10_000_000L
        val tilted = multiply(upright, rotation(1, 16.0))
        vm.onRotationChange(tilted, 1, now)
        vm.onCollectiveChange(true, false)
        vm.onButtonPress(0)
        assertEquals(0.5f, inputs.getInputs().pitch, 0.001f)
        now += 499_999_999L
        assertEquals(0.5f, inputs.getInputs().pitch, 0.001f)
        now += 1L
        assertEquals(0f, inputs.getInputs().pitch, 0f)
        assertEquals(1f, inputs.getInputs().throttle, 0f)
        assertEquals(1, inputs.getInputs().buttons)
        vm.onButtonPress(2)
        assertEquals(0f, inputs.getInputs().pitch, 0f)
        assertEquals(5, inputs.getInputs().buttons)
        vm.onRotationChange(rotation(0, 0.0), 1, now - 600_000_000L)
        assertFalse(vm.motionSuspended.value)
        vm.onRotationChange(rotation(0, 0.0), 1, now + 1L)
        assertFalse(vm.motionSuspended.value)
        vm.onAccelerometerChange(floatArrayOf(0f, 0f, 9.81f), now)
        assertTrue(vm.motionSuspended.value)
        assertEquals(1f, inputs.getInputs().throttle, 0f)
        assertEquals(5, inputs.getInputs().buttons)
        now += 10_000_000L
        vm.onRotationChange(tilted, 1, now)
        assertFalse(vm.motionSuspended.value)
        assertEquals(0f, inputs.getInputs().pitch, 0f)
        now += 10_000_000L
        vm.onRotationChange(multiply(upright, rotation(1, 32.0)), 1, now)
        assertEquals(0.5f, inputs.getInputs().pitch, 0.001f)
        vm.onRotationChange(rotation(0, 0.0), 1, now - 1L)
        assertFalse(vm.motionSuspended.value)
        assertEquals(0.5f, inputs.getInputs().pitch, 0.001f)
        now += 500_000_001L
        vm.changeFlightMode(false)
        vm.onAccelerometerChange(floatArrayOf(0f, 9.81f, 0f), now)
        now += 10_000_000L
        vm.onGyroscopeChange(floatArrayOf(0f, 0f, 3f), now)
        now += 100_000_000L
        vm.onGyroscopeChange(floatArrayOf(0f, 0f, 3f), now)
        assertTrue(inputs.getInputs().steering > 0f)
        vm.onThrottleChange(0.4f)
        vm.onButtonPress(3)
        now += 500_000_001L
        assertEquals(0f, inputs.getInputs().steering, 0f)
        assertEquals(0.4f, inputs.getInputs().throttle, 0f)
        assertEquals(8, inputs.getInputs().buttons)
        vm.onAccelerometerChange(floatArrayOf(0f, 9.81f, 0f), now)
        now += 10_000_000L
        vm.onGyroscopeChange(floatArrayOf(0f, 0f, 3f), now)
        assertEquals(0f, inputs.getInputs().steering, 0f)
        assertEquals(0.4f, inputs.getInputs().throttle, 0f)
        assertEquals(8, inputs.getInputs().buttons)
        now += 100_000_000L
        vm.onGyroscopeChange(floatArrayOf(0f, 0f, 3f), now)
        assertTrue(inputs.getInputs().steering > 0f)
    }

    @Test fun nullAndLegacySettingsKeepNewPreferencesSafe() {
        val file = File(files.root, "settings.json").apply { writeText("null") }
        val settings = SettingsRepository(file)
        assertFalse(settings.getFlightInvertX())
        assertFalse(settings.getFlightInvertY())
        assertEquals(1f, settings.getFlightScaleX(), 0f)
        assertEquals(1f, settings.getFlightScaleY(), 0f)
        settings.setFlightInvertX(true)
        assertTrue(SettingsRepository(file).getFlightInvertX())
        file.writeText("""{"serverAddr":"127.0.0.1","altLayoutSelected":true}""")
        val legacy = SettingsRepository(file)
        assertEquals("127.0.0.1", legacy.getServerAddr())
        assertTrue(legacy.getAltLayoutSelected())
        assertFalse(legacy.getFlightInvertX())
        assertFalse(legacy.getFlightInvertY())
        assertEquals(1f, legacy.getFlightScaleX(), 0f)
        assertEquals(1f, legacy.getFlightScaleY(), 0f)
        file.writeText("""{"flightScaleX":-1,"flightScaleY":0}""")
        val invalid = SettingsRepository(file)
        assertEquals(1f, invalid.getFlightScaleX(), 0f)
        assertEquals(1f, invalid.getFlightScaleY(), 0f)
    }
}
