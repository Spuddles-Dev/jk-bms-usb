package com.horse.jk_bms.repository

import com.horse.jk_bms.connection.BmsConnection
import com.horse.jk_bms.model.BmsConfig
import com.horse.jk_bms.model.BmsDeviceInfo
import com.horse.jk_bms.model.BmsFaultInfo
import com.horse.jk_bms.model.BmsRuntimeData
import com.horse.jk_bms.model.BmsSystemLog
import com.horse.jk_bms.protocol.FrameCode
import com.horse.jk_bms.usb.UsbDeviceInfo
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BmsRepository @Inject constructor(
    private val bmsConnection: BmsConnection,
) {
    val runtimeData: StateFlow<BmsRuntimeData?> = bmsConnection.runtimeData
    val config: StateFlow<BmsConfig?> = bmsConnection.config
    val deviceInfo: StateFlow<BmsDeviceInfo?> = bmsConnection.deviceInfo
    val faultInfo: StateFlow<BmsFaultInfo?> = bmsConnection.faultInfo
    val systemLog: StateFlow<BmsSystemLog?> = bmsConnection.systemLog
    val isConnected: StateFlow<Boolean> = bmsConnection.isConnected
    val isPolling: StateFlow<Boolean> = bmsConnection.isPolling

    fun listDevices(): List<UsbDeviceInfo> = bmsConnection.listDevices()

    suspend fun connect(deviceInfo: UsbDeviceInfo): Result<Unit> {
        val result = bmsConnection.connect(deviceInfo)
        if (result.isSuccess) {
            bmsConnection.startPolling()
        }
        return result
    }

    fun disconnect() {
        bmsConnection.disconnect()
    }

    suspend fun refreshConfig(): Result<*> = bmsConnection.queryFrame(FrameCode.CONFIG_READ)

    suspend fun refreshDeviceInfo(): Result<*> = bmsConnection.queryFrame(FrameCode.DEVICE_INFO)

    suspend fun refreshFaults(): Result<*> = bmsConnection.queryFrame(FrameCode.FAULT_INFO)

    suspend fun refreshSystemLog(): Result<*> = bmsConnection.queryFrame(FrameCode.SYSTEM_LOG)

    suspend fun writeConfig(config: BmsConfig): Result<Unit> = bmsConnection.writeConfig(config)
}
