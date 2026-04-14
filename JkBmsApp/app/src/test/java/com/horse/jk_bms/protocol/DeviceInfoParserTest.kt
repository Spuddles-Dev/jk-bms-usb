package com.horse.jk_bms.protocol

import org.junit.Assert.*
import org.junit.Test

class DeviceInfoParserTest {

    private fun makeData(setup: (ByteArray) -> Unit): ByteArray {
        val data = ByteArray(293)
        setup(data)
        return data
    }

    private fun writeString(data: ByteArray, offset: Int, value: String) {
        val bytes = value.toByteArray(Charsets.UTF_8)
        bytes.copyInto(data, offset, 0, bytes.size)
    }

    @Test
    fun testManuDeviceID() {
        val data = makeData { writeString(it, 0, "JK-B2A20S20P") }
        assertEquals("JK-B2A20S20P", DeviceInfoParser.parse(data).manuDeviceID)
    }

    @Test
    fun testManuDeviceIDNullTerminated() {
        val data = makeData { buf ->
            writeString(buf, 0, "JK")
            buf[2] = 0x00.toByte()
            writeString(buf, 3, "EXTRA")
        }
        assertEquals("JK", DeviceInfoParser.parse(data).manuDeviceID)
    }

    @Test
    fun testHardwareVersion() {
        val data = makeData { writeString(it, 16, "V1.0A") }
        assertEquals("V1.0A", DeviceInfoParser.parse(data).hardwareVersion)
    }

    @Test
    fun testSoftwareVersion() {
        val data = makeData { writeString(it, 24, "V2.3.0") }
        assertEquals("V2.3.0", DeviceInfoParser.parse(data).softwareVersion)
    }

    @Test
    fun testBluetoothName() {
        val data = makeData { writeString(it, 40, "JK_BMS_BT") }
        assertEquals("JK_BMS_BT", DeviceInfoParser.parse(data).bluetoothName)
    }

    @Test
    fun testBluetoothPwd() {
        val data = makeData { writeString(it, 56, "1234") }
        assertEquals("1234", DeviceInfoParser.parse(data).bluetoothPwd)
    }

    @Test
    fun testManufactureDate() {
        val data = makeData { writeString(it, 72, "20231015") }
        assertEquals("20231015", DeviceInfoParser.parse(data).manufactureDate)
    }

    @Test
    fun testDeviceSN() {
        val data = makeData { writeString(it, 80, "SN12345678") }
        assertEquals("SN12345678", DeviceInfoParser.parse(data).deviceSN)
    }

    @Test
    fun testUserData() {
        val data = makeData { writeString(it, 96, "UserNote") }
        assertEquals("UserNote", DeviceInfoParser.parse(data).userData)
    }

    @Test
    fun testSettingPassword() {
        val data = makeData { writeString(it, 112, "admin") }
        assertEquals("admin", DeviceInfoParser.parse(data).settingPassword)
    }

    @Test
    fun testUserData2() {
        val data = makeData { writeString(it, 128, "ExtraData") }
        assertEquals("ExtraData", DeviceInfoParser.parse(data).userData2)
    }

    @Test
    fun testHardwareOption() {
        val data = makeData { writeString(it, 184, "OPT-A") }
        assertEquals("OPT-A", DeviceInfoParser.parse(data).hardwareOption)
    }

    @Test
    fun testMaxCells() {
        val data = makeData { FieldEncoder.writeU8(it, 15, 24) }
        assertEquals(24, DeviceInfoParser.parse(data).maxCells)
    }

    @Test
    fun testOddRunTime() {
        val data = makeData { FieldEncoder.writeU32(it, 32, 86400L) }
        assertEquals(86400L, DeviceInfoParser.parse(data).oddRunTime)
    }

    @Test
    fun testPwrOnTimes() {
        val data = makeData { FieldEncoder.writeU32(it, 36, 42L) }
        assertEquals(42L, DeviceInfoParser.parse(data).pwrOnTimes)
    }

    @Test
    fun testAgencyId() {
        val data = makeData { FieldEncoder.writeU16(it, 176, 0x1001) }
        assertEquals(0x1001, DeviceInfoParser.parse(data).agencyId)
    }

    @Test
    fun testUart1ProtoNo() {
        val data = makeData { FieldEncoder.writeU8(it, 178, 3) }
        assertEquals(3, DeviceInfoParser.parse(data).uart1ProtoNo)
    }

    @Test
    fun testCanProtoNo() {
        val data = makeData { FieldEncoder.writeU8(it, 179, 2) }
        assertEquals(2, DeviceInfoParser.parse(data).canProtoNo)
    }

    @Test
    fun testUart2ProtoNo() {
        val data = makeData { FieldEncoder.writeU8(it, 212, 1) }
        assertEquals(1, DeviceInfoParser.parse(data).uart2ProtoNo)
    }

    @Test
    fun testUart3ProtoNo() {
        val data = makeData { FieldEncoder.writeU8(it, 264, 4) }
        assertEquals(4, DeviceInfoParser.parse(data).uart3ProtoNo)
    }

    @Test
    fun testLcdBuzzerTrigger() {
        val data = makeData { FieldEncoder.writeU8(it, 228, 7) }
        assertEquals(7, DeviceInfoParser.parse(data).lcdBuzzerTrigger)
    }

    @Test
    fun testDry1Trigger() {
        val data = makeData { FieldEncoder.writeU8(it, 229, 5) }
        assertEquals(5, DeviceInfoParser.parse(data).dry1Trigger)
    }

    @Test
    fun testDry2Trigger() {
        val data = makeData { FieldEncoder.writeU8(it, 230, 6) }
        assertEquals(6, DeviceInfoParser.parse(data).dry2Trigger)
    }

    @Test
    fun testUartMPTLVer() {
        val data = makeData { FieldEncoder.writeU8(it, 231, 2) }
        assertEquals(2, DeviceInfoParser.parse(data).uartMPTLVer)
    }

    @Test
    fun testLcdBuzzerTriggerVal() {
        val data = makeData { FieldEncoder.writeI32(it, 232, -500) }
        assertEquals(-500, DeviceInfoParser.parse(data).lcdBuzzerTriggerVal)
    }

    @Test
    fun testLcdBuzzerReleaseVal() {
        val data = makeData { FieldEncoder.writeI32(it, 236, 1000) }
        assertEquals(1000, DeviceInfoParser.parse(data).lcdBuzzerReleaseVal)
    }

    @Test
    fun testDry1TriggerVal() {
        val data = makeData { FieldEncoder.writeI32(it, 240, -200) }
        assertEquals(-200, DeviceInfoParser.parse(data).dry1TriggerVal)
    }

    @Test
    fun testDry1ReleaseVal() {
        val data = makeData { FieldEncoder.writeI32(it, 244, 300) }
        assertEquals(300, DeviceInfoParser.parse(data).dry1ReleaseVal)
    }

    @Test
    fun testDry2TriggerVal() {
        val data = makeData { FieldEncoder.writeI32(it, 248, -100) }
        assertEquals(-100, DeviceInfoParser.parse(data).dry2TriggerVal)
    }

    @Test
    fun testDry2ReleaseVal() {
        val data = makeData { FieldEncoder.writeI32(it, 252, 400) }
        assertEquals(400, DeviceInfoParser.parse(data).dry2ReleaseVal)
    }

    @Test
    fun testDataStoredPeriod() {
        val data = makeData { FieldEncoder.writeU32(it, 256, 60L) }
        assertEquals(60L, DeviceInfoParser.parse(data).dataStoredPeriod)
    }

    @Test
    fun testRcvTime() {
        val data = makeData { FieldEncoder.writeU8(it, 260, 25) }
        assertEquals(2.5f, DeviceInfoParser.parse(data).rcvTime, 0.1f)
    }

    @Test
    fun testRfvTime() {
        val data = makeData { FieldEncoder.writeU8(it, 261, 30) }
        assertEquals(3.0f, DeviceInfoParser.parse(data).rfvTime, 0.1f)
    }

    @Test
    fun testCanMPTLVer() {
        val data = makeData { FieldEncoder.writeU8(it, 262, 3) }
        assertEquals(3, DeviceInfoParser.parse(data).canMPTLVer)
    }

    @Test
    fun testEmergencyTime() {
        val data = makeData { FieldEncoder.writeU8(it, 263, 10) }
        assertEquals(10, DeviceInfoParser.parse(data).emergencyTime)
    }

    @Test
    fun testReBulkSOC() {
        val data = makeData { FieldEncoder.writeU8(it, 272, 20) }
        assertEquals(20, DeviceInfoParser.parse(data).reBulkSOC)
    }

    @Test
    fun testProtocolVer() {
        val data = makeData { FieldEncoder.writeU8(it, 292, 1) }
        assertEquals(1, DeviceInfoParser.parse(data).protocolVer)
    }

    @Test
    fun testCmdSupportFlags() {
        val data = makeData { buf ->
            buf[144] = 0x01.toByte()
            buf[175] = 0xFF.toByte()
        }
        val result = DeviceInfoParser.parse(data).cmdSupportFlags
        assertEquals(32, result.size)
        assertEquals(0x01.toByte(), result[0])
        assertEquals(0xFF.toByte(), result[31])
    }

    @Test
    fun testUart1ProtoEnabled() {
        val data = makeData { buf ->
            buf[180] = 0xAB.toByte()
            buf[183] = 0xCD.toByte()
        }
        val result = DeviceInfoParser.parse(data).uart1ProtoEnabled
        assertEquals(4, result.size)
        assertEquals(0xAB.toByte(), result[0])
        assertEquals(0xCD.toByte(), result[3])
    }

    @Test
    fun testCanProtoEnabled() {
        val data = makeData { buf ->
            buf[196] = 0x11.toByte()
            buf[199] = 0x22.toByte()
        }
        val result = DeviceInfoParser.parse(data).canProtoEnabled
        assertEquals(4, result.size)
        assertEquals(0x11.toByte(), result[0])
        assertEquals(0x22.toByte(), result[3])
    }

    @Test
    fun testUart2ProtoEnabled() {
        val data = makeData { buf ->
            buf[213] = 0x55.toByte()
            buf[216] = 0x66.toByte()
        }
        val result = DeviceInfoParser.parse(data).uart2ProtoEnabled
        assertEquals(4, result.size)
        assertEquals(0x55.toByte(), result[0])
        assertEquals(0x66.toByte(), result[3])
    }

    @Test
    fun testUart3ProtoEnabled() {
        val data = makeData { buf ->
            buf[265] = 0xAA.toByte()
            buf[271] = 0xBB.toByte()
        }
        val result = DeviceInfoParser.parse(data).uart3ProtoEnabled
        assertEquals(7, result.size)
        assertEquals(0xAA.toByte(), result[0])
        assertEquals(0xBB.toByte(), result[6])
    }

    @Test
    fun testEnableFlags() {
        val data = makeData { buf ->
            buf[281] = 0x01.toByte()
            buf[291] = 0xFF.toByte()
        }
        val result = DeviceInfoParser.parse(data).enableFlags
        assertEquals(11, result.size)
        assertEquals(0x01.toByte(), result[0])
        assertEquals(0xFF.toByte(), result[10])
    }

    @Test(expected = IllegalArgumentException::class)
    fun testRequireGuard() {
        DeviceInfoParser.parse(ByteArray(100))
    }
}
