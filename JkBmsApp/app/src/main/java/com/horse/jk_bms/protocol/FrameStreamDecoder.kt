package com.horse.jk_bms.protocol

class FrameStreamDecoder {
    private var buffer = ByteArray(0)
    val bufferedBytes: Int get() = buffer.size

    fun clear() {
        buffer = ByteArray(0)
    }

    fun append(bytes: ByteArray): List<RawFrame> {
        val frames = mutableListOf<RawFrame>()
        for (chunk in bytes.asList().chunked(BmsConstants.FRAME_SIZE)) {
            buffer += chunk.toByteArray()
            while (buffer.size >= BmsConstants.FRAME_SIZE) {
                val found = FrameDecoder.findFrameInBuffer(buffer)
                if (found != null) {
                    frames += found.first
                    buffer = buffer.copyOfRange(found.second, buffer.size)
                } else {
                    buffer = buffer.takeLast(BmsConstants.FRAME_SIZE - 1).toByteArray()
                    break
                }
            }
        }
        return frames
    }
}
