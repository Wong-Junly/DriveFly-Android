package com.devboi.drivelink.data.models

import com.devboi.drivelink.utils.CommonAngles

data class SettingsData (
    var serverAddr: String? = "",
    var usbServerAddr: String? = "",
    var bluetoothServerAddr: String? = "",
    var connectionMode: String? = ConnectionMode.WIFI.storageKey,
    var altLayoutSelected: Boolean = false,
    var pedalsAltDirection: Boolean = false,
    var hapticFeedback: Boolean = true,
    var flightInvertX: Boolean = false,
    var flightInvertY: Boolean = false,
    var flightScaleX: Float = 1f,
    var flightScaleY: Float = 1f,
    var steeringSlope1: Float = 1f,
    var steeringSlope2: Float = 1f,
    var maxSteeringAngle: Float = CommonAngles.DEG_135.radians
)
