package com.horse.jk_bms.protocol

import com.horse.jk_bms.model.BmsConfig

object ConfigParser {

    fun parse(data: ByteArray): BmsConfig {
        require(data.size >= 293) { "Data must be at least 293 bytes, got ${data.size}" }

        val volSmartSleep = FieldDecoder.readU32(data, 0) * 0.001f
        val volCellUV = FieldDecoder.readU32(data, 4) * 0.001f
        val volCellUVPR = FieldDecoder.readU32(data, 8) * 0.001f
        val volCellOV = FieldDecoder.readU32(data, 12) * 0.001f
        val volCellOVPR = FieldDecoder.readU32(data, 16) * 0.001f
        val volBalanTrig = FieldDecoder.readU32(data, 20) * 0.001f
        val volSOCP100 = FieldDecoder.readU32(data, 24) * 0.001f
        val volSOCP0 = FieldDecoder.readU32(data, 28) * 0.001f
        val volCellRCV = FieldDecoder.readU32(data, 32) * 0.001f
        val volCellRFV = FieldDecoder.readU32(data, 36) * 0.001f
        val volSysPwrOff = FieldDecoder.readU32(data, 40) * 0.001f
        val timBatCOC = FieldDecoder.readU32(data, 44) * 0.001f
        val timBatCOCPDly = FieldDecoder.readU32(data, 48)
        val timBatCOCPRDly = FieldDecoder.readU32(data, 52)
        val timBatDcOC = FieldDecoder.readU32(data, 56) * 0.001f
        val timBatDcOCPDly = FieldDecoder.readU32(data, 60)
        val timBatDcOCPRDly = FieldDecoder.readU32(data, 64)
        val timBatSCPRDly = FieldDecoder.readU32(data, 68)
        val curBalanMax = FieldDecoder.readU32(data, 72) * 0.001f
        val tmpBatCOT = FieldDecoder.readI32(data, 76) * 0.1f
        val tmpBatCOTPR = FieldDecoder.readI32(data, 80) * 0.1f
        val tmpBatDcOT = FieldDecoder.readI32(data, 84) * 0.1f
        val tmpBatDcOTPR = FieldDecoder.readI32(data, 88) * 0.1f
        val tmpBatCUT = FieldDecoder.readI32(data, 92) * 0.1f
        val tmpBatCUTPR = FieldDecoder.readI32(data, 96) * 0.1f
        val tmpMosOT = FieldDecoder.readI32(data, 100) * 0.1f
        val tmpMosOTPR = FieldDecoder.readI32(data, 104) * 0.1f
        val cellCount = FieldDecoder.readU32(data, 108)
        val batChargeEn = FieldDecoder.readU32(data, 112)
        val batDischargeEn = FieldDecoder.readU32(data, 116)
        val balanEn = FieldDecoder.readU32(data, 120)
        val capBatCell = FieldDecoder.readU32(data, 124) * 0.001f
        val scpDelay = FieldDecoder.readU32(data, 128)
        val volStartBalan = FieldDecoder.readU32(data, 132) * 0.001f
        val cellConWireRes = FloatArray(32) { i ->
            FieldDecoder.readU32(data, 136 + i * 4) * 0.001f
        }
        val devAddr = FieldDecoder.readU32(data, 264)
        val dischrgPreChrgT = FieldDecoder.readU32(data, 268)
        val currentRange = FieldDecoder.readU32(data, 272) * 0.001f
        val switchStatus = FieldDecoder.readBitmap(data, 276, 16)
        val tmpStartHeating = FieldDecoder.readI8(data, 278)
        val tmpStopHeating = FieldDecoder.readI8(data, 279)
        val timeSmartSleep = FieldDecoder.readU8(data, 280)
        val enableFlags = FieldDecoder.readBytes(data, 281, 9)
        val tmpBatDCHUT = FieldDecoder.readI8(data, 290)
        val tmpBatDCHUTPR = FieldDecoder.readI8(data, 291)

        return BmsConfig(
            volSmartSleep = volSmartSleep,
            volCellUV = volCellUV,
            volCellUVPR = volCellUVPR,
            volCellOV = volCellOV,
            volCellOVPR = volCellOVPR,
            volBalanTrig = volBalanTrig,
            volSOCP100 = volSOCP100,
            volSOCP0 = volSOCP0,
            volCellRCV = volCellRCV,
            volCellRFV = volCellRFV,
            volSysPwrOff = volSysPwrOff,
            timBatCOC = timBatCOC,
            timBatCOCPDly = timBatCOCPDly,
            timBatCOCPRDly = timBatCOCPRDly,
            timBatDcOC = timBatDcOC,
            timBatDcOCPDly = timBatDcOCPDly,
            timBatDcOCPRDly = timBatDcOCPRDly,
            timBatSCPRDly = timBatSCPRDly,
            curBalanMax = curBalanMax,
            tmpBatCOT = tmpBatCOT,
            tmpBatCOTPR = tmpBatCOTPR,
            tmpBatDcOT = tmpBatDcOT,
            tmpBatDcOTPR = tmpBatDcOTPR,
            tmpBatCUT = tmpBatCUT,
            tmpBatCUTPR = tmpBatCUTPR,
            tmpMosOT = tmpMosOT,
            tmpMosOTPR = tmpMosOTPR,
            cellCount = cellCount,
            batChargeEn = batChargeEn,
            batDischargeEn = batDischargeEn,
            balanEn = balanEn,
            capBatCell = capBatCell,
            scpDelay = scpDelay,
            volStartBalan = volStartBalan,
            cellConWireRes = cellConWireRes,
            devAddr = devAddr,
            dischrgPreChrgT = dischrgPreChrgT,
            currentRange = currentRange,
            switchStatus = switchStatus,
            tmpStartHeating = tmpStartHeating,
            tmpStopHeating = tmpStopHeating,
            timeSmartSleep = timeSmartSleep,
            enableFlags = enableFlags,
            tmpBatDCHUT = tmpBatDCHUT,
            tmpBatDCHUTPR = tmpBatDCHUTPR,
        )
    }
}
