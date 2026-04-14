package com.horse.jk_bms.model

data class BmsRuntimeData(
    val cellVoltages: FloatArray = FloatArray(32),
    val cellStatus: BooleanArray = BooleanArray(32),
    val cellVolAve: Float = 0f,
    val maxVoltDelta: Float = 0f,
    val celMaxVol: Int = 0,
    val celMinVol: Int = 0,
    val cellWireRes: FloatArray = FloatArray(32),
    val tempMos: Float = 0f,
    val batVol: Float = 0f,
    val batWatt: Float = 0f,
    val batCurrent: Float = 0f,
    val batTemp1: Float = 0f,
    val batTemp2: Float = 0f,
    val sysAlarm: BooleanArray = BooleanArray(32),
    val equCurrent: Float = 0f,
    val equStatus: Int = 0,
    val soc: Int = 0,
    val socCapabilityRemain: Float = 0f,
    val socFullChargeCapacity: Float = 0f,
    val socCycleCount: Long = 0,
    val socCycleCapacity: Float = 0f,
    val soh: Int = 0,
    val preDischarge: Boolean = false,
    val userAlarm: Int = 0,
    val runtime: Long = 0,
    val chargeStatus: Boolean = false,
    val dischargeStatus: Boolean = false,
    val userAlarm2: BooleanArray = BooleanArray(16),
    val timeDcOCPR: Int = 0,
    val timeDcSCPR: Int = 0,
    val timeCOCPR: Int = 0,
    val timeCSCPR: Int = 0,
    val timeUVPR: Int = 0,
    val timeOVPR: Int = 0,
    val tempSensorAbsent: BooleanArray = BooleanArray(8),
    val heatingStatus: Boolean = false,
    val timeEmerg: Int = 0,
    val dischrgCurCorrect: Int = 0,
    val volChargCur: Float = 0f,
    val volDischargCur: Float = 0f,
    val batVolCorrect: Float = 0f,
    val chargPWM: Int = 0,
    val dischargPWM: Int = 0,
    val totalBatVol: Float = 0f,
    val heatCurrent: Float = 0f,
    val accStatus: Boolean = false,
    val specialChargerSta: Boolean = false,
    val startupFlag: Int = 0,
    val volC: Float = 0f,
    val mcuid: Int = 0,
    val chargePlugged: Boolean = false,
    val sysRunTicks: Float = 0f,
    val pvdTrigTimestamps: Float = 0f,
    val batTemp3: Float = 0f,
    val batTemp4: Float = 0f,
    val batTemp5: Float = 0f,
    val chrgCurCorrect: Int = 0,
    val rtcCounter: Long = 0,
    val detailLogsCount: Long = 0,
    val timeEnterSleep: Long = 0,
    val pclModule: Boolean = false,
    val batteryType: Int = 0,
    val chargeStatusTime: Int = 0,
    val chargeStatus2: Int = 0,
    val switchStatus: BooleanArray = BooleanArray(3),
) {
    val activeCellCount: Int get() = cellVoltages.count { it > 0f }
    val isCharging: Boolean get() = batCurrent > 0f
    val isDischarging: Boolean get() = batCurrent < 0f
    val batteryTypeLabel: String get() = when (batteryType) {
        0 -> "LFP"
        1 -> "Li-ion"
        2 -> "LTO"
        else -> "Unknown"
    }

    override fun equals(other: Any?): Boolean = this === other
    override fun hashCode(): Int = System.identityHashCode(this)
}
