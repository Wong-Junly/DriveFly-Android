package com.devboi.drivelink.utils

fun Float.nearlyEqual(other: Float, epsilon: Float = 0.001f): Boolean =
    (kotlin.math.abs(this - other) < epsilon)

enum class CommonAngles(val radians: Float) {
    DEG_60(Math.PI.toFloat() / 3),
    DEG_90(Math.PI.toFloat() / 2),
    DEG_120(2f * Math.PI.toFloat() / 3),
    DEG_135(3f * Math.PI.toFloat() / 4f),
    DEG_180(Math.PI.toFloat()),
}