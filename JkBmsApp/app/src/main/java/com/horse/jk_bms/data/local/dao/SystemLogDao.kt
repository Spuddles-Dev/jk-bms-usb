package com.horse.jk_bms.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.horse.jk_bms.data.local.entity.SystemLogEntity

@Dao
interface SystemLogDao {
    @Insert
    suspend fun insert(entity: SystemLogEntity): Long

    @Query("SELECT * FROM system_log ORDER BY timestamp DESC")
    suspend fun getAll(): List<SystemLogEntity>

    @Query("DELETE FROM system_log WHERE timestamp < :before")
    suspend fun deleteOlderThan(before: Long): Int
}
