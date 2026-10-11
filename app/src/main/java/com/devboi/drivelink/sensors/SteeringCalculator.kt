package com.devboi.drivelink.sensors

import com.devboi.drivelink.data.SettingsRepository
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.atan2
import kotlin.math.pow
import kotlin.math.sign

// Math to calculate the angle from the sensors
class SteeringCalculator (
    private val settingsRepo: SettingsRepository
) {

    // Calculate the weights for the gyro and the accelerometer dynamically
    private var alphaMin = 0.92f
    private var alphaMax = 0.97f
    private var speedScale = 3.0f

    private var alpha = 0.93f
    private var zeroOffset = 0f
    private var accelValues = FloatArray(3)
    private var lastGyroTimestamp = 0L
    private var steering = 0f           // In range [-1, 1]
    private var accelerometerAngle = 0f
    private var hasAcceleration = false


    fun onAccelerometerChange(values: FloatArray, timestamp: Long) {
        if (values.size < 3 || values.take(3).any { !it.isFinite() }) return
        accelValues[0] = values[0]
        accelValues[1] = values[1]
        accelValues[2] = values[2]
        hasAcceleration = true
    }

    fun onGyroscopeChange(gyroValues: FloatArray, timestamp: Long): Float {
        if (!hasAcceleration || gyroValues.size < 3 || !gyroValues[2].isFinite()) return 0f

        if (lastGyroTimestamp != 0L) {  // Do not integrate on the first sample to prevent initial spike
            val dt = ((timestamp - lastGyroTimestamp) / 1_000_000_000f).coerceIn(0f, 0.1f)
            steering += gyroValues[2] * dt

            accelerometerAngle = normalisePi(atan2(accelValues[0], accelValues[1]) - zeroOffset)

            // Filter
            val rotationSpeed = abs(gyroValues[2])

            alpha = alphaMin + (alphaMax - alphaMin) * (rotationSpeed / speedScale).coerceIn(0f, 1f)

            steering = alpha * steering + (1f - alpha) * accelerometerAngle
        } else {
            calibrate() // Calibrate on startup
        }

        lastGyroTimestamp = timestamp

        // Normalise steering to a range of [-1,1]
        return calculateSteering((steering / settingsRepo.getMaxSteeringAngle()).coerceIn(-1f, 1f))
    }

    fun calibrate() {
        zeroOffset = atan2(accelValues[0], accelValues[1])
        steering = 0f
        lastGyroTimestamp = 0L
    }

    // Takes in a range [-1, 1] and outputs in a range [-1, 1]
    // based on the steering curve equation
    // Check out the math here: https://www.desmos.com/calculator/wjd0jlt5ac
    private fun calculateSteering(x: Float): Float {
        val slope1 = settingsRepo.getSteeringSlope1()
        val slope2 = settingsRepo.getSteeringSlope2()

        val absX = x.absoluteValue
        val a = slope1 + slope2 - 2f
        val b = 3f - 2f*slope1 - slope2
        return x.sign * (a * absX.pow(3) + b * absX.pow(2) + slope1 * absX)
    }

    // Helper function to normalise an angle in radians to be within the range of -π to +π.
    private fun normalisePi(a: Float): Float {
        var x = a
        if (x > Math.PI)  x -= (2 * Math.PI).toFloat()
        if (x < -Math.PI) x += (2 * Math.PI).toFloat()
        return x
    }
}
