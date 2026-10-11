package com.devboi.drivelink.data.models

enum class ConnectionMode(val storageKey: String, val displayName: String) {
    WIFI("wifi", "Wi-Fi"),
    USB("usb", "USB 网络"),
    BLUETOOTH("bluetooth", "蓝牙网络");

    companion object {
        fun fromStorageKey(key: String?): ConnectionMode =
            entries.firstOrNull { it.storageKey == key } ?: WIFI
    }
}
