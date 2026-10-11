package com.devboi.drivelink.data.models

data class InputData(
    val steering: Float,
    val throttle: Float,
    val brake: Float,
    val clutch: Float,
    val buttons: Int,
    val timestamp: Long,
    val flightMode: Boolean = false,
    val roll: Float = 0f,
    val pitch: Float = 0f,
    val yaw: Float = 0f,
    val flaps: Float = 0f,
    val motionTimestamp: Long = 0L
)
