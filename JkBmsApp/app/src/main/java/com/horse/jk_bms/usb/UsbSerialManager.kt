package com.horse.jk_bms.usb

import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import com.hoho.android.usbserial.driver.UsbSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import com.horse.jk_bms.protocol.BmsConstants.BAUD_RATE
import com.horse.jk_bms.protocol.BmsConstants.FRAME_SIZE
import com.horse.jk_bms.protocol.BmsConstants.QUERY_TIMEOUT_MS
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

sealed class UsbConnectionState {
    data object Disconnected : UsbConnectionState()
    data class Connecting(val deviceName: String) : UsbConnectionState()
    data class Connected(val deviceName: String) : UsbConnectionState()
    data class Error(val message: String) : UsbConnectionState()
}

data class UsbDeviceInfo(
    val device: UsbDevice,
    val port: UsbSerialPort,
    val driver: UsbSerialDriver,
)

@Singleton
class UsbSerialManager @Inject constructor(
    private val usbManager: UsbManager,
) {
    private var connection: UsbDeviceConnection? = null
    private var port: UsbSerialPort? = null

    val isConnected: Boolean get() = port?.isOpen == true

    fun listSerialDevices(): List<UsbDeviceInfo> {
        val drivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)
        return drivers.flatMap { driver ->
            driver.ports.map { port ->
                UsbDeviceInfo(driver.device, port, driver)
            }
        }
    }

    suspend fun connect(info: UsbDeviceInfo): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            disconnect()

            val conn = usbManager.openDevice(info.device)
                ?: return@withContext Result.failure(IOException("Failed to open USB device — permission denied?"))

            info.port.open(conn)
            info.port.setParameters(BAUD_RATE, UsbSerialPort.DATABITS_8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)
            info.port.setDTR(true)
            info.port.setRTS(true)

            connection = conn
            port = info.port
            Result.success(Unit)
        } catch (e: Exception) {
            port = null
            connection = null
            Result.failure(e)
        }
    }

    fun disconnect() {
        try {
            port?.close()
        } catch (_: Exception) {
        }
        port = null
        connection = null
    }

    suspend fun sendAndReceive(frame: ByteArray): Result<ByteArray> = withContext(Dispatchers.IO) {
        val p = port
        if (p == null || !p.isOpen) {
            return@withContext Result.failure(IOException("Not connected"))
        }
        try {
            p.write(frame, QUERY_TIMEOUT_MS.toInt())

            delay(20)

            val buffer = ByteArray(FRAME_SIZE)
            var totalRead = 0
            val startTime = System.currentTimeMillis()

            while (totalRead < FRAME_SIZE) {
                val elapsed = System.currentTimeMillis() - startTime
                if (elapsed > QUERY_TIMEOUT_MS) {
                    return@withContext Result.failure(IOException("Timeout waiting for response (${totalRead} bytes read)"))
                }

                val chunk = ByteArray(FRAME_SIZE - totalRead)
                val read = p.read(chunk, 100)
                if (read > 0) {
                    System.arraycopy(chunk, 0, buffer, totalRead, read)
                    totalRead += read
                }
            }

            Result.success(buffer.copyOfRange(0, totalRead))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
