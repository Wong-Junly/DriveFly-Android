package com.devboi.drivelink

import com.devboi.drivelink.data.InputRepository
import com.devboi.drivelink.network.InputSender
import com.devboi.drivelink.network.InputProtocol
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

class InputSenderTest {
    @Test fun acknowledgementsTimeoutAndStopCannotLeaveConnectedState() = runBlocking {
        val receiver = DatagramSocket(7001, InetAddress.getByName("127.0.0.1"))
        receiver.soTimeout = 30
        val running = AtomicBoolean(true)
        val acknowledge = AtomicBoolean(true)
        val worker = thread(isDaemon = true) {
            val packet = DatagramPacket(ByteArray(100), 100)
            while (running.get()) {
                try {
                    receiver.receive(packet)
                    if (acknowledge.get()) receiver.send(DatagramPacket(byteArrayOf(InputProtocol.ACK), 1, packet.address, packet.port))
                    packet.length = 100
                } catch (_: SocketTimeoutException) { }
            }
        }
        val sender = InputSender(InputRepository())
        try {
            sender.setAddress("127.0.0.1")
            sender.start()
            withTimeout(2000) { sender.isConnected.first { it } }
            acknowledge.set(false)
            withTimeout(2000) { sender.isConnected.first { !it } }
            acknowledge.set(true)
            withTimeout(2000) { sender.isConnected.first { it } }
            repeat(20) {
                sender.stop()
                delay(10)
                assertFalse(sender.isConnected.value)
                sender.start()
                withTimeout(2000) { sender.isConnected.first { it } }
            }
            sender.stop()
            delay(50)
            assertFalse(sender.isConnected.value)
        } finally {
            sender.stop()
            running.set(false)
            worker.join(1000)
            receiver.close()
        }
    }
}
