package com.horse.jk_bms.protocol

import org.junit.Assert.*
import org.junit.Test

class FaultInfoParserTest {

    private fun makeData(setup: (ByteArray) -> Unit): ByteArray {
        val data = ByteArray(293)
        setup(data)
        return data
    }

    @Test
    fun testZeroRecords() {
        val data = makeData { buf ->
            FieldEncoder.writeU16(buf, 0, 0)
            FieldEncoder.writeU8(buf, 2, 0)
        }
        val result = FaultInfoParser.parse(data)
        assertEquals(0, result.count)
        assertTrue(result.records.isEmpty())
    }

    @Test
    fun testBeginIndexAndCount() {
        val data = makeData { buf ->
            FieldEncoder.writeU16(buf, 0, 42)
            FieldEncoder.writeU8(buf, 2, 1)
        }
        val result = FaultInfoParser.parse(data)
        assertEquals(42, result.beginIndex)
        assertEquals(1, result.count)
    }

    @Test
    fun testSingleRecordAllFields() {
        val data = makeData { buf ->
            FieldEncoder.writeU8(buf, 2, 1)
            FieldEncoder.writeU32(buf, 3, 123456L)
            FieldEncoder.writeU8(buf, 7, 0x05)
            buf[8] = 0b00001001.toByte()
            FieldEncoder.writeU8(buf, 9, 7)
            FieldEncoder.writeU8(buf, 10, 2)
            FieldEncoder.writeU16(buf, 11, 3650)
            FieldEncoder.writeU16(buf, 13, 3100)
            FieldEncoder.writeU16(buf, 15, 5240)
            FieldEncoder.writeU16(buf, 17, 300)
            FieldEncoder.writeU16(buf, 19, 850)
            FieldEncoder.writeU16(buf, 21, 1000)
            FieldEncoder.writeU8(buf, 23, 45)
            FieldEncoder.writeU8(buf, 24, 20)
            FieldEncoder.writeU8(buf, 25, 38)
            FieldEncoder.writeU8(buf, 26, 15)
        }
        val result = FaultInfoParser.parse(data)
        assertEquals(1, result.records.size)
        val r = result.records[0]
        assertEquals(123456L, r.rtcCount)
        assertEquals(0x05, r.logCode)
        assertTrue(r.switchSta[0])
        assertFalse(r.switchSta[1])
        assertFalse(r.switchSta[2])
        assertTrue(r.switchSta[3])
        assertEquals(7, r.maxVolCellNo)
        assertEquals(2, r.minVolCellNo)
        assertEquals(3.65f, r.volCellMax, 0.001f)
        assertEquals(3.1f, r.volCellMin, 0.001f)
        assertEquals(52.4f, r.volBat, 0.01f)
        assertEquals(30.0f, r.curBat, 0.1f)
        assertEquals(85.0f, r.socCapRemain, 0.1f)
        assertEquals(100.0f, r.socFullChargeCap, 0.1f)
        assertEquals(45, r.maxTemp)
        assertEquals(20, r.minTemp)
        assertEquals(38, r.tempMos)
        assertEquals(1.5f, r.heatCurrent, 0.1f)
    }

    @Test
    fun testMultipleRecords() {
        val data = makeData { buf ->
            FieldEncoder.writeU8(buf, 2, 2)
            FieldEncoder.writeU32(buf, 3, 1000L)
            FieldEncoder.writeU32(buf, 27, 2000L)
        }
        val result = FaultInfoParser.parse(data)
        assertEquals(2, result.records.size)
        assertEquals(1000L, result.records[0].rtcCount)
        assertEquals(2000L, result.records[1].rtcCount)
    }

    @Test
    fun testScaleFactors() {
        val data = makeData { buf ->
            FieldEncoder.writeU8(buf, 2, 1)
            FieldEncoder.writeU16(buf, 11, 4000)
            FieldEncoder.writeU16(buf, 15, 4800)
            FieldEncoder.writeU16(buf, 17, 500)
            FieldEncoder.writeU8(buf, 26, 25)
        }
        val r = FaultInfoParser.parse(data).records[0]
        assertEquals(4.000f, r.volCellMax, 0.001f)
        assertEquals(48.00f, r.volBat, 0.01f)
        assertEquals(50.0f, r.curBat, 0.1f)
        assertEquals(2.5f, r.heatCurrent, 0.1f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testFaultInfoRequireGuard() {
        FaultInfoParser.parse(ByteArray(100))
    }

    @Test
    fun testSystemLogLogCount() {
        val data = makeData { FieldEncoder.writeU32(it, 0, 999L) }
        assertEquals(999L, SystemLogParser.parse(data).logCount)
    }

    @Test
    fun testSystemLogCheck() {
        val data = makeData { FieldEncoder.writeU8(it, 4, 0xAB) }
        assertEquals(0xAB, SystemLogParser.parse(data).check)
    }

    @Test
    fun testSystemLogAlarmLogContent() {
        val data = makeData { buf ->
            buf[5] = 0x11.toByte()
            buf[6] = 0x22.toByte()
            buf[254] = 0xFF.toByte()
        }
        val log = SystemLogParser.parse(data).alarmLog
        assertEquals(250, log.size)
        assertEquals(0x11.toByte(), log[0])
        assertEquals(0x22.toByte(), log[1])
        assertEquals(0xFF.toByte(), log[249])
    }

    @Test(expected = IllegalArgumentException::class)
    fun testSystemLogRequireGuard() {
        SystemLogParser.parse(ByteArray(100))
    }
}
