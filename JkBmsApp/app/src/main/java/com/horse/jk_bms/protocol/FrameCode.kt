package com.horse.jk_bms.protocol

enum class FrameCode(val code: Byte) {
    CONFIG_READ(0x01),
    RUNTIME_DATA(0x02),
    DEVICE_INFO(0x03),
    CONFIG_WRITE(0x04),
    SYSTEM_LOG(0x05),
    FAULT_INFO(0x06);

    companion object {
        private val codeMap = entries.associateBy { it.code }
        fun fromCode(code: Byte): FrameCode? = codeMap[code]
    }
}
