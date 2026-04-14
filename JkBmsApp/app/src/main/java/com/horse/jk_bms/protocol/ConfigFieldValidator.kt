package com.horse.jk_bms.protocol

object ConfigFieldValidator {

    data class FieldRule(
        val min: Float,
        val max: Float,
    )

    private val voltageRules = mapOf(
        "volSmartSleep" to FieldRule(0f, 5f),
        "volCellUV" to FieldRule(0f, 5f),
        "volCellUVPR" to FieldRule(0f, 5f),
        "volCellOV" to FieldRule(0f, 5f),
        "volCellOVPR" to FieldRule(0f, 5f),
        "volBalanTrig" to FieldRule(0f, 5f),
        "volSOCP100" to FieldRule(0f, 5f),
        "volSOCP0" to FieldRule(0f, 5f),
        "volCellRCV" to FieldRule(0f, 5f),
        "volCellRFV" to FieldRule(0f, 5f),
        "volSysPwrOff" to FieldRule(0f, 5f),
        "volStartBalan" to FieldRule(0f, 5f),
    )

    private val currentRules = mapOf(
        "timBatCOC" to FieldRule(0f, 500f),
        "timBatDcOC" to FieldRule(0f, 500f),
        "curBalanMax" to FieldRule(0f, 10f),
        "currentRange" to FieldRule(0f, 500f),
    )

    private val tempRules = mapOf(
        "tmpBatCOT" to FieldRule(-40f, 100f),
        "tmpBatCOTPR" to FieldRule(-40f, 100f),
        "tmpBatDcOT" to FieldRule(-40f, 100f),
        "tmpBatDcOTPR" to FieldRule(-40f, 100f),
        "tmpBatCUT" to FieldRule(-40f, 100f),
        "tmpBatCUTPR" to FieldRule(-40f, 100f),
        "tmpMosOT" to FieldRule(-40f, 120f),
        "tmpMosOTPR" to FieldRule(-40f, 120f),
        "tmpStartHeating" to FieldRule(-40f, 60f),
        "tmpStopHeating" to FieldRule(-40f, 60f),
        "tmpBatDCHUT" to FieldRule(-40f, 100f),
        "tmpBatDCHUTPR" to FieldRule(-40f, 100f),
    )

    private val capacityRules = mapOf(
        "capBatCell" to FieldRule(0f, 10000f),
    )

    private val countRules = mapOf(
        "cellCount" to FieldRule(1f, 32f),
    )

    private val delayRules = mapOf(
        "timBatCOCPDly" to FieldRule(0f, 65535f),
        "timBatCOCPRDly" to FieldRule(0f, 65535f),
        "timBatDcOCPDly" to FieldRule(0f, 65535f),
        "timBatDcOCPRDly" to FieldRule(0f, 65535f),
        "timBatSCPRDly" to FieldRule(0f, 65535f),
        "scpDelay" to FieldRule(0f, 65535f),
    )

    private val allRules = voltageRules + currentRules + tempRules + capacityRules + countRules + delayRules

    fun validate(fieldName: String, value: Float): Boolean {
        val rule = allRules[fieldName] ?: return true
        return value in rule.min..rule.max
    }

    fun getRange(fieldName: String): Pair<Float, Float>? = allRules[fieldName]?.let { it.min to it.max }

    fun validateAll(config: com.horse.jk_bms.model.BmsConfig): Map<String, Boolean> {
        val fields = mapOf(
            "volSmartSleep" to config.volSmartSleep,
            "volCellUV" to config.volCellUV,
            "volCellUVPR" to config.volCellUVPR,
            "volCellOV" to config.volCellOV,
            "volCellOVPR" to config.volCellOVPR,
            "volBalanTrig" to config.volBalanTrig,
            "volSOCP100" to config.volSOCP100,
            "volSOCP0" to config.volSOCP0,
            "volStartBalan" to config.volStartBalan,
            "timBatCOC" to config.timBatCOC,
            "timBatDcOC" to config.timBatDcOC,
            "curBalanMax" to config.curBalanMax,
            "currentRange" to config.currentRange,
            "tmpBatCOT" to config.tmpBatCOT,
            "tmpBatDcOT" to config.tmpBatDcOT,
            "tmpBatCUT" to config.tmpBatCUT,
            "tmpMosOT" to config.tmpMosOT,
            "capBatCell" to config.capBatCell,
            "cellCount" to config.cellCount.toFloat(),
        )
        return fields.mapValues { (name, value) -> validate(name, value) }
    }
}
