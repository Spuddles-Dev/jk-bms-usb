package com.horse.jk_bms.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.horse.jk_bms.data.local.Converters
import com.horse.jk_bms.data.local.entity.FaultRecordEntity
import com.horse.jk_bms.data.local.entity.RuntimeDataEntity
import com.horse.jk_bms.data.local.entity.SessionEntity
import com.horse.jk_bms.data.repository.DataLogRepository
import com.horse.jk_bms.history.ChartSample
import com.horse.jk_bms.history.ExtremaSampler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.coroutines.coroutineContext
import javax.inject.Inject

enum class HistoryMetric(val title: String, val unit: String) {
    VOLTAGE("Pack voltage", "V"), CURRENT("Current", "A"), SOC("State of charge", "%"),
    MOS("MOS temperature", "°C"), DELTA("Cell voltage delta", "V"), CELL("Selected cell", "V"),
}

data class HistoryState(val session: SessionEntity? = null, val metric: HistoryMetric = HistoryMetric.VOLTAGE,
    val cell: Int = 1, val samples: List<ChartSample> = emptyList(), val faults: List<FaultRecordEntity> = emptyList(),
    val loading: Boolean = false, val error: String? = null, val sampleCount: Long = 0)

@HiltViewModel
class HistoryViewModel @Inject constructor(private val repository: DataLogRepository) : ViewModel() {
    val sessions = repository.sessions.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val mutable = MutableStateFlow(HistoryState())
    val state = mutable.asStateFlow()
    private var load: Job? = null

    fun select(session: SessionEntity, metric: HistoryMetric = mutable.value.metric, cell: Int = mutable.value.cell) {
        load?.cancel()
        mutable.value = HistoryState(session = session, metric = metric, cell = cell, loading = true)
        load = viewModelScope.launch {
            try {
                val sampler = ExtremaSampler(session.startedAt, session.endedAt ?: System.currentTimeMillis())
                var after = 0L
                var count = 0L
                val upper = repository.latestRuntimeId()
                while (true) {
                    coroutineContext.ensureActive()
                    val page = repository.runtimePage(0, upper, after, session.id)
                    if (page.isEmpty()) break
                    page.forEach { row ->
                        val value = when (metric) {
                            HistoryMetric.VOLTAGE -> row.batVol
                            HistoryMetric.CURRENT -> row.batCurrent
                            HistoryMetric.SOC -> row.soc.toFloat()
                            HistoryMetric.MOS -> row.tempMos
                            HistoryMetric.DELTA -> row.maxVoltDelta
                            HistoryMetric.CELL -> Converters.toFloatArray(row.cellVoltages).getOrNull(cell - 1)?.takeIf { it > 0 } ?: Float.NaN
                        }
                        sampler.add(row.timestamp, value)
                    }
                    count += page.size
                    after = page.last().id
                }
                mutable.value = mutable.value.copy(samples = sampler.samples(), faults = repository.sessionFaults(session.id),
                    sampleCount = count, loading = false)
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                mutable.value = mutable.value.copy(error = error.message, loading = false)
            }
        }
    }

    fun rename(name: String) {
        val session = mutable.value.session ?: return
        viewModelScope.launch {
            try { repository.rename(session.deviceId, name) } catch (error: Exception) {
                if (error is CancellationException) throw error
                mutable.value = mutable.value.copy(error = error.message)
            }
        }
    }
}
