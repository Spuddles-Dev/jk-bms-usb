package com.horse.jk_bms.protocol

import com.horse.jk_bms.protocol.BmsConstants.DATA_OFFSET
import com.horse.jk_bms.protocol.BmsConstants.DATA_SIZE
import com.horse.jk_bms.protocol.BmsConstants.FRAME_SIZE
import com.horse.jk_bms.protocol.BmsConstants.HEADER_MAGIC
import com.horse.jk_bms.protocol.BmsConstants.HEADER_SIZE

data class RawFrame(
    val frameCode: FrameCode,
    val counter: Int,
    val data: ByteArray,
) {
    override fun equals(other: Any?): Boolean = this === other
    override fun hashCode(): Int = System.identityHashCode(this)
}

object FrameDecoder {

    fun decode(frame: ByteArray): RawFrame? {
        if (frame.size != FRAME_SIZE) return null
        if (!validateHeader(frame)) return null
        if (!Checksum.validate(frame)) return null

        val codeByte = frame[4]
        val frameCode = FrameCode.fromCode(codeByte) ?: return null
        val counter = frame[5].toInt() and 0xFF
        val data = frame.copyOfRange(DATA_OFFSET, DATA_OFFSET + DATA_SIZE)

        return RawFrame(frameCode, counter, data)
    }

    fun findFrameInBuffer(buffer: ByteArray): Pair<RawFrame, Int>? {
        if (buffer.size < FRAME_SIZE) return null

        for (i in 0..(buffer.size - FRAME_SIZE)) {
            if (matchHeader(buffer, i)) {
                val candidate = buffer.copyOfRange(i, i + FRAME_SIZE)
                val decoded = decode(candidate)
                if (decoded != null) {
                    return decoded to (i + FRAME_SIZE)
                }
            }
        }
        return null
    }

    private fun validateHeader(frame: ByteArray): Boolean {
        for (i in HEADER_MAGIC.indices) {
            if (frame[i] != HEADER_MAGIC[i]) return false
        }
        return true
    }

    private fun matchHeader(buffer: ByteArray, offset: Int): Boolean {
        for (i in HEADER_MAGIC.indices) {
            if (buffer[offset + i] != HEADER_MAGIC[i]) return false
        }
        return true
    }
}
