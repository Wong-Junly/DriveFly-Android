package com.devboi.drivelink

import com.devboi.drivelink.data.models.InputData
import com.devboi.drivelink.network.InputProtocol
import org.junit.Assert.*
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class InputProtocolTest {
    @Test fun racingAndFlightPacketsMatchDesktopOffsets() {
        val b = ByteBuffer.allocate(InputProtocol.MAX_PACKET_SIZE)
        val race = InputData(-0.5f, 0.75f, 0.25f, 1f, 0x81, 123456789L)
        assertEquals(29, InputProtocol.encode(race, b))
        b.flip()
        assertEquals(1, b.get().toInt())
        assertEquals(-0.5f, b.float, 0f)
        assertEquals(0.75f, b.float, 0f)
        assertEquals(0.25f, b.float, 0f)
        assertEquals(1f, b.float, 0f)
        assertEquals(0x81, b.int)
        assertEquals(123456789L, b.long)
        val flight = race.copy(flightMode = true, roll = -1f, pitch = 0.5f, yaw = 1f, flaps = 0.25f)
        assertEquals(33, InputProtocol.encode(flight, b))
        assertEquals(ByteOrder.LITTLE_ENDIAN, b.order())
        assertEquals(-1f, b.getFloat(1), 0f)
        assertEquals(0.5f, b.getFloat(5), 0f)
        assertEquals(1f, b.getFloat(9), 0f)
        assertEquals(0.75f, b.getFloat(13), 0f)
        assertEquals(0.25f, b.getFloat(17), 0f)
        assertEquals(0x81, b.getInt(21))
        assertEquals(123456789L, b.getLong(25))
        assertEquals(0, b.array()[1].toInt())
        assertEquals(0x80, b.array()[3].toInt() and 0xff)
        assertEquals(0xbf, b.array()[4].toInt() and 0xff)
        val bytes = b.array().copyOf()
        assertEquals(33, InputProtocol.encode(flight.copy(motionTimestamp = Long.MAX_VALUE), b))
        assertArrayEquals(bytes, b.array())
    }

    @Test fun invalidValuesNeverReachNetwork() {
        val base = InputData(0f, 0f, 0f, 0f, 0, 1L)
        for (input in listOf(base.copy(steering=Float.NaN), base.copy(throttle=2f),
            base.copy(flightMode=true, pitch=Float.POSITIVE_INFINITY), base.copy(timestamp=-1L))) {
            try {
                InputProtocol.encode(input, ByteBuffer.allocate(33))
                fail("Expected invalid input rejection")
            } catch (_: IllegalArgumentException) { }
        }
    }
}
