package com.horse.jk_bms.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.horse.jk_bms.BuildConfig
import com.horse.jk_bms.data.backup.StoredBackup
import com.horse.jk_bms.model.BmsConfig
import com.horse.jk_bms.model.BmsDeviceInfo
import com.horse.jk_bms.protocol.ConfigFieldValidator
import com.horse.jk_bms.protocol.ConfigSchema
import com.horse.jk_bms.repository.BmsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

data class SettingsState(
    val config: BmsConfig? = null,
    val editConfig: BmsConfig? = null,
    val inputs: Map<String, String> = emptyMap(),
    val dirty: Set<String> = emptySet(),
    val conflicts: Set<String> = emptySet(),
    val inputErrors: Map<String, String> = emptyMap(),
    val isWriting: Boolean = false,
    val writeSuccess: Boolean? = null,
    val error: String? = null,
    val backupMessage: String? = null,
    val backups: List<StoredBackup> = emptyList(),
) {
    val hasUnsavedChanges: Boolean get() = dirty.isNotEmpty()
    val isValid: Boolean get() = inputErrors.isEmpty() && conflicts.isEmpty() &&
        editConfig?.let { ConfigFieldValidator.validateAll(it).values.all { valid -> valid } } == true
}

fun parseConfigNumber(input: String): Double? {
    val trimmed = input.trim()
    if (trimmed.count { it == ',' } > 1 || (',' in trimmed && '.' in trimmed)) return null
    return trimmed.replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() }
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: BmsRepository,
    private val backups: com.horse.jk_bms.data.backup.ConfigBackupStore,
) : ViewModel() {
    private val mutableState = MutableStateFlow(SettingsState())
    val state = mutableState.asStateFlow()
    private var baseline: BmsConfig? = null
    private var backupArrays: BmsConfig? = null
    val writesEnabled: Boolean get() = BuildConfig.CONFIG_WRITES_VERIFIED

    init {
        viewModelScope.launch {
            repository.config.collect { incoming ->
                if (incoming == null) {
                    baseline = null
                    backupArrays = null
                    mutableState.value = SettingsState()
                } else {
                    val previous = baseline
                    val dirty = mutableState.value.dirty
                    val conflicts = ConfigSchema.fields.filter {
                        it.name in dirty && previous != null && it.get(previous) != it.get(incoming)
                    }.map { it.name }.toMutableSet()
                    if ("arrays" in dirty && previous != null &&
                        (!previous.cellConWireRes.contentEquals(incoming.cellConWireRes) ||
                            !previous.switchStatus.contentEquals(incoming.switchStatus))) conflicts.add("arrays")
                    val inputs = mutableState.value.inputs.toMutableMap()
                    ConfigSchema.fields.filter { it.name !in dirty }.forEach { inputs[it.name] = format(it.name, incoming) }
                    baseline = if (previous == null) incoming else {
                        var merged: BmsConfig = previous
                        ConfigSchema.fields.filter { it.name !in dirty }.forEach { merged = it.set(merged, it.get(incoming)) }
                        merged
                    }
                    mutableState.value = mutableState.value.copy(config = incoming, inputs = inputs, conflicts = conflicts)
                    rebuild()
                    refreshBackups()
                }
            }
        }
    }

    fun updateInput(name: String, input: String) {
        val current = mutableState.value
        val remote = current.config ?: return
        val dirty = current.dirty.toMutableSet().apply {
            if (input == format(name, remote)) remove(name) else add(name)
        }
        mutableState.value = current.copy(inputs = current.inputs + (name to input), dirty = dirty, writeSuccess = null)
        rebuild()
    }

    fun resetEdits() {
        val remote = mutableState.value.config ?: return
        baseline = remote
        backupArrays = null
        mutableState.value = SettingsState(config = remote, editConfig = remote,
            inputs = ConfigSchema.fields.associate { it.name to format(it.name, remote) },
            backups = mutableState.value.backups)
    }

    private fun rebuild() {
        val current = mutableState.value
        var edited = current.config ?: return
        val errors = mutableMapOf<String, String>()
        current.dirty.filter { it != "arrays" }.forEach { name ->
            val field = ConfigSchema.fields.first { it.name == name }
            val value = parseConfigNumber(current.inputs[name].orEmpty())
            if (value == null || !field.accepts(value)) {
                errors[name] = "Enter ${field.min}–${field.max}${if (field.integral) " as a whole number" else ""}"
            } else {
                edited = field.set(edited, value)
            }
        }
        backupArrays?.let { backup ->
            edited = edited.copy(cellConWireRes = backup.cellConWireRes.copyOf(), switchStatus = backup.switchStatus.copyOf(),
                rawPayload = backup.rawPayload?.copyOf())
        }
        mutableState.value = current.copy(editConfig = edited, inputErrors = errors,
            conflicts = current.conflicts.intersect(current.dirty))
    }

    fun writeConfig() {
        val current = mutableState.value
        if (!current.isValid || current.isWriting || !writesEnabled) return
        val edited = current.editConfig ?: return
        val remote = current.config ?: return
        mutableState.value = current.copy(isWriting = true, writeSuccess = null, error = null)
        viewModelScope.launch {
            try {
                val result = repository.writeConfig(edited, remote)
                if (result.isSuccess) resetEdits()
                mutableState.value = mutableState.value.copy(writeSuccess = result.isSuccess,
                    error = result.exceptionOrNull()?.message)
            } catch (error: CancellationException) {
                throw error
            } finally { mutableState.value = mutableState.value.copy(isWriting = false) }
        }
    }

    fun refreshBackups() {
        val device = repository.deviceInfo.value ?: return
        viewModelScope.launch {
            try {
                val saved = backups.list(device)
                if (isCurrentDevice(device)) mutableState.value = mutableState.value.copy(backups = saved)
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                mutableState.value = mutableState.value.copy(error = error.message)
            }
        }
    }

    fun saveBackup(name: String = "") {
        val config = mutableState.value.config ?: return
        val device = repository.deviceInfo.value ?: return
        viewModelScope.launch {
            try {
                backups.save(device, config, name)
                mutableState.value = mutableState.value.copy(backupMessage = "Configuration backup saved")
                refreshBackups()
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                mutableState.value = mutableState.value.copy(error = error.message)
            }
        }
    }

    fun loadLatestBackup() = loadBackup(null)

    fun loadBackup(id: String?) {
        val device = repository.deviceInfo.value ?: return
        viewModelScope.launch {
            try {
                val restored = (if (id == null) backups.latest(device) else backups.load(device, id))
                    ?: throw IllegalStateException("No matching backup")
                require(isCurrentDevice(device)) { "The connected device changed; select a backup again" }
                val remote = mutableState.value.config ?: return@launch
                backupArrays = restored
                ConfigSchema.fields.forEach { updateInput(it.name, format(it.name, restored)) }
                val arraysChanged = !restored.cellConWireRes.contentEquals(remote.cellConWireRes) ||
                    !restored.switchStatus.contentEquals(remote.switchStatus)
                mutableState.value = mutableState.value.copy(dirty = (mutableState.value.dirty - "arrays") +
                    if (arraysChanged) setOf("arrays") else emptySet())
                rebuild()
                mutableState.value = mutableState.value.copy(backupMessage = "Backup loaded for comparison; review changes before writing")
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                mutableState.value = mutableState.value.copy(error = error.message)
            }
        }
    }

    private fun isCurrentDevice(device: BmsDeviceInfo): Boolean {
        val current = repository.deviceInfo.value ?: return false
        return current.deviceSN == device.deviceSN && current.hardwareVersion == device.hardwareVersion &&
            current.softwareVersion == device.softwareVersion
    }

    private fun format(name: String, config: BmsConfig): String {
        val field = ConfigSchema.fields.first { it.name == name }
        return if (field.integral) field.get(config).toLong().toString() else String.format(Locale.US, "%.3f", field.get(config))
    }
}
