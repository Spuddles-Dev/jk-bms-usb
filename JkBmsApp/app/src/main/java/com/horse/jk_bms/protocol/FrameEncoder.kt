package com.horse.jk_bms.protocol

import com.horse.jk_bms.model.BmsConfig
import kotlin.math.roundToLong
import com.horse.jk_bms.protocol.BmsConstants.COUNTER_OFFSET
import com.horse.jk_bms.protocol.BmsConstants.DATA_OFFSET
import com.horse.jk_bms.protocol.BmsConstants.DATA_SIZE
import com.horse.jk_bms.protocol.BmsConstants.FRAME_CODE_OFFSET
import com.horse.jk_bms.protocol.BmsConstants.FRAME_SIZE
import com.horse.jk_bms.protocol.BmsConstants.HEADER_MAGIC
import com.horse.jk_bms.protocol.BmsConstants.HEADER_SIZE

object FrameEncoder {
    private fun scaled(value: Float, factor: Double): Long {
        require(value.isFinite()) { "Non-finite configuration value" }
        return (value.toDouble() * factor).roundToLong()
    }

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

        FieldEncoder.writeU32(data, 0, scaled(config.volSmartSleep, 1000.0))
        FieldEncoder.writeU32(data, 4, scaled(config.volCellUV, 1000.0))
        FieldEncoder.writeU32(data, 8, scaled(config.volCellUVPR, 1000.0))
        FieldEncoder.writeU32(data, 12, scaled(config.volCellOV, 1000.0))
        FieldEncoder.writeU32(data, 16, scaled(config.volCellOVPR, 1000.0))
        FieldEncoder.writeU32(data, 20, scaled(config.volBalanTrig, 1000.0))
        FieldEncoder.writeU32(data, 24, scaled(config.volSOCP100, 1000.0))
        FieldEncoder.writeU32(data, 28, scaled(config.volSOCP0, 1000.0))
        FieldEncoder.writeU32(data, 32, scaled(config.volCellRCV, 1000.0))
        FieldEncoder.writeU32(data, 36, scaled(config.volCellRFV, 1000.0))
        FieldEncoder.writeU32(data, 40, scaled(config.volSysPwrOff, 1000.0))
        FieldEncoder.writeU32(data, 44, scaled(config.timBatCOC, 1000.0))
        FieldEncoder.writeU32(data, 48, config.timBatCOCPDly)
        FieldEncoder.writeU32(data, 52, config.timBatCOCPRDly)
        FieldEncoder.writeU32(data, 56, scaled(config.timBatDcOC, 1000.0))
        FieldEncoder.writeU32(data, 60, config.timBatDcOCPDly)
        FieldEncoder.writeU32(data, 64, config.timBatDcOCPRDly)
        FieldEncoder.writeU32(data, 68, config.timBatSCPRDly)
        FieldEncoder.writeU32(data, 72, scaled(config.curBalanMax, 1000.0))
        FieldEncoder.writeI32(data, 76, scaled(config.tmpBatCOT, 10.0).toInt())
        FieldEncoder.writeI32(data, 80, scaled(config.tmpBatCOTPR, 10.0).toInt())
        FieldEncoder.writeI32(data, 84, scaled(config.tmpBatDcOT, 10.0).toInt())
        FieldEncoder.writeI32(data, 88, scaled(config.tmpBatDcOTPR, 10.0).toInt())
        FieldEncoder.writeI32(data, 92, scaled(config.tmpBatCUT, 10.0).toInt())
        FieldEncoder.writeI32(data, 96, scaled(config.tmpBatCUTPR, 10.0).toInt())
        FieldEncoder.writeI32(data, 100, scaled(config.tmpMosOT, 10.0).toInt())
        FieldEncoder.writeI32(data, 104, scaled(config.tmpMosOTPR, 10.0).toInt())
        FieldEncoder.writeU32(data, 108, config.cellCount)
        FieldEncoder.writeU32(data, 112, config.batChargeEn)
        FieldEncoder.writeU32(data, 116, config.batDischargeEn)
        FieldEncoder.writeU32(data, 120, config.balanEn)
        FieldEncoder.writeU32(data, 124, scaled(config.capBatCell, 1000.0))
        FieldEncoder.writeU32(data, 128, config.scpDelay)
        FieldEncoder.writeU32(data, 132, scaled(config.volStartBalan, 1000.0))

        for (i in 0 until 32) {
            FieldEncoder.writeU32(data, 136 + i * 4, scaled(config.cellConWireRes[i], 1000.0))
        }

        FieldEncoder.writeU32(data, 264, config.devAddr)
        FieldEncoder.writeU32(data, 268, config.dischrgPreChrgT)
        FieldEncoder.writeU32(data, 272, scaled(config.currentRange, 1000.0))
        FieldEncoder.writeBitmap(data, 276, config.switchStatus)
        FieldEncoder.writeI8(data, 278, config.tmpStartHeating)
        FieldEncoder.writeI8(data, 279, config.tmpStopHeating)
        FieldEncoder.writeU8(data, 280, config.timeSmartSleep)

        FieldEncoder.writeI8(data, 281, config.tmpBatDCHUT)
        FieldEncoder.writeI8(data, 282, config.tmpBatDCHUTPR)

        config.rawPayload?.takeIf { it.size == 293 }?.let { raw ->
            val original = ConfigParser.parse(raw)
            ConfigSchema.fields.take(34).forEachIndexed { index, field ->
                if (field.get(config) == field.get(original)) raw.copyInto(data, index * 4, index * 4, index * 4 + 4)
            }
            for (i in 0 until 32) {
                if (config.cellConWireRes[i] == original.cellConWireRes[i]) {
                    val offset = 136 + i * 4
                    raw.copyInto(data, offset, offset, offset + 4)
                }
            }
            ConfigSchema.fields.filter { it.name in listOf("devAddr", "dischrgPreChrgT", "currentRange") }
                .forEachIndexed { index, field ->
                    val offset = 264 + index * 4
                    if (field.get(config) == field.get(original)) raw.copyInto(data, offset, offset, offset + 4)
                }
        }
        val frame = ByteArray(FRAME_SIZE)
        System.arraycopy(HEADER_MAGIC, 0, frame, 0, HEADER_SIZE)
        frame[FRAME_CODE_OFFSET] = FrameCode.CONFIG_WRITE.code
        frame[COUNTER_OFFSET] = (counter and 0xFF).toByte()
        System.arraycopy(data, 0, frame, DATA_OFFSET, DATA_SIZE)
        Checksum.writeChecksum(frame)
        return frame
    }
}
