package com.horse.jk_bms.ui.screen.logs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.horse.jk_bms.viewmodel.LogsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogsScreen(
    onBack: () -> Unit,
    viewModel: LogsViewModel = hiltViewModel(),
) {
    val systemLog by viewModel.systemLog.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("System Log") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
            )
        },
    ) { padding ->
        if (systemLog == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text("No log data available")
            }
            return@Scaffold
        }

        val log = systemLog!!

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                "Log Count: ${log.logCount}",
                style = MaterialTheme.typography.titleMedium,
            )

            if (log.alarmLog.isEmpty()) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "No alarm log entries",
                        modifier = Modifier.padding(16.dp),
                    )
                }
            } else {
                Text(
                    "Alarm Log (${log.alarmLog.size} bytes)",
                    style = MaterialTheme.typography.titleSmall,
                )

                val entries = parseAlarmLog(log.alarmLog)
                if (entries.isNotEmpty()) {
                    entries.forEach { entry ->
                        AlarmEntryCard(entry)
                    }
                } else {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Raw Hex Dump",
                                style = MaterialTheme.typography.labelMedium,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                log.alarmLog.toList().chunked(16).joinToString("\n") { chunk ->
                                    chunk.joinToString(" ") { "%02X".format(it) }
                                },
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class AlarmEntry(
    val index: Int,
    val code: Int,
    val description: String,
)

private val ALARM_CODES = mapOf(
    0x00 to "Cell Over-Voltage",
    0x01 to "Cell Under-Voltage",
    0x02 to "Battery Over-Voltage",
    0x03 to "Battery Under-Voltage",
    0x04 to "Charge Over-Current",
    0x05 to "Discharge Over-Current",
    0x06 to "Charge Over-Temperature",
    0x07 to "Charge Under-Temperature",
    0x08 to "Discharge Over-Temperature",
    0x09 to "Discharge Under-Temperature",
    0x0A to "MOS Over-Temperature",
    0x0B to "Short Circuit Protection",
    0x0C to "Charge MOS Fault",
    0x0D to "Discharge MOS Fault",
    0x0E to "Cell Balance Failure",
    0x0F to "EEPROM Error",
    0x10 to "SOC Too Low",
    0x11 to "Sensor Failure",
    0x12 to "Heating Failure",
    0x13 to "Communication Timeout",
)

private fun parseAlarmLog(bytes: ByteArray): List<AlarmEntry> {
    if (bytes.size < 2) return emptyList()
    val entries = mutableListOf<AlarmEntry>()
    var offset = 0
    var index = 1
    while (offset + 1 < bytes.size) {
        val code = bytes[offset].toInt() and 0xFF
        if (code == 0xFF && offset + 1 < bytes.size && bytes[offset + 1] == 0xFF.toByte()) break
        val desc = ALARM_CODES[code] ?: "Unknown (0x%02X)".format(code)
        entries.add(AlarmEntry(index, code, desc))
        offset += 2
        index++
    }
    return entries
}

@Composable
private fun AlarmEntryCard(entry: AlarmEntry) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "#${entry.index}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                entry.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                "0x%02X".format(entry.code),
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
