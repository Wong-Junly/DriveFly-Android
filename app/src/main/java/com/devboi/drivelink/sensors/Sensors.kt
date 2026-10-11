package com.devboi.drivelink.sensors

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor

class AccelerometerSensor(context: Context) :
    AndroidSensor(context, PackageManager.FEATURE_SENSOR_ACCELEROMETER, Sensor.TYPE_ACCELEROMETER)

class GyroscopeSensor(context: Context) :
    AndroidSensor(context, PackageManager.FEATURE_SENSOR_GYROSCOPE, Sensor.TYPE_GYROSCOPE)

class RotationSensor(context: Context) :
    AndroidSensor(context, PackageManager.FEATURE_SENSOR_GYROSCOPE, Sensor.TYPE_GAME_ROTATION_VECTOR)
