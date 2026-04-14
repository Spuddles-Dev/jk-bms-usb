package com.horse.jk_bms.protocol

import org.junit.Assert.*
import org.junit.Test

class FrameEncoderTest {

    @Test
    fun testBuildQueryRuntime() {
        val frame = FrameEncoder.buildQuery(FrameCode.RUNTIME_DATA, 0)
        
        assertEquals("Frame should be 300 bytes", 300, frame.size)
        assertArrayEquals("Header should match magic", BmsConstants.HEADER_MAGIC, frame.copyOfRange(0, 4))
        assertEquals("Frame code should be runtime", FrameCode.RUNTIME_DATA.code, frame[4])
        assertEquals("Counter should be 0", 0.toByte(), frame[5])
    }

    @Test
    fun testBuildQueryConfig() {
        val frame = FrameEncoder.buildQuery(FrameCode.CONFIG_READ, 5)
        
        assertEquals("Frame code should be config", FrameCode.CONFIG_READ.code, frame[4])
        assertEquals("Counter should be 5", 5.toByte(), frame[5])
    }

    @Test
    fun testBuildQueryAllFrameCodes() {
        val frameCodes = listOf(
            FrameCode.CONFIG_READ,
            FrameCode.RUNTIME_DATA,
            FrameCode.DEVICE_INFO,
            FrameCode.CONFIG_WRITE,
            FrameCode.SYSTEM_LOG,
            FrameCode.FAULT_INFO
        )
        
        frameCodes.forEach { code ->
            val frame = FrameEncoder.buildQuery(code, 10)
            assertEquals("Frame code ${code.name}", code.code, frame[4])
        }
    }

    @Test
    fun testBuildQueryCounterIncrement() {
        val frame1 = FrameEncoder.buildQuery(FrameCode.RUNTIME_DATA, 0)
        val frame2 = FrameEncoder.buildQuery(FrameCode.RUNTIME_DATA, 1)
        val frame255 = FrameEncoder.buildQuery(FrameCode.RUNTIME_DATA, 255)
        
        assertEquals(0.toByte(), frame1[5])
        assertEquals(1.toByte(), frame2[5])
        assertEquals(255.toByte(), frame255[5])
    }

    @Test
    fun testBuildQueryChecksum() {
        val frame = FrameEncoder.buildQuery(FrameCode.RUNTIME_DATA, 0)
        
        val calculatedChecksum = Checksum.calculate(frame)
        assertEquals("Checksum should be written to correct offset", calculatedChecksum, frame[299])
    }

    @Test
    fun testBuildConfigWrite() {
        val config = BmsConfig(
            volSmartSleep = 45.5f,
            volCellUV = 2.8f,
            volCellOV = 3.65f,
            cellCount = 20L,
            capBatCell = 100.0f,
            batChargeEn = 1L,
            batDischargeEn = 1L,
            balanEn = 1L
        )
        
        val frame = FrameEncoder.buildConfigWrite(config, 0)
        
        assertEquals("Frame should be 300 bytes", 300, frame.size)
        assertArrayEquals("Header should match magic", BmsConstants.HEADER_MAGIC, frame.copyOfRange(0, 4))
        assertEquals("Frame code should be config write", FrameCode.CONFIG_WRITE.code, frame[4])
        assertEquals("Counter should be 0", 0.toByte(), frame[5])
    }

    @Test
    fun testBuildConfigWriteVoltageScaling() {
        val config = BmsConfig(volSmartSleep = 45.5f)
        
        val frame = FrameEncoder.buildConfigWrite(config, 0)
        
        val voltageBytes = frame.copyOfRange(BmsConstants.DATA_OFFSET, BmsConstants.DATA_OFFSET + 4)
        val encodedValue = FieldDecoder.readU32(voltageBytes, 0) * 0.001f
        assertEquals("Voltage should be scaled by 0.001", 45.5f, encodedValue, 0.001f)
    }

    @Test
    fun testBuildConfigWriteCurrentScaling() {
        val config = BmsConfig(timBatCOC = 50.5f)
        
        val frame = FrameEncoder.buildConfigWrite(config, 0)
        
        val currentBytes = frame.copyOfRange(BmsConstants.DATA_OFFSET + 44, BmsConstants.DATA_OFFSET + 48)
        val encodedValue = FieldDecoder.readU32(currentBytes, 0) * 0.001f
        assertEquals("Current should be scaled by 0.001", 50.5f, encodedValue, 0.001f)
    }

    @Test
    fun testBuildConfigWriteTemperatureScaling() {
        val config = BmsConfig(tmpBatCOT = 65.5f)
        
        val frame = FrameEncoder.buildConfigWrite(config, 0)
        
        val tempBytes = frame.copyOfRange(BmsConstants.DATA_OFFSET + 76, BmsConstants.DATA_OFFSET + 80)
        val encodedValue = FieldDecoder.readI32(tempBytes, 0) * 0.1f
        assertEquals("Temperature should be scaled by 0.1", 65.5f, encodedValue, 0.1f)
    }

    @Test
    fun testBuildConfigWriteChecksum() {
        val config = BmsConfig()
        val frame = FrameEncoder.buildConfigWrite(config, 0)
        
        val calculatedChecksum = Checksum.calculate(frame)
        assertEquals("Checksum should be correct", calculatedChecksum, frame[299])
    }

    @Test
    fun testBuildConfigWriteWireResistanceArray() {
        val resistances = FloatArray(32) { it * 0.001f }
        val config = BmsConfig(cellConWireRes = resistances)
        
        val frame = FrameEncoder.buildConfigWrite(config, 0)
        
        for (i in 0 until 32) {
            val offset = BmsConstants.DATA_OFFSET + 136 + i * 4
            val value = FieldDecoder.readU32(frame, offset) * 0.001f
            assertEquals("Wire resistance at index $i", i * 0.001f, value, 0.000001f)
        }
    }

    @Test
    fun testBuildConfigWriteSwitchSettings() {
        val config = BmsConfig(
            batChargeEn = 1L,
            batDischargeEn = 1L,
            balanEn = 0L
        )
        
        val frame = FrameEncoder.buildConfigWrite(config, 0)
        
        assertEquals("Charge enable should be 1", 1L, FieldDecoder.readU32(frame, BmsConstants.DATA_OFFSET + 112))
        assertEquals("Discharge enable should be 1", 1L, FieldDecoder.readU32(frame, BmsConstants.DATA_OFFSET + 116))
        assertEquals("Balance enable should be 0", 0L, FieldDecoder.readU32(frame, BmsConstants.DATA_OFFSET + 120))
    }
}
