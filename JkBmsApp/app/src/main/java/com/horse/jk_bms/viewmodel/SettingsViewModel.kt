package com.horse.jk_bms.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.horse.jk_bms.model.BmsConfig
import com.horse.jk_bms.protocol.ConfigFieldValidator
import com.horse.jk_bms.repository.BmsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsState(
    val config: BmsConfig? = null,
    val editConfig: BmsConfig? = null,
    val isWriting: Boolean = false,
    val writeSuccess: Boolean? = null,
    val error: String? = null,
) {
    val hasUnsavedChanges: Boolean
        get() = config != null && editConfig != null && config != editConfig

    val isValid: Boolean
        get() {
            val cfg = editConfig ?: return false
            return ConfigFieldValidator.validateAll(cfg).all { it.value }
        }
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: BmsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    val config: StateFlow<BmsConfig?> = repository.config

    init {
        viewModelScope.launch {
            repository.config.collect { cfg ->
                if (cfg != null) {
                    _state.value = _state.value.copy(
                        config = cfg,
                        editConfig = if (_state.value.editConfig == null) cfg else _state.value.editConfig,
                    )
                }
            }
        }
    }

    fun updateEditConfig(transform: (BmsConfig) -> BmsConfig) {
        val current = _state.value.editConfig ?: return
        _state.value = _state.value.copy(editConfig = transform(current))
    }

    fun resetEdits() {
        val original = _state.value.config ?: return
        _state.value = _state.value.copy(editConfig = original, writeSuccess = null, error = null)
    }

    fun writeConfig() {
        val editCfg = _state.value.editConfig ?: return
        if (!_state.value.isValid) return
        _state.value = _state.value.copy(isWriting = true, writeSuccess = null, error = null)
        viewModelScope.launch {
            val result = repository.writeConfig(editCfg)
            _state.value = _state.value.copy(
                isWriting = false,
                writeSuccess = result.isSuccess,
                error = if (result.isFailure) result.exceptionOrNull()?.message else null,
            )
        }
    }

    fun clearWriteStatus() {
        _state.value = _state.value.copy(writeSuccess = null, error = null)
    }
}
