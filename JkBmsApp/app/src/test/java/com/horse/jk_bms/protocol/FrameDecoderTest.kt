package com.horse.jk_bms.protocol

import org.junit.Assert.*
import org.junit.Test

class FrameDecoderTest {

    @Test
    fun testDecodeValidRuntimeFrame() {
        val frame = ByteArray(300)
        System.arraycopy(BmsConstants.HEADER_MAGIC, 0, frame, 0, 4)
        frame[4] = 0x02.toByte()
        frame[5] = 0x00.toByte()
        Checksum.writeChecksum(frame)
        
        val result = FrameDecoder.decode(frame)
        
        assertNotNull("Should decode valid runtime frame", result)
        assertEquals(FrameCode.RUNTIME_DATA, result?.frameCode)
        assertEquals(0, result?.counter)
    }

    @Test
    fun testDecodeValidConfigFrame() {
        val frame = ByteArray(300)
        System.arraycopy(BmsConstants.HEADER_MAGIC, 0, frame, 0, 4)
        frame[4] = 0x01.toByte()
        frame[5] = 0x05.toByte()
        Checksum.writeChecksum(frame)
        
        val result = FrameDecoder.decode(frame)
        
        assertNotNull("Should decode valid config frame", result)
        assertEquals(FrameCode.CONFIG_READ, result?.frameCode)
        assertEquals(5, result?.counter)
    }

    @Test
    fun testDecodeInvalidHeader() {
        val frame = ByteArray(300)
        frame[0] = 0x00.toByte()
        frame[1] = 0x00.toByte()
        frame[2] = 0x00.toByte()
        frame[3] = 0x00.toByte()
        
        val result = FrameDecoder.decode(frame)
        
        assertNull("Should reject frame with invalid header", result)
    }

    @Test
    fun testDecodeInvalidChecksum() {
        val frame = ByteArray(300)
        System.arraycopy(BmsConstants.HEADER_MAGIC, 0, frame, 0, 4)
        frame[4] = 0x02.toByte()
        frame[5] = 0x00.toByte()
        frame[299] = 0x00.toByte()
        
        val result = FrameDecoder.decode(frame)
        
        assertNull("Should reject frame with invalid checksum", result)
    }

    @Test
    fun testDecodeTooShort() {
        val frame = ByteArray(200)
        
        val result = FrameDecoder.decode(frame)
        
        assertNull("Should reject frame shorter than 300 bytes", result)
    }

    @Test
    fun testDecodeUnknownFrameCode() {
        val frame = ByteArray(300)
        System.arraycopy(BmsConstants.HEADER_MAGIC, 0, frame, 0, 4)
        frame[4] = 0xFF.toByte()
        frame[5] = 0x00.toByte()
        Checksum.writeChecksum(frame)
        
        val result = FrameDecoder.decode(frame)
        
        assertNull("Should reject unknown frame code", result)
    }

    @Test
    fun testFindFrameInBuffer() {
        val buffer = ByteArray(500)
        val frame = ByteArray(300)
        System.arraycopy(BmsConstants.HEADER_MAGIC, 0, frame, 0, 4)
        frame[4] = 0x02.toByte()
        frame[5] = 0x00.toByte()
        Checksum.writeChecksum(frame)
        
        System.arraycopy(frame, 0, buffer, 100, 300)
        
        val result = FrameDecoder.findFrameInBuffer(buffer)
        
        assertNotNull("Should find frame in buffer", result)
        assertEquals(FrameCode.RUNTIME_DATA, result?.first?.frameCode)
        assertEquals(400, result?.second)
    }

    @Test
    fun testFindFrameInBufferTooShort() {
        val buffer = ByteArray(100)
        
        val result = FrameDecoder.findFrameInBuffer(buffer)
        
        assertNull("Should not find frame in too-short buffer", result)
    }

    @Test
    fun testCounterWrapAround() {
        val frame = ByteArray(300)
        System.arraycopy(BmsConstants.HEADER_MAGIC, 0, frame, 0, 4)
        frame[4] = 0x02.toByte()
        frame[5] = 0xFF.toByte()
        Checksum.writeChecksum(frame)
        
        val result = FrameDecoder.decode(frame)
        
        assertNotNull("Should decode frame with counter 255", result)
        assertEquals(255, result?.counter)
    }
}
