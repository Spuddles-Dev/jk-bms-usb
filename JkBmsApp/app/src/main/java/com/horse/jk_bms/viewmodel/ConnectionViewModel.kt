package com.horse.jk_bms.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.horse.jk_bms.repository.BmsRepository
import com.horse.jk_bms.usb.UsbDeviceInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ConnectionState(
    val devices: List<UsbDeviceInfo> = emptyList(),
    val isScanning: Boolean = false,
    val isConnecting: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class ConnectionViewModel @Inject constructor(
    application: Application,
    private val repository: BmsRepository,
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(ConnectionState())
    val state: StateFlow<ConnectionState> = _state

    init {
        refreshDevices()
    }

    fun refreshDevices() {
        _state.value = _state.value.copy(isScanning = true, error = null)
        try {
            val devices = repository.listDevices()
            _state.value = _state.value.copy(devices = devices, isScanning = false)
        } catch (e: Exception) {
            _state.value = _state.value.copy(isScanning = false, error = e.message)
        }
    }

    fun connect(deviceInfo: UsbDeviceInfo, onConnected: () -> Unit) {
        _state.value = _state.value.copy(isConnecting = true, error = null)
        viewModelScope.launch {
            val result = repository.connect(deviceInfo)
            _state.value = _state.value.copy(isConnecting = false)
            if (result.isSuccess) {
                onConnected()
            } else {
                _state.value = _state.value.copy(
                    error = result.exceptionOrNull()?.message ?: "Connection failed"
                )
            }
        }
    }
}
