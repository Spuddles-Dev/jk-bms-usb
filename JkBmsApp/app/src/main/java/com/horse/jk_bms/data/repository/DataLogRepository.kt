package com.horse.jk_bms.data.repository

import com.horse.jk_bms.data.local.dao.ConfigDao
import com.horse.jk_bms.data.local.dao.DeviceInfoDao
import com.horse.jk_bms.data.local.dao.FaultDao
import com.horse.jk_bms.data.local.dao.RuntimeDataDao
import com.horse.jk_bms.data.local.dao.SystemLogDao
import com.horse.jk_bms.data.local.entity.ConfigEntity
import com.horse.jk_bms.data.local.entity.DeviceInfoEntity
import com.horse.jk_bms.data.local.entity.FaultRecordEntity
import com.horse.jk_bms.data.local.entity.RuntimeDataEntity
import com.horse.jk_bms.data.local.entity.SystemLogEntity
import com.horse.jk_bms.data.local.entity.toEntity
import com.horse.jk_bms.model.BmsConfig
import com.horse.jk_bms.model.BmsDeviceInfo
import com.horse.jk_bms.model.BmsFaultInfo
import com.horse.jk_bms.model.BmsRuntimeData
import com.horse.jk_bms.model.BmsSystemLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataLogRepository @Inject constructor(
    private val runtimeDataDao: RuntimeDataDao,
    private val configDao: ConfigDao,
    private val deviceInfoDao: DeviceInfoDao,
    private val faultDao: FaultDao,
    private val systemLogDao: SystemLogDao,
) {
    suspend fun logRuntimeData(data: BmsRuntimeData) = withContext(Dispatchers.IO) {
        runtimeDataDao.insert(data.toEntity())
    }

    suspend fun logConfig(config: BmsConfig) = withContext(Dispatchers.IO) {
        configDao.insert(config.toEntity())
    }

    suspend fun logDeviceInfo(info: BmsDeviceInfo) = withContext(Dispatchers.IO) {
        deviceInfoDao.insert(info.toEntity())
    }

    suspend fun logFaultInfo(faultInfo: BmsFaultInfo) = withContext(Dispatchers.IO) {
        val entities = faultInfo.records.map { it.toEntity() }
        if (entities.isNotEmpty()) {
            faultDao.insertAll(entities)
        }
    }

    suspend fun logSystemLog(log: BmsSystemLog) = withContext(Dispatchers.IO) {
        systemLogDao.insert(log.toEntity())
    }

    suspend fun getRuntimeDataRange(from: Long): List<RuntimeDataEntity> = withContext(Dispatchers.IO) {
        runtimeDataDao.getRange(from)
    }

    suspend fun getLatestRuntimeData(): RuntimeDataEntity? = withContext(Dispatchers.IO) {
        runtimeDataDao.getLatest()
    }

    suspend fun getLatestConfig(): ConfigEntity? = withContext(Dispatchers.IO) {
        configDao.getLatest()
    }

    suspend fun getLatestDeviceInfo(): DeviceInfoEntity? = withContext(Dispatchers.IO) {
        deviceInfoDao.getLatest()
    }

    suspend fun getAllFaults(): List<FaultRecordEntity> = withContext(Dispatchers.IO) {
        faultDao.getAll()
    }

    suspend fun getAllConfigs(): List<ConfigEntity> = withContext(Dispatchers.IO) {
        configDao.getAll()
    }

    suspend fun getAllDeviceInfo(): List<DeviceInfoEntity> = withContext(Dispatchers.IO) {
        deviceInfoDao.getAll()
    }

    suspend fun getAllSystemLogs(): List<SystemLogEntity> = withContext(Dispatchers.IO) {
        systemLogDao.getAll()
    }

    suspend fun getRuntimeDataCount(): Int = withContext(Dispatchers.IO) {
        runtimeDataDao.count()
    }

    suspend fun cleanup() = withContext(Dispatchers.IO) {
        val sevenDaysAgo = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L
        runtimeDataDao.deleteOlderThan(sevenDaysAgo)
        systemLogDao.deleteOlderThan(sevenDaysAgo)
    }
}
