package com.horse.jk_bms.protocol

import com.horse.jk_bms.model.BmsConfig

object ConfigFieldValidator {
    fun validate(fieldName: String, value: Float): Boolean =
        ConfigSchema.fields.find { it.name == fieldName }?.accepts(value.toDouble()) ?: false

    fun getRange(fieldName: String): Pair<Float, Float>? =
        ConfigSchema.fields.find { it.name == fieldName }?.let { it.min.toFloat() to it.max.toFloat() }

    fun validateAll(config: BmsConfig): Map<String, Boolean> = buildMap {
        ConfigSchema.fields.forEach { put(it.name, it.accepts(it.get(config))) }
        put("cellConWireRes", config.cellConWireRes.size == 32 &&
            config.cellConWireRes.all { it.isFinite() && it in 0f..2000f })
        put("switchStatus", config.switchStatus.size == 16)
        put("enableFlags", config.enableFlags.size == 9)
        put("voltageRecovery", config.volCellUV < config.volCellUVPR &&
            config.volCellUVPR < config.volCellOVPR && config.volCellOVPR < config.volCellOV)
        put("socVoltages", config.volSOCP0 < config.volSOCP100)
        put("chargeTemperatureRecovery", config.tmpBatCOTPR < config.tmpBatCOT && config.tmpBatCUTPR > config.tmpBatCUT)
        put("dischargeTemperatureRecovery", config.tmpBatDcOTPR < config.tmpBatDcOT &&
            config.tmpBatDCHUTPR > config.tmpBatDCHUT)
        put("mosTemperatureRecovery", config.tmpMosOTPR < config.tmpMosOT)
        put("heatingTemperatureRecovery", config.tmpStartHeating < config.tmpStopHeating)
    }

    fun requireValid(config: BmsConfig) {
        val invalid = validateAll(config).filterValues { !it }.keys
        require(invalid.isEmpty()) { "Invalid configuration: ${invalid.joinToString()}" }
    }
}
