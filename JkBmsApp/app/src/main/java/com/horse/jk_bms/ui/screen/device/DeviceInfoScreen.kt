package com.horse.jk_bms.ui.screen.device

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
import com.horse.jk_bms.viewmodel.DeviceInfoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceInfoScreen(
    onBack: () -> Unit,
    viewModel: DeviceInfoViewModel = hiltViewModel(),
) {
    val deviceInfo by viewModel.deviceInfo.collectAsState()
    val info = deviceInfo

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Device Info") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
            )
        },
    ) { padding ->
        if (info == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text("No device info available")
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            InfoRow("Device ID", info.manuDeviceID)
            InfoRow("Hardware Version", info.hardwareVersion)
            InfoRow("Software Version", info.softwareVersion)
            InfoRow("Max Cells", "${info.maxCells}")
            InfoRow("Serial Number", info.deviceSN)
            InfoRow("BLE Name", info.bluetoothName)
            InfoRow("Manufacture Date", info.manufactureDate)
            InfoRow("Total Runtime", formatSeconds(info.oddRunTime))
            InfoRow("Power-on Times", "${info.pwrOnTimes}")
            InfoRow("Data Store Period", "${info.dataStoredPeriod} s")
            InfoRow("Agency ID", "${info.agencyId}")
            InfoRow("Protocol Version", "${info.protocolVer}")

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            InfoRow("UART1 Protocol", "${info.uart1ProtoNo}")
            InfoRow("CAN Protocol", "${info.canProtoNo}")
            InfoRow("UART2 Protocol", "${info.uart2ProtoNo}")
            InfoRow("UART3 Protocol", "${info.uart3ProtoNo}")
            InfoRow("Re-Bulk SOC", "${info.reBulkSOC}%")
            InfoRow("Emergency Time", "${info.emergencyTime} min")
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.Medium)
    }
}

private fun formatSeconds(seconds: Long): String {
    val days = seconds / 86400
    val hours = (seconds % 86400) / 3600
    return "${days}d ${hours}h"
}
