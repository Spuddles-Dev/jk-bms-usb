package com.horse.jk_bms.diagnostics

import com.horse.jk_bms.protocol.FrameCode
import java.util.ArrayDeque
import javax.inject.Inject
import javax.inject.Singleton

data class TraceEntry(val timestampMs: Long, val direction: String, val hex: String)

@Singleton
class ProtocolTrace @Inject constructor() {
    private val entries = ArrayDeque<TraceEntry>()

    @Synchronized
    fun record(direction: String, frame: ByteArray) {
        if (frame.size < 5 || frame[4] == FrameCode.DEVICE_INFO.code) return
        entries.addLast(TraceEntry(System.currentTimeMillis(), direction, frame.joinToString("") { "%02x".format(it) }))
        while (entries.size > 256) entries.removeFirst()
    }

    @Synchronized
    fun snapshot(): List<TraceEntry> = entries.toList()
}
