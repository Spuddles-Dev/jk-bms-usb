package com.horse.jk_bms.diagnostics

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.horse.jk_bms.protocol.FrameCode
import com.horse.jk_bms.protocol.FrameEncoder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CaptureReplayTest {
    @Test fun testSyntheticFixtureReplaysWithoutTransport() {
        val gson = Gson()
        val fixture = javaClass.getResourceAsStream("/fixtures/demo-runtime-capture.json")!!.bufferedReader().use {
            gson.fromJson(it, JsonObject::class.java)
        }
        val entries = fixture.getAsJsonArray("entries").map { gson.fromJson(it, TraceEntry::class.java) }
        val samples = CaptureReplay.runtimes(entries)
        assertEquals(3, samples.size)
        assertEquals(16, samples.first().activeCellCount)
        assertEquals(-10f, samples.first().batCurrent)
        assertTrue(samples.first().batVol > 52.8f)
        assertEquals(78, samples.last().soc)
    }

    @Test fun testTraceIsBoundedAndExcludesCredentialFrames() {
        val trace = ProtocolTrace()
        trace.record("rx", FrameEncoder.buildQuery(FrameCode.DEVICE_INFO, 0))
        assertTrue(trace.snapshot().isEmpty())
        repeat(400) { trace.record("rx", FrameEncoder.buildQuery(FrameCode.RUNTIME_DATA, it)) }
        assertEquals(256, trace.snapshot().size)
    }
}
