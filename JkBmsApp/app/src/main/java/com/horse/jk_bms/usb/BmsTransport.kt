package com.horse.jk_bms.usb

import com.horse.jk_bms.protocol.FrameCode
import com.horse.jk_bms.protocol.RawFrame

interface BmsTransport {
    val isConnected: Boolean
    fun listSerialDevices(): List<UsbDeviceInfo>
    suspend fun connect(info: UsbDeviceInfo): Result<Unit>
    fun disconnect()
    suspend fun exchange(frame: ByteArray, expected: FrameCode): Result<RawFrame>
}
