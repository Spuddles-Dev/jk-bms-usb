package com.horse.jk_bms.protocol

import java.nio.ByteBuffer
import java.nio.ByteOrder

object FieldDecoder {

    fun readU8(data: ByteArray, offset: Int): Int {
        return data[offset].toInt() and 0xFF
    }

    fun readI8(data: ByteArray, offset: Int): Int {
        return data[offset].toInt()
    }

    fun readU16(data: ByteArray, offset: Int): Int {
        val buf = ByteBuffer.wrap(data, offset, 2).order(ByteOrder.LITTLE_ENDIAN)
        return buf.short.toInt() and 0xFFFF
    }

    fun readI16(data: ByteArray, offset: Int): Int {
        val buf = ByteBuffer.wrap(data, offset, 2).order(ByteOrder.LITTLE_ENDIAN)
        return buf.short.toInt()
    }

    fun readU32(data: ByteArray, offset: Int): Long {
        val buf = ByteBuffer.wrap(data, offset, 4).order(ByteOrder.LITTLE_ENDIAN)
        return buf.int.toLong() and 0xFFFFFFFFL
    }

    fun readI32(data: ByteArray, offset: Int): Int {
        val buf = ByteBuffer.wrap(data, offset, 4).order(ByteOrder.LITTLE_ENDIAN)
        return buf.int
    }

    fun readF32(data: ByteArray, offset: Int): Float {
        val buf = ByteBuffer.wrap(data, offset, 4).order(ByteOrder.LITTLE_ENDIAN)
        return buf.float
    }

    fun readU16Array(data: ByteArray, offset: Int, count: Int): IntArray {
        val result = IntArray(count)
        for (i in 0 until count) {
            result[i] = readU16(data, offset + i * 2)
        }
        return result
    }

    fun readU32Array(data: ByteArray, offset: Int, count: Int): LongArray {
        val result = LongArray(count)
        for (i in 0 until count) {
            result[i] = readU32(data, offset + i * 4)
        }
        return result
    }

    fun readBytes(data: ByteArray, offset: Int, length: Int): ByteArray {
        return data.copyOfRange(offset, offset + length)
    }

    fun readString(data: ByteArray, offset: Int, length: Int): String {
        val raw = data.copyOfRange(offset, offset + length)
        val end = raw.indexOfFirst { it == 0.toByte() }
        val strBytes = if (end >= 0) raw.copyOfRange(0, end) else raw
        return String(strBytes, Charsets.UTF_8)
    }

    fun readBitmap(data: ByteArray, offset: Int, bitCount: Int): BooleanArray {
        val byteCount = (bitCount + 7) / 8
        val result = BooleanArray(bitCount)
        for (i in 0 until bitCount) {
            val byteIdx = i / 8
            val bitIdx = i % 8
            if (byteIdx < byteCount && offset + byteIdx < data.size) {
                result[i] = (data[offset + byteIdx].toInt() shr bitIdx) and 1 == 1
            }
        }
        return result
    }
}
