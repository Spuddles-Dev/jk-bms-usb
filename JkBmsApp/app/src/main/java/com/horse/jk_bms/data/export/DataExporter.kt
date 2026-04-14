package com.horse.jk_bms.data.export

import com.horse.jk_bms.data.local.entity.FaultRecordEntity
import com.horse.jk_bms.data.local.entity.RuntimeDataEntity
import com.horse.jk_bms.data.repository.DataLogRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

enum class ExportFormat { CSV, JSON }

@Singleton
class DataExporter @Inject constructor(
    private val dataLogRepository: DataLogRepository,
) {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    suspend fun exportRuntimeData(
        fromTimestamp: Long,
        format: ExportFormat,
        outputDir: File,
    ): File = withContext(Dispatchers.IO) {
        val entities = dataLogRepository.getRuntimeDataRange(fromTimestamp)
        val dateStr = dateFormat.format(Date(fromTimestamp))
        val content = when (format) {
            ExportFormat.CSV -> CsvFormatter.formatRuntimeData(entities)
            ExportFormat.JSON -> JsonFormatter.formatRuntimeData(entities)
        }
        val ext = when (format) {
            ExportFormat.CSV -> "csv"
            ExportFormat.JSON -> "json"
        }
        val file = File(outputDir, "jk-bms-runtime-$dateStr.$ext")
        file.writeText(content)
        file
    }

    suspend fun exportFaults(
        outputDir: File,
        format: ExportFormat,
    ): File = withContext(Dispatchers.IO) {
        val entities = dataLogRepository.getAllFaults()
        val dateStr = dateFormat.format(Date())
        val ext = when (format) {
            ExportFormat.CSV -> "csv"
            ExportFormat.JSON -> "json"
        }
        val file = File(outputDir, "jk-bms-faults-$dateStr.$ext")
        when (format) {
            ExportFormat.CSV -> file.writeText(formatFaultsCsv(entities))
            ExportFormat.JSON -> file.writeText(formatFaultsJson(entities))
        }
        file
    }

    private fun formatFaultsCsv(entities: List<FaultRecordEntity>): String {
        val sb = StringBuilder()
        sb.appendLine("timestamp,log_code,max_vol_cell,min_vol_cell,cell_max_v,cell_min_v,bat_v,bat_a,soc_remain,soc_full,max_temp,min_temp,mos_temp")
        for (e in entities) {
            sb.appendLine(
                "${e.timestamp},${e.logCode},${e.maxVolCellNo},${e.minVolCellNo}," +
                "${e.volCellMax},${e.volCellMin},${e.volBat},${e.curBat}," +
                "${e.socCapRemain},${e.socFullChargeCap},${e.maxTemp},${e.minTemp},${e.tempMos}"
            )
        }
        return sb.toString()
    }

    private fun formatFaultsJson(entities: List<FaultRecordEntity>): String {
        val arr = org.json.JSONArray()
        for (e in entities) {
            val obj = org.json.JSONObject().apply {
                put("timestamp", e.timestamp)
                put("log_code", e.logCode)
                put("max_vol_cell", e.maxVolCellNo)
                put("min_vol_cell", e.minVolCellNo)
                put("cell_max_v", e.volCellMax)
                put("cell_min_v", e.volCellMin)
                put("bat_v", e.volBat)
                put("bat_a", e.curBat)
                put("soc_remain", e.socCapRemain)
                put("soc_full", e.socFullChargeCap)
                put("max_temp", e.maxTemp)
                put("min_temp", e.minTemp)
                put("mos_temp", e.tempMos)
            }
            arr.put(obj)
        }
        return arr.toString(2)
    }
}
