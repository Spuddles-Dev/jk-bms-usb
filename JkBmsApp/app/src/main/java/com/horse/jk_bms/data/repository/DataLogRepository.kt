package com.horse.jk_bms.data.repository

import androidx.room.withTransaction
import com.google.gson.Gson
import com.horse.jk_bms.data.local.JkBmsDatabase
import com.horse.jk_bms.data.local.entity.ConfigEntity
import com.horse.jk_bms.data.local.entity.DeviceInfoEntity
import com.horse.jk_bms.data.local.entity.FaultRecordEntity
import com.horse.jk_bms.data.local.entity.RuntimeDataEntity
import com.horse.jk_bms.data.local.entity.SessionEntity
import com.horse.jk_bms.data.local.entity.SystemLogEntity
import com.horse.jk_bms.data.local.entity.WriteAuditEntity
import com.horse.jk_bms.data.local.entity.toEntity
import com.horse.jk_bms.model.BmsConfig
import com.horse.jk_bms.model.BmsDeviceInfo
import com.horse.jk_bms.model.BmsFaultInfo
import com.horse.jk_bms.model.BmsRuntimeData
import com.horse.jk_bms.model.BmsSystemLog
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataLogRepository @Inject constructor(private val db: JkBmsDatabase) {
    private val mutex = Mutex()
    private var sessionId = ""
    private var deviceId = ""
    private var lastConfig: ConfigEntity? = null
    private var lastDevice: DeviceInfoEntity? = null
    private var lastLog: SystemLogEntity? = null
    private var pendingSession: SessionEntity? = null
    private val seenFaults = LinkedHashSet<String>()
    private val gson = Gson()
    val sessions = db.sessionDao().observe()

    suspend fun startSession(id: String, adapterIdentity: String) = mutex.withLock {
        sessionId = id
        deviceId = adapterIdentity
        lastConfig = null
        lastDevice = null
        lastLog = null
        seenFaults.clear()
        pendingSession = SessionEntity(id, deviceId, "Battery", System.currentTimeMillis())
        ensureSession()
        cleanupInternal()
    }

    suspend fun endSession() = mutex.withLock {
        if (sessionId.isNotEmpty()) db.sessionDao().end(sessionId, System.currentTimeMillis())
        sessionId = ""
        deviceId = ""
    }

    suspend fun logRuntimeData(data: BmsRuntimeData, observedAt: Long = System.currentTimeMillis()) = mutex.withLock {
        if (sessionId.isNotEmpty()) {
            ensureSession()
            db.runtimeDataDao().insert(data.toEntity().copy(timestamp = observedAt, sessionId = sessionId, deviceId = deviceId))
        }
    }

    suspend fun logConfig(config: BmsConfig) = mutex.withLock {
        if (sessionId.isEmpty()) return@withLock
        ensureSession()
        val comparable = config.toEntity().copy(id = 0, timestamp = 0, sessionId = "", deviceId = "")
        if (comparable != lastConfig) {
            db.configDao().insert(config.toEntity().copy(sessionId = sessionId, deviceId = deviceId))
            lastConfig = comparable
        }
    }

    suspend fun logDeviceInfo(info: BmsDeviceInfo) = mutex.withLock {
        if (sessionId.isEmpty()) return@withLock
        ensureSession()
        val sanitized = info.copy(bluetoothPwd = "", settingPassword = "")
        if (info.deviceSN.isNotBlank()) deviceId = "bms:${info.deviceSN}"
        val name = db.sessionDao().nameForDevice(deviceId) ?: "Battery ${info.deviceSN.ifBlank { "USB" }}"
        db.sessionDao().identify(sessionId, deviceId, info.softwareVersion, name)
        val comparable = sanitized.toEntity().copy(id = 0, timestamp = 0, sessionId = "", deviceId = "")
        if (comparable != lastDevice) {
            db.deviceInfoDao().insert(sanitized.toEntity().copy(sessionId = sessionId, deviceId = deviceId))
            lastDevice = comparable
        }
    }

    suspend fun logFaultInfo(info: BmsFaultInfo) = mutex.withLock {
        if (sessionId.isEmpty()) return@withLock
        ensureSession()
        val fresh = info.records.mapIndexedNotNull { index, record ->
            val key = "${info.beginIndex + index}:${gson.toJson(record)}"
            if (key in seenFaults) null else key to record.toEntity().copy(sessionId = sessionId, deviceId = deviceId, eventKey = key)
        }
        db.faultDao().insertAll(fresh.map { it.second })
        fresh.forEach { seenFaults.add(it.first) }
        while (seenFaults.size > 2048) seenFaults.remove(seenFaults.first())
    }

    suspend fun logSystemLog(log: BmsSystemLog) = mutex.withLock {
        if (sessionId.isEmpty()) return@withLock
        ensureSession()
        val comparable = log.toEntity().copy(id = 0, timestamp = 0, sessionId = "", deviceId = "")
        if (comparable != lastLog) {
            db.systemLogDao().insert(log.toEntity().copy(sessionId = sessionId, deviceId = deviceId))
            lastLog = comparable
        }
    }

    suspend fun logWrite(before: BmsConfig, after: BmsConfig, outcome: String) = mutex.withLock {
        ensureSession()
        db.sessionDao().audit(WriteAuditEntity(
            timestamp = System.currentTimeMillis(), sessionId = sessionId, deviceId = deviceId,
            beforeJson = gson.toJson(before), afterJson = gson.toJson(after), outcome = outcome,
        ))
    }

    suspend fun rename(device: String, name: String) {
        require(name.isNotBlank() && name.length <= 80)
        db.sessionDao().rename(device, name.trim())
    }

    suspend fun getRuntimeDataRange(from: Long): List<RuntimeDataEntity> = db.runtimeDataDao().getRange(from)
    suspend fun getLatestRuntimeData(): RuntimeDataEntity? = db.runtimeDataDao().getLatest()
    suspend fun getLatestConfig(): ConfigEntity? = db.configDao().getLatest()
    suspend fun getLatestDeviceInfo(): DeviceInfoEntity? = db.deviceInfoDao().getLatest()
    suspend fun getAllFaults(): List<FaultRecordEntity> = db.faultDao().getAll()
    suspend fun getAllConfigs(): List<ConfigEntity> = db.configDao().getAll()
    suspend fun getAllDeviceInfo(): List<DeviceInfoEntity> = db.deviceInfoDao().getAll()
    suspend fun getAllSystemLogs(): List<SystemLogEntity> = db.systemLogDao().getAll()
    suspend fun getRuntimeDataCount(): Int = db.runtimeDataDao().count()
    suspend fun runtimePage(from: Long, throughId: Long, afterId: Long, session: String? = null): List<RuntimeDataEntity> =
        db.runtimeDataDao().page(from, throughId, afterId, session)
    suspend fun latestRuntimeId(): Long = db.runtimeDataDao().latestId() ?: 0
    suspend fun faultPage(afterId: Long, throughId: Long): List<FaultRecordEntity> = db.faultDao().page(afterId, throughId)
    suspend fun latestFaultId(): Long = db.faultDao().latestId() ?: 0
    suspend fun sessionFaults(session: String): List<FaultRecordEntity> = db.faultDao().forSession(session)

    suspend fun cleanup() = mutex.withLock { cleanupInternal() }

    private suspend fun ensureSession() {
        pendingSession?.let {
            db.sessionDao().insert(it)
            pendingSession = null
        }
    }

    private suspend fun cleanupInternal() = db.withTransaction {
        val cutoff = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L
        db.runtimeDataDao().deleteOlderThan(cutoff)
        db.configDao().deleteOlderThan(cutoff)
        db.deviceInfoDao().deleteOlderThan(cutoff)
        db.faultDao().deleteOlderThan(cutoff)
        db.systemLogDao().deleteOlderThan(cutoff)
        db.sessionDao().cleanup(cutoff, sessionId)
        db.sessionDao().cleanupAudit(cutoff)
    }
}
