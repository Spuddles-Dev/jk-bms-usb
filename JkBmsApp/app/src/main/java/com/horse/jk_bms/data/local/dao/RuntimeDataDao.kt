package com.horse.jk_bms.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.horse.jk_bms.data.local.entity.RuntimeDataEntity

@Dao
interface RuntimeDataDao {
    @Insert
    suspend fun insert(entity: RuntimeDataEntity): Long

    @Query("SELECT * FROM runtime_data WHERE timestamp >= :from ORDER BY timestamp ASC")
    suspend fun getRange(from: Long): List<RuntimeDataEntity>

    @Query("SELECT * FROM runtime_data ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatest(): RuntimeDataEntity?

    @Query("DELETE FROM runtime_data WHERE timestamp < :before")
    suspend fun deleteOlderThan(before: Long): Int

    @Query("SELECT COUNT(*) FROM runtime_data")
    suspend fun count(): Int
}
