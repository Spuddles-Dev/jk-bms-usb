package com.horse.jk_bms.ui.screen.logs

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.horse.jk_bms.viewmodel.LogsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogsScreen(
    onBack: () -> Unit,
    viewModel: LogsViewModel = hiltViewModel(),
) {
    val systemLog by viewModel.systemLog.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("System Log") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
            )
        },
    ) { padding ->
        if (systemLog == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text("No log data available")
            }
            return@Scaffold
        }

        val log = systemLog!!

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
        ) {
            Text(
                "Log Count: ${log.logCount}",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (log.alarmLog.isEmpty()) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "No alarm log entries",
                        modifier = Modifier.padding(16.dp),
                    )
                }
            } else {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Alarm Log (${log.alarmLog.size} bytes)",
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            log.alarmLog.joinToString(" ") { "%02X".format(it) },
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}
