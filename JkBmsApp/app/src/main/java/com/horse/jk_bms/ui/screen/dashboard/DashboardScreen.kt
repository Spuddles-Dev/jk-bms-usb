package com.horse.jk_bms.ui.screen.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.horse.jk_bms.model.BmsRuntimeData
import com.horse.jk_bms.viewmodel.DashboardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onCellsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onDeviceInfoClick: () -> Unit,
    onFaultsClick: () -> Unit,
    onLogsClick: () -> Unit,
    onDisconnect: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val runtimeData by viewModel.runtimeData.collectAsState()
    val isConnected by viewModel.isConnected.collectAsState()
    val lastDataTimestamp by viewModel.lastDataTimestamp.collectAsState()
    var showExportDialog by remember { mutableStateOf(false) }

    val isStale = lastDataTimestamp > 0 && (System.currentTimeMillis() - lastDataTimestamp) > 2000

    var staleRefresh by remember { mutableStateOf(0L) }
    LaunchedEffect(isConnected) {
        while (isConnected) {
            staleRefresh = System.currentTimeMillis()
            kotlinx.coroutines.delay(1000)
        }
    }
    val isStaleLive = lastDataTimestamp > 0 && (System.currentTimeMillis() - lastDataTimestamp) > 2000

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("JK-BMS Dashboard")
                        if (isStaleLive) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "STALE",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showExportDialog = true }) {
                        Icon(Icons.Default.FileDownload, "Export")
                    }
                    IconButton(onClick = onDeviceInfoClick) {
                        Icon(Icons.Default.Info, "Device Info")
                    }
                    IconButton(onClick = onDisconnect) {
                        Icon(Icons.Default.UsbOff, "Disconnect")
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = true,
                    onClick = {},
                    icon = { Icon(Icons.Default.Dashboard, "Dashboard") },
                    label = { Text("Dashboard") },
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onCellsClick,
                    icon = { Icon(Icons.Default.BatteryStd, "Cells") },
                    label = { Text("Cells") },
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onSettingsClick,
                    icon = { Icon(Icons.Default.Settings, "Settings") },
                    label = { Text("Settings") },
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onFaultsClick,
                    icon = { Icon(Icons.Default.Warning, "Faults") },
                    label = { Text("Faults") },
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onLogsClick,
                    icon = { Icon(Icons.Default.Description, "Logs") },
                    label = { Text("Logs") },
                )
            }
        },
    ) { padding ->
        if (showExportDialog) {
            ExportDialog(onDismiss = { showExportDialog = false })
        }

        if (!isConnected) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text("Disconnected", color = MaterialTheme.colorScheme.error)
            }
            return@Scaffold
        }

        val data = runtimeData
        if (data == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Waiting for BMS data...")
                }
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatusRow("Battery Voltage", "%.2f V".format(data.batVol))
            StatusRow("Current", "%.2f A".format(data.batCurrent))
            StatusRow("Power", "%.1f W".format(data.batWatt))
            StatusRow("SOC", "${data.soc}%")
            StatusRow("SOH", "${data.soh}%")
            StatusRow("Battery Type", data.batteryTypeLabel)
            StatusRow("Cycle Count", "${data.socCycleCount}")

            if (lastDataTimestamp > 0) {
                val elapsed = (System.currentTimeMillis() - lastDataTimestamp) / 1000
                val timeStr = when {
                    elapsed < 5 -> "just now"
                    elapsed < 60 -> "${elapsed}s ago"
                    elapsed < 3600 -> "${elapsed / 60}m ago"
                    else -> "${elapsed / 3600}h ago"
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    Text(
                        "Updated $timeStr",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isStaleLive) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            HorizontalDivider()

            Text("Temperatures", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            StatusRow("MOS", "%.1f °C".format(data.tempMos))
            StatusRow("Battery 1", "%.1f °C".format(data.batTemp1))
            StatusRow("Battery 2", "%.1f °C".format(data.batTemp2))
            if (data.batTemp3 != 0f) StatusRow("Battery 3", "%.1f °C".format(data.batTemp3))
            if (data.batTemp4 != 0f) StatusRow("Battery 4", "%.1f °C".format(data.batTemp4))
            if (data.batTemp5 != 0f) StatusRow("Battery 5", "%.1f °C".format(data.batTemp5))

            HorizontalDivider()

            Text("Cells", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            StatusRow("Active Cells", "${data.activeCellCount}")
            StatusRow("Average Voltage", "%.3f V".format(data.cellVolAve))
            StatusRow("Max Voltage Delta", "%.3f V".format(data.maxVoltDelta))
            StatusRow("Highest Cell", "#${data.celMaxVol}")
            StatusRow("Lowest Cell", "#${data.celMinVol}")

            HorizontalDivider()

            Text("Capacity", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            StatusRow("Remaining", "%.2f Ah".format(data.socCapabilityRemain))
            StatusRow("Full Charge", "%.2f Ah".format(data.socFullChargeCapacity))
            StatusRow("Cycle Capacity", "%.2f Ah".format(data.socCycleCapacity))

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                StatusChip("Charging", data.chargeStatus, Icons.Default.BatteryChargingFull)
                StatusChip("Discharging", data.dischargeStatus, Icons.Default.BatteryAlert)
                StatusChip("Balancing", data.equStatus != 0, Icons.Default.Balance)
                StatusChip("Heating", data.heatingStatus, Icons.Default.LocalFireDepartment)
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (data.sysAlarm.any { it }) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Active Alarms: ${data.sysAlarm.count { it }}",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun StatusChip(label: String, active: Boolean, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = if (active) MaterialTheme.colorScheme.primary
                   else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = if (active) MaterialTheme.colorScheme.primary
                   else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
        )
    }
}
