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
        viewModelScope.launch {
            _state.value = ExportState(isExporting = true)
            try {
                val fromTimestamp = System.currentTimeMillis() - hours * 60 * 60 * 1000L
                val cacheDir = File(context.cacheDir, "exports").also { it.mkdirs() }
                val file = dataExporter.exportRuntimeData(fromTimestamp, format, cacheDir)
                _state.value = ExportState(exportComplete = true, exportedFile = file.absolutePath)
                shareFile(context, file)
            } catch (e: Exception) {
                _state.value = ExportState(error = e.message)
            }
        }
    }

    fun exportFaults(context: Context, format: ExportFormat) {
        viewModelScope.launch {
            _state.value = ExportState(isExporting = true)
            try {
                val cacheDir = File(context.cacheDir, "exports").also { it.mkdirs() }
                val file = dataExporter.exportFaults(cacheDir, format)
                _state.value = ExportState(exportComplete = true, exportedFile = file.absolutePath)
                shareFile(context, file)
            } catch (e: Exception) {
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
            type = "text/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Export BMS Data"))
    }

    fun resetState() {
        _state.value = ExportState()
    }
}
