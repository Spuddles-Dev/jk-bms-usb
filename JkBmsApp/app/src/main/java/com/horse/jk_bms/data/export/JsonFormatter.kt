package com.horse.jk_bms.data.export

import com.horse.jk_bms.data.local.Converters
import com.horse.jk_bms.data.local.entity.RuntimeDataEntity
import com.horse.jk_bms.data.local.entity.FaultRecordEntity
import org.json.JSONArray
import org.json.JSONObject

object JsonFormatter {
    fun faultRow(e: FaultRecordEntity): JSONObject = JSONObject().apply {
        put("observed_timestamp_ms", e.timestamp)
        put("observed_timestamp_utc", CsvFormatter.utc(e.timestamp))
        put("device_id", e.deviceId)
        put("session_id", e.sessionId)
        put("bms_rtc_count", e.rtcCount)
        put("log_code", e.logCode)
        put("switch_flags", JSONArray(Converters.toBooleanArray(e.switchSta).toList()))
        put("max_cell", e.maxVolCellNo)
        put("min_cell", e.minVolCellNo)
        put("max_cell_voltage_V", e.volCellMax)
        put("min_cell_voltage_V", e.volCellMin)
        put("pack_voltage_V", e.volBat)
        put("current_A", e.curBat)
        put("remaining_capacity_Ah", e.socCapRemain)
        put("full_charge_capacity_Ah", e.socFullChargeCap)
        put("max_temperature_C", e.maxTemp)
        put("min_temperature_C", e.minTemp)
        put("mos_temperature_C", e.tempMos)
        put("heating_current_A", e.heatCurrent)
    }

    fun runtimeRow(e: RuntimeDataEntity): JSONObject = JSONObject().apply {
        put("timestamp_ms", e.timestamp)
        put("timestamp_utc", CsvFormatter.utc(e.timestamp))
        put("device_id", e.deviceId)
        put("session_id", e.sessionId)
        put("battery_voltage_V", e.batVol)
        put("current_A", e.batCurrent)
        put("power_W", e.batWatt)
        put("soc_percent", e.soc)
        put("soh_percent", e.soh)
        val cells = Converters.toFloatArray(e.cellVoltages)
        put("active_cells", cells.count { it > 0 })
        put("cell_voltages_V", JSONArray(cells.map { if (it > 0) it else JSONObject.NULL }))
        put("avg_cell_voltage_V", e.cellVolAve)
        put("max_volt_delta_V", e.maxVoltDelta)
        put("mos_temp_C", e.tempMos)
        put("battery_temperatures_C", JSONArray(listOf(e.batTemp1, e.batTemp2, e.batTemp3, e.batTemp4, e.batTemp5)))
        put("temperature_sensor_flags_raw", JSONArray(Converters.toBooleanArray(e.tempSensorAbsent).toList()))
        put("remaining_capacity_Ah", e.socCapabilityRemain)
        put("full_charge_capacity_Ah", e.socFullChargeCapacity)
        put("charge_status", e.chargeStatus)
        put("discharge_status", e.dischargeStatus)
        put("heating_status", e.heatingStatus)
        put("cell_wiring_status", JSONArray(Converters.toBooleanArray(e.cellWireResStat).toList()))
        put("alarms", JSONArray(Converters.toBooleanArray(e.sysAlarm).toList()))
        put("capability_bytes", e.enableFlags)
    }

    fun formatRuntimeData(entities: List<RuntimeDataEntity>): String = JSONObject().apply {
        put("schema_version", 2)
        put("rows", JSONArray(entities.map(::runtimeRow)))
    }.toString()
}
