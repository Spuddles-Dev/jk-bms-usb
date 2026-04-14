package com.horse.jk_bms.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.horse.jk_bms.model.FaultRecord

@Entity(tableName = "fault_record")
data class FaultRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val rtcCount: Long,
    val logCode: Int,
    val switchSta: String,
    val maxVolCellNo: Int,
    val minVolCellNo: Int,
    val volCellMax: Float,
    val volCellMin: Float,
    val volBat: Float,
    val curBat: Float,
    val socCapRemain: Float,
    val socFullChargeCap: Float,
    val maxTemp: Int,
    val minTemp: Int,
    val tempMos: Int,
    val heatCurrent: Float,
)

fun FaultRecord.toEntity(): FaultRecordEntity = FaultRecordEntity(
    rtcCount = rtcCount,
    logCode = logCode,
    switchSta = com.horse.jk_bms.data.local.Converters.fromBooleanArray(switchSta),
    maxVolCellNo = maxVolCellNo,
    minVolCellNo = minVolCellNo,
    volCellMax = volCellMax,
    volCellMin = volCellMin,
    volBat = volBat,
    curBat = curBat,
    socCapRemain = socCapRemain,
    socFullChargeCap = socFullChargeCap,
    maxTemp = maxTemp,
    minTemp = minTemp,
    tempMos = tempMos,
    heatCurrent = heatCurrent,
)
