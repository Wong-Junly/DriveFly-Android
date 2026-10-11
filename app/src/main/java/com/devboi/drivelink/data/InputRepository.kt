package com.devboi.drivelink.data

import com.devboi.drivelink.data.models.InputData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow


class InputRepository(private val motionClock: () -> Long = System::nanoTime) {
    private val _inputData = MutableStateFlow(
        InputData(0f, 0f, 0f, 0f, 0, 0L)
    )
    val inputData = _inputData.asStateFlow()

    fun updateInputs(input: InputData) {
        _inputData.value = input
    }

    fun getInputs(): InputData {
        val input = inputData.value
        val age = motionClock() - input.motionTimestamp
        return if (input.motionTimestamp <= 0L || age < 0L || age >= 500_000_000L)
            input.copy(steering = 0f, roll = 0f, pitch = 0f) else input
    }
}
