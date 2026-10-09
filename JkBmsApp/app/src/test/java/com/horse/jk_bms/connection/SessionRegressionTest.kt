package com.horse.jk_bms.connection

import android.app.Application
import android.hardware.usb.UsbDevice
import com.hoho.android.usbserial.driver.UsbSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.horse.jk_bms.data.repository.DataLogRepository
import com.horse.jk_bms.diagnostics.ProtocolTrace
import com.horse.jk_bms.protocol.FrameCode
import com.horse.jk_bms.protocol.RawFrame
import com.horse.jk_bms.usb.BmsTransport
import com.horse.jk_bms.usb.UsbDeviceInfo
import com.horse.jk_bms.usb.UsbEventReceiver
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.async
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.concurrent.atomic.AtomicInteger

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35])
class SessionRegressionTest {
    private class Fake : BmsTransport {
        var connected = false
        val calls = AtomicInteger()
        val active = AtomicInteger()
        val maxActive = AtomicInteger()
        var connectDelay = 0L
        val connecting = CompletableDeferred<Unit>()
        override val isConnected get() = connected
        override fun listSerialDevices() = emptyList<UsbDeviceInfo>()
        override suspend fun connect(info: UsbDeviceInfo): Result<Unit> {
            connecting.complete(Unit)
            delay(connectDelay)
            connected = true
            return Result.success(Unit)
        }
        override fun disconnect() { connected = false }
        override suspend fun exchange(frame: ByteArray, expected: FrameCode): Result<RawFrame> {
            calls.incrementAndGet()
            val count = active.incrementAndGet()
            maxActive.updateAndGet { maxOf(it, count) }
            try {
                delay(5)
                return Result.success(RawFrame(expected, 0, ByteArray(293)))
            } finally { active.decrementAndGet() }
        }
    }

    private fun device(): UsbDeviceInfo {
        val device = mockk<UsbDevice>(relaxed = true)
        every { device.serialNumber } returns "adapter-one"
        val port = mockk<UsbSerialPort>()
        val driver = mockk<UsbSerialDriver>()
        every { driver.ports } returns listOf(port)
        return UsbDeviceInfo(device, port, driver)
    }

    @Test fun testForegroundShutdownCancelsPendingConnection() = runBlocking {
        val transport = Fake().apply { connectDelay = 30_000 }
        val connection = BmsConnection(RuntimeEnvironment.getApplication(), transport,
            mockk<DataLogRepository>(relaxed = true), UsbEventReceiver(), ProtocolTrace())
        try {
            val pending = async { connection.connect(device()) }
            transport.connecting.await()
            connection.requestDisconnect()
            pending.join()
            connection.disconnect()
            assertTrue(pending.isCancelled)
            assertFalse(transport.connected)
            assertFalse(connection.isConnected.value)
            assertEquals(SessionState.DISCONNECTED, connection.sessionState.value)
        } finally { connection.close() }
    }

    @Test fun testConcurrentRefreshesHaveOneOwnerAndDisconnectStopsTraffic() = runBlocking {
        val transport = Fake()
        val logging = mockk<DataLogRepository>(relaxed = true)
        val connection = BmsConnection(RuntimeEnvironment.getApplication(), transport, logging, UsbEventReceiver(), ProtocolTrace())
        try {
            assertTrue(connection.connect(device()).isSuccess)
            (0 until 20).map { async { connection.queryFrame(FrameCode.RUNTIME_DATA) } }.awaitAll()
            assertEquals(1, transport.maxActive.get())
            connection.disconnect()
            val calls = transport.calls.get()
            delay(350)
            assertEquals(calls, transport.calls.get())
            assertFalse(connection.isConnected.value)
            assertEquals(null, connection.runtimeData.value)
            assertTrue(connection.queryFrame(FrameCode.RUNTIME_DATA).isFailure)
        } finally { connection.close() }
    }

    @Test fun testPersistenceFailureDoesNotKillMonitoring() = runBlocking {
        val transport = Fake()
        val logging = mockk<DataLogRepository>(relaxed = true)
        coEvery { logging.logRuntimeData(any(), any()) } throws IllegalStateException("Disk full")
        val connection = BmsConnection(RuntimeEnvironment.getApplication(), transport, logging, UsbEventReceiver(), ProtocolTrace())
        try {
            connection.connect(device())
            delay(500)
            assertTrue(connection.isConnected.value)
            assertTrue(connection.queryFrame(FrameCode.RUNTIME_DATA).isSuccess)
            delay(50)
            assertTrue(connection.loggingError.value?.contains("Disk full") == true)
        } finally { connection.close() }
    }
}
