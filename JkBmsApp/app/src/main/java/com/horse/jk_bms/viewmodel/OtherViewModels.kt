package com.horse.jk_bms.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.horse.jk_bms.model.BmsFaultInfo
import com.horse.jk_bms.model.BmsRuntimeData
import com.horse.jk_bms.repository.BmsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private val mutableLoading = MutableStateFlow(false)
    val loading = mutableLoading.asStateFlow()
    private val mutableError = MutableStateFlow<String?>(null)
    val error = mutableError.asStateFlow()

    init { refresh() }

    fun refresh() {
        if (mutableLoading.value) return
        viewModelScope.launch {
            mutableLoading.value = true
            mutableError.value = null
            try {
                val result = repository.refreshSystemLog()
                mutableError.value = result.exceptionOrNull()?.message
            } finally { mutableLoading.value = false }
        }
    }
}
