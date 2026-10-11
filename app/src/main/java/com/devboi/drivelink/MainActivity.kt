package com.devboi.drivelink

import com.devboi.drivelink.data.SettingsRepository
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.devboi.drivelink.data.InputRepository
import com.devboi.drivelink.network.InputSender
import com.devboi.drivelink.sensors.AccelerometerSensor
import com.devboi.drivelink.sensors.GyroscopeSensor
import com.devboi.drivelink.sensors.MeasurableSensor
import com.devboi.drivelink.sensors.RotationSensor
import android.view.WindowManager
import com.devboi.drivelink.ui.App
import com.devboi.drivelink.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private lateinit var accelerometerSensor: MeasurableSensor
    private lateinit var gyroscopeSensor: MeasurableSensor
    private lateinit var rotationSensor: MeasurableSensor

    private lateinit var vm: MainViewModel
    private lateinit var sender: InputSender


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val inputRepo = InputRepository(SystemClock::elapsedRealtimeNanos)
        val settingsRepo = SettingsRepository(applicationContext)

        sender = InputSender(inputRepo)
        vm = MainViewModel(settingsRepo, inputRepo, sender, SystemClock::elapsedRealtimeNanos)

        sender.start()

        accelerometerSensor = AccelerometerSensor(applicationContext)
        gyroscopeSensor = GyroscopeSensor(applicationContext)
        rotationSensor = RotationSensor(applicationContext)

        accelerometerSensor.setOnSensorValueChangedListener { values, timestamp ->
            vm.onAccelerometerChange(values, timestamp)
        }
        gyroscopeSensor.setOnSensorValueChangedListener { values, timestamp ->
            vm.onGyroscopeChange(values, timestamp)
        }
        rotationSensor.setOnSensorValueChangedListener { values, timestamp ->
            @Suppress("DEPRECATION")
            val rotation = (getSystemService(WINDOW_SERVICE) as WindowManager).defaultDisplay.rotation
            vm.onRotationChange(values, rotation, timestamp)
        }

        enableEdgeToEdge()
        setContent {
            App(vm)
        }
    }

    override fun onResume() {
        super.onResume()
        accelerometerSensor.startListening()
        gyroscopeSensor.startListening()
        rotationSensor.startListening()
        vm.resumeInputs()
    }

    override fun onStop() {
        super.onStop()
        accelerometerSensor.stopListening()
        gyroscopeSensor.stopListening()
        rotationSensor.stopListening()
        vm.pauseInputs()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (::vm.isInitialized) {
            if (hasFocus) vm.resumeInputs() else vm.pauseInputs()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        sender.stop()
    }
}

@Preview(showBackground = true,
    device = "spec:orientation=landscape,width=411dp,height=891dp"
)
@Composable
fun AppPreview() {
    App(
        MainViewModel(
            SettingsRepository(LocalContext.current), InputRepository(), InputSender(InputRepository())
        )
    )
}
