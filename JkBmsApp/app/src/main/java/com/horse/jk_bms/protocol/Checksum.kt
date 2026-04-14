package com.horse.jk_bms.protocol

import com.horse.jk_bms.protocol.BmsConstants.CHECKSUM_OFFSET
import com.horse.jk_bms.protocol.BmsConstants.DATA_OFFSET
import com.horse.jk_bms.protocol.BmsConstants.FRAME_SIZE

object Checksum {
    fun calculate(frame: ByteArray): Byte {
        var sum = 0
        for (i in 0 until CHECKSUM_OFFSET) {
            sum += frame[i].toInt() and 0xFF
        }
        return (sum and 0xFF).toByte()
    }

    fun validate(frame: ByteArray): Boolean {
        if (frame.size != FRAME_SIZE) return false
        return calculate(frame) == frame[CHECKSUM_OFFSET]
    }

    fun writeChecksum(frame: ByteArray) {
        frame[CHECKSUM_OFFSET] = calculate(frame)
    }
}
