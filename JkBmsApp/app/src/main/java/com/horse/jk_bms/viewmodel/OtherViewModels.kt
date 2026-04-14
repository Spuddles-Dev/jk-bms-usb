package com.horse.jk_bms.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.horse.jk_bms.model.BmsFaultInfo
import com.horse.jk_bms.model.BmsRuntimeData
import com.horse.jk_bms.repository.BmsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CellsViewModel @Inject constructor(
    repository: BmsRepository,
) : ViewModel() {
    val runtimeData: StateFlow<BmsRuntimeData?> = repository.runtimeData
}

@HiltViewModel
class DeviceInfoViewModel @Inject constructor(
    private val repository: BmsRepository,
) : ViewModel() {
    val deviceInfo = repository.deviceInfo
}

@HiltViewModel
class FaultsViewModel @Inject constructor(
    private val repository: BmsRepository,
) : ViewModel() {
    val faultInfo: StateFlow<BmsFaultInfo?> = repository.faultInfo
}

@HiltViewModel
class LogsViewModel @Inject constructor(
    private val repository: BmsRepository,
) : ViewModel() {
    val systemLog = repository.systemLog
}
