package com.horse.jk_bms.ui.screen.history

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.horse.jk_bms.viewmodel.DiagnosticsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticsScreen(onBack: () -> Unit, viewModel: DiagnosticsViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    val capture = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.replay(context, uri)
    }
    Scaffold(topBar = { TopAppBar(title = { Text("Diagnostics and replay") }, navigationIcon = {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
    }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Export the latest 256 valid protocol frames. Device information and credentials are excluded.")
            TextButton(onClick = { viewModel.share(context) }) { Text("Export diagnostic capture") }
            TextButton(onClick = { capture.launch(arrayOf("*/*")) }) { Text("Replay a capture") }
            if (state.running || state.data != null) Text("DEMO REPLAY • captured data")
            Text("Replay stays separate from the USB session and does not send commands.")
            state.data?.let { Text("SOC ${it.soc}% • ${it.batVol} V • ${it.batCurrent} A • ${it.activeCellCount} cells") }
            state.error?.let { Text(it) }
            if (state.running) TextButton(onClick = viewModel::stop) { Text("Stop replay") }
        }
    }
}
