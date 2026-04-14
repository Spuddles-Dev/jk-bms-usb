package com.horse.jk_bms.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.horse.jk_bms.data.local.entity.DeviceInfoEntity

@Dao
interface DeviceInfoDao {
    @Insert
    suspend fun insert(entity: DeviceInfoEntity): Long

    @Query("SELECT * FROM device_info ORDER BY timestamp DESC")
    suspend fun getAll(): List<DeviceInfoEntity>

    @Query("SELECT * FROM device_info ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatest(): DeviceInfoEntity?
}
