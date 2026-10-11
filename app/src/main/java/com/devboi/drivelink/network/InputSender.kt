package com.devboi.drivelink.network

import com.devboi.drivelink.data.InputRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException
import java.net.PortUnreachableException
import java.nio.ByteBuffer

class InputSender(private val inputRepository: InputRepository) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null
    @Volatile private var socket: DatagramSocket? = null
    private var addressString = ""
    private val clockOrigin = System.nanoTime()
    private val _isConnected = MutableStateFlow(false)
    val isConnected = _isConnected.asStateFlow()
    private val _connectionError = MutableStateFlow<String?>(null)
    val connectionError = _connectionError.asStateFlow()

    @Synchronized fun start() {
        if (addressString.isBlank() || job?.isActive == true) return
        val host = addressString
        job = scope.launch {
            val thisJob = currentCoroutineContext().job
            var activeSocket: DatagramSocket? = null
            try {
                val addr = InetAddress.getByName(host)
                ensureActive()
                activeSocket = DatagramSocket().apply {
                    connect(addr, 7001)
                    soTimeout = 4
                }
                synchronized(this@InputSender) {
                    if (job !== thisJob || !isActive) return@launch
                    socket = activeSocket
                    _connectionError.value = null
                }
                val bytes = ByteArray(InputProtocol.MAX_PACKET_SIZE)
                val buffer = ByteBuffer.wrap(bytes)
                val outgoing = DatagramPacket(bytes, bytes.size, addr, 7001)
                val ackBytes = ByteArray(2)
                val incoming = DatagramPacket(ackBytes, ackBytes.size)
                var lastAck = 0L
                while (isActive) {
                    val now = 1L + (System.nanoTime() - clockOrigin) / 1_000_000
                    outgoing.length = InputProtocol.encode(inputRepository.getInputs().copy(timestamp = now), buffer)
                    try {
                        activeSocket.send(outgoing)
                        incoming.length = ackBytes.size
                        activeSocket.receive(incoming)
                        if (incoming.length == 1 && ackBytes[0] == InputProtocol.ACK) lastAck = now
                    } catch (_: SocketTimeoutException) {
                        // A running receiver and UDP delivery are confirmed only by its ACK.
                    } catch (_: PortUnreachableException) {
                        lastAck = 0L
                    }
                    synchronized(this@InputSender) {
                        if (job === thisJob && isActive) {
                            _isConnected.value = lastAck != 0L && now - lastAck <= 500
                        }
                    }
                    delay(10)
                }
            } catch (_: IOException) {
                synchronized(this@InputSender) {
                    if (job === thisJob && isActive) {
                        _connectionError.value = "连接失败，请检查电脑地址、接收端和防火墙。"
                    }
                }
            } finally {
                activeSocket?.close()
                synchronized(this@InputSender) {
                    if (job === thisJob) {
                        socket = null
                        job = null
                        _isConnected.value = false
                    }
                }
            }
        }
    }

    @Synchronized fun stop() {
        job?.cancel()
        job = null
        socket?.close()
        socket = null
        _isConnected.value = false
    }

    @Synchronized fun setAddress(address: String) {
        stop()
        addressString = address.trim()
        _connectionError.value = null
    }
}
