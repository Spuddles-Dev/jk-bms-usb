package com.horse.jk_bms.protocol

import com.horse.jk_bms.model.BmsDeviceInfo

object DeviceInfoParser {

    fun parse(data: ByteArray): BmsDeviceInfo {
        require(data.size >= 293) { "Data must be at least 293 bytes, got ${data.size}" }

        val manuDeviceID = FieldDecoder.readString(data, 0, 15)
        val maxCells = FieldDecoder.readU8(data, 15)
        val hardwareVersion = FieldDecoder.readString(data, 16, 8)
        val softwareVersion = FieldDecoder.readString(data, 24, 8)
        val oddRunTime = FieldDecoder.readU32(data, 32)
        val pwrOnTimes = FieldDecoder.readU32(data, 36)
        val bluetoothName = FieldDecoder.readString(data, 40, 16)
        val bluetoothPwd = FieldDecoder.readString(data, 56, 16)
        val manufactureDate = FieldDecoder.readString(data, 72, 8)
        val deviceSN = FieldDecoder.readString(data, 80, 16)
        val userData = FieldDecoder.readString(data, 96, 16)
        val settingPassword = FieldDecoder.readString(data, 112, 16)
        val userData2 = FieldDecoder.readString(data, 128, 16)
        val cmdSupportFlags = FieldDecoder.readBytes(data, 144, 32)
        val agencyId = FieldDecoder.readU16(data, 176)
        val uart1ProtoNo = FieldDecoder.readU8(data, 178)
        val canProtoNo = FieldDecoder.readU8(data, 179)
        val uart1ProtoEnabled = FieldDecoder.readBytes(data, 180, 4)
        val hardwareOption = FieldDecoder.readString(data, 184, 12)
        val canProtoEnabled = FieldDecoder.readBytes(data, 196, 4)
        val uart2ProtoNo = FieldDecoder.readU8(data, 212)
        val uart2ProtoEnabled = FieldDecoder.readBytes(data, 213, 4)
        val lcdBuzzerTrigger = FieldDecoder.readU8(data, 228)
        val dry1Trigger = FieldDecoder.readU8(data, 229)
        val dry2Trigger = FieldDecoder.readU8(data, 230)
        val uartMPTLVer = FieldDecoder.readU8(data, 231)
        val lcdBuzzerTriggerVal = FieldDecoder.readI32(data, 232)
        val lcdBuzzerReleaseVal = FieldDecoder.readI32(data, 236)
        val dry1TriggerVal = FieldDecoder.readI32(data, 240)
        val dry1ReleaseVal = FieldDecoder.readI32(data, 244)
        val dry2TriggerVal = FieldDecoder.readI32(data, 248)
        val dry2ReleaseVal = FieldDecoder.readI32(data, 252)
        val dataStoredPeriod = FieldDecoder.readU32(data, 256)
        val rcvTime = FieldDecoder.readU8(data, 260) * 0.1f
        val rfvTime = FieldDecoder.readU8(data, 261) * 0.1f
        val canMPTLVer = FieldDecoder.readU8(data, 262)
        val emergencyTime = FieldDecoder.readU8(data, 263)
        val uart3ProtoNo = FieldDecoder.readU8(data, 264)
        val uart3ProtoEnabled = FieldDecoder.readBytes(data, 265, 7)
        val reBulkSOC = FieldDecoder.readU8(data, 272)
        val enableFlags = FieldDecoder.readBytes(data, 281, 11)
        val protocolVer = FieldDecoder.readU8(data, 292)

        return BmsDeviceInfo(
            manuDeviceID = manuDeviceID,
            maxCells = maxCells,
            hardwareVersion = hardwareVersion,
            softwareVersion = softwareVersion,
            oddRunTime = oddRunTime,
            pwrOnTimes = pwrOnTimes,
            bluetoothName = bluetoothName,
            bluetoothPwd = bluetoothPwd,
            manufactureDate = manufactureDate,
            deviceSN = deviceSN,
            userData = userData,
            settingPassword = settingPassword,
            userData2 = userData2,
            cmdSupportFlags = cmdSupportFlags,
            agencyId = agencyId,
            uart1ProtoNo = uart1ProtoNo,
            canProtoNo = canProtoNo,
            uart1ProtoEnabled = uart1ProtoEnabled,
            hardwareOption = hardwareOption,
            canProtoEnabled = canProtoEnabled,
            uart2ProtoNo = uart2ProtoNo,
            uart2ProtoEnabled = uart2ProtoEnabled,
            lcdBuzzerTrigger = lcdBuzzerTrigger,
            dry1Trigger = dry1Trigger,
            dry2Trigger = dry2Trigger,
            uartMPTLVer = uartMPTLVer,
            lcdBuzzerTriggerVal = lcdBuzzerTriggerVal,
            lcdBuzzerReleaseVal = lcdBuzzerReleaseVal,
            dry1TriggerVal = dry1TriggerVal,
            dry1ReleaseVal = dry1ReleaseVal,
            dry2TriggerVal = dry2TriggerVal,
            dry2ReleaseVal = dry2ReleaseVal,
            dataStoredPeriod = dataStoredPeriod,
            rcvTime = rcvTime,
            rfvTime = rfvTime,
            canMPTLVer = canMPTLVer,
            emergencyTime = emergencyTime,
            uart3ProtoNo = uart3ProtoNo,
            uart3ProtoEnabled = uart3ProtoEnabled,
            reBulkSOC = reBulkSOC,
            enableFlags = enableFlags,
            protocolVer = protocolVer,
        )
    }
}
