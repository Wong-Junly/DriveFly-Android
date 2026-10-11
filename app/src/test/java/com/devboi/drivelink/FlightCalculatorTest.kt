package com.devboi.drivelink

import com.devboi.drivelink.sensors.FlightCalculator
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.cos
import kotlin.math.sin

class FlightCalculatorTest {
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

    private fun uprightRotation(axis: Int, degrees: Double): FloatArray =
        multiply(rotation(0, 90.0), rotation(axis, degrees))

    @Test fun landscapeAxesDeadzoneAndLimits() {
        val c = FlightCalculator()
        c.update(uprightRotation(2, 0.0), 1)
        assertArrayEquals(floatArrayOf(1f, 0f), c.update(uprightRotation(2, -30.0), 1), 0.001f)
        assertArrayEquals(floatArrayOf(0f, 1f), c.update(uprightRotation(1, 30.0), 1), 0.001f)
        assertArrayEquals(floatArrayOf(0f, 0f), c.update(uprightRotation(1, 1.9), 1), 0.001f)
        assertEquals(-1f, c.update(uprightRotation(1, -60.0), 1)!![1], 0.001f)
        assertEquals(-1f, c.update(uprightRotation(1, 30.0), 3)!![1], 0.001f)
        assertEquals(1f, c.update(uprightRotation(0, 30.0), 0)!![1], 0.001f)
        assertEquals(-1f, c.update(uprightRotation(0, 30.0), 2)!![1], 0.001f)
    }

    @Test fun arbitraryHomeAndQuaternionSignAreEquivalent() {
        val c = FlightCalculator()
        val home = multiply(rotation(0, 75.0), rotation(2, 179.0))
        assertArrayEquals(floatArrayOf(0f, 0f), c.update(home, 1), 0.001f)
        val current = multiply(home, rotation(1, 16.0))
        val axes = c.update(current, 1)!!
        assertEquals(0.5f, axes[1], 0.001f)
        assertArrayEquals(axes, c.update(current.map { -it }.toFloatArray(), 1), 0.001f)
        c.calibrate()
        assertArrayEquals(floatArrayOf(0f, 0f), c.update(current, 1), 0.001f)
    }

    @Test fun wrapAroundCombinedAxesAndInvalidSamples() {
        val c = FlightCalculator()
        c.update(uprightRotation(2, 170.0), 1)
        assertEquals(-0.5f, c.update(uprightRotation(2, -174.0), 1)!![0], 0.001f)
        c.reset()
        c.update(uprightRotation(2, 0.0), 1)
        val combined = multiply(rotation(0, 90.0), multiply(rotation(2, -16.0), rotation(1, 16.0)))
        val axes = c.update(combined, 1)!!
        assertEquals(0.5f, axes[0], 0.001f)
        assertEquals(0.5f, axes[1], 0.001f)
        assertNull(c.update(floatArrayOf(), 1))
        assertNull(c.update(floatArrayOf(Float.NaN, 0f, 0f, 1f), 1))
        assertNull(c.update(floatArrayOf(0f, 0f, 0f, 0f), 1))
        assertArrayEquals(axes, c.update(combined, 1), 0.001f)
    }

    @Test fun independentAxisInversionKeepsCalibrationAndLimits() {
        val c = FlightCalculator()
        c.update(uprightRotation(2, 0.0), 1)
        val combined = multiply(rotation(0, 90.0), multiply(rotation(2, -16.0), rotation(1, 16.0)))
        assertArrayEquals(floatArrayOf(0.5f, 0.5f), c.update(combined, 1), 0.001f)
        assertArrayEquals(floatArrayOf(-0.5f, 0.5f), c.update(combined, 1, invertX = true), 0.001f)
        assertArrayEquals(floatArrayOf(0.5f, -0.5f), c.update(combined, 1, invertY = true), 0.001f)
        assertArrayEquals(floatArrayOf(-0.5f, -0.5f), c.update(combined, 1, true, true), 0.001f)
        assertEquals(-1f, c.update(uprightRotation(1, 60.0), 1, true, true)!![1], 0.001f)
        c.calibrate()
        assertArrayEquals(floatArrayOf(0f, 0f), c.update(uprightRotation(1, 60.0), 1, true, true), 0.001f)
    }

    @Test fun bothHorizontalFacesBoundaryAndRecovery() {
        val c = FlightCalculator()
        for (angle in listOf(0.0, 14.9, 15.0, 165.0, 165.1, 180.0)) {
            assertArrayEquals(floatArrayOf(0f, 0f), c.update(rotation(0, angle), 1), 0.001f)
            assertTrue(c.motionSuspended)
        }
        assertNull(c.update(floatArrayOf(Float.NaN, 0f, 0f, 1f), 1))
        assertTrue(c.motionSuspended)
        assertArrayEquals(floatArrayOf(0f, 0f), c.update(rotation(0, 15.1), 1), 0.001f)
        assertFalse(c.motionSuspended)
        assertFalse(c.update(rotation(0, 164.9), 1) == null)
        assertFalse(c.motionSuspended)
        c.update(rotation(0, 180.0), 1)
        assertArrayEquals(floatArrayOf(0f, 0f), c.update(uprightRotation(2, -16.0), 1), 0.001f)
        assertEquals(0.5f, c.update(uprightRotation(2, -32.0), 1)!![0], 0.001f)
    }

    @Test fun gravityFallbackValidatesBothFacesAndBoundaries() {
        fun gravity(angle: Double) = floatArrayOf(0f, sin(Math.toRadians(angle)).toFloat(), cos(Math.toRadians(angle)).toFloat())
        for (angle in listOf(0.0, 14.9, 15.0, 165.0, 165.1, 180.0)) {
            assertEquals(true, FlightCalculator.isHorizontalAcceleration(gravity(angle)))
        }
        for (angle in listOf(15.1, 90.0, 164.9)) {
            assertEquals(false, FlightCalculator.isHorizontalAcceleration(gravity(angle)))
        }
        assertNull(FlightCalculator.isHorizontalAcceleration(floatArrayOf()))
        assertNull(FlightCalculator.isHorizontalAcceleration(floatArrayOf(0f, 0f, 0f)))
        assertNull(FlightCalculator.isHorizontalAcceleration(floatArrayOf(0f, Float.NaN, 1f)))
    }

    @Test fun axisScaleAppliesAfterDeadzoneBeforeClampingAndCombinesWithInversion() {
        val c = FlightCalculator()
        c.update(uprightRotation(2, 0.0), 1)
        val combined = multiply(rotation(0, 90.0), multiply(rotation(2, -16.0), rotation(1, 16.0)))
        assertArrayEquals(floatArrayOf(0.5f, 0.5f), c.update(combined, 1), 0.001f)
        assertArrayEquals(floatArrayOf(-0.25f, 1f), c.update(combined, 1, invertX = true, scaleX = 0.5f, scaleY = 2f), 0.001f)
        assertEquals(0.5f, c.update(uprightRotation(1, 30.0), 1, scaleY = 0.5f)!![1], 0.001f)
        assertEquals(1f, c.update(uprightRotation(1, 60.0), 1, scaleY = 0.5f)!![1], 0.001f)
        assertEquals(-1f, c.update(uprightRotation(1, -60.0), 1, scaleY = 2f)!![1], 0.001f)
        assertEquals(0f, c.update(uprightRotation(1, 1.9), 1, scaleY = 2f)!![1], 0f)
        for (invalid in listOf(Float.NaN, Float.POSITIVE_INFINITY, 0f, -1f)) {
            assertNull(c.update(combined, 1, scaleX = invalid))
            assertNull(c.update(combined, 1, scaleY = invalid))
        }
    }
}
