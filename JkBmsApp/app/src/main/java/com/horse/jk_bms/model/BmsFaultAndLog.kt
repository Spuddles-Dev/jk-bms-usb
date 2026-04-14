package com.horse.jk_bms.model

data class FaultRecord(
    val rtcCount: Long = 0,
    val logCode: Int = 0,
    val switchSta: BooleanArray = BooleanArray(4),
    val maxVolCellNo: Int = 0,
    val minVolCellNo: Int = 0,
    val volCellMax: Float = 0f,
    val volCellMin: Float = 0f,
    val volBat: Float = 0f,
    val curBat: Float = 0f,
    val socCapRemain: Float = 0f,
    val socFullChargeCap: Float = 0f,
    val maxTemp: Int = 0,
    val minTemp: Int = 0,
    val tempMos: Int = 0,
    val heatCurrent: Float = 0f,
) {
    val timestamp: Long get() = rtcCount
    override fun equals(other: Any?): Boolean = this === other
    override fun hashCode(): Int = System.identityHashCode(this)
}

data class BmsFaultInfo(
    val beginIndex: Int = 0,
    val count: Int = 0,
    val records: List<FaultRecord> = emptyList(),
)

data class BmsSystemLog(
    val logCount: Long = 0,
    val check: Int = 0,
    val alarmLog: ByteArray = ByteArray(0),
) {
    override fun equals(other: Any?): Boolean = this === other
    override fun hashCode(): Int = System.identityHashCode(this)
}
