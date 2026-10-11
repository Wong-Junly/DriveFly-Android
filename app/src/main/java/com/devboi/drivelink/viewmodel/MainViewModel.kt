package com.devboi.drivelink.viewmodel

import com.devboi.drivelink.data.SettingsRepository
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.devboi.drivelink.data.InputRepository
import com.devboi.drivelink.network.InputSender
import com.devboi.drivelink.sensors.SteeringCalculator
import com.devboi.drivelink.sensors.FlightCalculator
import com.devboi.drivelink.data.models.InputData
import com.devboi.drivelink.data.models.ConnectionMode
import com.devboi.drivelink.ui.screens.Screens
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow


class MainViewModel(
    private val settingsRepo: SettingsRepository,
    private val inputRepository: InputRepository,
    private val inputSender: InputSender,
    private val motionClock: () -> Long = System::nanoTime
): ViewModel() {

    // Input state
    private var steeringCalculator = SteeringCalculator(settingsRepo)
    private var steering: Float = 0f
    private var throttle: Float = 0f
    private var brake   : Float = 0f
    private var clutch  : Float = 0f
    private var buttons : Int = 0
    private val flightCalculator = FlightCalculator()
    private var usingRotation = false
    private var rotationTimestamp = 0L
    private var accelerationTimestamp = 0L
    private var gyroTimestamp = 0L
    private var motionTimestamp = 0L
    val flightMode = mutableStateOf(false)
    val flightThrottle = mutableStateOf(0f)
    val flightFlaps = mutableStateOf(0f)
    val flightRoll = mutableStateOf(0f)
    val flightPitch = mutableStateOf(0f)
    val flightReady = mutableStateOf(false)
    val motionSuspended = mutableStateOf(false)
    val flightInvertX = mutableStateOf(settingsRepo.getFlightInvertX())
    val flightInvertY = mutableStateOf(settingsRepo.getFlightInvertY())
    val flightScaleX = mutableStateOf(settingsRepo.getFlightScaleX())
    val flightScaleY = mutableStateOf(settingsRepo.getFlightScaleY())
    val resetEpoch = mutableStateOf(0)

    fun changeFlightMode(enabled: Boolean) {
        if (flightMode.value == enabled) return
        flightMode.value = enabled
        resetInputs()
        calibrateSteering()
        calibrateFlight()
    }

    fun onCollectiveChange(up: Boolean, down: Boolean) {
        flightThrottle.value = if (up && !down) 1f else 0f
        flightFlaps.value = if (down && !up) 1f else 0f
        updateInputSnapshot()
    }

    fun onRotationChange(values: FloatArray, screenRotation: Int, timestamp: Long = motionClock()) {
        val now = motionClock()
        if (!isFresh(timestamp, now) || timestamp <= rotationTimestamp) return
        if (!usingRotation || !isFresh(rotationTimestamp, now)) flightCalculator.reset()
        val axes = flightCalculator.update(
            values, screenRotation, flightInvertX.value, flightInvertY.value,
            flightScaleX.value, flightScaleY.value
        ) ?: return
        rotationTimestamp = timestamp
        usingRotation = true
        flightReady.value = true
        updateMotionSuspended(flightCalculator.motionSuspended)
        if (flightMode.value) {
            flightRoll.value = axes[0]
            flightPitch.value = axes[1]
            motionTimestamp = timestamp
        }
        updateInputSnapshot()
    }

    private fun updateMotionSuspended(suspended: Boolean) {
        if (motionSuspended.value == suspended) return
        motionSuspended.value = suspended
        steeringCalculator.calibrate()
        steering = 0f
        flightRoll.value = 0f
        flightPitch.value = 0f
        updateInputSnapshot()
    }

    private fun isFresh(timestamp: Long, now: Long): Boolean =
        timestamp > 0L && now >= timestamp && now - timestamp < 500_000_000L

    fun calibrateFlight() {
        if (motionSuspended.value) return
        flightCalculator.calibrate()
        flightRoll.value = 0f
        flightPitch.value = 0f
        motionTimestamp = 0L
        updateInputSnapshot()
    }

    fun resetInputs() {
        resetEpoch.value += 1
        steering = 0f
        throttle = 0f
        brake = 0f
        clutch = 0f
        buttons = 0
        flightThrottle.value = 0f
        flightFlaps.value = 0f
        flightRoll.value = 0f
        flightPitch.value = 0f
        motionTimestamp = 0L
        updateInputSnapshot()
    }

    fun pauseInputs() {
        resetInputs()
        flightReady.value = false
        usingRotation = false
        flightCalculator.reset()
        inputSender.stop()
    }

    fun resumeInputs() {
        resetInputs()
        calibrateSteering()
        calibrateFlight()
        if (currentScreen.value == Screens.Home) inputSender.start()
    }

    fun onThrottleChange(value: Float) {
        if (!value.isFinite()) return
        throttle = value.coerceIn(0f, 1f)
        updateInputSnapshot()
    }
    fun onBrakeChange(value: Float) {
        if (!value.isFinite()) return
        brake = value.coerceIn(0f, 1f)
        updateInputSnapshot()
    }
    fun onClutchChange(value: Float) {
        if (!value.isFinite()) return
        clutch = value.coerceIn(0f, 1f)
        updateInputSnapshot()
    }
    fun onSteeringChange(value: Float, timestamp: Long = motionClock()) {
        if (!value.isFinite() || flightMode.value || motionSuspended.value) return
        if (!isFresh(timestamp, motionClock())) return
        steering = value.coerceIn(-1f, 1f)
        motionTimestamp = timestamp
        updateInputSnapshot()
    }

    fun onButtonPress(buttonIdx: Int) {
        if (buttonIdx !in 0..7) return
        buttons = buttons or (1 shl buttonIdx)
        updateInputSnapshot()
    }

    fun onButtonRelease(buttonIdx: Int) {
        if (buttonIdx !in 0..7) return
        buttons = buttons and (1 shl buttonIdx).inv()
        updateInputSnapshot()
    }

    fun onAccelerometerChange(accelValues: FloatArray, timestamp: Long) {
        val now = motionClock()
        if (!isFresh(timestamp, now) || timestamp <= accelerationTimestamp) return
        val suspended = FlightCalculator.isHorizontalAcceleration(accelValues) ?: return
        accelerationTimestamp = timestamp
        steeringCalculator.onAccelerometerChange(accelValues, timestamp)
        if (!usingRotation || !isFresh(rotationTimestamp, now)) {
            if (usingRotation) flightCalculator.reset()
            usingRotation = false
            updateMotionSuspended(suspended)
        }
    }
    fun onGyroscopeChange(gyroValues: FloatArray, timestamp: Long) {
        val now = motionClock()
        if (!isFresh(timestamp, now) || timestamp <= gyroTimestamp) return
        if (gyroValues.size < 3 || gyroValues.take(3).any { !it.isFinite() }) return
        gyroTimestamp = timestamp
        if (motionSuspended.value || flightMode.value || !isFresh(accelerationTimestamp, now)) return
        if (!isFresh(motionTimestamp, now)) steeringCalculator.calibrate()
        val newAngle = steeringCalculator.onGyroscopeChange(gyroValues, timestamp)
        onSteeringChange(newAngle, timestamp)
    }

    private fun updateInputSnapshot() {
        inputRepository.updateInputs(InputData(
            steering, if (flightMode.value) flightThrottle.value else throttle,
            brake, clutch, buttons, System.nanoTime() / 1_000_000,
            flightMode.value, flightRoll.value, flightPitch.value,
            0f, flightFlaps.value, motionTimestamp
        ))
    }

    // Settings
    var altLayout = mutableStateOf(settingsRepo.getAltLayoutSelected())
        private set
    var connectionMode = mutableStateOf(settingsRepo.getConnectionMode())
        private set
    var serverAddr = mutableStateOf(settingsRepo.getServerAddr(connectionMode.value))
        private set
    var maxSteeringAngle = mutableStateOf(settingsRepo.getMaxSteeringAngle())
        private set
    var hapticsEnabled = mutableStateOf(settingsRepo.getHapticEnabled())
        private set
    var pedalsUpsideDown = mutableStateOf(settingsRepo.getPedalsDirection())
        private set

    fun changeServerAddr(addr: String) {
        settingsRepo.setServerAddr(addr, connectionMode.value)
        useSelectedConnection()
    }

    fun changeConnectionMode(mode: ConnectionMode) {
        if (connectionMode.value == mode) return
        connectionMode.value = mode
        settingsRepo.setConnectionMode(mode)
        useSelectedConnection()
    }

    private fun useSelectedConnection() {
        resetInputs()
        serverAddr.value = settingsRepo.getServerAddr(connectionMode.value)
        inputSender.setAddress(serverAddr.value)
        if (currentScreen.value == Screens.Home) inputSender.start()
    }
    fun changeIsAltLayoutSelected(isAltLayout: Boolean) {
        altLayout.value = isAltLayout
        settingsRepo.setAltLayoutSelected(isAltLayout)
    }
    fun changeMaxSteeringAngle(angle: Float) {
        maxSteeringAngle.value = angle
        settingsRepo.setMaxSteeringAngle(angle)
    }
    fun changeHapticsEnabled(newVal: Boolean) {
        hapticsEnabled.value = newVal
        settingsRepo.setHapticEnabled(newVal)
    }
    fun changeArePedalsUpsideDown(areUpsideDown: Boolean) {
        pedalsUpsideDown.value = areUpsideDown
        settingsRepo.setPedalsAltDirection(areUpsideDown)
    }
    fun changeFlightInvertX(inverted: Boolean) {
        if (flightInvertX.value != inverted) flightRoll.value = -flightRoll.value
        flightInvertX.value = inverted
        settingsRepo.setFlightInvertX(inverted)
        updateInputSnapshot()
    }
    fun changeFlightInvertY(inverted: Boolean) {
        if (flightInvertY.value != inverted) flightPitch.value = -flightPitch.value
        flightInvertY.value = inverted
        settingsRepo.setFlightInvertY(inverted)
        updateInputSnapshot()
    }
    fun changeFlightScaleX(scale: Float) {
        if (!scale.isFinite() || scale <= 0f) return
        flightScaleX.value = scale.coerceIn(0.25f, 2f)
        settingsRepo.setFlightScaleX(flightScaleX.value)
    }
    fun changeFlightScaleY(scale: Float) {
        if (!scale.isFinite() || scale <= 0f) return
        flightScaleY.value = scale.coerceIn(0.25f, 2f)
        settingsRepo.setFlightScaleY(flightScaleY.value)
    }
    fun saveSettings() {
        settingsRepo.save()
    }

    fun calibrateSteering() {
        if (motionSuspended.value) return
        steeringCalculator.calibrate()
        steering = 0f
        updateInputSnapshot()
    }

    private val _steeringSlope1 = MutableStateFlow(settingsRepo.getSteeringSlope1())
    val steeringCurveSlope1 = _steeringSlope1.asStateFlow()
    private val _steeringSlope2 = MutableStateFlow(settingsRepo.getSteeringSlope2())
    val steeringCurveSlope2 = _steeringSlope2.asStateFlow()

    fun onSlope1Change(value: Float) {
        _steeringSlope1.value = value
        settingsRepo.setSteeringSlope1(value)
    }
    fun onSlope2Change(value: Float) {
        _steeringSlope2.value = value
        settingsRepo.setSteeringSlope2(value)
    }

    fun resetSettingsToDefault() {
        settingsRepo.resetSettings()
        connectionMode.value = settingsRepo.getConnectionMode()
        useSelectedConnection()
        changeFlightInvertX(settingsRepo.getFlightInvertX())
        changeFlightInvertY(settingsRepo.getFlightInvertY())
        changeFlightScaleX(settingsRepo.getFlightScaleX())
        changeFlightScaleY(settingsRepo.getFlightScaleY())
    }

    // App stuff
    val isConnected = inputSender.isConnected
    val connectionError = inputSender.connectionError
    val currentScreen = mutableStateOf(Screens.Home)

    fun changeScreen(screen: Screens) {
        resetInputs()
        currentScreen.value = screen
        if (screen == Screens.Settings) inputSender.stop() else resumeInputs()
    }

    init {
        inputSender.setAddress(serverAddr.value)
    }
}
