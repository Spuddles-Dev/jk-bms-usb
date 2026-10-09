package com.horse.jk_bms.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.horse.jk_bms.data.local.entity.SessionEntity
import com.horse.jk_bms.data.local.entity.WriteAuditEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insert(session: SessionEntity)
    @Query("SELECT * FROM sessions ORDER BY startedAt DESC") fun observe(): Flow<List<SessionEntity>>
    @Query("UPDATE sessions SET endedAt = :time WHERE id = :id") suspend fun end(id: String, time: Long)
    @Query("UPDATE sessions SET deviceId = :deviceId, firmware = :firmware, name = :name WHERE id = :id")
    suspend fun identify(id: String, deviceId: String, firmware: String, name: String)
    @Query("UPDATE sessions SET name = :name WHERE deviceId = :deviceId")
    suspend fun rename(deviceId: String, name: String)
    @Query("SELECT name FROM sessions WHERE deviceId = :deviceId ORDER BY startedAt DESC LIMIT 1")
    suspend fun nameForDevice(deviceId: String): String?
    @Query("DELETE FROM sessions WHERE startedAt < :before AND id != :active")
    suspend fun cleanup(before: Long, active: String)
    @Insert suspend fun audit(entry: WriteAuditEntity)
    @Query("DELETE FROM write_audit WHERE timestamp < :before") suspend fun cleanupAudit(before: Long)
}
