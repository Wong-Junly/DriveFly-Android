package com.devboi.drivelink.sensors

abstract class MeasurableSensor(
    protected val sensorType: Int
) {

    protected var onSensorValueChanged: ((FloatArray, Long) -> Unit)? = null

    abstract val doesSensorExist: Boolean
    abstract fun startListening()
    abstract fun stopListening()

    fun setOnSensorValueChangedListener(listener: (FloatArray, Long) -> Unit) {
        onSensorValueChanged = listener
    }
}