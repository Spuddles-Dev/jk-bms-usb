package com.horse.jk_bms.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.horse.jk_bms.repository.BmsRepository
import com.horse.jk_bms.usb.UsbDeviceInfo
import com.horse.jk_bms.usb.UsbEventReceiver
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private val repository: BmsRepository,
    usbEvents: UsbEventReceiver,
) : ViewModel() {
    private val mutableState = MutableStateFlow(ConnectionState())
    val state = mutableState.asStateFlow()

    init {
        refreshDevices()
        viewModelScope.launch { usbEvents.events.collect { refreshDevices() } }
    }

    fun refreshDevices() {
        try {
            mutableState.value = mutableState.value.copy(devices = repository.listDevices(), error = null)
        } catch (error: Exception) {
            mutableState.value = mutableState.value.copy(error = error.message)
        }
    }

    fun connect(info: UsbDeviceInfo, onConnected: () -> Unit) {
        if (mutableState.value.isConnecting) return
        mutableState.value = mutableState.value.copy(isConnecting = true, error = null)
        viewModelScope.launch {
            try {
                val result = repository.connect(info)
                mutableState.value = mutableState.value.copy(error = result.exceptionOrNull()?.message)
                if (result.isSuccess) onConnected()
            } catch (error: CancellationException) {
                throw error
            } finally {
                mutableState.value = mutableState.value.copy(isConnecting = false)
            }
        }
    }
}
