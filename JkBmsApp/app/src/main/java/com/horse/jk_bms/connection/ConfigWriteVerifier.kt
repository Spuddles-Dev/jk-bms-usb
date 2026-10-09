package com.horse.jk_bms.connection
import com.horse.jk_bms.protocol.ConfigParser
import com.horse.jk_bms.protocol.ConfigFieldValidator
import com.horse.jk_bms.protocol.FrameEncoder
import com.horse.jk_bms.protocol.FrameCode
import com.horse.jk_bms.protocol.ConfigSchema

import com.horse.jk_bms.model.BmsConfig
import com.horse.jk_bms.usb.BmsTransport
import java.io.IOException

object ConfigWriteVerifier {
    suspend fun write(
        transport: BmsTransport,
        requested: BmsConfig,
        baseline: BmsConfig,
        nextCounter: () -> Int,
    ): BmsConfig {
        ConfigFieldValidator.requireValid(requested)
        val freshFrame = transport.exchange(FrameEncoder.buildQuery(FrameCode.CONFIG_READ, nextCounter()), FrameCode.CONFIG_READ).getOrThrow()
        require(freshFrame.frameCode == FrameCode.CONFIG_READ) { "Unexpected baseline response" }
        val fresh = ConfigParser.parse(freshFrame.data)
        require(FrameEncoder.buildConfigWrite(fresh, 0).contentEquals(FrameEncoder.buildConfigWrite(baseline, 0))) {
            "Settings changed on the BMS. Refresh and review your changes."
        }
        val writeFrame = FrameEncoder.buildConfigWrite(requested, nextCounter())
        val ack = transport.exchange(writeFrame, FrameCode.CONFIG_WRITE).getOrThrow()
        if (ack.frameCode != FrameCode.CONFIG_WRITE) throw IOException("Unexpected write response; outcome unknown")
        val readback = transport.exchange(
            FrameEncoder.buildQuery(FrameCode.CONFIG_READ, nextCounter()), FrameCode.CONFIG_READ,
        ).getOrThrow()
        if (readback.frameCode != FrameCode.CONFIG_READ) throw IOException("Unexpected readback; outcome unknown")
        val applied = ConfigParser.parse(readback.data)
        val expected = FrameEncoder.buildConfigWrite(requested, 0)
        val actual = FrameEncoder.buildConfigWrite(applied, 0)
        if (!expected.contentEquals(actual)) {
            val fields = ConfigSchema.fields.filter { it.get(requested) != it.get(applied) }.map { it.label }
            throw IOException("Readback mismatch: ${fields.ifEmpty { listOf("array or wire values") }.joinToString()}")
        }
        return applied
    }
}
