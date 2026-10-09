package com.horse.jk_bms.viewmodel

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.horse.jk_bms.data.export.DataExporter
import com.horse.jk_bms.data.export.ExportFormat
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import java.io.File
import javax.inject.Inject

data class ExportState(
    val isExporting: Boolean = false,
    val exportComplete: Boolean = false,
    val exportedFile: String = "",
    val error: String? = null,
)

@HiltViewModel
class ExportViewModel @Inject constructor(
    private val dataExporter: DataExporter,
) : ViewModel() {

    private val _state = MutableStateFlow(ExportState())
    val state: StateFlow<ExportState> = _state.asStateFlow()

    fun exportRuntimeData(context: Context, format: ExportFormat, hours: Int = 24) {
        if (_state.value.isExporting) return
        _state.value = ExportState(isExporting = true)
        viewModelScope.launch {
            _state.value = ExportState(isExporting = true)
            try {
                val fromTimestamp = System.currentTimeMillis() - hours * 60 * 60 * 1000L
                val cacheDir = File(context.cacheDir, "exports").also { it.mkdirs() }
                val file = dataExporter.exportRuntimeData(fromTimestamp, format, cacheDir)
                shareFile(context, file)
                _state.value = ExportState(exportComplete = true, exportedFile = file.absolutePath)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _state.value = ExportState(error = e.message)
            }
        }
    }

    fun exportFaults(context: Context, format: ExportFormat) {
        if (_state.value.isExporting) return
        _state.value = ExportState(isExporting = true)
        viewModelScope.launch {
            _state.value = ExportState(isExporting = true)
            try {
                val cacheDir = File(context.cacheDir, "exports").also { it.mkdirs() }
                val file = dataExporter.exportFaults(cacheDir, format)
                shareFile(context, file)
                _state.value = ExportState(exportComplete = true, exportedFile = file.absolutePath)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _state.value = ExportState(error = e.message)
            }
        }
    }

    private fun shareFile(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = if (file.extension == "json") "application/json" else "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Export BMS Data"))
    }

    fun resetState() {
        if (_state.value.isExporting) return
        _state.value = ExportState()
    }
}
