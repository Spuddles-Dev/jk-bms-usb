package com.horse.jk_bms.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.horse.jk_bms.data.local.entity.FaultRecordEntity

@Dao
interface FaultDao {
    @Query("SELECT MAX(id) FROM fault_record")
    suspend fun latestId(): Long?

    @Query("SELECT * FROM fault_record WHERE id > :afterId AND id <= :throughId ORDER BY id LIMIT 500")
    suspend fun page(afterId: Long, throughId: Long): List<FaultRecordEntity>

    @Query("SELECT * FROM fault_record WHERE sessionId = :session ORDER BY timestamp DESC LIMIT 200")
    suspend fun forSession(session: String): List<FaultRecordEntity>
    @Insert
    suspend fun insert(entity: FaultRecordEntity): Long

    @Insert
    suspend fun insertAll(entities: List<FaultRecordEntity>): List<Long>

    @Query("SELECT * FROM fault_record ORDER BY timestamp DESC")
    suspend fun getAll(): List<FaultRecordEntity>

    @Query("DELETE FROM fault_record WHERE timestamp < :before")
    suspend fun deleteOlderThan(before: Long): Int
}
