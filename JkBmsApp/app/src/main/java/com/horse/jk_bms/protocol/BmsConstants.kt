package com.horse.jk_bms.protocol

object BmsConstants {
    const val FRAME_SIZE = 300
    const val HEADER_SIZE = 4
    const val FRAME_CODE_OFFSET = 4
    const val COUNTER_OFFSET = 5
    const val DATA_OFFSET = 6
    const val DATA_SIZE = 293
    const val CHECKSUM_OFFSET = 299

    val HEADER_MAGIC = byteArrayOf(0x55, 0xAA.toByte(), 0xEB.toByte(), 0x90.toByte())

    const val BAUD_RATE = 115200
    const val SEND_INTERVAL_MS = 20L
    const val QUERY_TIMEOUT_MS = 350L
    const val RECV_POLL_INTERVAL_MS = 5L

    const val DEFAULT_ADDR_CODE = 1
}
