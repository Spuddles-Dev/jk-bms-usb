package com.horse.jk_bms.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.horse.jk_bms.data.local.entity.ConfigEntity

@Dao
interface ConfigDao {
    @Insert
    suspend fun insert(entity: ConfigEntity): Long

    @Query("SELECT * FROM config ORDER BY timestamp DESC")
    suspend fun getAll(): List<ConfigEntity>

    @Query("SELECT * FROM config ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatest(): ConfigEntity?

    @Query("DELETE FROM config WHERE timestamp < :before")
    suspend fun deleteOlderThan(before: Long): Int
}
