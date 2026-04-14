package com.horse.jk_bms.protocol

object FieldEncoder {

    fun writeU8(data: ByteArray, offset: Int, value: Int) {
        data[offset] = (value and 0xFF).toByte()
    }

    fun writeI8(data: ByteArray, offset: Int, value: Int) {
        data[offset] = value.toByte()
    }

    fun writeU16(data: ByteArray, offset: Int, value: Int) {
        data[offset] = (value and 0xFF).toByte()
        data[offset + 1] = ((value shr 8) and 0xFF).toByte()
    }

    fun writeI16(data: ByteArray, offset: Int, value: Int) {
        writeU16(data, offset, value)
    }

    fun writeU32(data: ByteArray, offset: Int, value: Long) {
        data[offset] = (value and 0xFF).toByte()
        data[offset + 1] = ((value shr 8) and 0xFF).toByte()
        data[offset + 2] = ((value shr 16) and 0xFF).toByte()
        data[offset + 3] = ((value shr 24) and 0xFF).toByte()
    }

    fun writeI32(data: ByteArray, offset: Int, value: Int) {
        writeU32(data, offset, value.toLong())
    }

    fun writeF32(data: ByteArray, offset: Int, value: Float) {
        val bits = java.lang.Float.floatToRawIntBits(value)
        writeU32(data, offset, bits.toLong())
    }

    fun writeU32Array(data: ByteArray, offset: Int, values: LongArray) {
        for (i in values.indices) {
            writeU32(data, offset + i * 4, values[i])
        }
    }

    fun writeBitmap(data: ByteArray, offset: Int, bits: BooleanArray) {
        val byteCount = (bits.size + 7) / 8
        for (i in 0 until byteCount) {
            var byte = 0
            for (b in 0 until 8) {
                val bitIdx = i * 8 + b
                if (bitIdx < bits.size && bits[bitIdx]) {
                    byte = byte or (1 shl b)
                }
            }
            data[offset + i] = byte.toByte()
        }
    }
}
