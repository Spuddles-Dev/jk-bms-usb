package com.horse.jk_bms.data.export

import com.horse.jk_bms.data.repository.DataLogRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext

enum class ExportFormat { CSV, JSON }

@Singleton
class DataExporter @Inject constructor(private val repository: DataLogRepository) {
    suspend fun exportRuntimeData(fromTimestamp: Long, format: ExportFormat, outputDir: File): File =
        withContext(Dispatchers.IO) {
            outputDir.mkdirs()
            val file = File(outputDir, "jk-bms-runtime-${UUID.randomUUID()}.${format.name.lowercase()}")
            try {
                val upper = repository.latestRuntimeId()
                var after = 0L
                file.bufferedWriter().use { writer ->
                    if (format == ExportFormat.CSV) writer.appendLine(CsvFormatter.header)
                    else writer.write("{\"schema_version\":2,\"kind\":\"runtime\",\"rows\":[")
                    var first = true
                    while (true) {
                        coroutineContext.ensureActive()
                        val page = repository.runtimePage(fromTimestamp, upper, after)
                        if (page.isEmpty()) break
                        for (entry in page) {
                            if (format == ExportFormat.CSV) writer.appendLine(CsvFormatter.runtimeRow(entry))
                            else {
                                if (!first) writer.write(",")
                                writer.write(JsonFormatter.runtimeRow(entry).toString())
                            }
                            first = false
                        }
                        after = page.last().id
                    }
                    if (format == ExportFormat.JSON) writer.write("]}")
                }
                file
            } catch (error: Exception) { file.delete(); throw error }
        }

    suspend fun exportFaults(outputDir: File, format: ExportFormat): File = withContext(Dispatchers.IO) {
        outputDir.mkdirs()
        val file = File(outputDir, "jk-bms-faults-${UUID.randomUUID()}.${format.name.lowercase()}")
        try {
            val upper = repository.latestFaultId()
            var after = 0L
            file.bufferedWriter().use { writer ->
                if (format == ExportFormat.CSV) writer.appendLine(
                    "schema_version,observed_timestamp_ms,observed_timestamp_utc,device_id,session_id,bms_rtc_count,log_code,max_cell,min_cell,max_voltage_V,min_voltage_V,pack_voltage_V,current_A")
                else writer.write("{\"schema_version\":2,\"kind\":\"faults\",\"rows\":[")
                var first = true
                while (true) {
                    coroutineContext.ensureActive()
                    val page = repository.faultPage(after, upper)
                    if (page.isEmpty()) break
                    for (entry in page) {
                        if (format == ExportFormat.CSV) writer.appendLine(listOf(
                            2, entry.timestamp, CsvFormatter.utc(entry.timestamp), entry.deviceId, entry.sessionId, entry.rtcCount, entry.logCode,
                            entry.maxVolCellNo, entry.minVolCellNo, entry.volCellMax, entry.volCellMin, entry.volBat,
                            entry.curBat).joinToString(",") { CsvFormatter.escape(it) })
                        else {
                            if (!first) writer.write(",")
                            writer.write(JsonFormatter.faultRow(entry).toString())
                        }
                        first = false
                    }
                    after = page.last().id
                }
                if (format == ExportFormat.JSON) writer.write("]}")
            }
            file
        } catch (error: Exception) { file.delete(); throw error }
    }
}
