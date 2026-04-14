package com.horse.jk_bms.ui.screen.faults

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.horse.jk_bms.model.FaultRecord
import com.horse.jk_bms.viewmodel.FaultsViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaultsScreen(
    onBack: () -> Unit,
    viewModel: FaultsViewModel = hiltViewModel(),
) {
    val faultInfo by viewModel.faultInfo.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Fault History") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
            )
        },
    ) { padding ->
        if (faultInfo == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text("No fault data available")
            }
            return@Scaffold
        }

        val info = faultInfo!!

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                "${info.count} fault records (starting at index ${info.beginIndex})",
                style = MaterialTheme.typography.titleMedium,
            )

            if (info.records.isEmpty()) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "No fault records",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            info.records.forEachIndexed { index, record ->
                FaultRecordCard(record, index + 1)
            }
        }
    }
}

@Composable
private fun FaultRecordCard(record: FaultRecord, index: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "Record #$index",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    formatTimestamp(record.rtcCount),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            InfoRow("Fault", describeFaultCode(record.logCode))
            InfoRow("Log Code", "${record.logCode} (0x%02X)".format(record.logCode))
            InfoRow("Battery Voltage", "%.2f V".format(record.volBat))
            InfoRow("Battery Current", "%.1f A".format(record.curBat))
            InfoRow("Max Cell Voltage", "%.3f V (#${record.maxVolCellNo})".format(record.volCellMax))
            InfoRow("Min Cell Voltage", "%.3f V (#${record.minVolCellNo})".format(record.volCellMin))
            InfoRow("Remaining Capacity", "%.1f Ah".format(record.socCapRemain))
            InfoRow("Max Temp", "${record.maxTemp} °C")
            InfoRow("Min Temp", "${record.minTemp} °C")
            InfoRow("MOS Temp", "${record.tempMos} °C")
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall)
    }
}

private fun formatTimestamp(rtcCount: Long): String {
    val epoch = rtcCount * 1000 + 1577836800000L
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(epoch))
}

private val FAULT_CODE_NAMES = mapOf(
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
    0x0B to "Short Circuit",
    0x0C to "Charge MOS Fault",
    0x0D to "Discharge MOS Fault",
    0x0E to "Balance Failure",
    0x0F to "EEPROM Error",
    0x10 to "SOC Low",
    0x11 to "Sensor Failure",
    0x12 to "Heating Fault",
    0x13 to "Communication Timeout",
)

private fun describeFaultCode(code: Int): String {
    return FAULT_CODE_NAMES[code] ?: "Unknown Fault"
}
