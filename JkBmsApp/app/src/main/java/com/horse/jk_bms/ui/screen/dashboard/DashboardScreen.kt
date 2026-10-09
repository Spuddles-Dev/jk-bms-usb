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
import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onCellsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onDeviceInfoClick: () -> Unit,
    onFaultsClick: () -> Unit,
    onLogsClick: () -> Unit,
    onDisconnect: () -> Unit,
    onHistory: () -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val runtimeData by viewModel.runtimeData.collectAsState()
    val isConnected by viewModel.isConnected.collectAsState()
    val lastDataTimestamp by viewModel.lastDataTimestamp.collectAsState()
    var showExportDialog by remember { mutableStateOf(false) }

    val ageMs by viewModel.dataAgeMs.collectAsState()
    val sessionState by viewModel.sessionState.collectAsState()
    val loggingError by viewModel.loggingError.collectAsState()
    val backgroundEnabled by viewModel.backgroundEnabled.collectAsState()
    var backgroundError by remember { mutableStateOf<String?>(null) }
    val notifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) backgroundError = viewModel.enableBackground().exceptionOrNull()?.message
        else backgroundError = "Notification permission is needed for background alerts"
    }
    val isStaleLive = isConnected && ageMs > 2000

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
                    IconButton(onClick = { viewModel.disconnect(onDisconnect) }) {
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
            Text("Session: ${sessionState.name.lowercase().replace('_', ' ')}")
            TextButton(onClick = onHistory) { Text("Session history and trends") }
            TextButton(onClick = {
                if (backgroundEnabled) viewModel.disableBackground()
                else if (Build.VERSION.SDK_INT >= 33) notifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                else backgroundError = viewModel.enableBackground().exceptionOrNull()?.message
            }) { Text(if (backgroundEnabled) "Stop background monitoring" else "Enable background monitoring and alerts") }
            if (!backgroundEnabled) Text("Monitoring stops when the app leaves the foreground")
            backgroundError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            loggingError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            StatusRow("Battery Voltage", "%.2f V".format(data.batVol))
            StatusRow("Current", "%.2f A".format(data.batCurrent))
            StatusRow("Power", "%.1f W".format(data.batWatt))
            StatusRow("SOC", "${data.soc}%")
            StatusRow("SOH", "${data.soh}%")
            StatusRow("Battery Type", data.batteryTypeLabel)
            StatusRow("Cycle Count", "${data.socCycleCount}")

            if (lastDataTimestamp > 0) {
                val elapsed = ageMs / 1000
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
            StatusRow("Battery 3", "%.1f °C".format(data.batTemp3))
            StatusRow("Battery 4", "%.1f °C".format(data.batTemp4))
            StatusRow("Battery 5", "%.1f °C".format(data.batTemp5))
            Text("Sensor flags (raw): ${data.tempSensorAbsent.joinToString("") { if (it) "1" else "0" }}")
            Text("Sensor flag polarity awaits hardware validation; zero readings may be unavailable sensors.")

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
