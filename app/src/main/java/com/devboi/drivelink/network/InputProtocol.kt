package com.devboi.drivelink.network

import com.devboi.drivelink.data.models.InputData
import java.nio.ByteBuffer
import java.nio.ByteOrder

object InputProtocol {
    const val ACK: Byte = 0x7e
    const val MAX_PACKET_SIZE = 33

    fun encode(input: InputData, buffer: ByteBuffer): Int {
        buffer.clear()
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        val axes = if (input.flightMode) {
            floatArrayOf(input.roll, input.pitch, input.yaw, input.throttle, input.flaps)
        } else {
            floatArrayOf(input.steering, input.throttle, input.brake, input.clutch)
        }
        axes.forEachIndexed { index, value ->
            val signed = if (input.flightMode) index < 3 else index == 0
            require(value.isFinite() && value in (if (signed) -1f else 0f)..1f)
        }
        require(input.timestamp >= 0)
        buffer.put(if (input.flightMode) 2.toByte() else 1.toByte())
        axes.forEach { buffer.putFloat(it) }
        buffer.putInt(input.buttons)
        buffer.putLong(input.timestamp)
        return buffer.position()
    }
}
