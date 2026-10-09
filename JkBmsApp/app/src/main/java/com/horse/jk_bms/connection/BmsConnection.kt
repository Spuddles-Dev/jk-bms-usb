package com.horse.jk_bms.connection

import android.content.Context
import android.os.SystemClock
import com.horse.jk_bms.BuildConfig
import com.horse.jk_bms.data.repository.DataLogRepository
import com.horse.jk_bms.diagnostics.ProtocolTrace
import com.horse.jk_bms.model.BmsConfig
import com.horse.jk_bms.model.BmsDeviceInfo
import com.horse.jk_bms.model.BmsFaultInfo
import com.horse.jk_bms.model.BmsRuntimeData
import com.horse.jk_bms.model.BmsSystemLog
import com.horse.jk_bms.protocol.ConfigParser
import com.horse.jk_bms.connection.ConfigWriteVerifier
import com.horse.jk_bms.protocol.DeviceInfoParser
import com.horse.jk_bms.protocol.FaultInfoParser
import com.horse.jk_bms.protocol.FrameCode
import com.horse.jk_bms.protocol.FrameEncoder
import com.horse.jk_bms.protocol.RuntimeDataParser
import com.horse.jk_bms.protocol.SystemLogParser
import com.horse.jk_bms.usb.BmsTransport
import com.horse.jk_bms.usb.UsbDeviceInfo
import com.horse.jk_bms.usb.UsbEvent
import com.horse.jk_bms.usb.UsbEventReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

sealed class BmsEvent {
    data class Error(val message: String) : BmsEvent()
}

@Singleton
class BmsConnection @Inject constructor(
    @ApplicationContext context: Context,
    private val transport: BmsTransport,
    private val dataLogRepository: DataLogRepository,
    private val usbEvents: UsbEventReceiver,
    private val trace: ProtocolTrace,
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val lifecycle = Mutex()
    private val exchanges = Mutex()
    private val mutableEvents = MutableSharedFlow<BmsEvent>(extraBufferCapacity = 64)
    val events = mutableEvents.asSharedFlow()
    private val runtime = MutableStateFlow<BmsRuntimeData?>(null)
    val runtimeData = runtime.asStateFlow()
    private val settings = MutableStateFlow<BmsConfig?>(null)
    val config = settings.asStateFlow()
    private val device = MutableStateFlow<BmsDeviceInfo?>(null)
    val deviceInfo = device.asStateFlow()
    private val faults = MutableStateFlow<BmsFaultInfo?>(null)
    val faultInfo = faults.asStateFlow()
    private val logs = MutableStateFlow<BmsSystemLog?>(null)
    val systemLog = logs.asStateFlow()
    private val connected = MutableStateFlow(false)
    val isConnected = connected.asStateFlow()
    private val polling = MutableStateFlow(false)
    val isPolling = polling.asStateFlow()
    private val timestamp = MutableStateFlow(0L)
    val lastDataTimestamp = timestamp.asStateFlow()
    private val state = MutableStateFlow(SessionState.DISCONNECTED)
    val sessionState = state.asStateFlow()
    private val age = MutableStateFlow(Long.MAX_VALUE)
    val dataAgeMs = age.asStateFlow()
    private val logError = MutableStateFlow<String?>(null)
    val loggingError = logError.asStateFlow()
    private val logging = Channel<suspend () -> Unit>(128)
    private var poller: Job? = null
    @Volatile private var connectingJob: Job? = null
    @Volatile private var reconnecter: Job? = null
    private var counter = 0
    private var generation = 0L
    private var lastReceived = 0L
    private var selected: UsbDeviceInfo? = null
    private var reconnectSerial: String? = null
    @Volatile private var desiredConnection = false
    private val writer: Job

    init {
        usbEvents.register(context)
        writer = scope.launch {
            for (operation in logging) {
                try { operation(); logError.value = null } catch (error: Exception) {
                    if (error is CancellationException) throw error
                    logError.value = "Recording paused: ${error.message ?: "storage error"}"
                }
            }
        }
        scope.launch {
            while (isActive) {
                age.value = telemetryAge(SystemClock.elapsedRealtime(), lastReceived)
                if (connected.value && state.value != SessionState.RECOVERING) {
                    state.value = if (age.value > 2000) SessionState.STALE else SessionState.LIVE
                }
                delay(1000)
            }
        }
        scope.launch {
            usbEvents.events.collect { event ->
                when (event) {
                    is UsbEvent.DeviceDetached -> if (selected?.device?.deviceId == event.device.deviceId) {
                        lifecycle.withLock { disconnectInternal(SessionState.RECOVERING) }
                    }
                    is UsbEvent.DeviceAttached -> {
                        val previous = selected
                        val serial = reconnectSerial
                        if (desiredConnection && !connected.value && previous != null && serial != null && reconnecter?.isActive != true) {
                            val match = listDevices().singleOrNull {
                                it.device.vendorId == previous.device.vendorId &&
                                    it.device.productId == previous.device.productId &&
                                    it.portIndex == previous.portIndex && it.serial == serial
                            }
                            if (match != null) reconnecter = scope.launch { if (desiredConnection) connect(match) }
                        }
                    }
                }
            }
        }
    }

    fun listDevices(): List<UsbDeviceInfo> = transport.listSerialDevices()

    suspend fun connect(info: UsbDeviceInfo): Result<Unit> = lifecycle.withLock {
        desiredConnection = true
        disconnectInternal()
        reconnectSerial = null
        selected = info
        state.value = SessionState.PERMISSION_PENDING
        connectingJob = currentCoroutineContext()[Job]
        try {
            val result = exchanges.withLock { transport.connect(info) }
            if (result.isFailure) {
                state.value = SessionState.DISCONNECTED
                mutableEvents.tryEmit(BmsEvent.Error(result.exceptionOrNull()?.message ?: "Connection failed"))
                return@withLock result
            }
            reconnectSerial = info.serial
            state.value = SessionState.CONNECTING
            connected.value = true
            val sessionId = UUID.randomUUID().toString()
            logging.send { dataLogRepository.startSession(sessionId, "usb:${info.device.vendorId}:${info.device.productId}:${info.serial.orEmpty()}:${info.portIndex}") }
            startPolling()
            Result.success(Unit)
        } catch (error: CancellationException) {
            transport.disconnect()
            connected.value = false
            state.value = SessionState.DISCONNECTED
            throw error
        } finally { connectingJob = null }
    }

    suspend fun disconnect() {
        desiredConnection = false
        val caller = currentCoroutineContext()[Job]
        connectingJob?.takeIf { it != caller }?.cancel()
        reconnecter?.takeIf { it != caller }?.cancel()
        lifecycle.withLock {
            reconnectSerial = null
            disconnectInternal()
            selected = null
        }
    }

    fun requestDisconnect() {
        desiredConnection = false
        connectingJob?.cancel()
        reconnecter?.cancel()
        scope.launch { disconnect() }
    }

    private suspend fun disconnectInternal(finalState: SessionState = SessionState.DISCONNECTED) {
        poller?.cancelAndJoin()
        poller = null
        exchanges.withLock {
            generation++
            transport.disconnect()
            connected.value = false
            polling.value = false
            state.value = finalState
            runtime.value = null
            settings.value = null
            device.value = null
            faults.value = null
            logs.value = null
            lastReceived = 0
            timestamp.value = 0
            age.value = Long.MAX_VALUE
            logging.send { dataLogRepository.endSession() }
        }
    }

    private fun startPolling() {
        if (poller?.isActive == true) return
        polling.value = true
        poller = scope.launch {
            try {
                queryFrame(FrameCode.DEVICE_INFO)
                queryFrame(FrameCode.CONFIG_READ)
                var cycles = 0
                var failures = 0
                while (isActive && connected.value) {
                    val result = queryFrame(FrameCode.RUNTIME_DATA)
                    failures = if (result.isFailure) failures + 1 else 0
                    if (cycles++ % 20 == 0) queryFrame(FrameCode.FAULT_INFO)
                    if (cycles % 600 == 0) log { dataLogRepository.cleanup() }
                    if (failures >= 5) {
                        state.value = SessionState.RECOVERING
                        delay(2000)
                        failures = 0
                    }
                    delay(250)
                }
            } finally { polling.value = false }
        }
    }

    suspend fun queryFrame(code: FrameCode): Result<*> {
        val epoch = generation
        return exchanges.withLock {
            if (!connected.value || epoch != generation) return@withLock Result.failure<Nothing>(IOException("Session disconnected"))
            try {
                val request = FrameEncoder.buildQuery(code, counter++)
                trace.record("tx", request)
                val response = transport.exchange(request, code).getOrThrow()
                require(response.frameCode == code) { "Unexpected response: ${response.frameCode}" }
                val raw = request.copyOf().apply {
                    this[4] = response.frameCode.code
                    this[5] = response.counter.toByte()
                    response.data.copyInto(this, 6)
                    com.horse.jk_bms.protocol.Checksum.writeChecksum(this)
                }
                trace.record("rx", raw)
                val parsed: Any = when (code) {
                    FrameCode.RUNTIME_DATA -> RuntimeDataParser.parse(response.data).also {
                        runtime.value = it
                        lastReceived = SystemClock.elapsedRealtime()
                        timestamp.value = System.currentTimeMillis()
                        age.value = 0
                        state.value = SessionState.LIVE
                        val observedAt = timestamp.value
                        log { dataLogRepository.logRuntimeData(it, observedAt) }
                    }
                    FrameCode.CONFIG_READ -> ConfigParser.parse(response.data).also { settings.value = it; log { dataLogRepository.logConfig(it) } }
                    FrameCode.DEVICE_INFO -> DeviceInfoParser.parse(response.data).also { device.value = it; log { dataLogRepository.logDeviceInfo(it) } }
                    FrameCode.FAULT_INFO -> FaultInfoParser.parse(response.data).also { faults.value = it; log { dataLogRepository.logFaultInfo(it) } }
                    FrameCode.SYSTEM_LOG -> SystemLogParser.parse(response.data).also { logs.value = it; log { dataLogRepository.logSystemLog(it) } }
                    FrameCode.CONFIG_WRITE -> throw IOException("Use verified configuration transaction")
                }
                Result.success(parsed)
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                mutableEvents.tryEmit(BmsEvent.Error(error.message ?: "Request failed"))
                Result.failure<Nothing>(error)
            }
        }
    }

    suspend fun writeConfig(requested: BmsConfig, baseline: BmsConfig): Result<Unit> = exchanges.withLock {
        if (!BuildConfig.CONFIG_WRITES_VERIFIED) {
            return@withLock Result.failure(IOException("Configuration writes await hardware compatibility validation"))
        }
        if (!connected.value) return@withLock Result.failure(IOException("Disconnected"))
        try {
            val applied = ConfigWriteVerifier.write(transport, requested, baseline) { counter++ }
            settings.value = applied
            log { dataLogRepository.logWrite(baseline, applied, "verified") }
            Result.success(Unit)
        } catch (error: Exception) {
            log { dataLogRepository.logWrite(baseline, requested, "uncertain: ${error.message}") }
            if (error is CancellationException) throw error
            Result.failure(error)
        }
    }

    private fun log(operation: suspend () -> Unit) {
        if (!logging.trySend(operation).isSuccess) logError.value = "Recording queue full; some samples were not saved"
    }

    suspend fun close() {
        disconnect()
        logging.close()
        writer.join()
        scope.cancel()
    }
}
