package com.horse.jk_bms.protocol

import com.horse.jk_bms.model.BmsRuntimeData

object RuntimeDataParser {

    fun parse(data: ByteArray): BmsRuntimeData {
        require(data.size >= 293) { "Data must be at least 293 bytes, got ${data.size}" }

        val cellVoltages = FloatArray(32) { i ->
            FieldDecoder.readU16(data, i * 2) * 0.001f
        }

        val cellStatus = FieldDecoder.readBitmap(data, 64, 32)

        val cellVolAve = FieldDecoder.readU16(data, 68) * 0.001f
        val maxVoltDelta = FieldDecoder.readU16(data, 70) * 0.001f
        val celMaxVol = FieldDecoder.readU8(data, 72)
        val celMinVol = FieldDecoder.readU8(data, 73)

        val cellWireRes = FloatArray(32) { i ->
            FieldDecoder.readU16(data, 74 + i * 2) * 0.001f
        }

        val tempMos = FieldDecoder.readI16(data, 138) * 0.1f
        val cellWireResStat = FieldDecoder.readBitmap(data, 140, 32)
        val batVol = FieldDecoder.readI32(data, 144) * 0.001f
        val batWatt = FieldDecoder.readU32(data, 148) * 0.001f
        val batCurrent = FieldDecoder.readI32(data, 152) * 0.001f
        val batTemp1 = FieldDecoder.readI16(data, 156) * 0.1f
        val batTemp2 = FieldDecoder.readI16(data, 158) * 0.1f
        val sysAlarm = FieldDecoder.readBitmap(data, 160, 32)
        val equCurrent = FieldDecoder.readI16(data, 164) * 0.001f
        val equStatus = FieldDecoder.readU8(data, 166)
        val soc = FieldDecoder.readU8(data, 167)
        val socCapabilityRemain = FieldDecoder.readU32(data, 168) * 0.001f
        val socFullChargeCapacity = FieldDecoder.readU32(data, 172) * 0.001f
        val socCycleCount = FieldDecoder.readU32(data, 176)
        val socCycleCapacity = FieldDecoder.readU32(data, 180) * 0.001f
        val soh = FieldDecoder.readU8(data, 184)
        val preDischarge = FieldDecoder.readU8(data, 185) != 0
        val userAlarm = FieldDecoder.readU16(data, 186)
        val runtime = FieldDecoder.readU32(data, 188)
        val chargeStatus = FieldDecoder.readU8(data, 192) != 0
        val dischargeStatus = FieldDecoder.readU8(data, 193) != 0
        val userAlarm2 = FieldDecoder.readBitmap(data, 194, 16)
        val timeDcOCPR = FieldDecoder.readU16(data, 196)
        val timeDcSCPR = FieldDecoder.readU16(data, 198)
        val timeCOCPR = FieldDecoder.readU16(data, 200)
        val timeCSCPR = FieldDecoder.readU16(data, 202)
        val timeUVPR = FieldDecoder.readU16(data, 204)
        val timeOVPR = FieldDecoder.readU16(data, 206)
        val tempSensorAbsent = FieldDecoder.readBitmap(data, 208, 8)
        val heatingStatus = FieldDecoder.readU8(data, 209) != 0
        val timeEmerg = FieldDecoder.readU16(data, 212)
        val dischrgCurCorrect = FieldDecoder.readU16(data, 214)
        val volChargCur = FieldDecoder.readU16(data, 216) * 0.001f
        val volDischargCur = FieldDecoder.readU16(data, 218) * 0.001f
        val batVolCorrect = FieldDecoder.readF32(data, 220)
        val chargPWM = FieldDecoder.readU16(data, 224)
        val dischargPWM = FieldDecoder.readU16(data, 226)
        val totalBatVol = FieldDecoder.readU16(data, 228) * 0.01f
        val heatCurrent = FieldDecoder.readI16(data, 230) * 0.001f
        val accStatus = FieldDecoder.readU8(data, 233) != 0
        val specialChargerSta = FieldDecoder.readU8(data, 234) != 0
        val startupFlag = FieldDecoder.readU8(data, 235)
        val volC = FieldDecoder.readU8(data, 236).toFloat()
        val mcuid = FieldDecoder.readU8(data, 237)
        val chargePlugged = FieldDecoder.readU8(data, 239) != 0
        val sysRunTicks = FieldDecoder.readU32(data, 240) * 0.1f
        val pvdTrigTimestamps = FieldDecoder.readU32(data, 244) * 0.1f
        val batTemp3 = FieldDecoder.readI16(data, 248) * 0.1f
        val batTemp4 = FieldDecoder.readI16(data, 250) * 0.1f
        val batTemp5 = FieldDecoder.readI16(data, 252) * 0.1f
        val chrgCurCorrect = FieldDecoder.readU16(data, 254)
        val rtcCounter = FieldDecoder.readU32(data, 256)
        val detailLogsCount = FieldDecoder.readU32(data, 260)
        val timeEnterSleep = FieldDecoder.readU32(data, 264)
        val pclModule = FieldDecoder.readU8(data, 268) != 0
        val batteryType = FieldDecoder.readU8(data, 269)
        val chargeStatusTime = FieldDecoder.readU16(data, 272)
        val chargeStatus2 = FieldDecoder.readU8(data, 274)
        val switchStatus = FieldDecoder.readBitmap(data, 275, 3)
        val enableFlags = FieldDecoder.readBytes(data, 281, 12)

        return BmsRuntimeData(
            cellVoltages = cellVoltages,
            cellStatus = cellStatus,
            cellVolAve = cellVolAve,
            maxVoltDelta = maxVoltDelta,
            celMaxVol = celMaxVol,
            celMinVol = celMinVol,
            cellWireRes = cellWireRes,
            tempMos = tempMos,
            batVol = batVol,
            batWatt = batWatt,
            batCurrent = batCurrent,
            batTemp1 = batTemp1,
            batTemp2 = batTemp2,
            sysAlarm = sysAlarm,
            equCurrent = equCurrent,
            equStatus = equStatus,
            soc = soc,
            socCapabilityRemain = socCapabilityRemain,
            socFullChargeCapacity = socFullChargeCapacity,
            socCycleCount = socCycleCount,
            socCycleCapacity = socCycleCapacity,
            soh = soh,
            preDischarge = preDischarge,
            userAlarm = userAlarm,
            runtime = runtime,
            chargeStatus = chargeStatus,
            dischargeStatus = dischargeStatus,
            userAlarm2 = userAlarm2,
            timeDcOCPR = timeDcOCPR,
            timeDcSCPR = timeDcSCPR,
            timeCOCPR = timeCOCPR,
            timeCSCPR = timeCSCPR,
            timeUVPR = timeUVPR,
            timeOVPR = timeOVPR,
            tempSensorAbsent = tempSensorAbsent,
            heatingStatus = heatingStatus,
            timeEmerg = timeEmerg,
            dischrgCurCorrect = dischrgCurCorrect,
            volChargCur = volChargCur,
            volDischargCur = volDischargCur,
            batVolCorrect = batVolCorrect,
            chargPWM = chargPWM,
            dischargPWM = dischargPWM,
            totalBatVol = totalBatVol,
            heatCurrent = heatCurrent,
            accStatus = accStatus,
            specialChargerSta = specialChargerSta,
            startupFlag = startupFlag,
            volC = volC,
            mcuid = mcuid,
            chargePlugged = chargePlugged,
            sysRunTicks = sysRunTicks,
            pvdTrigTimestamps = pvdTrigTimestamps,
            batTemp3 = batTemp3,
            batTemp4 = batTemp4,
            batTemp5 = batTemp5,
            chrgCurCorrect = chrgCurCorrect,
            rtcCounter = rtcCounter,
            detailLogsCount = detailLogsCount,
            timeEnterSleep = timeEnterSleep,
            pclModule = pclModule,
            batteryType = batteryType,
            chargeStatusTime = chargeStatusTime,
            chargeStatus2 = chargeStatus2,
            switchStatus = switchStatus,
        )
    }
}
