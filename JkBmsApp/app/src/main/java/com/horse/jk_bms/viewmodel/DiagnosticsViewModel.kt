package com.horse.jk_bms.viewmodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.horse.jk_bms.BuildConfig
import com.horse.jk_bms.diagnostics.CaptureReplay
import com.horse.jk_bms.diagnostics.ProtocolTrace
import com.horse.jk_bms.diagnostics.TraceEntry
import com.horse.jk_bms.model.BmsRuntimeData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID
import javax.inject.Inject

data class DiagnosticCapture(val schemaVersion: Int = 1, val appVersion: String = BuildConfig.VERSION_NAME,
    val entries: List<TraceEntry> = emptyList())
data class ReplayState(val running: Boolean = false, val data: BmsRuntimeData? = null, val error: String? = null)

@HiltViewModel
class DiagnosticsViewModel @Inject constructor(private val trace: ProtocolTrace) : ViewModel() {
    private val mutable = MutableStateFlow(ReplayState())
    val state = mutable.asStateFlow()
    private var playback: Job? = null
    private val gson = Gson()

    fun share(context: Context) {
        viewModelScope.launch {
            try {
                val file = withContext(Dispatchers.IO) {
                    val directory = File(context.cacheDir, "exports").apply { mkdirs() }
                    File(directory, "jk-bms-diagnostics-${UUID.randomUUID()}.json")
                        .also { it.writeText(gson.toJson(DiagnosticCapture(entries = trace.snapshot()))) }
                }
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }, "Export diagnostic capture"))
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                mutable.value = mutable.value.copy(error = error.message)
            }
        }
    }

    fun replay(context: Context, uri: Uri) {
        playback?.cancel()
        playback = viewModelScope.launch {
            try {
                val samples = withContext(Dispatchers.IO) {
                    val output = ByteArrayOutputStream()
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        val chunk = ByteArray(4096)
                        while (true) {
                            val count = input.read(chunk)
                            if (count < 0) break
                            require(output.size() + count <= 2 * 1024 * 1024) { "Capture exceeds 2 MiB" }
                            output.write(chunk, 0, count)
                        }
                    } ?: error("Capture could not be opened")
                    val capture = gson.fromJson(output.toString("UTF-8"), DiagnosticCapture::class.java)
                    require(capture.schemaVersion == 1) { "Unsupported capture version" }
                    CaptureReplay.runtimes(capture.entries)
                }
                require(samples.isNotEmpty()) { "Capture contains no runtime responses" }
                mutable.value = ReplayState(running = true)
                samples.forEach { mutable.value = ReplayState(running = true, data = it); delay(250) }
                mutable.value = mutable.value.copy(running = false)
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                mutable.value = ReplayState(error = error.message)
            }
        }
    }

    fun stop() { playback?.cancel(); mutable.value = ReplayState() }
}
