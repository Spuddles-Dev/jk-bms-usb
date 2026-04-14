package com.horse.jk_bms.protocol

import com.horse.jk_bms.model.BmsFaultInfo
import com.horse.jk_bms.model.BmsSystemLog
import com.horse.jk_bms.model.FaultRecord

object FaultInfoParser {

    fun parse(data: ByteArray): BmsFaultInfo {
        require(data.size >= 293) { "Data must be at least 293 bytes, got ${data.size}" }

        val beginIndex = FieldDecoder.readU16(data, 0)
        val count = FieldDecoder.readU8(data, 2)

        val records = mutableListOf<FaultRecord>()
        if (count > 0) {
            records.add(parseRecord(data, 3))
        }
        for (i in 1 until count) {
            val offset = 3 + i * 24
            if (offset + 24 <= 291) {
                records.add(parseRecord(data, offset))
            }
        }

        return BmsFaultInfo(
            beginIndex = beginIndex,
            count = count,
            records = records,
        )
    }

    private fun parseRecord(data: ByteArray, offset: Int): FaultRecord {
        return FaultRecord(
            rtcCount = FieldDecoder.readU32(data, offset),
            logCode = FieldDecoder.readU8(data, offset + 4),
            switchSta = FieldDecoder.readBitmap(data, offset + 5, 4),
            maxVolCellNo = FieldDecoder.readU8(data, offset + 6),
            minVolCellNo = FieldDecoder.readU8(data, offset + 7),
            volCellMax = FieldDecoder.readU16(data, offset + 8) * 0.001f,
            volCellMin = FieldDecoder.readU16(data, offset + 10) * 0.001f,
            volBat = FieldDecoder.readU16(data, offset + 12) * 0.01f,
            curBat = FieldDecoder.readU16(data, offset + 14) * 0.1f,
            socCapRemain = FieldDecoder.readU16(data, offset + 16) * 0.1f,
            socFullChargeCap = FieldDecoder.readU16(data, offset + 18) * 0.1f,
            maxTemp = FieldDecoder.readU8(data, offset + 20),
            minTemp = FieldDecoder.readU8(data, offset + 21),
            tempMos = FieldDecoder.readU8(data, offset + 22),
            heatCurrent = FieldDecoder.readU8(data, offset + 23) * 0.1f,
        )
    }
}

object SystemLogParser {

    fun parse(data: ByteArray): BmsSystemLog {
        require(data.size >= 293) { "Data must be at least 293 bytes, got ${data.size}" }

        val logCount = FieldDecoder.readU32(data, 0)
        val check = FieldDecoder.readU8(data, 4)
        val alarmLog = FieldDecoder.readBytes(data, 5, 250)

        return BmsSystemLog(
            logCount = logCount,
            check = check,
            alarmLog = alarmLog,
        )
    }
}
