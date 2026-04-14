package com.horse.jk_bms.ui.screen.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.horse.jk_bms.model.BmsConfig
import com.horse.jk_bms.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val config = state.editConfig

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("BMS Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
            )
        },
    ) { padding ->
        if (config == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        var showConfirmDialog by remember { mutableStateOf(false) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionHeader("Voltage Settings")
            ConfigField("Smart Sleep Voltage", "%.3f V".format(config.volSmartSleep), "V")
            ConfigField("Cell UVP", "%.3f V".format(config.volCellUV), "V")
            ConfigField("Cell UVPR", "%.3f V".format(config.volCellUVPR), "V")
            ConfigField("Cell OVP", "%.3f V".format(config.volCellOV), "V")
            ConfigField("Cell OVPR", "%.3f V".format(config.volCellOVPR), "V")
            ConfigField("Balance Trigger", "%.3f V".format(config.volBalanTrig), "V")
            ConfigField("SOC 100% Volt", "%.3f V".format(config.volSOCP100), "V")
            ConfigField("SOC 0% Volt", "%.3f V".format(config.volSOCP0), "V")
            ConfigField("Start Balance Volt", "%.3f V".format(config.volStartBalan), "V")

            SectionHeader("Current Settings")
            ConfigField("Charge Current Limit", "%.1f A".format(config.timBatCOC), "A")
            ConfigField("Discharge Current Limit", "%.1f A".format(config.timBatDcOC), "A")
            ConfigField("Max Balance Current", "%.1f A".format(config.curBalanMax), "A")
            ConfigField("Current Range", "%.1f A".format(config.currentRange), "A")

            SectionHeader("Temperature Protections")
            ConfigField("Charge OTP", "%.1f °C".format(config.tmpBatCOT), "°C")
            ConfigField("Charge OTPR", "%.1f °C".format(config.tmpBatCOTPR), "°C")
            ConfigField("Discharge OTP", "%.1f °C".format(config.tmpBatDcOT), "°C")
            ConfigField("Discharge OTPR", "%.1f °C".format(config.tmpBatDcOTPR), "°C")
            ConfigField("Charge UTP", "%.1f °C".format(config.tmpBatCUT), "°C")
            ConfigField("Charge UTPR", "%.1f °C".format(config.tmpBatCUTPR), "°C")
            ConfigField("MOS OTP", "%.1f °C".format(config.tmpMosOT), "°C")
            ConfigField("MOS OTPR", "%.1f °C".format(config.tmpMosOTPR), "°C")

            SectionHeader("Battery")
            ConfigField("Cell Count", "${config.cellCount}", "")
            ConfigField("Battery Capacity", "%.1f Ah".format(config.capBatCell), "Ah")

            SectionHeader("Protection Delays")
            ConfigField("Charge OCP Delay", "${config.timBatCOCPDly} s", "s")
            ConfigField("Charge OCPR Time", "${config.timBatCOCPRDly} s", "s")
            ConfigField("Discharge OCP Delay", "${config.timBatDcOCPDly} s", "s")
            ConfigField("Discharge OCPR Time", "${config.timBatDcOCPRDly} s", "s")

            SectionHeader("Switches")
            SwitchRow("Charge Enabled", config.batChargeEn == 1L)
            SwitchRow("Discharge Enabled", config.batDischargeEn == 1L)
            SwitchRow("Balance Enabled", config.balanEn == 1L)

            Spacer(modifier = Modifier.height(16.dp))

            if (state.writeSuccess == true) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        "Config written successfully",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }

            if (state.error != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        state.error!!,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }

            Button(
                onClick = { showConfirmDialog = true },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isWriting,
            ) {
                if (state.isWriting) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Write Configuration to BMS")
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        if (showConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showConfirmDialog = false },
                title = { Text("Confirm Write") },
                text = { Text("Are you sure you want to write these settings to the BMS? Incorrect values may damage your battery.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showConfirmDialog = false
                            viewModel.writeConfig()
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                    ) {
                        Text("Write")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showConfirmDialog = false }) {
                        Text("Cancel")
                    }
                },
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 8.dp),
    )
    HorizontalDivider()
}

@Composable
private fun ConfigField(label: String, value: String, unit: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = {})
    }
}
