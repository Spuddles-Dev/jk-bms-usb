package com.horse.jk_bms.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import com.horse.jk_bms.model.BmsRuntimeData
import com.horse.jk_bms.repository.BmsRepository
import com.horse.jk_bms.monitoring.BackgroundMonitorController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

data class DashboardState(
    val runtimeData: BmsRuntimeData? = null,
    val isConnected: Boolean = false,
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: BmsRepository,
    private val background: BackgroundMonitorController,
) : ViewModel() {

    val runtimeData: StateFlow<BmsRuntimeData?> = repository.runtimeData
    val isConnected: StateFlow<Boolean> = repository.isConnected
    val deviceInfo = repository.deviceInfo
    val lastDataTimestamp: StateFlow<Long> = repository.lastDataTimestamp
    val dataAgeMs = repository.dataAgeMs
    val sessionState = repository.sessionState
    val loggingError = repository.loggingError
    val backgroundEnabled = background.enabled

    fun enableBackground(): Result<Unit> = background.start()
    fun disableBackground() = background.stopBackground()

    fun disconnect(onDisconnected: () -> Unit) {
        viewModelScope.launch {
            background.stopBackground()
            repository.disconnect()
            onDisconnected()
        }
    }
}
