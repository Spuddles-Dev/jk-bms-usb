package com.horse.jk_bms.protocol

import org.junit.Assert.*
import org.junit.Test

class ConfigParserTest {

    private fun makeData(setup: (ByteArray) -> Unit): ByteArray {
        val data = ByteArray(293)
        setup(data)
        return data
    }

    @Test
    fun testVolSmartSleep() {
        val data = makeData { FieldEncoder.writeU32(it, 0, 45500L) }
        assertEquals(45.5f, ConfigParser.parse(data).volSmartSleep, 0.001f)
    }

    @Test
    fun testVolCellUV() {
        val data = makeData { FieldEncoder.writeU32(it, 4, 2800L) }
        assertEquals(2.8f, ConfigParser.parse(data).volCellUV, 0.001f)
    }

    @Test
    fun testVolCellUVPR() {
        val data = makeData { FieldEncoder.writeU32(it, 8, 2600L) }
        assertEquals(2.6f, ConfigParser.parse(data).volCellUVPR, 0.001f)
    }

    @Test
    fun testVolCellOV() {
        val data = makeData { FieldEncoder.writeU32(it, 12, 3650L) }
        assertEquals(3.65f, ConfigParser.parse(data).volCellOV, 0.001f)
    }

    @Test
    fun testVolCellOVPR() {
        val data = makeData { FieldEncoder.writeU32(it, 16, 3700L) }
        assertEquals(3.7f, ConfigParser.parse(data).volCellOVPR, 0.001f)
    }

    @Test
    fun testVolBalanTrig() {
        val data = makeData { FieldEncoder.writeU32(it, 20, 3300L) }
        assertEquals(3.3f, ConfigParser.parse(data).volBalanTrig, 0.001f)
    }

    @Test
    fun testVolSOCP100() {
        val data = makeData { FieldEncoder.writeU32(it, 24, 3600L) }
        assertEquals(3.6f, ConfigParser.parse(data).volSOCP100, 0.001f)
    }

    @Test
    fun testVolSOCP0() {
        val data = makeData { FieldEncoder.writeU32(it, 28, 3000L) }
        assertEquals(3.0f, ConfigParser.parse(data).volSOCP0, 0.001f)
    }

    @Test
    fun testVolCellRCV() {
        val data = makeData { FieldEncoder.writeU32(it, 32, 3100L) }
        assertEquals(3.1f, ConfigParser.parse(data).volCellRCV, 0.001f)
    }

    @Test
    fun testVolCellRFV() {
        val data = makeData { FieldEncoder.writeU32(it, 36, 3550L) }
        assertEquals(3.55f, ConfigParser.parse(data).volCellRFV, 0.001f)
    }

    @Test
    fun testVolSysPwrOff() {
        val data = makeData { FieldEncoder.writeU32(it, 40, 44000L) }
        assertEquals(44.0f, ConfigParser.parse(data).volSysPwrOff, 0.001f)
    }

    @Test
    fun testTimBatCOC() {
        val data = makeData { FieldEncoder.writeU32(it, 44, 50500L) }
        assertEquals(50.5f, ConfigParser.parse(data).timBatCOC, 0.001f)
    }

    @Test
    fun testTimBatCOCPDly() {
        val data = makeData { FieldEncoder.writeU32(it, 48, 100L) }
        assertEquals(100L, ConfigParser.parse(data).timBatCOCPDly)
    }

    @Test
    fun testTimBatCOCPRDly() {
        val data = makeData { FieldEncoder.writeU32(it, 52, 200L) }
        assertEquals(200L, ConfigParser.parse(data).timBatCOCPRDly)
    }

    @Test
    fun testTimBatDcOC() {
        val data = makeData { FieldEncoder.writeU32(it, 56, 75000L) }
        assertEquals(75.0f, ConfigParser.parse(data).timBatDcOC, 0.001f)
    }

    @Test
    fun testTimBatDcOCPDly() {
        val data = makeData { FieldEncoder.writeU32(it, 60, 300L) }
        assertEquals(300L, ConfigParser.parse(data).timBatDcOCPDly)
    }

    @Test
    fun testTimBatDcOCPRDly() {
        val data = makeData { FieldEncoder.writeU32(it, 64, 400L) }
        assertEquals(400L, ConfigParser.parse(data).timBatDcOCPRDly)
    }

    @Test
    fun testTimBatSCPRDly() {
        val data = makeData { FieldEncoder.writeU32(it, 68, 500L) }
        assertEquals(500L, ConfigParser.parse(data).timBatSCPRDly)
    }

    @Test
    fun testCurBalanMax() {
        val data = makeData { FieldEncoder.writeU32(it, 72, 750L) }
        assertEquals(0.75f, ConfigParser.parse(data).curBalanMax, 0.001f)
    }

    @Test
    fun testTmpBatCOT() {
        val data = makeData { FieldEncoder.writeI32(it, 76, 650) }
        assertEquals(65.0f, ConfigParser.parse(data).tmpBatCOT, 0.1f)
    }

    @Test
    fun testTmpBatCOTPR() {
        val data = makeData { FieldEncoder.writeI32(it, 80, 600) }
        assertEquals(60.0f, ConfigParser.parse(data).tmpBatCOTPR, 0.1f)
    }

    @Test
    fun testTmpBatDcOT() {
        val data = makeData { FieldEncoder.writeI32(it, 84, 700) }
        assertEquals(70.0f, ConfigParser.parse(data).tmpBatDcOT, 0.1f)
    }

    @Test
    fun testTmpBatDcOTPR() {
        val data = makeData { FieldEncoder.writeI32(it, 88, 650) }
        assertEquals(65.0f, ConfigParser.parse(data).tmpBatDcOTPR, 0.1f)
    }

    @Test
    fun testTmpBatCUTNegative() {
        val data = makeData { FieldEncoder.writeI32(it, 92, -200) }
        assertEquals(-20.0f, ConfigParser.parse(data).tmpBatCUT, 0.1f)
    }

    @Test
    fun testTmpBatCUTPR() {
        val data = makeData { FieldEncoder.writeI32(it, 96, -150) }
        assertEquals(-15.0f, ConfigParser.parse(data).tmpBatCUTPR, 0.1f)
    }

    @Test
    fun testTmpMosOT() {
        val data = makeData { FieldEncoder.writeI32(it, 100, 850) }
        assertEquals(85.0f, ConfigParser.parse(data).tmpMosOT, 0.1f)
    }

    @Test
    fun testTmpMosOTPR() {
        val data = makeData { FieldEncoder.writeI32(it, 104, 800) }
        assertEquals(80.0f, ConfigParser.parse(data).tmpMosOTPR, 0.1f)
    }

    @Test
    fun testCellCount() {
        val data = makeData { FieldEncoder.writeU32(it, 108, 20L) }
        assertEquals(20L, ConfigParser.parse(data).cellCount)
    }

    @Test
    fun testBatChargeEn() {
        val data = makeData { FieldEncoder.writeU32(it, 112, 1L) }
        assertEquals(1L, ConfigParser.parse(data).batChargeEn)
    }

    @Test
    fun testBatDischargeEn() {
        val data = makeData { FieldEncoder.writeU32(it, 116, 1L) }
        assertEquals(1L, ConfigParser.parse(data).batDischargeEn)
    }

    @Test
    fun testBalanEn() {
        val data = makeData { FieldEncoder.writeU32(it, 120, 0L) }
        assertEquals(0L, ConfigParser.parse(data).balanEn)
    }

    @Test
    fun testCapBatCell() {
        val data = makeData { FieldEncoder.writeU32(it, 124, 100000L) }
        assertEquals(100.0f, ConfigParser.parse(data).capBatCell, 0.001f)
    }

    @Test
    fun testScpDelay() {
        val data = makeData { FieldEncoder.writeU32(it, 128, 10L) }
        assertEquals(10L, ConfigParser.parse(data).scpDelay)
    }

    @Test
    fun testVolStartBalan() {
        val data = makeData { FieldEncoder.writeU32(it, 132, 3350L) }
        assertEquals(3.35f, ConfigParser.parse(data).volStartBalan, 0.001f)
    }

    @Test
    fun testCellConWireResArray() {
        val data = makeData { buf ->
            for (i in 0 until 32) {
                FieldEncoder.writeU32(buf, 136 + i * 4, (i * 1000).toLong())
            }
        }
        val result = ConfigParser.parse(data).cellConWireRes
        assertEquals(32, result.size)
        for (i in 0 until 32) {
            assertEquals(i.toFloat(), result[i], 0.001f)
        }
    }

    @Test
    fun testDevAddr() {
        val data = makeData { FieldEncoder.writeU32(it, 264, 1L) }
        assertEquals(1L, ConfigParser.parse(data).devAddr)
    }

    @Test
    fun testDischrgPreChrgT() {
        val data = makeData { FieldEncoder.writeU32(it, 268, 5L) }
        assertEquals(5L, ConfigParser.parse(data).dischrgPreChrgT)
    }

    @Test
    fun testCurrentRange() {
        val data = makeData { FieldEncoder.writeU32(it, 272, 200000L) }
        assertEquals(200.0f, ConfigParser.parse(data).currentRange, 0.001f)
    }

    @Test
    fun testSwitchStatus() {
        val data = makeData { buf ->
            FieldEncoder.writeBitmap(buf, 276, BooleanArray(16).also {
                it[0] = true
                it[3] = true
            })
        }
        val result = ConfigParser.parse(data).switchStatus
        assertEquals(16, result.size)
        assertTrue(result[0])
        assertFalse(result[1])
        assertFalse(result[2])
        assertTrue(result[3])
    }

    @Test
    fun testTmpStartHeating() {
        val data = makeData { FieldEncoder.writeI8(it, 278, -10) }
        assertEquals(-10, ConfigParser.parse(data).tmpStartHeating)
    }

    @Test
    fun testTmpStopHeating() {
        val data = makeData { FieldEncoder.writeI8(it, 279, 5) }
        assertEquals(5, ConfigParser.parse(data).tmpStopHeating)
    }

    @Test
    fun testTimeSmartSleep() {
        val data = makeData { FieldEncoder.writeU8(it, 280, 30) }
        assertEquals(30, ConfigParser.parse(data).timeSmartSleep)
    }

    @Test
    fun testEnableFlags() {
        val data = makeData { buf ->
            buf[281] = 0x01.toByte()
            buf[282] = 0x02.toByte()
            buf[288] = 0xFF.toByte()
        }
        val flags = ConfigParser.parse(data).enableFlags
        assertEquals(9, flags.size)
        assertEquals(0x01.toByte(), flags[0])
        assertEquals(0x02.toByte(), flags[1])
        assertEquals(0xFF.toByte(), flags[7])
    }

    @Test
    fun testTmpBatDCHUT() {
        val data = makeData { FieldEncoder.writeI8(it, 290, -20) }
        assertEquals(-20, ConfigParser.parse(data).tmpBatDCHUT)
    }

    @Test
    fun testTmpBatDCHUTPR() {
        val data = makeData { FieldEncoder.writeI8(it, 291, -15) }
        assertEquals(-15, ConfigParser.parse(data).tmpBatDCHUTPR)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testRequireGuard() {
        ConfigParser.parse(ByteArray(100))
    }
}
