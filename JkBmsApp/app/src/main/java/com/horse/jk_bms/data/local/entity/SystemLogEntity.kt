package com.horse.jk_bms.data.local.entity

import androidx.room.Entity
import androidx.room.ColumnInfo
import androidx.room.Index
import androidx.room.PrimaryKey
import com.horse.jk_bms.model.BmsSystemLog

@Entity(tableName = "system_log", indices = [Index("timestamp"), Index("sessionId", "timestamp")])
data class SystemLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "''") val sessionId: String = "",
    @ColumnInfo(defaultValue = "''") val deviceId: String = "",
    val logCount: Long,
    val check: Int,
    val alarmLog: String,
)

fun BmsSystemLog.toEntity(): SystemLogEntity = SystemLogEntity(
    logCount = logCount,
    check = check,
    alarmLog = com.horse.jk_bms.data.local.Converters.fromByteArray(alarmLog),
)
