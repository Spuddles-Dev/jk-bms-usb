package com.horse.jk_bms.ui.screen.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.horse.jk_bms.data.export.ExportFormat
import com.horse.jk_bms.viewmodel.ExportViewModel

@Composable
fun ExportDialog(
    onDismiss: () -> Unit,
    viewModel: ExportViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    var selectedFormat by remember { mutableStateOf(ExportFormat.CSV) }
    var exportType by remember { mutableStateOf("runtime") }
    var hours by remember { mutableIntStateOf(24) }

    LaunchedEffect(state.exportComplete, state.error) {
        if (state.exportComplete || state.error != null) {
            onDismiss()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export Data") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Format", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedFormat == ExportFormat.CSV,
                        onClick = { selectedFormat = ExportFormat.CSV },
                        label = { Text("CSV") },
                    )
                    FilterChip(
                        selected = selectedFormat == ExportFormat.JSON,
                        onClick = { selectedFormat = ExportFormat.JSON },
                        label = { Text("JSON") },
                    )
                }

                Text("Data Type", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = exportType == "runtime",
                        onClick = { exportType = "runtime" },
                        label = { Text("Runtime") },
                    )
                    FilterChip(
                        selected = exportType == "faults",
                        onClick = { exportType = "faults" },
                        label = { Text("Faults") },
                    )
                }

                if (exportType == "runtime") {
                    Text("Time Range", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(1, 6, 24, 72).forEach { h ->
                            FilterChip(
                                selected = hours == h,
                                onClick = { hours = h },
                                label = { Text("${h}h") },
                            )
                        }
                    }
                }

                if (state.isExporting) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (exportType == "runtime") {
                        viewModel.exportRuntimeData(context, selectedFormat, hours)
                    } else {
                        viewModel.exportFaults(context, selectedFormat)
                    }
                },
                enabled = !state.isExporting,
            ) {
                Text("Export")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
