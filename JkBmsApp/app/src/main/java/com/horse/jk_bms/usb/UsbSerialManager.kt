package com.horse.jk_bms.usb

import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.os.SystemClock
import com.hoho.android.usbserial.driver.UsbSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import com.horse.jk_bms.protocol.BmsConstants
import com.horse.jk_bms.protocol.FrameCode
import com.horse.jk_bms.protocol.FrameStreamDecoder
import com.horse.jk_bms.protocol.RawFrame
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext

data class UsbDeviceInfo(val device: UsbDevice, val port: UsbSerialPort, val driver: UsbSerialDriver) {
    val portIndex: Int get() = driver.ports.indexOf(port)
    val serial: String? get() = try { device.serialNumber } catch (_: SecurityException) { null }
}

@Singleton
class UsbSerialManager @Inject constructor(
    private val usbManager: UsbManager,
    private val permissions: UsbPermissionRequester,
) : BmsTransport {
    private var connection: UsbDeviceConnection? = null
    private var port: UsbSerialPort? = null
    private val decoder = FrameStreamDecoder()
    override val isConnected: Boolean get() = port?.isOpen == true

    override fun listSerialDevices(): List<UsbDeviceInfo> =
        UsbSerialProber.getDefaultProber().findAllDrivers(usbManager).flatMap { driver ->
            driver.ports.map { UsbDeviceInfo(driver.device, it, driver) }
        }

    override suspend fun connect(info: UsbDeviceInfo): Result<Unit> = withContext(Dispatchers.IO) {
        var acquired: UsbDeviceConnection? = null
        var opened: UsbSerialPort? = null
        try {
            disconnect()
            if (!permissions.awaitPermission(info.device)) throw IOException("USB permission denied or adapter removed")
            coroutineContext.ensureActive()
            acquired = usbManager.openDevice(info.device) ?: throw IOException("USB adapter unavailable")
            opened = info.port
            opened.open(acquired)
            opened.setParameters(BmsConstants.BAUD_RATE, UsbSerialPort.DATABITS_8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)
            opened.dtr = true
            opened.rts = true
            connection = acquired
            port = opened
            decoder.clear()
            Result.success(Unit)
        } catch (error: Exception) {
            try { opened?.close() } catch (_: Exception) { }
            acquired?.close()
            if (error is CancellationException) throw error
            Result.failure(error)
        }
    }

    override fun disconnect() {
        try { port?.close() } catch (_: Exception) { }
        connection?.close()
        port = null
        connection = null
        decoder.clear()
    }

    override suspend fun exchange(frame: ByteArray, expected: FrameCode): Result<RawFrame> = withContext(Dispatchers.IO) {
        try {
            val activePort = port ?: throw IOException("Not connected")
            activePort.write(frame, BmsConstants.QUERY_TIMEOUT_MS.toInt())
            val deadline = SystemClock.elapsedRealtime() + BmsConstants.QUERY_TIMEOUT_MS
            val chunk = ByteArray(1024)
            while (SystemClock.elapsedRealtime() < deadline) {
                coroutineContext.ensureActive()
                val timeout = (deadline - SystemClock.elapsedRealtime()).coerceIn(1, 100).toInt()
                val count = activePort.read(chunk, timeout)
                if (count > 0) {
                    val matches = decoder.append(chunk.copyOf(count)).filter { it.frameCode == expected }
                    if (matches.isNotEmpty()) return@withContext Result.success(matches.first())
                }
            }
            Result.failure(IOException("Timeout waiting for ${expected.name}"))
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            Result.failure(error)
        }
    }
}
