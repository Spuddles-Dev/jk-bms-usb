package com.horse.jk_bms.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.horse.jk_bms.model.BmsSystemLog

@Entity(tableName = "system_log")
data class SystemLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val logCount: Long,
    val check: Int,
    val alarmLog: String,
)

fun BmsSystemLog.toEntity(): SystemLogEntity = SystemLogEntity(
    logCount = logCount,
    check = check,
    alarmLog = com.horse.jk_bms.data.local.Converters.fromByteArray(alarmLog),
)
