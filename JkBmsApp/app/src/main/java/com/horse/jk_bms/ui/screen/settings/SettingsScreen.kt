package com.horse.jk_bms.ui.screen.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.horse.jk_bms.protocol.ConfigFieldValidator
import com.horse.jk_bms.protocol.ConfigSchema
import com.horse.jk_bms.data.export.CsvFormatter
import com.horse.jk_bms.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    var confirm by remember { mutableStateOf(false) }
    var backupName by remember { mutableStateOf("") }
    var chooseBackup by remember { mutableStateOf(false) }
    Scaffold(topBar = {
        TopAppBar(title = { Text("BMS Settings") }, navigationIcon = {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
        }, actions = { TextButton(onClick = viewModel::resetEdits) { Text("Reset") } })
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (state.config == null) {
                Text("Connect to a BMS to read settings")
                return@Column
            }
            if (!viewModel.writesEnabled) Text("Monitoring preview: writes await hardware validation")
            OutlinedTextField(value = backupName, onValueChange = { backupName = it },
                label = { Text("Backup name (optional)") }, isError = backupName.length > 80,
                modifier = Modifier.fillMaxWidth(), singleLine = true)
            TextButton(onClick = { viewModel.saveBackup(backupName) }, enabled = backupName.length <= 80) {
                Text("Save configuration backup")
            }
            TextButton(onClick = viewModel::loadLatestBackup) { Text("Compare latest backup") }
            TextButton(onClick = { viewModel.refreshBackups(); chooseBackup = true }) { Text("Choose saved backup") }
            DropdownMenu(expanded = chooseBackup, onDismissRequest = { chooseBackup = false }) {
                if (state.backups.isEmpty()) DropdownMenuItem(text = { Text("No matching backups") }, onClick = {}, enabled = false)
                state.backups.forEach { backup ->
                    DropdownMenuItem(text = { Text("${backup.name} • ${CsvFormatter.utc(backup.savedAt)}") },
                        onClick = { chooseBackup = false; viewModel.loadBackup(backup.id) })
                }
            }
            state.backupMessage?.let { Text(it) }
            state.error?.let { Text(it) }
            if (state.conflicts.isNotEmpty()) Text("BMS settings changed: ${state.conflicts.joinToString()}. Reset to refresh.")
            ConfigSchema.fields.forEach { field ->
                OutlinedTextField(
                    value = state.inputs[field.name].orEmpty(),
                    onValueChange = { viewModel.updateInput(field.name, it) },
                    label = { Text(field.label) },
                    suffix = { Text(field.unit) },
                    supportingText = { Text(state.inputErrors[field.name] ?: "${field.min}–${field.max}") },
                    isError = field.name in state.inputErrors || field.name in state.conflicts,
                    enabled = !state.isWriting,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = if (field.integral) KeyboardType.Number else KeyboardType.Decimal),
                )
            }
            state.editConfig?.let { edited ->
                val invalid = ConfigFieldValidator.validateAll(edited).filterValues { !it }.keys
                if (invalid.isNotEmpty()) Text("Check configuration: ${invalid.joinToString()}")
                Text("Capability bits are read-only. Array values are preserved unless a backup is selected.")
                if ("arrays" in state.dirty) {
                    Text("Backup connection-wire resistances: ${edited.cellConWireRes.joinToString()}")
                    Text("Backup switch bits: ${edited.switchStatus.indices.filter { edited.switchStatus[it] }}")
                }
            }
            if (state.writeSuccess == true) Text("Configuration verified by readback")
            Button(onClick = { confirm = true }, enabled = viewModel.writesEnabled && state.hasUnsavedChanges &&
                state.isValid && !state.isWriting, modifier = Modifier.fillMaxWidth()) { Text("Review and write") }
            Spacer(Modifier.height(24.dp))
        }
        if (confirm) {
            val changes = ConfigSchema.fields.filter { it.name in state.dirty }.joinToString("\n") {
                "${it.label}: ${state.config?.let(it.get)} → ${state.inputs[it.name]} ${it.unit}"
            } + if ("arrays" in state.dirty) buildString {
                val before = state.config
                val after = state.editConfig
                if (before != null && after != null) {
                    before.cellConWireRes.indices.forEach { index ->
                        if (before.cellConWireRes[index] != after.cellConWireRes[index]) {
                            append("\nWire ${index + 1}: ${before.cellConWireRes[index]} → ${after.cellConWireRes[index]} mΩ")
                        }
                    }
                    before.switchStatus.indices.forEach { index ->
                        if (before.switchStatus[index] != after.switchStatus[index]) {
                            append("\nSwitch bit $index: ${before.switchStatus[index]} → ${after.switchStatus[index]}")
                        }
                    }
                }
            } else ""
            AlertDialog(onDismissRequest = { confirm = false }, title = { Text("Review changes") },
                text = { Text(changes) },
                confirmButton = { TextButton(onClick = { confirm = false; viewModel.writeConfig() }) { Text("Write and verify") } },
                dismissButton = { TextButton(onClick = { confirm = false }) { Text("Cancel") } })
        }
    }
}
