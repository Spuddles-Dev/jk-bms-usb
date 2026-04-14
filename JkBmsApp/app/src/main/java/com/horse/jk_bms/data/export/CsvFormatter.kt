package com.horse.jk_bms.data.export

import com.horse.jk_bms.data.local.entity.RuntimeDataEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvFormatter {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    fun formatRuntimeData(entities: List<RuntimeDataEntity>): String {
        val sb = StringBuilder()
        sb.appendLine("timestamp,battery_voltage,current,power,soc,soh,battery_type,cycle_count,mos_temp,bat_temp1,bat_temp2,bat_temp3,bat_temp4,bat_temp5,active_cells,avg_cell_voltage,max_volt_delta,remaining_capacity,full_charge_capacity,heating_status,charge_status,discharge_status")
        for (e in entities) {
            sb.appendLine(
                "${dateFormat.format(Date(e.timestamp))}," +
                "${e.batVol},${e.batCurrent},${e.batWatt}," +
                "${e.soc},${e.soh},${e.batteryType},${e.socCycleCount}," +
                "${e.tempMos},${e.batTemp1},${e.batTemp2},${e.batTemp3},${e.batTemp4},${e.batTemp5}," +
                "${e.celMaxVol},${e.cellVolAve},${e.maxVoltDelta}," +
                "${e.socCapabilityRemain},${e.socFullChargeCapacity}," +
                "${e.heatingStatus},${e.chargeStatus},${e.dischargeStatus}"
            )
        }
        return sb.toString()
    }
}
