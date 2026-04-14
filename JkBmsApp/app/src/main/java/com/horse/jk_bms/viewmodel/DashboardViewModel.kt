package com.horse.jk_bms.viewmodel

import androidx.lifecycle.ViewModel
import com.horse.jk_bms.model.BmsRuntimeData
import com.horse.jk_bms.repository.BmsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

data class DashboardState(
    val runtimeData: BmsRuntimeData? = null,
    val isConnected: Boolean = false,
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    repository: BmsRepository,
) : ViewModel() {

    val runtimeData: StateFlow<BmsRuntimeData?> = repository.runtimeData
    val isConnected: StateFlow<Boolean> = repository.isConnected
    val deviceInfo = repository.deviceInfo
    val lastDataTimestamp: StateFlow<Long> = repository.lastDataTimestamp
}
