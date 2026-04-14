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
            SectionHeader("Device")
            InfoRow("Device ID", info.manuDeviceID)
            InfoRow("Serial Number", info.deviceSN)
            InfoRow("Hardware Version", info.hardwareVersion)
            InfoRow("Software Version", info.softwareVersion)
            InfoRow("Hardware Option", info.hardwareOption)
            InfoRow("Protocol Version", "${info.protocolVer}")
            InfoRow("Max Cells", "${info.maxCells}")
            InfoRow("Manufacture Date", info.manufactureDate)
            InfoRow("Agency ID", "${info.agencyId}")

            SectionHeader("Runtime")
            InfoRow("Total Runtime", formatSeconds(info.oddRunTime))
            InfoRow("Power-on Times", "${info.pwrOnTimes}")
            InfoRow("Data Store Period", "${info.dataStoredPeriod} s")
            InfoRow("Emergency Time", "${info.emergencyTime} min")

            SectionHeader("Bluetooth")
            InfoRow("BLE Name", info.bluetoothName)
            InfoRow("BLE Password", info.bluetoothPwd)
            InfoRow("Setting Password", info.settingPassword)

            SectionHeader("User Data")
            InfoRow("User Data", info.userData)
            InfoRow("User Data 2", info.userData2)

            SectionHeader("Protocols")
            InfoRow("UART1 Protocol", "${info.uart1ProtoNo}")
            InfoRow("CAN Protocol", "${info.canProtoNo}")
            InfoRow("UART2 Protocol", "${info.uart2ProtoNo}")
            InfoRow("UART3 Protocol", "${info.uart3ProtoNo}")
            InfoRow("UART MPTL Version", "${info.uartMPTLVer}")
            InfoRow("CAN MPTL Version", "${info.canMPTLVer}")

            SectionHeader("Triggers")
            InfoRow("LCD/Buzzer Trigger", "${info.lcdBuzzerTrigger}")
            InfoRow("LCD/Buzzer Trigger Val", "${info.lcdBuzzerTriggerVal}")
            InfoRow("LCD/Buzzer Release Val", "${info.lcdBuzzerReleaseVal}")
            InfoRow("Dry Contact 1 Trigger", "${info.dry1Trigger}")
            InfoRow("Dry 1 Trigger Val", "${info.dry1TriggerVal}")
            InfoRow("Dry 1 Release Val", "${info.dry1ReleaseVal}")
            InfoRow("Dry Contact 2 Trigger", "${info.dry2Trigger}")
            InfoRow("Dry 2 Trigger Val", "${info.dry2TriggerVal}")
            InfoRow("Dry 2 Release Val", "${info.dry2ReleaseVal}")

            SectionHeader("Charging")
            InfoRow("Re-Bulk SOC", "${info.reBulkSOC}%")
            InfoRow("RCV Time", "${info.rcvTime} s")
            InfoRow("RFV Time", "${info.rfvTime} s")
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 8.dp),
    )
    HorizontalDivider()
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
