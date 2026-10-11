package com.devboi.drivelink.data

import android.content.Context
import com.devboi.drivelink.data.models.ConnectionMode
import com.devboi.drivelink.data.models.SettingsData
import com.google.gson.Gson
import java.io.File

class SettingsRepository(private val file: File) {
    constructor(context: Context) : this(File(context.filesDir, "settings.json"))

    private val gson = Gson()

    private val settings: SettingsData = load()

    private fun load(): SettingsData {
        if (!file.exists()) return SettingsData()

        return try {
            gson.fromJson(file.readText(), SettingsData::class.java) ?: SettingsData()
        } catch (_: Exception) {
            SettingsData()
        }
    }

    fun save() {
        file.writeText(gson.toJson(settings))
    }

    fun getConnectionMode(): ConnectionMode = ConnectionMode.fromStorageKey(settings.connectionMode)

    fun setConnectionMode(mode: ConnectionMode) {
        settings.connectionMode = mode.storageKey
        save()
    }

    // Keep the original field as the Wi-Fi profile so upgrading preserves its address.
    fun getServerAddr(mode: ConnectionMode = ConnectionMode.WIFI): String = when (mode) {
        ConnectionMode.WIFI -> settings.serverAddr
        ConnectionMode.USB -> settings.usbServerAddr
        ConnectionMode.BLUETOOTH -> settings.bluetoothServerAddr
    }.orEmpty()

    fun setServerAddr(addr: String, mode: ConnectionMode = ConnectionMode.WIFI) {
        when (mode) {
            ConnectionMode.WIFI -> settings.serverAddr = addr.trim()
            ConnectionMode.USB -> settings.usbServerAddr = addr.trim()
            ConnectionMode.BLUETOOTH -> settings.bluetoothServerAddr = addr.trim()
        }
        save()
    }

    fun getSteeringSlope1(): Float {
        return settings.steeringSlope1
    }
    fun getSteeringSlope2(): Float {
        return settings.steeringSlope2
    }
    fun setSteeringSlope1(newVal: Float) {
        settings.steeringSlope1 = newVal
    }
    fun setSteeringSlope2(newVal: Float) {
        settings.steeringSlope2 = newVal
    }

    fun getHapticEnabled(): Boolean {
        return settings.hapticFeedback
    }
    fun setHapticEnabled(newVal: Boolean) {
        settings.hapticFeedback = newVal
    }

    fun getMaxSteeringAngle(): Float {
        return settings.maxSteeringAngle
    }
    fun setMaxSteeringAngle(newVal: Float) {
        settings.maxSteeringAngle = newVal
    }
    
    fun getAltLayoutSelected(): Boolean {
        return settings.altLayoutSelected
    }
    fun setAltLayoutSelected(layoutSelected: Boolean) {
        settings.altLayoutSelected = layoutSelected
        save()
    }

    fun getFlightInvertX(): Boolean = settings.flightInvertX

    fun setFlightInvertX(inverted: Boolean) {
        settings.flightInvertX = inverted
        save()
    }

    fun getFlightInvertY(): Boolean = settings.flightInvertY

    fun setFlightInvertY(inverted: Boolean) {
        settings.flightInvertY = inverted
        save()
    }

    fun getFlightScaleX(): Float = settings.flightScaleX.takeIf { it.isFinite() && it > 0f }?.coerceIn(0.25f, 2f) ?: 1f

    fun setFlightScaleX(scale: Float) {
        if (!scale.isFinite() || scale <= 0f) return
        settings.flightScaleX = scale.coerceIn(0.25f, 2f)
        save()
    }

    fun getFlightScaleY(): Float = settings.flightScaleY.takeIf { it.isFinite() && it > 0f }?.coerceIn(0.25f, 2f) ?: 1f

    fun setFlightScaleY(scale: Float) {
        if (!scale.isFinite() || scale <= 0f) return
        settings.flightScaleY = scale.coerceIn(0.25f, 2f)
        save()
    }

    fun getPedalsDirection(): Boolean {
        return settings.pedalsAltDirection
    }
    fun setPedalsAltDirection(layoutSelected: Boolean) {
        settings.pedalsAltDirection = layoutSelected
    }

    fun resetSettings() {
        val defaults = SettingsData()

        settings.altLayoutSelected = defaults.altLayoutSelected
        settings.serverAddr = defaults.serverAddr
        settings.usbServerAddr = defaults.usbServerAddr
        settings.bluetoothServerAddr = defaults.bluetoothServerAddr
        settings.connectionMode = defaults.connectionMode
        settings.steeringSlope1 = defaults.steeringSlope1
        settings.steeringSlope2 = defaults.steeringSlope2
        settings.maxSteeringAngle = defaults.maxSteeringAngle
        settings.hapticFeedback = defaults.hapticFeedback
        settings.pedalsAltDirection = defaults.pedalsAltDirection
        settings.flightInvertX = defaults.flightInvertX
        settings.flightInvertY = defaults.flightInvertY
        settings.flightScaleX = defaults.flightScaleX
        settings.flightScaleY = defaults.flightScaleY
        save()
    }
}
