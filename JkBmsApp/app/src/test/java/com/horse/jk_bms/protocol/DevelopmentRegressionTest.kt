package com.horse.jk_bms.protocol

import com.horse.jk_bms.model.BmsConfig
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

fun validConfig(): BmsConfig {
    var config = BmsConfig()
    ConfigSchema.fields.forEach { field ->
        val midpoint = (field.min + field.max) / 2
        config = field.set(config, if (field.integral) midpoint.toLong().toDouble() else midpoint)
    }
    return config.copy(volCellUV = 2.8f, volCellUVPR = 2.9f, volCellOVPR = 3.5f, volCellOV = 3.6f,
        volSOCP0 = 3f, volSOCP100 = 3.4f, tmpBatCOT = 60f, tmpBatCOTPR = 55f,
        tmpBatCUT = -10f, tmpBatCUTPR = 0f, tmpBatDcOT = 60f, tmpBatDcOTPR = 55f,
        tmpMosOT = 90f, tmpMosOTPR = 80f, tmpBatDCHUT = -10, tmpBatDCHUTPR = 0,
        tmpStartHeating = 0, tmpStopHeating = 10)
}

fun readFrame(config: BmsConfig): RawFrame {
    val payload = FrameEncoder.buildConfigWrite(config, 0).copyOfRange(6, 299)
    config.enableFlags.copyInto(payload, 281)
    FieldEncoder.writeI8(payload, 290, config.tmpBatDCHUT)
    FieldEncoder.writeI8(payload, 291, config.tmpBatDCHUTPR)
    return RawFrame(FrameCode.CONFIG_READ, 0, payload)
}

class DevelopmentRegressionTest {
    @Test fun testNoiseAndEveryFragmentSizeRecover() {
        val expected = FrameEncoder.buildQuery(FrameCode.RUNTIME_DATA, 7)
        for (chunk in 1..32) {
            val decoder = FrameStreamDecoder()
            val results = (byteArrayOf(0x12, 0x55) + expected).asList().chunked(chunk)
                .flatMap { decoder.append(it.toByteArray()) }
            assertEquals(1, results.size)
            assertEquals(7, results.single().counter)
        }
    }

    @Test fun testCorruptionFollowedByBackToBackFrames() {
        val bad = FrameEncoder.buildQuery(FrameCode.RUNTIME_DATA, 1).apply { this[100] = 2 }
        val good = FrameEncoder.buildQuery(FrameCode.CONFIG_READ, 2)
        val second = FrameEncoder.buildQuery(FrameCode.SYSTEM_LOG, 3)
        assertEquals(listOf(2, 3), FrameStreamDecoder().append(bad + good + second).map { it.counter })
    }

    @Test fun testNoiseMemoryBoundAndLateTail() {
        val decoder = FrameStreamDecoder()
        repeat(100) { decoder.append(ByteArray(4096) { 0x22 }) }
        assertTrue(decoder.bufferedBytes < 300)
        val frame = FrameEncoder.buildQuery(FrameCode.RUNTIME_DATA, 5)
        assertTrue(decoder.append(frame.copyOfRange(0, 130)).isEmpty())
        assertEquals(5, decoder.append(frame.copyOfRange(130, 300)).single().counter)
    }

    @Test fun testExactScalingAndIndependentWriteTail() {
        val frame = FrameEncoder.buildConfigWrite(BmsConfig(volCellUV = 3.3f, tmpBatCUT = -12.3f,
            tmpBatDCHUT = -21, tmpBatDCHUTPR = 7, enableFlags = ByteArray(9) { 99 }), 0)
        assertEquals(3300L, FieldDecoder.readU32(frame, 10))
        assertEquals(-123, FieldDecoder.readI32(frame, 98))
        assertArrayEquals(byteArrayOf(-21, 7) + ByteArray(10), frame.copyOfRange(287, 299))
    }

    @Test fun testUntouchedRawCapacityPreserved() {
        val raw = readFrame(validConfig()).data
        FieldEncoder.writeU32(raw, 124, 16_777_217)
        val parsed = ConfigParser.parse(raw)
        val edited = FrameEncoder.buildConfigWrite(parsed.copy(volCellUV = 2.7f), 0)
        assertEquals(16_777_217L, FieldDecoder.readU32(edited, 130))
    }

    @Test fun testEveryNumericFieldHasFiniteAndRangeValidation() {
        val config = validConfig()
        assertTrue(ConfigFieldValidator.validateAll(config).values.all { it })
        ConfigSchema.fields.forEach { field ->
            assertFalse(field.accepts(Double.NaN))
            assertFalse(field.accepts(Double.POSITIVE_INFINITY))
            assertFalse(field.accepts(field.min - 1))
            assertFalse(field.accepts(field.max + 1))
            assertTrue(field.accepts(field.min))
            assertTrue(field.accepts(field.max))
            if (field.integral) assertFalse(field.accepts(field.min + 0.5))
        }
        assertFalse(ConfigFieldValidator.validateAll(config.copy(timBatCOCPDly = -1)).getValue("timBatCOCPDly"))
        assertFalse(ConfigFieldValidator.validateAll(config.copy(cellConWireRes = FloatArray(31))).getValue("cellConWireRes"))
    }

    @Test fun testContentEqualityAndRecoveryRelationships() {
        val config = validConfig()
        assertEquals(config, config.copy(cellConWireRes = config.cellConWireRes.copyOf()))
        assertEquals(config.hashCode(), config.copy().hashCode())
        assertFalse(ConfigFieldValidator.validateAll(config.copy(volCellUVPR = 2.7f)).getValue("voltageRecovery"))
        assertFalse(ConfigFieldValidator.validate("unknown", 1f))
    }
}
