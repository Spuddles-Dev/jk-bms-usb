package com.horse.jk_bms.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.horse.jk_bms.data.local.dao.ConfigDao
import com.horse.jk_bms.data.local.dao.DeviceInfoDao
import com.horse.jk_bms.data.local.dao.FaultDao
import com.horse.jk_bms.data.local.dao.RuntimeDataDao
import com.horse.jk_bms.data.local.dao.SystemLogDao
import com.horse.jk_bms.data.local.dao.SessionDao
import com.horse.jk_bms.data.local.entity.SessionEntity
import com.horse.jk_bms.data.local.entity.WriteAuditEntity
import com.horse.jk_bms.data.local.entity.ConfigEntity
import com.horse.jk_bms.data.local.entity.DeviceInfoEntity
import com.horse.jk_bms.data.local.entity.FaultRecordEntity
import com.horse.jk_bms.data.local.entity.RuntimeDataEntity
import com.horse.jk_bms.data.local.entity.SystemLogEntity

@Database(
    entities = [
        RuntimeDataEntity::class,
        ConfigEntity::class,
        DeviceInfoEntity::class,
        FaultRecordEntity::class,
        SystemLogEntity::class,
        SessionEntity::class,
        WriteAuditEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class JkBmsDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    abstract fun runtimeDataDao(): RuntimeDataDao
    abstract fun configDao(): ConfigDao
    abstract fun deviceInfoDao(): DeviceInfoDao
    abstract fun faultDao(): FaultDao
    abstract fun systemLogDao(): SystemLogDao
}
