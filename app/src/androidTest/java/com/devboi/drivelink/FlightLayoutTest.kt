package com.devboi.drivelink

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.test.platform.app.InstrumentationRegistry
import com.devboi.drivelink.data.InputRepository
import com.devboi.drivelink.data.SettingsRepository
import com.devboi.drivelink.network.InputSender
import com.devboi.drivelink.ui.App
import com.devboi.drivelink.viewmodel.MainViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class FlightLayoutTest {
    @get:Rule val compose = createComposeRule()

    @Test fun opposingCollectiveTouchesCancelAndReleasingOneRestoresTheOther() {
        val settings = SettingsRepository(InstrumentationRegistry.getInstrumentation().context)
        val inputs = InputRepository()
        val vm = MainViewModel(settings, inputs, InputSender(inputs))
        vm.changeFlightMode(true)
        compose.setContent { App(vm) }
        val increasePoint = compose.onNodeWithContentDescription("总距升高，按住升高，松手停止").fetchSemanticsNode().boundsInRoot.center
        val decreasePoint = compose.onNodeWithContentDescription("总距降低，按住降低，松手停止").fetchSemanticsNode().boundsInRoot.center
        assertTrue(increasePoint.y < decreasePoint.y)
        compose.onRoot().performTouchInput { down(0, increasePoint) }
        compose.runOnIdle {
            assertEquals(1f, inputs.getInputs().throttle, 0f)
            assertEquals(0f, inputs.getInputs().flaps, 0f)
        }
        compose.onRoot().performTouchInput { down(1, decreasePoint) }
        compose.runOnIdle {
            assertEquals(0f, inputs.getInputs().throttle, 0f)
            assertEquals(0f, inputs.getInputs().flaps, 0f)
        }
        compose.onRoot().performTouchInput { up(1) }
        compose.runOnIdle { assertEquals(1f, inputs.getInputs().throttle, 0f) }
        compose.onRoot().performTouchInput { cancel() }
        compose.runOnIdle {
            assertEquals(0f, inputs.getInputs().throttle, 0f)
            assertEquals(0f, inputs.getInputs().flaps, 0f)
        }
    }

    @Test fun pausedGyroKeepsButtonsUsableAndResetReleasesHeldInputs() {
        val settings = SettingsRepository(InstrumentationRegistry.getInstrumentation().context)
        val inputs = InputRepository()
        val vm = MainViewModel(settings, inputs, InputSender(inputs))
        vm.changeFlightMode(true)
        vm.onRotationChange(floatArrayOf(0f, 0f, 0f, 1f), 1)
        compose.setContent { App(vm) }
        compose.onNodeWithText("以当前姿势回中").assertIsNotEnabled()
        val unlock = compose.onNodeWithContentDescription("解锁载具").fetchSemanticsNode().boundsInRoot.center
        val supply = compose.onNodeWithContentDescription("部署补给箱").fetchSemanticsNode().boundsInRoot.center
        val attack = compose.onNodeWithContentDescription("攻击").fetchSemanticsNode().boundsInRoot.center
        val countermeasures = compose.onNodeWithContentDescription("对抗装置").fetchSemanticsNode().boundsInRoot.center
        val turnLeft = compose.onNodeWithContentDescription("机头左转").fetchSemanticsNode().boundsInRoot.center
        val turnRight = compose.onNodeWithContentDescription("机头右转").fetchSemanticsNode().boundsInRoot.center
        assertTrue(unlock.x < attack.x)
        assertTrue(unlock.y < supply.y)
        assertTrue(attack.y < countermeasures.y)
        assertTrue(turnLeft.x < turnRight.x)
        assertTrue(turnLeft.y > supply.y)
        for ((position, mask) in listOf(unlock to 1, supply to 4, attack to 8, countermeasures to 2, turnLeft to 16, turnRight to 32)) {
            compose.onRoot().performTouchInput { down(0, position) }
            compose.runOnIdle { assertEquals(mask, inputs.getInputs().buttons) }
            compose.onRoot().performTouchInput { up(0) }
            compose.runOnIdle { assertEquals(0, inputs.getInputs().buttons) }
        }
        compose.onRoot().performTouchInput { down(0, turnLeft); down(1, turnRight) }
        compose.runOnIdle { assertEquals(48, inputs.getInputs().buttons) }
        compose.onRoot().performTouchInput { up(0) }
        compose.runOnIdle { assertEquals(32, inputs.getInputs().buttons) }
        compose.onRoot().performTouchInput { cancel() }
        compose.runOnIdle { assertEquals(0, inputs.getInputs().buttons) }
        val collective = compose.onNodeWithContentDescription("总距升高，按住升高，松手停止").fetchSemanticsNode().boundsInRoot.center
        compose.onRoot().performTouchInput { down(0, collective); down(1, attack) }
        compose.runOnIdle {
            assertEquals(1f, inputs.getInputs().throttle, 0f)
            assertEquals(8, inputs.getInputs().buttons)
            vm.resetInputs()
        }
        compose.waitForIdle()
        compose.runOnIdle {
            assertEquals(0f, inputs.getInputs().throttle, 0f)
            assertEquals(0f, inputs.getInputs().flaps, 0f)
            assertEquals(0, inputs.getInputs().buttons)
        }
        compose.onRoot().performTouchInput { cancel() }
        compose.onRoot().performTouchInput { down(0, collective) }
        compose.runOnIdle { assertEquals(1f, inputs.getInputs().throttle, 0f) }
        compose.onRoot().performTouchInput { up(0) }
        compose.runOnIdle { assertEquals(0f, inputs.getInputs().throttle, 0f) }
    }
}
