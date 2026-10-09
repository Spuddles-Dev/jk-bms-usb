package com.horse.jk_bms.data.backup

import android.content.Context
import com.google.gson.Gson
import com.horse.jk_bms.model.BmsConfig
import com.horse.jk_bms.model.BmsDeviceInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class ConfigBackup(val version: Int, val deviceId: String, val hardware: String, val firmware: String,
    val savedAt: Long, val config: BmsConfig, val name: String? = null)

data class StoredBackup(val id: String, val name: String, val savedAt: Long, val config: BmsConfig)

@Singleton
class ConfigBackupStore @Inject constructor(@ApplicationContext context: Context) {
    private val directory = File(context.filesDir, "config-backups")
    private val gson = Gson()

    suspend fun save(device: BmsDeviceInfo, config: BmsConfig, name: String = ""): File = withContext(Dispatchers.IO) {
        require(device.deviceSN.isNotBlank()) { "A BMS serial number is required for a backup" }
        require(name.length <= 80) { "Backup name must be at most 80 characters" }
        directory.mkdirs()
        val backup = ConfigBackup(1, device.deviceSN, device.hardwareVersion, device.softwareVersion,
            System.currentTimeMillis(), config, name.trim().ifBlank { "Configuration" })
        val file = File(directory, "${UUID.randomUUID()}.json")
        try { file.writeText(gson.toJson(backup)) } catch (error: Exception) { file.delete(); throw error }
        directory.listFiles()?.sortedByDescending { it.lastModified() }?.drop(50)?.forEach { it.delete() }
        file
    }

    suspend fun list(device: BmsDeviceInfo): List<StoredBackup> = withContext(Dispatchers.IO) {
        directory.listFiles()?.filter { it.extension == "json" }?.mapNotNull { file ->
            val backup = runCatching { gson.fromJson(file.readText(), ConfigBackup::class.java) }.getOrNull()
            if (backup != null && backup.version == 1 && backup.deviceId == device.deviceSN &&
                backup.hardware == device.hardwareVersion && backup.firmware == device.softwareVersion) {
                StoredBackup(file.nameWithoutExtension, backup.name ?: "Configuration", backup.savedAt, backup.config)
            } else null
        }?.sortedByDescending { it.savedAt } ?: emptyList()
    }

    suspend fun latest(device: BmsDeviceInfo): BmsConfig? = list(device).firstOrNull()?.config

    suspend fun load(device: BmsDeviceInfo, id: String): BmsConfig? = list(device).firstOrNull { it.id == id }?.config
}
