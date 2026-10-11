package com.devboi.drivelink.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

abstract class AndroidSensor(
    private val context: Context,
    private val sensorFeature: String,
    sensorType: Int
): MeasurableSensor(sensorType), SensorEventListener {

    override val doesSensorExist: Boolean
        get() = context.packageManager.hasSystemFeature(sensorFeature) &&
            context.getSystemService(SensorManager::class.java).getDefaultSensor(sensorType) != null

    private lateinit var sensorManager: SensorManager
    private var sensor: Sensor? = null

    override fun startListening() {
        if(!doesSensorExist) {
            return
        }

        if(!::sensorManager.isInitialized && sensor == null) {
            sensorManager = context.getSystemService(SensorManager::class.java) as SensorManager
            sensor = sensorManager.getDefaultSensor(sensorType)
        }

        sensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    override fun stopListening() {
        if(!doesSensorExist || !::sensorManager.isInitialized) {
            return
        }

        sensor?.let {
            sensorManager.unregisterListener(this)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (!doesSensorExist) {
            return
        }
        if (event?.sensor?.type == sensorType) {
            onSensorValueChanged?.invoke(event.values, event.timestamp)
        }
    }

    // Do nothing, we don't need it for the sensors we use in the app
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
