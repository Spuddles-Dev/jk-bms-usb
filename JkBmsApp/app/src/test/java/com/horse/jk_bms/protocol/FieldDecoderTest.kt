package com.horse.jk_bms.protocol

import org.junit.Assert.*
import org.junit.Test

class FieldDecoderTest {

    // --- readU8 ---

    @Test
    fun testReadU8Zero() {
        val data = ByteArray(8)
        assertEquals(0, FieldDecoder.readU8(data, 0))
    }

    @Test
    fun testReadU8Max() {
        val data = ByteArray(8)
        data[2] = 0xFF.toByte()
        assertEquals(255, FieldDecoder.readU8(data, 2))
    }

    @Test
    fun testReadU8Mid() {
        val data = ByteArray(8)
        data[3] = 0x7F.toByte()
        assertEquals(127, FieldDecoder.readU8(data, 3))
    }

    // --- readI8 ---

    @Test
    fun testReadI8Positive() {
        val data = ByteArray(8)
        data[1] = 0x7F.toByte()
        assertEquals(127, FieldDecoder.readI8(data, 1))
    }

    @Test
    fun testReadI8Negative() {
        val data = ByteArray(8)
        data[1] = 0xFF.toByte()
        assertEquals(-1, FieldDecoder.readI8(data, 1))
    }

    @Test
    fun testReadI8MinValue() {
        val data = ByteArray(8)
        data[0] = 0x80.toByte()
        assertEquals(-128, FieldDecoder.readI8(data, 0))
    }

    // --- readU16 ---

    @Test
    fun testReadU16LittleEndian() {
        val data = ByteArray(8)
        data[0] = 0x34.toByte()
        data[1] = 0x12.toByte()
        assertEquals(0x1234, FieldDecoder.readU16(data, 0))
    }

    @Test
    fun testReadU16Max() {
        val data = ByteArray(8)
        data[2] = 0xFF.toByte()
        data[3] = 0xFF.toByte()
        assertEquals(65535, FieldDecoder.readU16(data, 2))
    }

    @Test
    fun testReadU16Zero() {
        val data = ByteArray(8)
        assertEquals(0, FieldDecoder.readU16(data, 0))
    }

    // --- readI16 ---

    @Test
    fun testReadI16Negative() {
        val data = ByteArray(8)
        data[0] = 0x00.toByte()
        data[1] = 0x80.toByte()
        assertEquals(-32768, FieldDecoder.readI16(data, 0))
    }

    @Test
    fun testReadI16Positive() {
        val data = ByteArray(8)
        data[0] = 0xFF.toByte()
        data[1] = 0x7F.toByte()
        assertEquals(32767, FieldDecoder.readI16(data, 0))
    }

    @Test
    fun testReadI16MinusOne() {
        val data = ByteArray(8)
        data[0] = 0xFF.toByte()
        data[1] = 0xFF.toByte()
        assertEquals(-1, FieldDecoder.readI16(data, 0))
    }

    // --- readU32 ---

    @Test
    fun testReadU32LittleEndian() {
        val data = ByteArray(8)
        data[0] = 0x04.toByte()
        data[1] = 0x03.toByte()
        data[2] = 0x02.toByte()
        data[3] = 0x01.toByte()
        assertEquals(0x01020304L, FieldDecoder.readU32(data, 0))
    }

    @Test
    fun testReadU32Max() {
        val data = ByteArray(8)
        data[0] = 0xFF.toByte()
        data[1] = 0xFF.toByte()
        data[2] = 0xFF.toByte()
        data[3] = 0xFF.toByte()
        assertEquals(4294967295L, FieldDecoder.readU32(data, 0))
    }

    @Test
    fun testReadU32Zero() {
        val data = ByteArray(8)
        assertEquals(0L, FieldDecoder.readU32(data, 0))
    }

    // --- readI32 ---

    @Test
    fun testReadI32Negative() {
        val data = ByteArray(8)
        data[0] = 0x00.toByte()
        data[1] = 0x00.toByte()
        data[2] = 0x00.toByte()
        data[3] = 0x80.toByte()
        assertEquals(Int.MIN_VALUE, FieldDecoder.readI32(data, 0))
    }

    @Test
    fun testReadI32Positive() {
        val data = ByteArray(8)
        data[0] = 0xFF.toByte()
        data[1] = 0xFF.toByte()
        data[2] = 0xFF.toByte()
        data[3] = 0x7F.toByte()
        assertEquals(Int.MAX_VALUE, FieldDecoder.readI32(data, 0))
    }

    @Test
    fun testReadI32MinusOne() {
        val data = ByteArray(8)
        data[0] = 0xFF.toByte()
        data[1] = 0xFF.toByte()
        data[2] = 0xFF.toByte()
        data[3] = 0xFF.toByte()
        assertEquals(-1, FieldDecoder.readI32(data, 0))
    }

    // --- readF32 ---

    @Test
    fun testReadF32KnownValue() {
        val data = ByteArray(8)
        val bits = java.lang.Float.floatToRawIntBits(3.14f)
        data[0] = (bits and 0xFF).toByte()
        data[1] = ((bits shr 8) and 0xFF).toByte()
        data[2] = ((bits shr 16) and 0xFF).toByte()
        data[3] = ((bits shr 24) and 0xFF).toByte()
        assertEquals(3.14f, FieldDecoder.readF32(data, 0), 0.0001f)
    }

    @Test
    fun testReadF32Zero() {
        val data = ByteArray(8)
        assertEquals(0.0f, FieldDecoder.readF32(data, 0), 0.0001f)
    }

    @Test
    fun testReadF32Negative() {
        val data = ByteArray(8)
        val bits = java.lang.Float.floatToRawIntBits(-1.5f)
        data[0] = (bits and 0xFF).toByte()
        data[1] = ((bits shr 8) and 0xFF).toByte()
        data[2] = ((bits shr 16) and 0xFF).toByte()
        data[3] = ((bits shr 24) and 0xFF).toByte()
        assertEquals(-1.5f, FieldDecoder.readF32(data, 0), 0.0001f)
    }

    // --- readU16Array ---

    @Test
    fun testReadU16Array() {
        val data = ByteArray(16)
        data[0] = 0x02.toByte(); data[1] = 0x01.toByte() // 0x0102
        data[2] = 0x04.toByte(); data[3] = 0x03.toByte() // 0x0304
        data[4] = 0x06.toByte(); data[5] = 0x05.toByte() // 0x0506
        val result = FieldDecoder.readU16Array(data, 0, 3)
        assertEquals(3, result.size)
        assertEquals(0x0102, result[0])
        assertEquals(0x0304, result[1])
        assertEquals(0x0506, result[2])
    }

    @Test
    fun testReadU16ArrayOffset() {
        val data = ByteArray(16)
        data[4] = 0xAB.toByte(); data[5] = 0x00.toByte() // 0x00AB at offset 4
        val result = FieldDecoder.readU16Array(data, 4, 1)
        assertEquals(0x00AB, result[0])
    }

    // --- readU32Array ---

    @Test
    fun testReadU32Array() {
        val data = ByteArray(16)
        data[0] = 0x01.toByte(); data[1] = 0x00.toByte(); data[2] = 0x00.toByte(); data[3] = 0x00.toByte()
        data[4] = 0x02.toByte(); data[5] = 0x00.toByte(); data[6] = 0x00.toByte(); data[7] = 0x00.toByte()
        val result = FieldDecoder.readU32Array(data, 0, 2)
        assertEquals(2, result.size)
        assertEquals(1L, result[0])
        assertEquals(2L, result[1])
    }

    // --- readBytes ---

    @Test
    fun testReadBytes() {
        val data = byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05, 0x06)
        val result = FieldDecoder.readBytes(data, 2, 3)
        assertArrayEquals(byteArrayOf(0x03, 0x04, 0x05), result)
    }

    @Test
    fun testReadBytesFromStart() {
        val data = byteArrayOf(0xAA.toByte(), 0xBB.toByte(), 0xCC.toByte())
        val result = FieldDecoder.readBytes(data, 0, 3)
        assertArrayEquals(data, result)
    }

    // --- readString ---

    @Test
    fun testReadStringNullTerminated() {
        val data = ByteArray(16)
        data[0] = 'A'.code.toByte()
        data[1] = 'B'.code.toByte()
        data[2] = 'C'.code.toByte()
        data[3] = 0x00.toByte()
        data[4] = 'X'.code.toByte() // ignored after null
        assertEquals("ABC", FieldDecoder.readString(data, 0, 8))
    }

    @Test
    fun testReadStringFullLength() {
        val data = ByteArray(16)
        data[0] = 'H'.code.toByte()
        data[1] = 'i'.code.toByte()
        assertEquals("Hi", FieldDecoder.readString(data, 0, 2))
    }

    @Test
    fun testReadStringEmpty() {
        val data = ByteArray(8)
        data[0] = 0x00.toByte()
        assertEquals("", FieldDecoder.readString(data, 0, 4))
    }

    @Test
    fun testReadStringWithOffset() {
        val data = ByteArray(16)
        data[5] = 'Z'.code.toByte()
        data[6] = 0x00.toByte()
        assertEquals("Z", FieldDecoder.readString(data, 5, 4))
    }

    // --- readBitmap ---

    @Test
    fun testReadBitmapAllZeros() {
        val data = ByteArray(4)
        val result = FieldDecoder.readBitmap(data, 0, 8)
        assertEquals(8, result.size)
        result.forEach { assertFalse(it) }
    }

    @Test
    fun testReadBitmapAllOnes() {
        val data = ByteArray(4)
        data[0] = 0xFF.toByte()
        val result = FieldDecoder.readBitmap(data, 0, 8)
        assertEquals(8, result.size)
        result.forEach { assertTrue(it) }
    }

    @Test
    fun testReadBitmapAlternating() {
        val data = ByteArray(4)
        data[0] = 0b10101010.toByte() // bits 1,3,5,7 set
        val result = FieldDecoder.readBitmap(data, 0, 8)
        assertFalse(result[0])
        assertTrue(result[1])
        assertFalse(result[2])
        assertTrue(result[3])
        assertFalse(result[4])
        assertTrue(result[5])
        assertFalse(result[6])
        assertTrue(result[7])
    }

    @Test
    fun testReadBitmap32Bits() {
        val data = ByteArray(8)
        data[0] = 0xFF.toByte()
        data[1] = 0x00.toByte()
        data[2] = 0xFF.toByte()
        data[3] = 0x00.toByte()
        val result = FieldDecoder.readBitmap(data, 0, 32)
        assertEquals(32, result.size)
        for (i in 0..7)   assertTrue(result[i])
        for (i in 8..15)  assertFalse(result[i])
        for (i in 16..23) assertTrue(result[i])
        for (i in 24..31) assertFalse(result[i])
    }

    @Test
    fun testReadBitmapPartialLastByte() {
        val data = ByteArray(4)
        data[0] = 0b00000111.toByte() // only 3 bits used
        val result = FieldDecoder.readBitmap(data, 0, 3)
        assertEquals(3, result.size)
        assertTrue(result[0])
        assertTrue(result[1])
        assertTrue(result[2])
    }
}
