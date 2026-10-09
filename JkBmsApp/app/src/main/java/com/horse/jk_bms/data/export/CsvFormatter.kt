package com.horse.jk_bms.data.export

import com.horse.jk_bms.data.local.Converters
import com.horse.jk_bms.data.local.entity.RuntimeDataEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object CsvFormatter {
    val header = (listOf("schema_version", "timestamp_ms", "timestamp_utc", "device_id", "session_id",
        "battery_voltage_V", "current_A", "power_W", "soc_percent", "soh_percent", "active_cells",
        "mos_temp_C", "bat_temp1_C", "bat_temp2_C", "bat_temp3_C", "bat_temp4_C", "bat_temp5_C",
        "avg_cell_voltage_V", "max_volt_delta_V", "remaining_capacity_Ah", "full_charge_capacity_Ah",
        "charging", "discharging", "heating") + (1..32).map { "cell_${it}_V" }).joinToString(",")

    fun utc(timestamp: Long): String = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }.format(Date(timestamp))

    fun escape(value: Any?): String {
        val text = value?.toString().orEmpty()
        return if (text.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"${text.replace("\"", "\"\"")}\"" else text
    }

    fun runtimeRow(e: RuntimeDataEntity): String {
        val cells = Converters.toFloatArray(e.cellVoltages)
        return (listOf(2, e.timestamp, utc(e.timestamp), e.deviceId, e.sessionId, e.batVol, e.batCurrent,
            e.batWatt, e.soc, e.soh, cells.count { it > 0f }, e.tempMos, e.batTemp1, e.batTemp2, e.batTemp3,
            e.batTemp4, e.batTemp5, e.cellVolAve, e.maxVoltDelta, e.socCapabilityRemain,
            e.socFullChargeCapacity, e.chargeStatus, e.dischargeStatus, e.heatingStatus) +
            (0 until 32).map { cells.getOrNull(it)?.takeIf { voltage -> voltage > 0 } })
            .joinToString(",") { escape(it) }
    }

    fun formatRuntimeData(entities: List<RuntimeDataEntity>): String = buildString {
        appendLine(header)
        entities.forEach { appendLine(runtimeRow(it)) }
    }
}
