package com.horse.jk_bms.ui.screen.cells

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
import com.horse.jk_bms.viewmodel.CellsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CellsScreen(
    onBack: () -> Unit,
    viewModel: CellsViewModel = hiltViewModel(),
) {
    val runtimeData by viewModel.runtimeData.collectAsState()
    val data = runtimeData

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cell Voltages") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
            )
        },
    ) { padding ->
        if (data == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text("No data available")
            }
            return@Scaffold
        }

        val maxVoltage = data.cellVoltages.maxOfOrNull { it } ?: 0f
        val minVoltage = data.cellVoltages.filter { it > 0f }.minOfOrNull { it } ?: 0f

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(4.dp))
            CellSummaryRow("Avg Voltage", "%.3f V".format(data.cellVolAve))
            CellSummaryRow("Max Delta", "%.3f V".format(data.maxVoltDelta))
            CellSummaryRow("Highest", "#${data.celMaxVol}")
            CellSummaryRow("Lowest", "#${data.celMinVol}")
            CellSummaryRow("Balance Current", "%.3f A".format(data.equCurrent))

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text("Individual Cells", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))

            for (i in 0 until 32) {
                val voltage = data.cellVoltages[i]
                if (voltage <= 0f) continue

                val isMax = i + 1 == data.celMaxVol
                val isMin = i + 1 == data.celMinVol

                CellBar(
                    cellNumber = i + 1,
                    voltage = voltage,
                    maxVoltage = maxVoltage,
                    wireResistance = data.cellWireRes[i],
                    isMax = isMax,
                    isMin = isMin,
                )
            }
        }
    }
}

@Composable
private fun CellSummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun CellBar(
    cellNumber: Int,
    voltage: Float,
    maxVoltage: Float,
    wireResistance: Float,
    isMax: Boolean,
    isMin: Boolean,
) {
    val fraction = if (maxVoltage > 0f) voltage / maxVoltage else 0f
    val color = when {
        isMax -> MaterialTheme.colorScheme.error
        isMin -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "%02d".format(cellNumber),
                modifier = Modifier.width(32.dp),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = color,
            )
            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier
                    .weight(1f)
                    .height(16.dp)
                    .padding(horizontal = 4.dp),
                color = color,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
            Text(
                text = "%.3f V".format(voltage),
                modifier = Modifier.width(72.dp),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
            )
            if (wireResistance > 0f) {
                Text(
                    text = "%.1f mΩ".format(wireResistance * 1000),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
