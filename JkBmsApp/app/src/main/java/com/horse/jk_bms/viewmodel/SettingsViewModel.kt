package com.horse.jk_bms.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.horse.jk_bms.model.BmsConfig
import com.horse.jk_bms.repository.BmsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsState(
    val config: BmsConfig? = null,
    val editConfig: BmsConfig? = null,
    val isWriting: Boolean = false,
    val writeSuccess: Boolean? = null,
    val error: String? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: BmsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state

    val config: StateFlow<BmsConfig?> = repository.config

    init {
        viewModelScope.launch {
            repository.config.collect { cfg ->
                if (cfg != null) {
                    _state.value = _state.value.copy(config = cfg, editConfig = cfg)
                }
            }
        }
    }

    fun updateEditConfig(config: BmsConfig) {
        _state.value = _state.value.copy(editConfig = config)
    }

    fun writeConfig() {
        val editCfg = _state.value.editConfig ?: return
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
