package com.horse.jk_bms.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.horse.jk_bms.data.local.entity.FaultRecordEntity

@Dao
interface FaultDao {
    @Insert
    suspend fun insert(entity: FaultRecordEntity): Long

    @Insert
    suspend fun insertAll(entities: List<FaultRecordEntity>): List<Long>

    @Query("SELECT * FROM fault_record ORDER BY timestamp DESC")
    suspend fun getAll(): List<FaultRecordEntity>

    @Query("DELETE FROM fault_record WHERE timestamp < :before")
    suspend fun deleteOlderThan(before: Long): Int
}
