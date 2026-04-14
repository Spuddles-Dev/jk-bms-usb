package com.horse.jk_bms.protocol

import com.horse.jk_bms.model.BmsConfig
import com.horse.jk_bms.protocol.BmsConstants.COUNTER_OFFSET
import com.horse.jk_bms.protocol.BmsConstants.DATA_OFFSET
import com.horse.jk_bms.protocol.BmsConstants.DATA_SIZE
import com.horse.jk_bms.protocol.BmsConstants.FRAME_CODE_OFFSET
import com.horse.jk_bms.protocol.BmsConstants.FRAME_SIZE
import com.horse.jk_bms.protocol.BmsConstants.HEADER_MAGIC
import com.horse.jk_bms.protocol.BmsConstants.HEADER_SIZE

object FrameEncoder {

    fun buildQuery(frameCode: FrameCode, counter: Int): ByteArray {
        val frame = ByteArray(FRAME_SIZE)
        System.arraycopy(HEADER_MAGIC, 0, frame, 0, HEADER_SIZE)
        frame[FRAME_CODE_OFFSET] = frameCode.code
        frame[COUNTER_OFFSET] = (counter and 0xFF).toByte()
        Checksum.writeChecksum(frame)
        return frame
    }

    fun buildConfigWrite(config: BmsConfig, counter: Int): ByteArray {
        val data = ByteArray(DATA_SIZE)

        FieldEncoder.writeU32(data, 0, (config.volSmartSleep / 0.001f).toLong())
        FieldEncoder.writeU32(data, 4, (config.volCellUV / 0.001f).toLong())
        FieldEncoder.writeU32(data, 8, (config.volCellUVPR / 0.001f).toLong())
        FieldEncoder.writeU32(data, 12, (config.volCellOV / 0.001f).toLong())
        FieldEncoder.writeU32(data, 16, (config.volCellOVPR / 0.001f).toLong())
        FieldEncoder.writeU32(data, 20, (config.volBalanTrig / 0.001f).toLong())
        FieldEncoder.writeU32(data, 24, (config.volSOCP100 / 0.001f).toLong())
        FieldEncoder.writeU32(data, 28, (config.volSOCP0 / 0.001f).toLong())
        FieldEncoder.writeU32(data, 32, (config.volCellRCV / 0.001f).toLong())
        FieldEncoder.writeU32(data, 36, (config.volCellRFV / 0.001f).toLong())
        FieldEncoder.writeU32(data, 40, (config.volSysPwrOff / 0.001f).toLong())
        FieldEncoder.writeU32(data, 44, (config.timBatCOC / 0.001f).toLong())
        FieldEncoder.writeU32(data, 48, config.timBatCOCPDly)
        FieldEncoder.writeU32(data, 52, config.timBatCOCPRDly)
        FieldEncoder.writeU32(data, 56, (config.timBatDcOC / 0.001f).toLong())
        FieldEncoder.writeU32(data, 60, config.timBatDcOCPDly)
        FieldEncoder.writeU32(data, 64, config.timBatDcOCPRDly)
        FieldEncoder.writeU32(data, 68, config.timBatSCPRDly)
        FieldEncoder.writeU32(data, 72, (config.curBalanMax / 0.001f).toLong())
        FieldEncoder.writeI32(data, 76, (config.tmpBatCOT / 0.1f).toInt())
        FieldEncoder.writeI32(data, 80, (config.tmpBatCOTPR / 0.1f).toInt())
        FieldEncoder.writeI32(data, 84, (config.tmpBatDcOT / 0.1f).toInt())
        FieldEncoder.writeI32(data, 88, (config.tmpBatDcOTPR / 0.1f).toInt())
        FieldEncoder.writeI32(data, 92, (config.tmpBatCUT / 0.1f).toInt())
        FieldEncoder.writeI32(data, 96, (config.tmpBatCUTPR / 0.1f).toInt())
        FieldEncoder.writeI32(data, 100, (config.tmpMosOT / 0.1f).toInt())
        FieldEncoder.writeI32(data, 104, (config.tmpMosOTPR / 0.1f).toInt())
        FieldEncoder.writeU32(data, 108, config.cellCount)
        FieldEncoder.writeU32(data, 112, config.batChargeEn)
        FieldEncoder.writeU32(data, 116, config.batDischargeEn)
        FieldEncoder.writeU32(data, 120, config.balanEn)
        FieldEncoder.writeU32(data, 124, (config.capBatCell / 0.001f).toLong())
        FieldEncoder.writeU32(data, 128, config.scpDelay)
        FieldEncoder.writeU32(data, 132, (config.volStartBalan / 0.001f).toLong())

        for (i in 0 until 32) {
            FieldEncoder.writeU32(data, 136 + i * 4, (config.cellConWireRes[i] / 0.001f).toLong())
        }

        FieldEncoder.writeU32(data, 264, config.devAddr)
        FieldEncoder.writeU32(data, 268, config.dischrgPreChrgT)
        FieldEncoder.writeU32(data, 272, (config.currentRange / 0.001f).toLong())
        FieldEncoder.writeBitmap(data, 276, config.switchStatus)
        FieldEncoder.writeI8(data, 278, config.tmpStartHeating)
        FieldEncoder.writeI8(data, 279, config.tmpStopHeating)
        FieldEncoder.writeU8(data, 280, config.timeSmartSleep)
        System.arraycopy(config.enableFlags, 0, data, 281, minOf(config.enableFlags.size, 9))
        FieldEncoder.writeI8(data, 290, config.tmpBatDCHUT)
        FieldEncoder.writeI8(data, 291, config.tmpBatDCHUTPR)

        val frame = ByteArray(FRAME_SIZE)
        System.arraycopy(HEADER_MAGIC, 0, frame, 0, HEADER_SIZE)
        frame[FRAME_CODE_OFFSET] = FrameCode.CONFIG_WRITE.code
        frame[COUNTER_OFFSET] = (counter and 0xFF).toByte()
        System.arraycopy(data, 0, frame, DATA_OFFSET, DATA_SIZE)
        Checksum.writeChecksum(frame)
        return frame
    }
}
