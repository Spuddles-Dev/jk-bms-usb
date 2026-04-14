package com.horse.jk_bms.protocol

import org.junit.Assert.*
import org.junit.Test

class RuntimeDataParserTest {

    private fun makeData(setup: (ByteArray) -> Unit): ByteArray {
        val data = ByteArray(293)
        setup(data)
        return data
    }

    @Test
    fun testCellVoltagesArray() {
        val data = makeData { buf ->
            for (i in 0 until 32) {
                FieldEncoder.writeU16(buf, i * 2, 3300 + i)
            }
        }
        val result = RuntimeDataParser.parse(data).cellVoltages
        assertEquals(32, result.size)
        for (i in 0 until 32) {
            assertEquals((3300 + i) * 0.001f, result[i], 0.001f)
        }
    }

    @Test
    fun testCellStatus() {
        val data = makeData { buf ->
            buf[64] = 0b10101010.toByte()
            buf[65] = 0x00.toByte()
            buf[66] = 0x00.toByte()
            buf[67] = 0x00.toByte()
        }
        val result = RuntimeDataParser.parse(data).cellStatus
        assertEquals(32, result.size)
        assertFalse(result[0])
        assertTrue(result[1])
        assertFalse(result[2])
        assertTrue(result[3])
    }

    @Test
    fun testCellVolAve() {
        val data = makeData { FieldEncoder.writeU16(it, 68, 3350) }
        assertEquals(3.35f, RuntimeDataParser.parse(data).cellVolAve, 0.001f)
    }

    @Test
    fun testMaxVoltDelta() {
        val data = makeData { FieldEncoder.writeU16(it, 70, 15) }
        assertEquals(0.015f, RuntimeDataParser.parse(data).maxVoltDelta, 0.001f)
    }

    @Test
    fun testCelMaxVol() {
        val data = makeData { FieldEncoder.writeU8(it, 72, 7) }
        assertEquals(7, RuntimeDataParser.parse(data).celMaxVol)
    }

    @Test
    fun testCelMinVol() {
        val data = makeData { FieldEncoder.writeU8(it, 73, 3) }
        assertEquals(3, RuntimeDataParser.parse(data).celMinVol)
    }

    @Test
    fun testCellWireResArray() {
        val data = makeData { buf ->
            for (i in 0 until 32) {
                FieldEncoder.writeU16(buf, 74 + i * 2, 100 + i)
            }
        }
        val result = RuntimeDataParser.parse(data).cellWireRes
        assertEquals(32, result.size)
        for (i in 0 until 32) {
            assertEquals((100 + i) * 0.001f, result[i], 0.001f)
        }
    }

    @Test
    fun testTempMos() {
        val data = makeData { FieldEncoder.writeI16(it, 138, 350) }
        assertEquals(35.0f, RuntimeDataParser.parse(data).tempMos, 0.1f)
    }

    @Test
    fun testTempMosNegative() {
        val data = makeData { FieldEncoder.writeI16(it, 138, -50) }
        assertEquals(-5.0f, RuntimeDataParser.parse(data).tempMos, 0.1f)
    }

    @Test
    fun testBatVol() {
        val data = makeData { FieldEncoder.writeI32(it, 144, 52400) }
        assertEquals(52.4f, RuntimeDataParser.parse(data).batVol, 0.001f)
    }

    @Test
    fun testBatVolNegative() {
        val data = makeData { FieldEncoder.writeI32(it, 144, -100) }
        assertEquals(-0.1f, RuntimeDataParser.parse(data).batVol, 0.001f)
    }

    @Test
    fun testBatWatt() {
        val data = makeData { FieldEncoder.writeU32(it, 148, 5000000L) }
        assertEquals(5000.0f, RuntimeDataParser.parse(data).batWatt, 0.001f)
    }

    @Test
    fun testBatCurrentCharging() {
        val data = makeData { FieldEncoder.writeI32(it, 152, 20000) }
        assertEquals(20.0f, RuntimeDataParser.parse(data).batCurrent, 0.001f)
    }

    @Test
    fun testBatCurrentDischarging() {
        val data = makeData { FieldEncoder.writeI32(it, 152, -30000) }
        assertEquals(-30.0f, RuntimeDataParser.parse(data).batCurrent, 0.001f)
    }

    @Test
    fun testBatTemp1() {
        val data = makeData { FieldEncoder.writeI16(it, 156, 250) }
        assertEquals(25.0f, RuntimeDataParser.parse(data).batTemp1, 0.1f)
    }

    @Test
    fun testBatTemp1Negative() {
        val data = makeData { FieldEncoder.writeI16(it, 156, -100) }
        assertEquals(-10.0f, RuntimeDataParser.parse(data).batTemp1, 0.1f)
    }

    @Test
    fun testBatTemp2() {
        val data = makeData { FieldEncoder.writeI16(it, 158, 300) }
        assertEquals(30.0f, RuntimeDataParser.parse(data).batTemp2, 0.1f)
    }

    @Test
    fun testSysAlarm() {
        val data = makeData { buf ->
            buf[160] = 0x01.toByte()
            buf[161] = 0x00.toByte()
            buf[162] = 0x00.toByte()
            buf[163] = 0x00.toByte()
        }
        val result = RuntimeDataParser.parse(data).sysAlarm
        assertEquals(32, result.size)
        assertTrue(result[0])
        assertFalse(result[1])
    }

    @Test
    fun testEquCurrent() {
        val data = makeData { FieldEncoder.writeI16(it, 164, 500) }
        assertEquals(0.5f, RuntimeDataParser.parse(data).equCurrent, 0.001f)
    }

    @Test
    fun testEquCurrentNegative() {
        val data = makeData { FieldEncoder.writeI16(it, 164, -200) }
        assertEquals(-0.2f, RuntimeDataParser.parse(data).equCurrent, 0.001f)
    }

    @Test
    fun testEquStatus() {
        val data = makeData { FieldEncoder.writeU8(it, 166, 3) }
        assertEquals(3, RuntimeDataParser.parse(data).equStatus)
    }

    @Test
    fun testSoc() {
        val data = makeData { FieldEncoder.writeU8(it, 167, 85) }
        assertEquals(85, RuntimeDataParser.parse(data).soc)
    }

    @Test
    fun testSocFull() {
        val data = makeData { FieldEncoder.writeU8(it, 167, 100) }
        assertEquals(100, RuntimeDataParser.parse(data).soc)
    }

    @Test
    fun testSocCapabilityRemain() {
        val data = makeData { FieldEncoder.writeU32(it, 168, 85000L) }
        assertEquals(85.0f, RuntimeDataParser.parse(data).socCapabilityRemain, 0.001f)
    }

    @Test
    fun testSocFullChargeCapacity() {
        val data = makeData { FieldEncoder.writeU32(it, 172, 100000L) }
        assertEquals(100.0f, RuntimeDataParser.parse(data).socFullChargeCapacity, 0.001f)
    }

    @Test
    fun testSocCycleCount() {
        val data = makeData { FieldEncoder.writeU32(it, 176, 42L) }
        assertEquals(42L, RuntimeDataParser.parse(data).socCycleCount)
    }

    @Test
    fun testSocCycleCapacity() {
        val data = makeData { FieldEncoder.writeU32(it, 180, 50000L) }
        assertEquals(50.0f, RuntimeDataParser.parse(data).socCycleCapacity, 0.001f)
    }

    @Test
    fun testSoh() {
        val data = makeData { FieldEncoder.writeU8(it, 184, 97) }
        assertEquals(97, RuntimeDataParser.parse(data).soh)
    }

    @Test
    fun testPreDischargeTrue() {
        val data = makeData { FieldEncoder.writeU8(it, 185, 1) }
        assertTrue(RuntimeDataParser.parse(data).preDischarge)
    }

    @Test
    fun testPreDischargeFalse() {
        val data = makeData { FieldEncoder.writeU8(it, 185, 0) }
        assertFalse(RuntimeDataParser.parse(data).preDischarge)
    }

    @Test
    fun testUserAlarm() {
        val data = makeData { FieldEncoder.writeU16(it, 186, 0x0003) }
        assertEquals(0x0003, RuntimeDataParser.parse(data).userAlarm)
    }

    @Test
    fun testRuntime() {
        val data = makeData { FieldEncoder.writeU32(it, 188, 3600L) }
        assertEquals(3600L, RuntimeDataParser.parse(data).runtime)
    }

    @Test
    fun testChargeStatusTrue() {
        val data = makeData { FieldEncoder.writeU8(it, 192, 1) }
        assertTrue(RuntimeDataParser.parse(data).chargeStatus)
    }

    @Test
    fun testChargeStatusFalse() {
        val data = makeData { FieldEncoder.writeU8(it, 192, 0) }
        assertFalse(RuntimeDataParser.parse(data).chargeStatus)
    }

    @Test
    fun testDischargeStatusTrue() {
        val data = makeData { FieldEncoder.writeU8(it, 193, 1) }
        assertTrue(RuntimeDataParser.parse(data).dischargeStatus)
    }

    @Test
    fun testUserAlarm2() {
        val data = makeData { buf ->
            buf[194] = 0x05.toByte()
            buf[195] = 0x00.toByte()
        }
        val result = RuntimeDataParser.parse(data).userAlarm2
        assertEquals(16, result.size)
        assertTrue(result[0])
        assertFalse(result[1])
        assertTrue(result[2])
    }

    @Test
    fun testTimeDcOCPR() {
        val data = makeData { FieldEncoder.writeU16(it, 196, 30) }
        assertEquals(30, RuntimeDataParser.parse(data).timeDcOCPR)
    }

    @Test
    fun testTimeDcSCPR() {
        val data = makeData { FieldEncoder.writeU16(it, 198, 60) }
        assertEquals(60, RuntimeDataParser.parse(data).timeDcSCPR)
    }

    @Test
    fun testTimeCOCPR() {
        val data = makeData { FieldEncoder.writeU16(it, 200, 90) }
        assertEquals(90, RuntimeDataParser.parse(data).timeCOCPR)
    }

    @Test
    fun testTimeCSCPR() {
        val data = makeData { FieldEncoder.writeU16(it, 202, 120) }
        assertEquals(120, RuntimeDataParser.parse(data).timeCSCPR)
    }

    @Test
    fun testTimeUVPR() {
        val data = makeData { FieldEncoder.writeU16(it, 204, 150) }
        assertEquals(150, RuntimeDataParser.parse(data).timeUVPR)
    }

    @Test
    fun testTimeOVPR() {
        val data = makeData { FieldEncoder.writeU16(it, 206, 180) }
        assertEquals(180, RuntimeDataParser.parse(data).timeOVPR)
    }

    @Test
    fun testTempSensorAbsent() {
        val data = makeData { it[208] = 0b00000011.toByte() }
        val result = RuntimeDataParser.parse(data).tempSensorAbsent
        assertEquals(8, result.size)
        assertTrue(result[0])
        assertTrue(result[1])
        assertFalse(result[2])
    }

    @Test
    fun testHeatingStatusTrue() {
        val data = makeData { FieldEncoder.writeU8(it, 209, 1) }
        assertTrue(RuntimeDataParser.parse(data).heatingStatus)
    }

    @Test
    fun testHeatingStatusFalse() {
        val data = makeData { FieldEncoder.writeU8(it, 209, 0) }
        assertFalse(RuntimeDataParser.parse(data).heatingStatus)
    }

    @Test
    fun testTimeEmerg() {
        val data = makeData { FieldEncoder.writeU16(it, 212, 10) }
        assertEquals(10, RuntimeDataParser.parse(data).timeEmerg)
    }

    @Test
    fun testDischrgCurCorrect() {
        val data = makeData { FieldEncoder.writeU16(it, 214, 1000) }
        assertEquals(1000, RuntimeDataParser.parse(data).dischrgCurCorrect)
    }

    @Test
    fun testVolChargCur() {
        val data = makeData { FieldEncoder.writeU16(it, 216, 4200) }
        assertEquals(4.2f, RuntimeDataParser.parse(data).volChargCur, 0.001f)
    }

    @Test
    fun testVolDischargCur() {
        val data = makeData { FieldEncoder.writeU16(it, 218, 3800) }
        assertEquals(3.8f, RuntimeDataParser.parse(data).volDischargCur, 0.001f)
    }

    @Test
    fun testBatVolCorrect() {
        val data = makeData { FieldEncoder.writeF32(it, 220, 1.05f) }
        assertEquals(1.05f, RuntimeDataParser.parse(data).batVolCorrect, 0.0001f)
    }

    @Test
    fun testChargPWM() {
        val data = makeData { FieldEncoder.writeU16(it, 224, 512) }
        assertEquals(512, RuntimeDataParser.parse(data).chargPWM)
    }

    @Test
    fun testDischargPWM() {
        val data = makeData { FieldEncoder.writeU16(it, 226, 768) }
        assertEquals(768, RuntimeDataParser.parse(data).dischargPWM)
    }

    @Test
    fun testTotalBatVol() {
        val data = makeData { FieldEncoder.writeU16(it, 228, 5240) }
        assertEquals(52.4f, RuntimeDataParser.parse(data).totalBatVol, 0.01f)
    }

    @Test
    fun testHeatCurrent() {
        val data = makeData { FieldEncoder.writeI16(it, 230, 2000) }
        assertEquals(2.0f, RuntimeDataParser.parse(data).heatCurrent, 0.001f)
    }

    @Test
    fun testHeatCurrentNegative() {
        val data = makeData { FieldEncoder.writeI16(it, 230, -500) }
        assertEquals(-0.5f, RuntimeDataParser.parse(data).heatCurrent, 0.001f)
    }

    @Test
    fun testAccStatusTrue() {
        val data = makeData { FieldEncoder.writeU8(it, 233, 1) }
        assertTrue(RuntimeDataParser.parse(data).accStatus)
    }

    @Test
    fun testSpecialChargerStaTrue() {
        val data = makeData { FieldEncoder.writeU8(it, 234, 1) }
        assertTrue(RuntimeDataParser.parse(data).specialChargerSta)
    }

    @Test
    fun testStartupFlag() {
        val data = makeData { FieldEncoder.writeU8(it, 235, 2) }
        assertEquals(2, RuntimeDataParser.parse(data).startupFlag)
    }

    @Test
    fun testVolC() {
        val data = makeData { FieldEncoder.writeU8(it, 236, 48) }
        assertEquals(48.0f, RuntimeDataParser.parse(data).volC, 0.001f)
    }

    @Test
    fun testMcuid() {
        val data = makeData { FieldEncoder.writeU8(it, 237, 5) }
        assertEquals(5, RuntimeDataParser.parse(data).mcuid)
    }

    @Test
    fun testChargePluggedTrue() {
        val data = makeData { FieldEncoder.writeU8(it, 239, 1) }
        assertTrue(RuntimeDataParser.parse(data).chargePlugged)
    }

    @Test
    fun testChargePluggedFalse() {
        val data = makeData { FieldEncoder.writeU8(it, 239, 0) }
        assertFalse(RuntimeDataParser.parse(data).chargePlugged)
    }

    @Test
    fun testSysRunTicks() {
        val data = makeData { FieldEncoder.writeU32(it, 240, 36000L) }
        assertEquals(3600.0f, RuntimeDataParser.parse(data).sysRunTicks, 0.1f)
    }

    @Test
    fun testPvdTrigTimestamps() {
        val data = makeData { FieldEncoder.writeU32(it, 244, 100L) }
        assertEquals(10.0f, RuntimeDataParser.parse(data).pvdTrigTimestamps, 0.1f)
    }

    @Test
    fun testBatTemp3() {
        val data = makeData { FieldEncoder.writeI16(it, 248, 280) }
        assertEquals(28.0f, RuntimeDataParser.parse(data).batTemp3, 0.1f)
    }

    @Test
    fun testBatTemp4() {
        val data = makeData { FieldEncoder.writeI16(it, 250, 290) }
        assertEquals(29.0f, RuntimeDataParser.parse(data).batTemp4, 0.1f)
    }

    @Test
    fun testBatTemp5() {
        val data = makeData { FieldEncoder.writeI16(it, 252, 300) }
        assertEquals(30.0f, RuntimeDataParser.parse(data).batTemp5, 0.1f)
    }

    @Test
    fun testChrgCurCorrect() {
        val data = makeData { FieldEncoder.writeU16(it, 254, 1100) }
        assertEquals(1100, RuntimeDataParser.parse(data).chrgCurCorrect)
    }

    @Test
    fun testRtcCounter() {
        val data = makeData { FieldEncoder.writeU32(it, 256, 86400L) }
        assertEquals(86400L, RuntimeDataParser.parse(data).rtcCounter)
    }

    @Test
    fun testDetailLogsCount() {
        val data = makeData { FieldEncoder.writeU32(it, 260, 99L) }
        assertEquals(99L, RuntimeDataParser.parse(data).detailLogsCount)
    }

    @Test
    fun testTimeEnterSleep() {
        val data = makeData { FieldEncoder.writeU32(it, 264, 7200L) }
        assertEquals(7200L, RuntimeDataParser.parse(data).timeEnterSleep)
    }

    @Test
    fun testPclModuleTrue() {
        val data = makeData { FieldEncoder.writeU8(it, 268, 1) }
        assertTrue(RuntimeDataParser.parse(data).pclModule)
    }

    @Test
    fun testPclModuleFalse() {
        val data = makeData { FieldEncoder.writeU8(it, 268, 0) }
        assertFalse(RuntimeDataParser.parse(data).pclModule)
    }

    @Test
    fun testBatteryType() {
        val data = makeData { FieldEncoder.writeU8(it, 269, 1) }
        assertEquals(1, RuntimeDataParser.parse(data).batteryType)
    }

    @Test
    fun testChargeStatusTime() {
        val data = makeData { FieldEncoder.writeU16(it, 272, 600) }
        assertEquals(600, RuntimeDataParser.parse(data).chargeStatusTime)
    }

    @Test
    fun testChargeStatus2() {
        val data = makeData { FieldEncoder.writeU8(it, 274, 2) }
        assertEquals(2, RuntimeDataParser.parse(data).chargeStatus2)
    }

    @Test
    fun testSwitchStatus() {
        val data = makeData { buf ->
            buf[275] = 0b00000101.toByte()
        }
        val result = RuntimeDataParser.parse(data).switchStatus
        assertEquals(3, result.size)
        assertTrue(result[0])
        assertFalse(result[1])
        assertTrue(result[2])
    }

    @Test(expected = IllegalArgumentException::class)
    fun testRequireGuard() {
        RuntimeDataParser.parse(ByteArray(100))
    }
}
