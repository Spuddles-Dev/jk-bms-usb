package com.horse.jk_bms.diagnostics

import com.horse.jk_bms.model.BmsRuntimeData
import com.horse.jk_bms.protocol.FrameCode
import com.horse.jk_bms.protocol.FrameStreamDecoder
import com.horse.jk_bms.protocol.RuntimeDataParser

object CaptureReplay {
    fun runtimes(entries: List<TraceEntry>): List<BmsRuntimeData> {
        require(entries.size <= 1024) { "Capture has too many entries" }
        val decoder = FrameStreamDecoder()
        return entries.filter { it.direction == "rx" }.flatMap { entry ->
            require(entry.hex.length <= 2048 && entry.hex.length % 2 == 0) { "Invalid capture frame length" }
            val bytes = entry.hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
            decoder.append(bytes).filter { it.frameCode == FrameCode.RUNTIME_DATA }
                .map { RuntimeDataParser.parse(it.data) }
        }
    }
}
