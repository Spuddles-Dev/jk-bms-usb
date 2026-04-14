package com.horse.jk_bms.data.export

import com.horse.jk_bms.data.local.entity.RuntimeDataEntity
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object JsonFormatter {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    fun formatRuntimeData(entities: List<RuntimeDataEntity>): String {
        val arr = JSONArray()
        for (e in entities) {
            val obj = JSONObject().apply {
                put("timestamp", dateFormat.format(Date(e.timestamp)))
                put("battery_voltage", e.batVol)
                put("current", e.batCurrent)
                put("power", e.batWatt)
                put("soc", e.soc)
                put("soh", e.soh)
                put("battery_type", e.batteryType)
                put("cycle_count", e.socCycleCount)
                put("mos_temp", e.tempMos)
                put("bat_temp1", e.batTemp1)
                put("bat_temp2", e.batTemp2)
                put("bat_temp3", e.batTemp3)
                put("bat_temp4", e.batTemp4)
                put("bat_temp5", e.batTemp5)
                put("max_volt_cell", e.celMaxVol)
                put("min_volt_cell", e.celMinVol)
                put("avg_cell_voltage", e.cellVolAve)
                put("max_volt_delta", e.maxVoltDelta)
                put("remaining_capacity", e.socCapabilityRemain)
                put("full_charge_capacity", e.socFullChargeCapacity)
                put("heating_status", e.heatingStatus)
                put("charge_status", e.chargeStatus)
                put("discharge_status", e.dischargeStatus)
            }
            arr.put(obj)
        }
        return arr.toString(2)
    }
}
