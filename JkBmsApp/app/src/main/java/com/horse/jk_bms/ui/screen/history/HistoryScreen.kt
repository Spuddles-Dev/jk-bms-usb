package com.horse.jk_bms.ui.screen.history

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.horse.jk_bms.data.export.CsvFormatter
import com.horse.jk_bms.viewmodel.HistoryMetric
import com.horse.jk_bms.viewmodel.HistoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(onBack: () -> Unit, onDiagnostics: () -> Unit = {}, viewModel: HistoryViewModel = hiltViewModel()) {
    val sessions by viewModel.sessions.collectAsState()
    val state by viewModel.state.collectAsState()
    var name by remember(state.session?.id) { mutableStateOf(state.session?.name.orEmpty()) }
    Scaffold(topBar = { TopAppBar(title = { Text("Session history") }, navigationIcon = {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
    }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Text("Seven days of local history. Gaps indicate missing measurements.")
                TextButton(onClick = onDiagnostics) { Text("Diagnostics and replay") }
                if (sessions.isEmpty()) Text("No recorded sessions yet")
            }
            items(sessions, key = { it.id }) { session ->
                Card(onClick = { viewModel.select(session) }, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(session.name)
                        Text(CsvFormatter.utc(session.startedAt))
                        Text("${session.deviceId} • ${session.firmware}")
                    }
                }
            }
            state.session?.let { session ->
                item {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Battery name") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true)
                    TextButton(onClick = { viewModel.rename(name) }, enabled = name.isNotBlank()) { Text("Save name") }
                    TextButton(onClick = { viewModel.select(session) }) { Text("Refresh history") }
                    HistoryMetric.entries.chunked(2).forEach { pair ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            pair.forEach { metric ->
                                FilterChip(selected = state.metric == metric, onClick = { viewModel.select(session, metric) },
                                    label = { Text(metric.title) })
                            }
                        }
                    }
                    if (state.metric == HistoryMetric.CELL) {
                        Row {
                            TextButton(onClick = { viewModel.select(session, state.metric, (state.cell - 1).coerceAtLeast(1)) }) { Text("Previous") }
                            Text("Cell ${state.cell}")
                            TextButton(onClick = { viewModel.select(session, state.metric, (state.cell + 1).coerceAtMost(32)) }) { Text("Next") }
                        }
                    }
                    if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
                    state.error?.let { Text(it) }
                    val samples = state.samples
                    if (samples.isEmpty() && !state.loading) Text("No measurements for this selection")
                    if (samples.isNotEmpty()) {
                        val low = samples.minOf { it.value }
                        val high = samples.maxOf { it.value }
                        Text("${state.metric.title}: ${"%.3f".format(low)}–${"%.3f".format(high)} ${state.metric.unit}")
                        Text("${state.sampleCount} recorded samples")
                        Canvas(Modifier.fillMaxWidth().height(220.dp).semantics {
                            contentDescription = "${state.metric.title}, range $low to $high ${state.metric.unit}"
                        }) {
                            val start = samples.first().timestamp
                            val span = (samples.last().timestamp - start).coerceAtLeast(1).toFloat()
                            fun point(timestamp: Long, value: Float) = Offset(
                                ((timestamp - start).toFloat() / span) * size.width,
                                size.height - ((value - low) / (high - low).coerceAtLeast(0.001f)) * size.height,
                            )
                            samples.zipWithNext().forEach { (a, b) ->
                                if (a.segment == b.segment) drawLine(Color(0xff2979ff), point(a.timestamp, a.value), point(b.timestamp, b.value), 3f)
                            }
                            samples.forEach { drawCircle(Color(0xff2979ff), 2f, point(it.timestamp, it.value)) }
                            state.faults.forEach { fault ->
                                if (fault.timestamp in start..samples.last().timestamp) {
                                    val x = ((fault.timestamp - start).toFloat() / span) * size.width
                                    drawLine(Color.Red, Offset(x, 0f), Offset(x, size.height), 2f)
                                }
                            }
                        }
                    }
                }
                items(state.faults, key = { it.id }) { fault ->
                    Text("Fault ${fault.logCode}: observed ${CsvFormatter.utc(fault.timestamp)}; BMS counter ${fault.rtcCount}")
                }
            }
        }
    }
}
