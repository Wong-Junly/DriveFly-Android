package com.devboi.drivelink

import com.devboi.drivelink.data.InputRepository
import com.devboi.drivelink.data.SettingsRepository
import com.devboi.drivelink.data.models.ConnectionMode
import com.devboi.drivelink.network.InputProtocol
import com.devboi.drivelink.network.InputSender
import com.devboi.drivelink.ui.screens.Screens
import com.devboi.drivelink.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

class ConnectionProfilesTest {
    @get:Rule val files = TemporaryFolder()

    @Test fun legacyWifiAddressAndIndependentProfilesSurviveReload() {
        val file = File(files.root, "settings.json").apply {
            writeText("""{"serverAddr":"192.168.1.8","altLayoutSelected":true}""")
        }
        val settings = SettingsRepository(file)
        assertEquals(ConnectionMode.WIFI, settings.getConnectionMode())
        assertEquals("192.168.1.8", settings.getServerAddr())
        assertEquals("", settings.getServerAddr(ConnectionMode.USB))
        assertEquals("", settings.getServerAddr(ConnectionMode.BLUETOOTH))
        settings.setServerAddr(" 192.168.42.20 ", ConnectionMode.USB)
        settings.setServerAddr("192.168.44.30", ConnectionMode.BLUETOOTH)
        settings.setConnectionMode(ConnectionMode.BLUETOOTH)
        val restored = SettingsRepository(file)
        assertEquals(ConnectionMode.BLUETOOTH, restored.getConnectionMode())
        assertEquals("192.168.1.8", restored.getServerAddr(ConnectionMode.WIFI))
        assertEquals("192.168.42.20", restored.getServerAddr(ConnectionMode.USB))
        assertEquals("192.168.44.30", restored.getServerAddr(ConnectionMode.BLUETOOTH))
        assertTrue(restored.getAltLayoutSelected())
    }

    @Test fun unknownOrNullModeSafelyUsesWifi() {
        val file = File(files.root, "settings.json")
        for (mode in listOf("\"future-transport\"", "null", "\"\"")) {
            file.writeText("""{"connectionMode":$mode,"serverAddr":"192.168.1.8","usbServerAddr":null,"bluetoothServerAddr":null}""")
            val settings = SettingsRepository(file)
            assertEquals(ConnectionMode.WIFI, settings.getConnectionMode())
            assertEquals("192.168.1.8", settings.getServerAddr())
            assertEquals("", settings.getServerAddr(ConnectionMode.USB))
            assertEquals("", settings.getServerAddr(ConnectionMode.BLUETOOTH))
        }
        file.writeText("""{"serverAddr":null}""")
        assertEquals("", SettingsRepository(file).getServerAddr())
    }

    @Test fun switchingProfilesClearsInputsAndRestoresOnlySelectedAddress() {
        val file = File(files.root, "settings.json")
        val settings = SettingsRepository(file)
        settings.setServerAddr("192.168.1.8")
        settings.setServerAddr("192.168.42.20", ConnectionMode.USB)
        settings.setServerAddr("192.168.44.30", ConnectionMode.BLUETOOTH)
        val inputs = InputRepository()
        val sender = InputSender(inputs)
        val vm = MainViewModel(settings, inputs, sender)
        try {
            vm.changeScreen(Screens.Settings)
            vm.onSteeringChange(0.5f)
            vm.onThrottleChange(1f)
            vm.onBrakeChange(1f)
            vm.onClutchChange(1f)
            vm.onButtonPress(0)
            val epoch = vm.resetEpoch.value
            vm.changeConnectionMode(ConnectionMode.USB)
            assertTrue(vm.resetEpoch.value > epoch)
            assertEquals("192.168.42.20", vm.serverAddr.value)
            assertEquals(0f, inputs.getInputs().steering, 0f)
            assertEquals(0f, inputs.getInputs().throttle, 0f)
            assertEquals(0f, inputs.getInputs().brake, 0f)
            assertEquals(0f, inputs.getInputs().clutch, 0f)
            assertEquals(0, inputs.getInputs().buttons)
            assertFalse(sender.isConnected.value)

            vm.changeServerAddr("192.168.42.21")
            vm.changeFlightMode(true)
            vm.onCollectiveChange(true, false)
            vm.onButtonPress(3)
            vm.changeConnectionMode(ConnectionMode.BLUETOOTH)
            assertEquals("192.168.44.30", vm.serverAddr.value)
            assertEquals(0f, inputs.getInputs().throttle, 0f)
            assertEquals(0f, inputs.getInputs().flaps, 0f)
            assertEquals(0, inputs.getInputs().buttons)
            vm.changeConnectionMode(ConnectionMode.USB)
            assertEquals("192.168.42.21", vm.serverAddr.value)
            vm.changeConnectionMode(ConnectionMode.WIFI)
            assertEquals("192.168.1.8", vm.serverAddr.value)
            vm.changeConnectionMode(ConnectionMode.BLUETOOTH)
            val restoredInputs = InputRepository()
            val restored = MainViewModel(SettingsRepository(file), restoredInputs, InputSender(restoredInputs))
            assertEquals(ConnectionMode.BLUETOOTH, restored.connectionMode.value)
            assertEquals("192.168.44.30", restored.serverAddr.value)

            vm.resetSettingsToDefault()
            assertEquals(ConnectionMode.WIFI, vm.connectionMode.value)
            assertEquals("", vm.serverAddr.value)
            val defaults = SettingsRepository(file)
            ConnectionMode.entries.forEach { assertEquals("", defaults.getServerAddr(it)) }
            assertEquals(ConnectionMode.WIFI, defaults.getConnectionMode())
        } finally {
            vm.pauseInputs()
        }
    }

    @Test fun switchingAnActiveProfileNeedsAcknowledgementFromNewTarget() = runBlocking {
        val wifi = AckReceiver("127.0.0.1", true)
        val usb = AckReceiver("127.0.0.2", false)
        val settings = SettingsRepository(File(files.root, "settings.json"))
        settings.setServerAddr("127.0.0.1")
        settings.setServerAddr("127.0.0.2", ConnectionMode.USB)
        val inputs = InputRepository()
        val sender = InputSender(inputs)
        val vm = MainViewModel(settings, inputs, sender)
        try {
            vm.resumeInputs()
            withTimeout(2000) { sender.isConnected.first { it } }
            vm.onThrottleChange(1f)
            vm.onButtonPress(0)
            vm.changeConnectionMode(ConnectionMode.USB)
            assertFalse(sender.isConnected.value)
            withTimeout(2000) { while (usb.received.get() == 0) delay(10) }
            delay(50)
            val oldPacketCount = wifi.received.get()
            delay(100)
            assertEquals(oldPacketCount, wifi.received.get())
            assertFalse(sender.isConnected.value)
            assertEquals(0f, inputs.getInputs().throttle, 0f)
            assertEquals(0, inputs.getInputs().buttons)
            usb.acknowledge.set(true)
            withTimeout(2000) { sender.isConnected.first { it } }
            vm.changeScreen(Screens.Settings)
            vm.changeConnectionMode(ConnectionMode.WIFI)
            delay(50)
            assertFalse(sender.isConnected.value)
            assertEquals("127.0.0.1", vm.serverAddr.value)
        } finally {
            vm.pauseInputs()
            wifi.close()
            usb.close()
        }
    }

    private class AckReceiver(address: String, ack: Boolean) {
        val acknowledge = AtomicBoolean(ack)
        val received = AtomicInteger()
        private val running = AtomicBoolean(true)
        private val socket = DatagramSocket(7001, InetAddress.getByName(address)).apply { soTimeout = 30 }
        private val worker = thread(isDaemon = true) {
            val packet = DatagramPacket(ByteArray(100), 100)
            while (running.get()) {
                try {
                    socket.receive(packet)
                    received.incrementAndGet()
                    if (acknowledge.get()) socket.send(DatagramPacket(byteArrayOf(InputProtocol.ACK), 1, packet.address, packet.port))
                    packet.length = 100
                } catch (_: SocketTimeoutException) { }
            }
        }

        fun close() {
            running.set(false)
            worker.join(1000)
            socket.close()
        }
    }
}
