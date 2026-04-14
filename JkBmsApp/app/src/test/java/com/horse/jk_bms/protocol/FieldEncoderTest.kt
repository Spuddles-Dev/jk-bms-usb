package com.horse.jk_bms.protocol

import org.junit.Assert.*
import org.junit.Test

class FieldEncoderTest {

    @Test
    fun testWriteU8RoundTrip() {
        val data = ByteArray(8)
        FieldEncoder.writeU8(data, 3, 200)
        assertEquals(200, FieldDecoder.readU8(data, 3))
    }

    @Test
    fun testWriteU8RawByte() {
        val data = ByteArray(8)
        FieldEncoder.writeU8(data, 0, 0xAB)
        assertEquals(0xAB.toByte(), data[0])
    }

    @Test
    fun testWriteU8Zero() {
        val data = ByteArray(8)
        FieldEncoder.writeU8(data, 0, 0)
        assertEquals(0, FieldDecoder.readU8(data, 0))
    }

    @Test
    fun testWriteU8Max() {
        val data = ByteArray(8)
        FieldEncoder.writeU8(data, 0, 255)
        assertEquals(255, FieldDecoder.readU8(data, 0))
    }

    @Test
    fun testWriteI8Negative() {
        val data = ByteArray(8)
        FieldEncoder.writeI8(data, 0, -1)
        assertEquals(-1, FieldDecoder.readI8(data, 0))
    }

    @Test
    fun testWriteI8MinValue() {
        val data = ByteArray(8)
        FieldEncoder.writeI8(data, 0, -128)
        assertEquals(-128, FieldDecoder.readI8(data, 0))
    }

    @Test
    fun testWriteI8Positive() {
        val data = ByteArray(8)
        FieldEncoder.writeI8(data, 0, 127)
        assertEquals(127, FieldDecoder.readI8(data, 0))
    }

    @Test
    fun testWriteU16LittleEndian() {
        val data = ByteArray(8)
        FieldEncoder.writeU16(data, 0, 0x1234)
        assertEquals(0x34.toByte(), data[0])
        assertEquals(0x12.toByte(), data[1])
    }

    @Test
    fun testWriteU16RoundTrip() {
        val data = ByteArray(8)
        FieldEncoder.writeU16(data, 0, 65535)
        assertEquals(65535, FieldDecoder.readU16(data, 0))
    }

    @Test
    fun testWriteI16Negative() {
        val data = ByteArray(8)
        FieldEncoder.writeI16(data, 0, -1000)
        assertEquals(-1000, FieldDecoder.readI16(data, 0))
    }

    @Test
    fun testWriteI16MinValue() {
        val data = ByteArray(8)
        FieldEncoder.writeI16(data, 0, -32768)
        assertEquals(-32768, FieldDecoder.readI16(data, 0))
    }

    @Test
    fun testWriteU32LittleEndian() {
        val data = ByteArray(8)
        FieldEncoder.writeU32(data, 0, 0x01020304L)
        assertEquals(0x04.toByte(), data[0])
        assertEquals(0x03.toByte(), data[1])
        assertEquals(0x02.toByte(), data[2])
        assertEquals(0x01.toByte(), data[3])
    }

    @Test
    fun testWriteU32RoundTrip() {
        val data = ByteArray(8)
        FieldEncoder.writeU32(data, 0, 4294967295L)
        assertEquals(4294967295L, FieldDecoder.readU32(data, 0))
    }

    @Test
    fun testWriteU32AtOffset() {
        val data = ByteArray(8)
        FieldEncoder.writeU32(data, 4, 0x000000FFL)
        assertEquals(0xFF.toByte(), data[4])
        assertEquals(0x00.toByte(), data[5])
        assertEquals(0x00.toByte(), data[6])
        assertEquals(0x00.toByte(), data[7])
    }

    @Test
    fun testWriteI32Negative() {
        val data = ByteArray(8)
        FieldEncoder.writeI32(data, 0, Int.MIN_VALUE)
        assertEquals(Int.MIN_VALUE, FieldDecoder.readI32(data, 0))
    }

    @Test
    fun testWriteI32MinusOne() {
        val data = ByteArray(8)
        FieldEncoder.writeI32(data, 0, -1)
        assertEquals(-1, FieldDecoder.readI32(data, 0))
    }

    @Test
    fun testWriteI32Positive() {
        val data = ByteArray(8)
        FieldEncoder.writeI32(data, 0, 123456)
        assertEquals(123456, FieldDecoder.readI32(data, 0))
    }

    @Test
    fun testWriteF32KnownValue() {
        val data = ByteArray(8)
        FieldEncoder.writeF32(data, 0, 3.14f)
        assertEquals(3.14f, FieldDecoder.readF32(data, 0), 0.0001f)
    }

    @Test
    fun testWriteF32Negative() {
        val data = ByteArray(8)
        FieldEncoder.writeF32(data, 0, -273.15f)
        assertEquals(-273.15f, FieldDecoder.readF32(data, 0), 0.001f)
    }

    @Test
    fun testWriteF32Zero() {
        val data = ByteArray(8)
        FieldEncoder.writeF32(data, 0, 0.0f)
        assertEquals(0.0f, FieldDecoder.readF32(data, 0), 0.0001f)
    }

    @Test
    fun testWriteU32Array() {
        val data = ByteArray(16)
        FieldEncoder.writeU32Array(data, 0, longArrayOf(100L, 200L, 300L))
        assertEquals(100L, FieldDecoder.readU32(data, 0))
        assertEquals(200L, FieldDecoder.readU32(data, 4))
        assertEquals(300L, FieldDecoder.readU32(data, 8))
    }

    @Test
    fun testWriteU32ArrayAtOffset() {
        val data = ByteArray(16)
        FieldEncoder.writeU32Array(data, 4, longArrayOf(0xABCDL, 0x1234L))
        assertEquals(0xABCDL, FieldDecoder.readU32(data, 4))
        assertEquals(0x1234L, FieldDecoder.readU32(data, 8))
        assertEquals(0L, FieldDecoder.readU32(data, 0))
    }

    @Test
    fun testWriteBitmapAllTrue() {
        val data = ByteArray(4)
        FieldEncoder.writeBitmap(data, 0, BooleanArray(8) { true })
        assertEquals(0xFF.toByte(), data[0])
    }

    @Test
    fun testWriteBitmapAllFalse() {
        val data = ByteArray(4)
        FieldEncoder.writeBitmap(data, 0, BooleanArray(8) { false })
        assertEquals(0x00.toByte(), data[0])
    }

    @Test
    fun testWriteBitmapAlternating() {
        val data = ByteArray(4)
        FieldEncoder.writeBitmap(data, 0, booleanArrayOf(false, true, false, true, false, true, false, true))
        assertEquals(0b10101010.toByte(), data[0])
    }

    @Test
    fun testWriteBitmapPartialLastByte() {
        val data = ByteArray(4)
        FieldEncoder.writeBitmap(data, 0, booleanArrayOf(true, false, true))
        assertEquals(5.toByte(), data[0])
        assertEquals(0.toByte(), data[1])
    }

    @Test
    fun testWriteBitmapRoundTrip16Bits() {
        val data = ByteArray(8)
        val bits = booleanArrayOf(
            true, false, true, true, false, false, true, false,
            false, true, false, false, true, true, false, true
        )
        FieldEncoder.writeBitmap(data, 0, bits)
        val result = FieldDecoder.readBitmap(data, 0, 16)
        assertArrayEquals(bits, result)
    }
}
