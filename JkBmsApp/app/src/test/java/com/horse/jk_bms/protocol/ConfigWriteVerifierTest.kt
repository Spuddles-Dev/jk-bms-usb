package com.horse.jk_bms.protocol

import com.horse.jk_bms.connection.ConfigWriteVerifier
import com.horse.jk_bms.usb.BmsTransport
import com.horse.jk_bms.usb.UsbDeviceInfo
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConfigWriteVerifierTest {
    private class Fake(private val responses: List<RawFrame>) : BmsTransport {
        var exchanges = 0
        override val isConnected = true
        override fun listSerialDevices() = emptyList<UsbDeviceInfo>()
        override suspend fun connect(info: UsbDeviceInfo) = Result.success(Unit)
        override fun disconnect() = Unit
        override suspend fun exchange(frame: ByteArray, expected: FrameCode): Result<RawFrame> =
            Result.success(responses[exchanges++])
    }

    @Test fun testSuccessfulWriteRequiresFreshBaselineAckAndReadback() = runBlocking {
        val original = validConfig()
        val requested = original.copy(volCellUV = 2.7f)
        val fake = Fake(listOf(readFrame(original), RawFrame(FrameCode.CONFIG_WRITE, 0, ByteArray(293)), readFrame(requested)))
        val result = ConfigWriteVerifier.write(fake, requested, original) { 0 }
        assertEquals(2.7f, result.volCellUV, 0.0001f)
        assertEquals(3, fake.exchanges)
    }

    @Test fun testUnrelatedAckIsNotSuccess() = runBlocking {
        val original = validConfig()
        val fake = Fake(listOf(readFrame(original), RawFrame(FrameCode.RUNTIME_DATA, 0, ByteArray(293))))
        assertTrue(runCatching { ConfigWriteVerifier.write(fake, original.copy(volCellUV = 2.7f), original) { 0 } }.isFailure)
        assertEquals(2, fake.exchanges)
    }

    @Test fun testMismatchAndChangedBaselineAreRejected() = runBlocking {
        val original = validConfig()
        val requested = original.copy(volCellUV = 2.7f)
        val mismatch = Fake(listOf(readFrame(original), RawFrame(FrameCode.CONFIG_WRITE, 0, ByteArray(293)), readFrame(original)))
        assertTrue(runCatching { ConfigWriteVerifier.write(mismatch, requested, original) { 0 } }.exceptionOrNull()?.message?.contains("Readback mismatch") == true)
        val changed = Fake(listOf(readFrame(original.copy(volCellUV = 2.6f))))
        assertTrue(runCatching { ConfigWriteVerifier.write(changed, requested, original) { 0 } }.isFailure)
        assertEquals(1, changed.exchanges)
    }

    @Test fun testInvalidDraftNeverReachesTransport() = runBlocking {
        val fake = Fake(emptyList())
        assertTrue(runCatching { ConfigWriteVerifier.write(fake, validConfig().copy(scpDelay = -1), validConfig()) { 0 } }.isFailure)
        assertEquals(0, fake.exchanges)
    }
}
