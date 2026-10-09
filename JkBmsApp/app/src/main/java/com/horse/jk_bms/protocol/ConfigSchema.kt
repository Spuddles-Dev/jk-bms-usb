package com.horse.jk_bms.protocol

import com.horse.jk_bms.model.BmsConfig

class ConfigField(
    val name: String,
    val label: String,
    val unit: String,
    val min: Double,
    val max: Double,
    val integral: Boolean,
    val get: (BmsConfig) -> Double,
    val set: (BmsConfig, Double) -> BmsConfig,
) {
    fun accepts(value: Double): Boolean = value.isFinite() &&
        value >= min - (if (integral) 0.0 else 0.000001) &&
        value <= max + (if (integral) 0.0 else 0.000001) && (!integral || value % 1.0 == 0.0)
}

object ConfigSchema {
    val fields = listOf(
        ConfigField("volSmartSleep", "Vol. Smart Sleep", "V", 1.2, 4.4, false, { it.volSmartSleep.toDouble() }, { config, value -> config.copy(volSmartSleep = value.toFloat()) }),
        ConfigField("volCellUV", "Cell UVP", "V", 1.2, 4.4, false, { it.volCellUV.toDouble() }, { config, value -> config.copy(volCellUV = value.toFloat()) }),
        ConfigField("volCellUVPR", "Cell UVPR", "V", 1.2, 4.4, false, { it.volCellUVPR.toDouble() }, { config, value -> config.copy(volCellUVPR = value.toFloat()) }),
        ConfigField("volCellOV", "Cell OVP", "V", 1.2, 4.4, false, { it.volCellOV.toDouble() }, { config, value -> config.copy(volCellOV = value.toFloat()) }),
        ConfigField("volCellOVPR", "Cell OVPR", "V", 1.2, 4.4, false, { it.volCellOVPR.toDouble() }, { config, value -> config.copy(volCellOVPR = value.toFloat()) }),
        ConfigField("volBalanTrig", "Balance Trig. Volt.", "V", 0.003, 1.0, false, { it.volBalanTrig.toDouble() }, { config, value -> config.copy(volBalanTrig = value.toFloat()) }),
        ConfigField("volSOCP100", "SOC-100% Volt.", "V", 1.2, 4.4, false, { it.volSOCP100.toDouble() }, { config, value -> config.copy(volSOCP100 = value.toFloat()) }),
        ConfigField("volSOCP0", "SOC-0% Volt.", "V", 1.2, 4.4, false, { it.volSOCP0.toDouble() }, { config, value -> config.copy(volSOCP0 = value.toFloat()) }),
        ConfigField("volCellRCV", "Vol. Cell RCV", "V", 1.2, 4.4, false, { it.volCellRCV.toDouble() }, { config, value -> config.copy(volCellRCV = value.toFloat()) }),
        ConfigField("volCellRFV", "Vol. Cell RFV", "V", 1.2, 4.4, false, { it.volCellRFV.toDouble() }, { config, value -> config.copy(volCellRFV = value.toFloat()) }),
        ConfigField("volSysPwrOff", "Power Off Vol.", "V", 1.2, 4.4, false, { it.volSysPwrOff.toDouble() }, { config, value -> config.copy(volSysPwrOff = value.toFloat()) }),
        ConfigField("timBatCOC", "Continued Charge Curr.", "A", 1.0, 600.0, false, { it.timBatCOC.toDouble() }, { config, value -> config.copy(timBatCOC = value.toFloat()) }),
        ConfigField("timBatCOCPDly", "Charge OCP Delay", "s", 2.0, 600.0, true, { it.timBatCOCPDly.toDouble() }, { config, value -> config.copy(timBatCOCPDly = value.toLong()) }),
        ConfigField("timBatCOCPRDly", "Charge OCPR Time", "s", 2.0, 600.0, true, { it.timBatCOCPRDly.toDouble() }, { config, value -> config.copy(timBatCOCPRDly = value.toLong()) }),
        ConfigField("timBatDcOC", "Continued Discharge Curr.", "A", 1.0, 1200.0, false, { it.timBatDcOC.toDouble() }, { config, value -> config.copy(timBatDcOC = value.toFloat()) }),
        ConfigField("timBatDcOCPDly", "Discharge OCP Delay", "s", 2.0, 600.0, true, { it.timBatDcOCPDly.toDouble() }, { config, value -> config.copy(timBatDcOCPDly = value.toLong()) }),
        ConfigField("timBatDcOCPRDly", "Discharge OCPR Time", "s", 2.0, 600.0, true, { it.timBatDcOCPRDly.toDouble() }, { config, value -> config.copy(timBatDcOCPRDly = value.toLong()) }),
        ConfigField("timBatSCPRDly", "SCPR Time", "s", 2.0, 600.0, true, { it.timBatSCPRDly.toDouble() }, { config, value -> config.copy(timBatSCPRDly = value.toLong()) }),
        ConfigField("curBalanMax", "Max Balance Cur.", "A", 0.3, 15.0, false, { it.curBalanMax.toDouble() }, { config, value -> config.copy(curBalanMax = value.toFloat()) }),
        ConfigField("tmpBatCOT", "Charge OTP", "℃", 30.0, 80.0, false, { it.tmpBatCOT.toDouble() }, { config, value -> config.copy(tmpBatCOT = value.toFloat()) }),
        ConfigField("tmpBatCOTPR", "Charge OTPR", "℃", 30.0, 80.0, false, { it.tmpBatCOTPR.toDouble() }, { config, value -> config.copy(tmpBatCOTPR = value.toFloat()) }),
        ConfigField("tmpBatDcOT", "Discharge OTP", "℃", 30.0, 80.0, false, { it.tmpBatDcOT.toDouble() }, { config, value -> config.copy(tmpBatDcOT = value.toFloat()) }),
        ConfigField("tmpBatDcOTPR", "Discharge OTPR", "℃", 30.0, 80.0, false, { it.tmpBatDcOTPR.toDouble() }, { config, value -> config.copy(tmpBatDcOTPR = value.toFloat()) }),
        ConfigField("tmpBatCUT", "Charge UTP", "℃", -45.0, 20.0, false, { it.tmpBatCUT.toDouble() }, { config, value -> config.copy(tmpBatCUT = value.toFloat()) }),
        ConfigField("tmpBatCUTPR", "Charge UTPR", "℃", -45.0, 20.0, false, { it.tmpBatCUTPR.toDouble() }, { config, value -> config.copy(tmpBatCUTPR = value.toFloat()) }),
        ConfigField("tmpMosOT", "MOS OTP", "℃", 50.0, 110.0, false, { it.tmpMosOT.toDouble() }, { config, value -> config.copy(tmpMosOT = value.toFloat()) }),
        ConfigField("tmpMosOTPR", "MOS OTPR", "℃", 50.0, 110.0, false, { it.tmpMosOTPR.toDouble() }, { config, value -> config.copy(tmpMosOTPR = value.toFloat()) }),
        ConfigField("cellCount", "Cell Count", "", 2.0, 32.0, true, { it.cellCount.toDouble() }, { config, value -> config.copy(cellCount = value.toLong()) }),
        ConfigField("batChargeEn", "Charge Enabled", "", 0.0, 1.0, true, { it.batChargeEn.toDouble() }, { config, value -> config.copy(batChargeEn = value.toLong()) }),
        ConfigField("batDischargeEn", "Discharge Enabled", "", 0.0, 1.0, true, { it.batDischargeEn.toDouble() }, { config, value -> config.copy(batDischargeEn = value.toLong()) }),
        ConfigField("balanEn", "Balance Enabled", "", 0.0, 1.0, true, { it.balanEn.toDouble() }, { config, value -> config.copy(balanEn = value.toLong()) }),
        ConfigField("capBatCell", "Battery Capacity", "Ah", 2.0, 20000.0, false, { it.capBatCell.toDouble() }, { config, value -> config.copy(capBatCell = value.toFloat()) }),
        ConfigField("scpDelay", "SCP Delay", "μs", 0.0, 1000000.0, true, { it.scpDelay.toDouble() }, { config, value -> config.copy(scpDelay = value.toLong()) }),
        ConfigField("volStartBalan", "Start Balance Volt.", "V", 1.2, 4.25, false, { it.volStartBalan.toDouble() }, { config, value -> config.copy(volStartBalan = value.toFloat()) }),
        ConfigField("devAddr", "Device Addr.", "", 0.0, 65535.0, true, { it.devAddr.toDouble() }, { config, value -> config.copy(devAddr = value.toLong()) }),
        ConfigField("dischrgPreChrgT", "Dischrg. Pre. Chrg. T", "s", 0.0, 300.0, true, { it.dischrgPreChrgT.toDouble() }, { config, value -> config.copy(dischrgPreChrgT = value.toLong()) }),
        ConfigField("currentRange", "Current Range", "A", 100.0, 2000.0, false, { it.currentRange.toDouble() }, { config, value -> config.copy(currentRange = value.toFloat()) }),
        ConfigField("tmpStartHeating", "TMP Start Heating", "℃", -40.0, 100.0, true, { it.tmpStartHeating.toDouble() }, { config, value -> config.copy(tmpStartHeating = value.toInt()) }),
        ConfigField("tmpStopHeating", "TMP Stop Heating", "℃", -40.0, 100.0, true, { it.tmpStopHeating.toDouble() }, { config, value -> config.copy(tmpStopHeating = value.toInt()) }),
        ConfigField("timeSmartSleep", "Time Smart Sleep", "h", 1.0, 100.0, true, { it.timeSmartSleep.toDouble() }, { config, value -> config.copy(timeSmartSleep = value.toInt()) }),
        ConfigField("tmpBatDCHUT", "Discharge UTP", "℃", -40.0, 100.0, true, { it.tmpBatDCHUT.toDouble() }, { config, value -> config.copy(tmpBatDCHUT = value.toInt()) }),
        ConfigField("tmpBatDCHUTPR", "Discharge UTPR", "℃", -40.0, 100.0, true, { it.tmpBatDCHUTPR.toDouble() }, { config, value -> config.copy(tmpBatDCHUTPR = value.toInt()) }),
    )
}
