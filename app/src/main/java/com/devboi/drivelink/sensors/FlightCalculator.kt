package com.devboi.drivelink.sensors

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sign
import kotlin.math.sqrt

// Quaternion components follow Android's rotation vector: x, y, z, w.
class FlightCalculator {
    private var current: FloatArray? = null
    private var home: FloatArray? = null
    var motionSuspended: Boolean = false
        private set

    fun calibrate() {
        if (motionSuspended) return
        home = current?.copyOf()
    }

    fun reset() {
        current = null
        home = null
        motionSuspended = false
    }

    fun update(
        vector: FloatArray,
        screenRotation: Int,
        invertX: Boolean = false,
        invertY: Boolean = false,
        scaleX: Float = 1f,
        scaleY: Float = 1f
    ): FloatArray? {
        if (!scaleX.isFinite() || scaleX <= 0f || !scaleY.isFinite() || scaleY <= 0f) return null
        if (vector.size < 3 || vector.take(4).any { !it.isFinite() }) return null
        val w = if (vector.size >= 4) vector[3] else
            sqrt((1f - vector[0] * vector[0] - vector[1] * vector[1] - vector[2] * vector[2]).coerceAtLeast(0f))
        val norm = sqrt(vector[0] * vector[0] + vector[1] * vector[1] + vector[2] * vector[2] + w * w)
        if (!norm.isFinite() || norm < 0.001f) return null
        val q = floatArrayOf(vector[0] / norm, vector[1] / norm, vector[2] / norm, w / norm)
        current = q
        motionSuspended = abs(1f - 2f * (q[0] * q[0] + q[1] * q[1])) >= horizontalLimit
        if (motionSuspended) {
            home = null
            return floatArrayOf(0f, 0f)
        }
        if (home == null) home = q.copyOf()
        val h = home!!
        // Calibrated device frame: inverse(home) * current, not the world's heading.
        val x = h[3] * q[0] - h[0] * q[3] - h[1] * q[2] + h[2] * q[1]
        val y = h[3] * q[1] + h[0] * q[2] - h[1] * q[3] - h[2] * q[0]
        val z = h[3] * q[2] - h[0] * q[1] + h[1] * q[0] - h[2] * q[3]
        val relativeW = h[3] * q[3] + h[0] * q[0] + h[1] * q[1] + h[2] * q[2]
        val (screenX, screenY) = when (screenRotation) {
            1 -> y to -x
            2 -> -x to -y
            3 -> -y to x
            else -> x to y
        }
        val roll = -atan2(2f * (relativeW * z + screenX * screenY), 1f - 2f * (screenY * screenY + z * z))
        val pitch = atan2(2f * (relativeW * screenX + screenY * z), 1f - 2f * (screenX * screenX + screenY * screenY))
        return floatArrayOf(
            normalize(roll, scaleX) * if (invertX) -1f else 1f,
            normalize(pitch, scaleY) * if (invertY) -1f else 1f
        )
    }

    private fun normalize(radians: Float, scale: Float): Float {
        val degrees = Math.toDegrees(radians.toDouble()).toFloat()
        return if (abs(degrees) <= 2f) 0f else degrees.sign * ((abs(degrees) - 2f) / 28f * scale).coerceIn(0f, 1f)
    }

    companion object {
        private val horizontalLimit = cos(Math.toRadians(15.0)).toFloat()

        fun isHorizontalAcceleration(values: FloatArray): Boolean? {
            if (values.size < 3 || values.take(3).any { !it.isFinite() }) return null
            val norm = sqrt(values[0] * values[0] + values[1] * values[1] + values[2] * values[2])
            if (!norm.isFinite() || norm < 0.001f) return null
            return abs(values[2]) / norm >= horizontalLimit
        }
    }
}
