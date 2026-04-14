package com.horse.jk_bms.connection

import com.horse.jk_bms.data.repository.DataLogRepository
import com.horse.jk_bms.model.BmsConfig
import com.horse.jk_bms.model.BmsDeviceInfo
import com.horse.jk_bms.model.BmsFaultInfo
import com.horse.jk_bms.model.BmsRuntimeData
import com.horse.jk_bms.model.BmsSystemLog
import com.horse.jk_bms.protocol.FrameCode
import com.horse.jk_bms.protocol.FrameDecoder
import com.horse.jk_bms.protocol.FrameEncoder
import com.horse.jk_bms.protocol.RuntimeDataParser
import com.horse.jk_bms.protocol.ConfigParser
import com.horse.jk_bms.protocol.DeviceInfoParser
import com.horse.jk_bms.protocol.FaultInfoParser
import com.horse.jk_bms.protocol.SystemLogParser
import com.horse.jk_bms.usb.UsbDeviceInfo
import com.horse.jk_bms.usb.UsbSerialManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

sealed class BmsEvent {
    data class RuntimeDataUpdated(val data: BmsRuntimeData) : BmsEvent()
    data class ConfigUpdated(val config: BmsConfig) : BmsEvent()
    data class DeviceInfoUpdated(val info: BmsDeviceInfo) : BmsEvent()
    data class FaultInfoUpdated(val faults: BmsFaultInfo) : BmsEvent()
    data class SystemLogUpdated(val log: BmsSystemLog) : BmsEvent()
    data class Error(val message: String) : BmsEvent()
}

@Singleton
class BmsConnection @Inject constructor(
    private val usbSerialManager: UsbSerialManager,
    private val dataLogRepository: DataLogRepository,
) {
    private val _events = MutableSharedFlow<BmsEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<BmsEvent> = _events.asSharedFlow()

    private val _runtimeData = MutableStateFlow<BmsRuntimeData?>(null)
    val runtimeData: StateFlow<BmsRuntimeData?> = _runtimeData.asStateFlow()

    private val _config = MutableStateFlow<BmsConfig?>(null)
    val config: StateFlow<BmsConfig?> = _config.asStateFlow()

    private val _deviceInfo = MutableStateFlow<BmsDeviceInfo?>(null)
    val deviceInfo: StateFlow<BmsDeviceInfo?> = _deviceInfo.asStateFlow()

    private val _faultInfo = MutableStateFlow<BmsFaultInfo?>(null)
    val faultInfo: StateFlow<BmsFaultInfo?> = _faultInfo.asStateFlow()

    private val _systemLog = MutableStateFlow<BmsSystemLog?>(null)
    val systemLog: StateFlow<BmsSystemLog?> = _systemLog.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _isPolling = MutableStateFlow(false)
    val isPolling: StateFlow<Boolean> = _isPolling.asStateFlow()

    private val _lastDataTimestamp = MutableStateFlow(0L)
    val lastDataTimestamp: StateFlow<Long> = _lastDataTimestamp.asStateFlow()

    private var pollerJob: Job? = null
    private var counter = 0
    private var cleanupCounter = 0
    private var consecutiveFailures = 0
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun listDevices(): List<UsbDeviceInfo> = usbSerialManager.listSerialDevices()

    suspend fun connect(deviceInfo: UsbDeviceInfo): Result<Unit> {
        val result = usbSerialManager.connect(deviceInfo)
        if (result.isSuccess) {
            _isConnected.value = true
        } else {
            _isConnected.value = false
            _events.tryEmit(BmsEvent.Error(result.exceptionOrNull()?.message ?: "Connection failed"))
        }
        return result
    }

    fun disconnect() {
        stopPolling()
        usbSerialManager.disconnect()
        _isConnected.value = false
        _runtimeData.value = null
        _config.value = null
        _deviceInfo.value = null
        _faultInfo.value = null
        _systemLog.value = null
    }

    fun startPolling() {
        if (_isPolling.value) return
        _isPolling.value = true
        pollerJob = scope.launch {
            val pollSequence = listOf(
                FrameCode.RUNTIME_DATA,
                FrameCode.CONFIG_READ,
                FrameCode.DEVICE_INFO,
                FrameCode.FAULT_INFO,
            )
            var index = 0
            while (isActive && usbSerialManager.isConnected) {
                if (consecutiveFailures >= 5) {
                    delay(2000)
                    consecutiveFailures = 0
                }
                val frameCode = pollSequence[index % pollSequence.size]
                val result = queryFrame(frameCode)
                if (result.isFailure) {
                    consecutiveFailures++
                } else {
                    consecutiveFailures = 0
                }
                index++
                cleanupCounter++
                if (cleanupCounter >= 600) {
                    scope.launch { dataLogRepository.cleanup() }
                    cleanupCounter = 0
                }
                delay(100)
            }
            _isPolling.value = false
        }
    }

    fun stopPolling() {
        pollerJob?.cancel()
        pollerJob = null
        _isPolling.value = false
    }

    suspend fun queryFrame(frameCode: FrameCode): Result<*> {
        val queryFrame = FrameEncoder.buildQuery(frameCode, counter++)
        val result = usbSerialManager.sendAndReceive(queryFrame)

        if (result.isFailure) {
            val msg = result.exceptionOrNull()?.message ?: "Query failed"
            _events.tryEmit(BmsEvent.Error(msg))
            return result
        }

        val raw = FrameDecoder.findFrameInBuffer(result.getOrThrow())
        if (raw == null) {
            _events.tryEmit(BmsEvent.Error("Failed to decode response for ${frameCode.name}"))
            return Result.failure<Nothing>(Exception("Decode failed"))
        }

        val (rawFrame, _) = raw
        if (rawFrame.frameCode != frameCode) {
            _events.tryEmit(BmsEvent.Error("Unexpected frame code: ${rawFrame.frameCode}, expected $frameCode"))
            return Result.failure<Nothing>(Exception("Unexpected frame code"))
        }

        return when (frameCode) {
            FrameCode.RUNTIME_DATA -> {
                val data = RuntimeDataParser.parse(rawFrame.data)
                _runtimeData.value = data
                _lastDataTimestamp.value = System.currentTimeMillis()
                _events.tryEmit(BmsEvent.RuntimeDataUpdated(data))
                scope.launch { dataLogRepository.logRuntimeData(data) }
                Result.success(data)
            }
            FrameCode.CONFIG_READ -> {
                val cfg = ConfigParser.parse(rawFrame.data)
                _config.value = cfg
                _events.tryEmit(BmsEvent.ConfigUpdated(cfg))
                scope.launch { dataLogRepository.logConfig(cfg) }
                Result.success(cfg)
            }
            FrameCode.DEVICE_INFO -> {
                val info = DeviceInfoParser.parse(rawFrame.data)
                _deviceInfo.value = info
                _events.tryEmit(BmsEvent.DeviceInfoUpdated(info))
                scope.launch { dataLogRepository.logDeviceInfo(info) }
                Result.success(info)
            }
            FrameCode.FAULT_INFO -> {
                val faults = FaultInfoParser.parse(rawFrame.data)
                _faultInfo.value = faults
                _events.tryEmit(BmsEvent.FaultInfoUpdated(faults))
                scope.launch { dataLogRepository.logFaultInfo(faults) }
                Result.success(faults)
            }
            FrameCode.SYSTEM_LOG -> {
                val log = SystemLogParser.parse(rawFrame.data)
                _systemLog.value = log
                _events.tryEmit(BmsEvent.SystemLogUpdated(log))
                scope.launch { dataLogRepository.logSystemLog(log) }
                Result.success(log)
            }
            FrameCode.CONFIG_WRITE -> {
                Result.success(rawFrame)
            }
        }
    }

    suspend fun writeConfig(config: BmsConfig): Result<Unit> {
        stopPolling()
        val writeFrame = FrameEncoder.buildConfigWrite(config, counter++)
        val result = usbSerialManager.sendAndReceive(writeFrame)

        if (result.isFailure) {
            _events.tryEmit(BmsEvent.Error(result.exceptionOrNull()?.message ?: "Config write failed"))
            startPolling()
            return result.map {}
        }

        val raw = FrameDecoder.findFrameInBuffer(result.getOrThrow())
        if (raw == null) {
            _events.tryEmit(BmsEvent.Error("Failed to decode config write ACK"))
            startPolling()
            return Result.failure(Exception("No ACK"))
        }

        val (ackFrame, _) = raw
        val writtenConfig = ConfigParser.parse(ackFrame.data)
        _config.value = writtenConfig
        _events.tryEmit(BmsEvent.ConfigUpdated(writtenConfig))

        startPolling()
        return Result.success(Unit)
    }
}
