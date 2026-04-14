package com.horse.jk_bms.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.horse.jk_bms.repository.BmsRepository
import com.horse.jk_bms.usb.UsbDeviceInfo
import com.horse.jk_bms.usb.UsbEvent
import com.horse.jk_bms.usb.UsbEventReceiver
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ConnectionState(
    val devices: List<UsbDeviceInfo> = emptyList(),
    val isScanning: Boolean = false,
    val isConnecting: Boolean = false,
    val error: String? = null,
    val lastConnectedDevice: UsbDeviceInfo? = null,
)

@HiltViewModel
class ConnectionViewModel @Inject constructor(
    application: Application,
    private val repository: BmsRepository,
    private val usbEventReceiver: UsbEventReceiver,
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(ConnectionState())
    val state: StateFlow<ConnectionState> = _state.asStateFlow()

    init {
        usbEventReceiver.register(application)
        refreshDevices()
        viewModelScope.launch {
            usbEventReceiver.events.collect { event ->
                when (event) {
                    UsbEvent.DeviceAttached -> {
                        refreshDevices()
                        val lastDevice = _state.value.lastConnectedDevice
                        if (lastDevice != null && repository.isConnected.value) {
                            // already connected, ignore
                        } else if (lastDevice != null) {
                            tryAutoReconnect(lastDevice)
                        } else {
                            refreshDevices()
                        }
                    }
                    UsbEvent.DeviceDetached -> {
                        refreshDevices()
                    }
                }
            }
        }
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
                _state.value = _state.value.copy(lastConnectedDevice = deviceInfo)
                onConnected()
            } else {
                _state.value = _state.value.copy(
                    error = result.exceptionOrNull()?.message ?: "Connection failed"
                )
            }
        }
    }

    private fun tryAutoReconnect(targetDevice: UsbDeviceInfo) {
        viewModelScope.launch {
            val devices = repository.listDevices()
            val match = devices.find {
                it.device.vendorId == targetDevice.device.vendorId &&
                    it.device.productId == targetDevice.device.productId
            }
            if (match != null) {
                _state.value = _state.value.copy(isConnecting = true, error = null)
                val result = repository.connect(match)
                _state.value = _state.value.copy(isConnecting = false)
                if (result.isFailure) {
                    _state.value = _state.value.copy(
                        error = "Auto-reconnect failed: ${result.exceptionOrNull()?.message}"
                    )
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        usbEventReceiver.unregister(getApplication())
    }
}
