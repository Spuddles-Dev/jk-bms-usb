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
                actions = {
                    if (state.hasUnsavedChanges) {
                        TextButton(onClick = { viewModel.resetEdits() }) {
                            Text("Reset")
                        }
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
            EditableField("Smart Sleep Voltage", config.volSmartSleep, "V") { v ->
                viewModel.updateEditConfig { cfg -> cfg.copy(volSmartSleep = v) }
            }
            EditableField("Cell UVP", config.volCellUV, "V") { v ->
                viewModel.updateEditConfig { cfg -> cfg.copy(volCellUV = v) }
            }
            EditableField("Cell UVPR", config.volCellUVPR, "V") { v ->
                viewModel.updateEditConfig { cfg -> cfg.copy(volCellUVPR = v) }
            }
            EditableField("Cell OVP", config.volCellOV, "V") { v ->
                viewModel.updateEditConfig { cfg -> cfg.copy(volCellOV = v) }
            }
            EditableField("Cell OVPR", config.volCellOVPR, "V") { v ->
                viewModel.updateEditConfig { cfg -> cfg.copy(volCellOVPR = v) }
            }
            EditableField("Balance Trigger", config.volBalanTrig, "V") { v ->
                viewModel.updateEditConfig { cfg -> cfg.copy(volBalanTrig = v) }
            }
            EditableField("SOC 100% Volt", config.volSOCP100, "V") { v ->
                viewModel.updateEditConfig { cfg -> cfg.copy(volSOCP100 = v) }
            }
            EditableField("SOC 0% Volt", config.volSOCP0, "V") { v ->
                viewModel.updateEditConfig { cfg -> cfg.copy(volSOCP0 = v) }
            }
            EditableField("Start Balance Volt", config.volStartBalan, "V") { v ->
                viewModel.updateEditConfig { cfg -> cfg.copy(volStartBalan = v) }
            }

            SectionHeader("Current Settings")
            EditableField("Charge Current Limit", config.timBatCOC, "A") { v ->
                viewModel.updateEditConfig { cfg -> cfg.copy(timBatCOC = v) }
            }
            EditableField("Discharge Current Limit", config.timBatDcOC, "A") { v ->
                viewModel.updateEditConfig { cfg -> cfg.copy(timBatDcOC = v) }
            }
            EditableField("Max Balance Current", config.curBalanMax, "A") { v ->
                viewModel.updateEditConfig { cfg -> cfg.copy(curBalanMax = v) }
            }
            EditableField("Current Range", config.currentRange, "A") { v ->
                viewModel.updateEditConfig { cfg -> cfg.copy(currentRange = v) }
            }

            SectionHeader("Temperature Protections")
            EditableField("Charge OTP", config.tmpBatCOT, "°C") { v ->
                viewModel.updateEditConfig { cfg -> cfg.copy(tmpBatCOT = v) }
            }
            EditableField("Charge OTPR", config.tmpBatCOTPR, "°C") { v ->
                viewModel.updateEditConfig { cfg -> cfg.copy(tmpBatCOTPR = v) }
            }
            EditableField("Discharge OTP", config.tmpBatDcOT, "°C") { v ->
                viewModel.updateEditConfig { cfg -> cfg.copy(tmpBatDcOT = v) }
            }
            EditableField("Discharge OTPR", config.tmpBatDcOTPR, "°C") { v ->
                viewModel.updateEditConfig { cfg -> cfg.copy(tmpBatDcOTPR = v) }
            }
            EditableField("Charge UTP", config.tmpBatCUT, "°C") { v ->
                viewModel.updateEditConfig { cfg -> cfg.copy(tmpBatCUT = v) }
            }
            EditableField("Charge UTPR", config.tmpBatCUTPR, "°C") { v ->
                viewModel.updateEditConfig { cfg -> cfg.copy(tmpBatCUTPR = v) }
            }
            EditableField("MOS OTP", config.tmpMosOT, "°C") { v ->
                viewModel.updateEditConfig { cfg -> cfg.copy(tmpMosOT = v) }
            }
            EditableField("MOS OTPR", config.tmpMosOTPR, "°C") { v ->
                viewModel.updateEditConfig { cfg -> cfg.copy(tmpMosOTPR = v) }
            }

            SectionHeader("Battery")
            EditableField("Cell Count", config.cellCount.toFloat(), "", isInteger = true) { v ->
                viewModel.updateEditConfig { cfg -> cfg.copy(cellCount = v.toLong()) }
            }
            EditableField("Battery Capacity", config.capBatCell, "Ah") { v ->
                viewModel.updateEditConfig { cfg -> cfg.copy(capBatCell = v) }
            }

            SectionHeader("Protection Delays")
            EditableField("Charge OCP Delay", config.timBatCOCPDly.toFloat(), "s", isInteger = true) { v ->
                viewModel.updateEditConfig { cfg -> cfg.copy(timBatCOCPDly = v.toLong()) }
            }
            EditableField("Charge OCPR Time", config.timBatCOCPRDly.toFloat(), "s", isInteger = true) { v ->
                viewModel.updateEditConfig { cfg -> cfg.copy(timBatCOCPRDly = v.toLong()) }
            }
            EditableField("Discharge OCP Delay", config.timBatDcOCPDly.toFloat(), "s", isInteger = true) { v ->
                viewModel.updateEditConfig { cfg -> cfg.copy(timBatDcOCPDly = v.toLong()) }
            }
            EditableField("Discharge OCPR Time", config.timBatDcOCPRDly.toFloat(), "s", isInteger = true) { v ->
                viewModel.updateEditConfig { cfg -> cfg.copy(timBatDcOCPRDly = v.toLong()) }
            }

            SectionHeader("Switches")
            EditableSwitch("Charge Enabled", config.batChargeEn == 1L) { checked ->
                viewModel.updateEditConfig { cfg -> cfg.copy(batChargeEn = if (checked) 1L else 0L) }
            }
            EditableSwitch("Discharge Enabled", config.batDischargeEn == 1L) { checked ->
                viewModel.updateEditConfig { cfg -> cfg.copy(batDischargeEn = if (checked) 1L else 0L) }
            }
            EditableSwitch("Balance Enabled", config.balanEn == 1L) { checked ->
                viewModel.updateEditConfig { cfg -> cfg.copy(balanEn = if (checked) 1L else 0L) }
            }

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
                enabled = !state.isWriting && state.hasUnsavedChanges && state.isValid,
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
private fun EditableField(
    label: String,
    value: Float,
    unit: String,
    isInteger: Boolean = false,
    onValueChange: (Float) -> Unit,
) {
    var textValue by remember(value) {
        mutableStateOf(if (isInteger) value.toInt().toString() else "%.3f".format(value))
    }
    var isValid by remember { mutableStateOf(true) }

    OutlinedTextField(
        value = textValue,
        onValueChange = { input ->
            textValue = input
            val parsed = input.toFloatOrNull()
            if (parsed != null) {
                isValid = true
                onValueChange(parsed)
            } else {
                isValid = input.isBlank() || input == "-"
            }
        },
        label = { Text(label) },
        suffix = if (unit.isNotEmpty()) { { Text(unit) } } else null,
        isError = !isValid,
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(
            keyboardType = if (isInteger) KeyboardType.Number else KeyboardType.Decimal,
        ),
        singleLine = true,
    )
}

@Composable
private fun EditableSwitch(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
