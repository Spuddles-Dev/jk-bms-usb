package com.horse.jk_bms.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "sessions", indices = [Index("startedAt")])
data class SessionEntity(
    @PrimaryKey val id: String,
    val deviceId: String,
    val name: String,
    val startedAt: Long,
    val endedAt: Long? = null,
    val firmware: String = "",
)

@Entity(tableName = "write_audit", indices = [Index("timestamp")])
data class WriteAuditEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val sessionId: String,
    val deviceId: String,
    val beforeJson: String,
    val afterJson: String,
    val outcome: String,
)
