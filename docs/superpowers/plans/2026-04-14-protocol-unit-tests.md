# Protocol Unit Tests Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add exhaustive JUnit4 unit tests for all untested protocol-layer classes: `FieldDecoder`, `FieldEncoder`, `ConfigParser`, `RuntimeDataParser`, `DeviceInfoParser`, `FaultInfoParser`, and `SystemLogParser`.

**Architecture:** One test file per source class, placed in `JkBmsApp/app/src/test/java/com/horse/jk_bms/protocol/`. Each parser test uses a private `makeData {}` helper to produce a clean 293-byte array with only the relevant bytes set. `FieldEncoder` is used inside parser tests to write multi-byte values cleanly.

**Tech Stack:** JUnit4, Kotlin, JVM — no Android dependencies, no coroutines, no mocking required.

---

### Task 1: FieldDecoderTest

**Files:**
- Create: `JkBmsApp/app/src/test/java/com/horse/jk_bms/protocol/FieldDecoderTest.kt`

- [ ] **Step 1: Create the test file**

```kotlin
package com.horse.jk_bms.protocol

import org.junit.Assert.*
import org.junit.Test

class FieldDecoderTest {

    // --- readU8 ---

    @Test
    fun testReadU8Zero() {
        val data = ByteArray(8)
        assertEquals(0, FieldDecoder.readU8(data, 0))
    }

    @Test
    fun testReadU8Max() {
        val data = ByteArray(8)
        data[2] = 0xFF.toByte()
        assertEquals(255, FieldDecoder.readU8(data, 2))
    }

    @Test
    fun testReadU8Mid() {
        val data = ByteArray(8)
        data[3] = 0x7F.toByte()
        assertEquals(127, FieldDecoder.readU8(data, 3))
    }

    // --- readI8 ---

    @Test
    fun testReadI8Positive() {
        val data = ByteArray(8)
        data[1] = 0x7F.toByte()
        assertEquals(127, FieldDecoder.readI8(data, 1))
    }

    @Test
    fun testReadI8Negative() {
        val data = ByteArray(8)
        data[1] = 0xFF.toByte()
        assertEquals(-1, FieldDecoder.readI8(data, 1))
    }

    @Test
    fun testReadI8MinValue() {
        val data = ByteArray(8)
        data[0] = 0x80.toByte()
        assertEquals(-128, FieldDecoder.readI8(data, 0))
    }

    // --- readU16 ---

    @Test
    fun testReadU16LittleEndian() {
        val data = ByteArray(8)
        data[0] = 0x34.toByte()
        data[1] = 0x12.toByte()
        assertEquals(0x1234, FieldDecoder.readU16(data, 0))
    }

    @Test
    fun testReadU16Max() {
        val data = ByteArray(8)
        data[2] = 0xFF.toByte()
        data[3] = 0xFF.toByte()
        assertEquals(65535, FieldDecoder.readU16(data, 2))
    }

    @Test
    fun testReadU16Zero() {
        val data = ByteArray(8)
        assertEquals(0, FieldDecoder.readU16(data, 0))
    }

    // --- readI16 ---

    @Test
    fun testReadI16Negative() {
        val data = ByteArray(8)
        data[0] = 0x00.toByte()
        data[1] = 0x80.toByte()
        assertEquals(-32768, FieldDecoder.readI16(data, 0))
    }

    @Test
    fun testReadI16Positive() {
        val data = ByteArray(8)
        data[0] = 0xFF.toByte()
        data[1] = 0x7F.toByte()
        assertEquals(32767, FieldDecoder.readI16(data, 0))
    }

    @Test
    fun testReadI16MinusOne() {
        val data = ByteArray(8)
        data[0] = 0xFF.toByte()
        data[1] = 0xFF.toByte()
        assertEquals(-1, FieldDecoder.readI16(data, 0))
    }

    // --- readU32 ---

    @Test
    fun testReadU32LittleEndian() {
        val data = ByteArray(8)
        data[0] = 0x04.toByte()
        data[1] = 0x03.toByte()
        data[2] = 0x02.toByte()
        data[3] = 0x01.toByte()
        assertEquals(0x01020304L, FieldDecoder.readU32(data, 0))
    }

    @Test
    fun testReadU32Max() {
        val data = ByteArray(8)
        data[0] = 0xFF.toByte()
        data[1] = 0xFF.toByte()
        data[2] = 0xFF.toByte()
        data[3] = 0xFF.toByte()
        assertEquals(4294967295L, FieldDecoder.readU32(data, 0))
    }

    @Test
    fun testReadU32Zero() {
        val data = ByteArray(8)
        assertEquals(0L, FieldDecoder.readU32(data, 0))
    }

    // --- readI32 ---

    @Test
    fun testReadI32Negative() {
        val data = ByteArray(8)
        data[0] = 0x00.toByte()
        data[1] = 0x00.toByte()
        data[2] = 0x00.toByte()
        data[3] = 0x80.toByte()
        assertEquals(Int.MIN_VALUE, FieldDecoder.readI32(data, 0))
    }

    @Test
    fun testReadI32Positive() {
        val data = ByteArray(8)
        data[0] = 0xFF.toByte()
        data[1] = 0xFF.toByte()
        data[2] = 0xFF.toByte()
        data[3] = 0x7F.toByte()
        assertEquals(Int.MAX_VALUE, FieldDecoder.readI32(data, 0))
    }

    @Test
    fun testReadI32MinusOne() {
        val data = ByteArray(8)
        data[0] = 0xFF.toByte()
        data[1] = 0xFF.toByte()
        data[2] = 0xFF.toByte()
        data[3] = 0xFF.toByte()
        assertEquals(-1, FieldDecoder.readI32(data, 0))
    }

    // --- readF32 ---

    @Test
    fun testReadF32KnownValue() {
        val data = ByteArray(8)
        val bits = java.lang.Float.floatToRawIntBits(3.14f)
        data[0] = (bits and 0xFF).toByte()
        data[1] = ((bits shr 8) and 0xFF).toByte()
        data[2] = ((bits shr 16) and 0xFF).toByte()
        data[3] = ((bits shr 24) and 0xFF).toByte()
        assertEquals(3.14f, FieldDecoder.readF32(data, 0), 0.0001f)
    }

    @Test
    fun testReadF32Zero() {
        val data = ByteArray(8)
        assertEquals(0.0f, FieldDecoder.readF32(data, 0), 0.0001f)
    }

    @Test
    fun testReadF32Negative() {
        val data = ByteArray(8)
        val bits = java.lang.Float.floatToRawIntBits(-1.5f)
        data[0] = (bits and 0xFF).toByte()
        data[1] = ((bits shr 8) and 0xFF).toByte()
        data[2] = ((bits shr 16) and 0xFF).toByte()
        data[3] = ((bits shr 24) and 0xFF).toByte()
        assertEquals(-1.5f, FieldDecoder.readF32(data, 0), 0.0001f)
    }

    // --- readU16Array ---

    @Test
    fun testReadU16Array() {
        val data = ByteArray(16)
        data[0] = 0x02.toByte(); data[1] = 0x01.toByte() // 0x0102
        data[2] = 0x04.toByte(); data[3] = 0x03.toByte() // 0x0304
        data[4] = 0x06.toByte(); data[5] = 0x05.toByte() // 0x0506
        val result = FieldDecoder.readU16Array(data, 0, 3)
        assertEquals(3, result.size)
        assertEquals(0x0102, result[0])
        assertEquals(0x0304, result[1])
        assertEquals(0x0506, result[2])
    }

    @Test
    fun testReadU16ArrayOffset() {
        val data = ByteArray(16)
        data[4] = 0xAB.toByte(); data[5] = 0x00.toByte() // 0x00AB at offset 4
        val result = FieldDecoder.readU16Array(data, 4, 1)
        assertEquals(0x00AB, result[0])
    }

    // --- readU32Array ---

    @Test
    fun testReadU32Array() {
        val data = ByteArray(16)
        data[0] = 0x01.toByte(); data[1] = 0x00.toByte(); data[2] = 0x00.toByte(); data[3] = 0x00.toByte()
        data[4] = 0x02.toByte(); data[5] = 0x00.toByte(); data[6] = 0x00.toByte(); data[7] = 0x00.toByte()
        val result = FieldDecoder.readU32Array(data, 0, 2)
        assertEquals(2, result.size)
        assertEquals(1L, result[0])
        assertEquals(2L, result[1])
    }

    // --- readBytes ---

    @Test
    fun testReadBytes() {
        val data = byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05, 0x06)
        val result = FieldDecoder.readBytes(data, 2, 3)
        assertArrayEquals(byteArrayOf(0x03, 0x04, 0x05), result)
    }

    @Test
    fun testReadBytesFromStart() {
        val data = byteArrayOf(0xAA.toByte(), 0xBB.toByte(), 0xCC.toByte())
        val result = FieldDecoder.readBytes(data, 0, 3)
        assertArrayEquals(data, result)
    }

    // --- readString ---

    @Test
    fun testReadStringNullTerminated() {
        val data = ByteArray(16)
        data[0] = 'A'.code.toByte()
        data[1] = 'B'.code.toByte()
        data[2] = 'C'.code.toByte()
        data[3] = 0x00.toByte()
        data[4] = 'X'.code.toByte() // ignored after null
        assertEquals("ABC", FieldDecoder.readString(data, 0, 8))
    }

    @Test
    fun testReadStringFullLength() {
        val data = ByteArray(16)
        data[0] = 'H'.code.toByte()
        data[1] = 'i'.code.toByte()
        assertEquals("Hi", FieldDecoder.readString(data, 0, 2))
    }

    @Test
    fun testReadStringEmpty() {
        val data = ByteArray(8)
        data[0] = 0x00.toByte()
        assertEquals("", FieldDecoder.readString(data, 0, 4))
    }

    @Test
    fun testReadStringWithOffset() {
        val data = ByteArray(16)
        data[5] = 'Z'.code.toByte()
        data[6] = 0x00.toByte()
        assertEquals("Z", FieldDecoder.readString(data, 5, 4))
    }

    // --- readBitmap ---

    @Test
    fun testReadBitmapAllZeros() {
        val data = ByteArray(4)
        val result = FieldDecoder.readBitmap(data, 0, 8)
        assertEquals(8, result.size)
        result.forEach { assertFalse(it) }
    }

    @Test
    fun testReadBitmapAllOnes() {
        val data = ByteArray(4)
        data[0] = 0xFF.toByte()
        val result = FieldDecoder.readBitmap(data, 0, 8)
        assertEquals(8, result.size)
        result.forEach { assertTrue(it) }
    }

    @Test
    fun testReadBitmapAlternating() {
        val data = ByteArray(4)
        data[0] = 0b10101010.toByte() // bits 1,3,5,7 set
        val result = FieldDecoder.readBitmap(data, 0, 8)
        assertFalse(result[0])
        assertTrue(result[1])
        assertFalse(result[2])
        assertTrue(result[3])
        assertFalse(result[4])
        assertTrue(result[5])
        assertFalse(result[6])
        assertTrue(result[7])
    }

    @Test
    fun testReadBitmap32Bits() {
        val data = ByteArray(8)
        data[0] = 0xFF.toByte()
        data[1] = 0x00.toByte()
        data[2] = 0xFF.toByte()
        data[3] = 0x00.toByte()
        val result = FieldDecoder.readBitmap(data, 0, 32)
        assertEquals(32, result.size)
        for (i in 0..7)   assertTrue(result[i])
        for (i in 8..15)  assertFalse(result[i])
        for (i in 16..23) assertTrue(result[i])
        for (i in 24..31) assertFalse(result[i])
    }

    @Test
    fun testReadBitmapPartialLastByte() {
        val data = ByteArray(4)
        data[0] = 0b00000111.toByte() // only 3 bits used
        val result = FieldDecoder.readBitmap(data, 0, 3)
        assertEquals(3, result.size)
        assertTrue(result[0])
        assertTrue(result[1])
        assertTrue(result[2])
    }
}
```

- [ ] **Step 2: Run the tests**

```bash
cd JkBmsApp
./gradlew test --tests "com.horse.jk_bms.protocol.FieldDecoderTest"
```

Expected: `BUILD SUCCESSFUL` with all tests passing.

- [ ] **Step 3: Commit**

```bash
git add JkBmsApp/app/src/test/java/com/horse/jk_bms/protocol/FieldDecoderTest.kt
git commit -m "test: add exhaustive FieldDecoder unit tests"
```

---

### Task 2: FieldEncoderTest

**Files:**
- Create: `JkBmsApp/app/src/test/java/com/horse/jk_bms/protocol/FieldEncoderTest.kt`

- [ ] **Step 1: Create the test file**

```kotlin
package com.horse.jk_bms.protocol

import org.junit.Assert.*
import org.junit.Test

class FieldEncoderTest {

    // --- writeU8 ---

    @Test
    fun testWriteU8RoundTrip() {
        val data = ByteArray(8)
        FieldEncoder.writeU8(data, 3, 200)
        assertEquals(200, FieldDecoder.readU8(data, 3))
    }

    @Test
    fun testWriteU8RawByte() {
        val data = ByteArray(8)
        FieldEncoder.writeU8(data, 0, 0xAB)
        assertEquals(0xAB.toByte(), data[0])
    }

    @Test
    fun testWriteU8Zero() {
        val data = ByteArray(8)
        FieldEncoder.writeU8(data, 0, 0)
        assertEquals(0, FieldDecoder.readU8(data, 0))
    }

    @Test
    fun testWriteU8Max() {
        val data = ByteArray(8)
        FieldEncoder.writeU8(data, 0, 255)
        assertEquals(255, FieldDecoder.readU8(data, 0))
    }

    // --- writeI8 ---

    @Test
    fun testWriteI8Negative() {
        val data = ByteArray(8)
        FieldEncoder.writeI8(data, 0, -1)
        assertEquals(-1, FieldDecoder.readI8(data, 0))
    }

    @Test
    fun testWriteI8MinValue() {
        val data = ByteArray(8)
        FieldEncoder.writeI8(data, 0, -128)
        assertEquals(-128, FieldDecoder.readI8(data, 0))
    }

    @Test
    fun testWriteI8Positive() {
        val data = ByteArray(8)
        FieldEncoder.writeI8(data, 0, 127)
        assertEquals(127, FieldDecoder.readI8(data, 0))
    }

    // --- writeU16 ---

    @Test
    fun testWriteU16LittleEndian() {
        val data = ByteArray(8)
        FieldEncoder.writeU16(data, 0, 0x1234)
        assertEquals(0x34.toByte(), data[0]) // low byte first
        assertEquals(0x12.toByte(), data[1]) // high byte second
    }

    @Test
    fun testWriteU16RoundTrip() {
        val data = ByteArray(8)
        FieldEncoder.writeU16(data, 0, 65535)
        assertEquals(65535, FieldDecoder.readU16(data, 0))
    }

    // --- writeI16 ---

    @Test
    fun testWriteI16Negative() {
        val data = ByteArray(8)
        FieldEncoder.writeI16(data, 0, -1000)
        assertEquals(-1000, FieldDecoder.readI16(data, 0))
    }

    @Test
    fun testWriteI16MinValue() {
        val data = ByteArray(8)
        FieldEncoder.writeI16(data, 0, -32768)
        assertEquals(-32768, FieldDecoder.readI16(data, 0))
    }

    // --- writeU32 ---

    @Test
    fun testWriteU32LittleEndian() {
        val data = ByteArray(8)
        FieldEncoder.writeU32(data, 0, 0x01020304L)
        assertEquals(0x04.toByte(), data[0])
        assertEquals(0x03.toByte(), data[1])
        assertEquals(0x02.toByte(), data[2])
        assertEquals(0x01.toByte(), data[3])
    }

    @Test
    fun testWriteU32RoundTrip() {
        val data = ByteArray(8)
        FieldEncoder.writeU32(data, 0, 4294967295L)
        assertEquals(4294967295L, FieldDecoder.readU32(data, 0))
    }

    @Test
    fun testWriteU32AtOffset() {
        val data = ByteArray(8)
        FieldEncoder.writeU32(data, 4, 0x000000FFL)
        assertEquals(0xFF.toByte(), data[4])
        assertEquals(0x00.toByte(), data[5])
        assertEquals(0x00.toByte(), data[6])
        assertEquals(0x00.toByte(), data[7])
    }

    // --- writeI32 ---

    @Test
    fun testWriteI32Negative() {
        val data = ByteArray(8)
        FieldEncoder.writeI32(data, 0, Int.MIN_VALUE)
        assertEquals(Int.MIN_VALUE, FieldDecoder.readI32(data, 0))
    }

    @Test
    fun testWriteI32MinusOne() {
        val data = ByteArray(8)
        FieldEncoder.writeI32(data, 0, -1)
        assertEquals(-1, FieldDecoder.readI32(data, 0))
    }

    @Test
    fun testWriteI32Positive() {
        val data = ByteArray(8)
        FieldEncoder.writeI32(data, 0, 123456)
        assertEquals(123456, FieldDecoder.readI32(data, 0))
    }

    // --- writeF32 ---

    @Test
    fun testWriteF32KnownValue() {
        val data = ByteArray(8)
        FieldEncoder.writeF32(data, 0, 3.14f)
        assertEquals(3.14f, FieldDecoder.readF32(data, 0), 0.0001f)
    }

    @Test
    fun testWriteF32Negative() {
        val data = ByteArray(8)
        FieldEncoder.writeF32(data, 0, -273.15f)
        assertEquals(-273.15f, FieldDecoder.readF32(data, 0), 0.001f)
    }

    @Test
    fun testWriteF32Zero() {
        val data = ByteArray(8)
        FieldEncoder.writeF32(data, 0, 0.0f)
        assertEquals(0.0f, FieldDecoder.readF32(data, 0), 0.0001f)
    }

    // --- writeU32Array ---

    @Test
    fun testWriteU32Array() {
        val data = ByteArray(16)
        FieldEncoder.writeU32Array(data, 0, longArrayOf(100L, 200L, 300L))
        assertEquals(100L, FieldDecoder.readU32(data, 0))
        assertEquals(200L, FieldDecoder.readU32(data, 4))
        assertEquals(300L, FieldDecoder.readU32(data, 8))
    }

    @Test
    fun testWriteU32ArrayAtOffset() {
        val data = ByteArray(16)
        FieldEncoder.writeU32Array(data, 4, longArrayOf(0xABCDL, 0x1234L))
        assertEquals(0xABCDL, FieldDecoder.readU32(data, 4))
        assertEquals(0x1234L, FieldDecoder.readU32(data, 8))
        // bytes before offset untouched
        assertEquals(0L, FieldDecoder.readU32(data, 0))
    }

    // --- writeBitmap ---

    @Test
    fun testWriteBitmapAllTrue() {
        val data = ByteArray(4)
        FieldEncoder.writeBitmap(data, 0, BooleanArray(8) { true })
        assertEquals(0xFF.toByte(), data[0])
    }

    @Test
    fun testWriteBitmapAllFalse() {
        val data = ByteArray(4)
        FieldEncoder.writeBitmap(data, 0, BooleanArray(8) { false })
        assertEquals(0x00.toByte(), data[0])
    }

    @Test
    fun testWriteBitmapAlternating() {
        val data = ByteArray(4)
        // bits 1,3,5,7 true → 0b10101010
        FieldEncoder.writeBitmap(data, 0, booleanArrayOf(false, true, false, true, false, true, false, true))
        assertEquals(0b10101010.toByte(), data[0])
    }

    @Test
    fun testWriteBitmapPartialLastByte() {
        val data = ByteArray(4)
        // 3 bits: true, false, true → 0b00000101 = 5
        FieldEncoder.writeBitmap(data, 0, booleanArrayOf(true, false, true))
        assertEquals(5.toByte(), data[0])
        assertEquals(0.toByte(), data[1]) // untouched
    }

    @Test
    fun testWriteBitmapRoundTrip16Bits() {
        val data = ByteArray(8)
        val bits = booleanArrayOf(
            true, false, true, true, false, false, true, false,
            false, true, false, false, true, true, false, true
        )
        FieldEncoder.writeBitmap(data, 0, bits)
        val result = FieldDecoder.readBitmap(data, 0, 16)
        assertArrayEquals(bits, result)
    }
}
```

- [ ] **Step 2: Run the tests**

```bash
cd JkBmsApp
./gradlew test --tests "com.horse.jk_bms.protocol.FieldEncoderTest"
```

Expected: `BUILD SUCCESSFUL` with all tests passing.

- [ ] **Step 3: Commit**

```bash
git add JkBmsApp/app/src/test/java/com/horse/jk_bms/protocol/FieldEncoderTest.kt
git commit -m "test: add exhaustive FieldEncoder unit tests"
```

---

### Task 3: ConfigParserTest

**Files:**
- Create: `JkBmsApp/app/src/test/java/com/horse/jk_bms/protocol/ConfigParserTest.kt`

- [ ] **Step 1: Create the test file**

```kotlin
package com.horse.jk_bms.protocol

import org.junit.Assert.*
import org.junit.Test

class ConfigParserTest {

    private fun makeData(setup: (ByteArray) -> Unit): ByteArray {
        val data = ByteArray(293)
        setup(data)
        return data
    }

    // --- Voltage fields (U32 * 0.001f) ---

    @Test
    fun testVolSmartSleep() {
        val data = makeData { FieldEncoder.writeU32(it, 0, 45500L) }
        assertEquals(45.5f, ConfigParser.parse(data).volSmartSleep, 0.001f)
    }

    @Test
    fun testVolCellUV() {
        val data = makeData { FieldEncoder.writeU32(it, 4, 2800L) }
        assertEquals(2.8f, ConfigParser.parse(data).volCellUV, 0.001f)
    }

    @Test
    fun testVolCellUVPR() {
        val data = makeData { FieldEncoder.writeU32(it, 8, 2600L) }
        assertEquals(2.6f, ConfigParser.parse(data).volCellUVPR, 0.001f)
    }

    @Test
    fun testVolCellOV() {
        val data = makeData { FieldEncoder.writeU32(it, 12, 3650L) }
        assertEquals(3.65f, ConfigParser.parse(data).volCellOV, 0.001f)
    }

    @Test
    fun testVolCellOVPR() {
        val data = makeData { FieldEncoder.writeU32(it, 16, 3700L) }
        assertEquals(3.7f, ConfigParser.parse(data).volCellOVPR, 0.001f)
    }

    @Test
    fun testVolBalanTrig() {
        val data = makeData { FieldEncoder.writeU32(it, 20, 3300L) }
        assertEquals(3.3f, ConfigParser.parse(data).volBalanTrig, 0.001f)
    }

    @Test
    fun testVolSOCP100() {
        val data = makeData { FieldEncoder.writeU32(it, 24, 3600L) }
        assertEquals(3.6f, ConfigParser.parse(data).volSOCP100, 0.001f)
    }

    @Test
    fun testVolSOCP0() {
        val data = makeData { FieldEncoder.writeU32(it, 28, 3000L) }
        assertEquals(3.0f, ConfigParser.parse(data).volSOCP0, 0.001f)
    }

    @Test
    fun testVolCellRCV() {
        val data = makeData { FieldEncoder.writeU32(it, 32, 3100L) }
        assertEquals(3.1f, ConfigParser.parse(data).volCellRCV, 0.001f)
    }

    @Test
    fun testVolCellRFV() {
        val data = makeData { FieldEncoder.writeU32(it, 36, 3550L) }
        assertEquals(3.55f, ConfigParser.parse(data).volCellRFV, 0.001f)
    }

    @Test
    fun testVolSysPwrOff() {
        val data = makeData { FieldEncoder.writeU32(it, 40, 44000L) }
        assertEquals(44.0f, ConfigParser.parse(data).volSysPwrOff, 0.001f)
    }

    // --- Time / current fields ---

    @Test
    fun testTimBatCOC() {
        val data = makeData { FieldEncoder.writeU32(it, 44, 50500L) }
        assertEquals(50.5f, ConfigParser.parse(data).timBatCOC, 0.001f)
    }

    @Test
    fun testTimBatCOCPDly() {
        val data = makeData { FieldEncoder.writeU32(it, 48, 100L) }
        assertEquals(100L, ConfigParser.parse(data).timBatCOCPDly)
    }

    @Test
    fun testTimBatCOCPRDly() {
        val data = makeData { FieldEncoder.writeU32(it, 52, 200L) }
        assertEquals(200L, ConfigParser.parse(data).timBatCOCPRDly)
    }

    @Test
    fun testTimBatDcOC() {
        val data = makeData { FieldEncoder.writeU32(it, 56, 75000L) }
        assertEquals(75.0f, ConfigParser.parse(data).timBatDcOC, 0.001f)
    }

    @Test
    fun testTimBatDcOCPDly() {
        val data = makeData { FieldEncoder.writeU32(it, 60, 300L) }
        assertEquals(300L, ConfigParser.parse(data).timBatDcOCPDly)
    }

    @Test
    fun testTimBatDcOCPRDly() {
        val data = makeData { FieldEncoder.writeU32(it, 64, 400L) }
        assertEquals(400L, ConfigParser.parse(data).timBatDcOCPRDly)
    }

    @Test
    fun testTimBatSCPRDly() {
        val data = makeData { FieldEncoder.writeU32(it, 68, 500L) }
        assertEquals(500L, ConfigParser.parse(data).timBatSCPRDly)
    }

    @Test
    fun testCurBalanMax() {
        val data = makeData { FieldEncoder.writeU32(it, 72, 750L) }
        assertEquals(0.75f, ConfigParser.parse(data).curBalanMax, 0.001f)
    }

    // --- Temperature fields (I32 * 0.1f) ---

    @Test
    fun testTmpBatCOT() {
        val data = makeData { FieldEncoder.writeI32(it, 76, 650) }
        assertEquals(65.0f, ConfigParser.parse(data).tmpBatCOT, 0.1f)
    }

    @Test
    fun testTmpBatCOTPR() {
        val data = makeData { FieldEncoder.writeI32(it, 80, 600) }
        assertEquals(60.0f, ConfigParser.parse(data).tmpBatCOTPR, 0.1f)
    }

    @Test
    fun testTmpBatDcOT() {
        val data = makeData { FieldEncoder.writeI32(it, 84, 700) }
        assertEquals(70.0f, ConfigParser.parse(data).tmpBatDcOT, 0.1f)
    }

    @Test
    fun testTmpBatDcOTPR() {
        val data = makeData { FieldEncoder.writeI32(it, 88, 650) }
        assertEquals(65.0f, ConfigParser.parse(data).tmpBatDcOTPR, 0.1f)
    }

    @Test
    fun testTmpBatCUTNegative() {
        val data = makeData { FieldEncoder.writeI32(it, 92, -200) } // -20.0°C
        assertEquals(-20.0f, ConfigParser.parse(data).tmpBatCUT, 0.1f)
    }

    @Test
    fun testTmpBatCUTPR() {
        val data = makeData { FieldEncoder.writeI32(it, 96, -150) } // -15.0°C
        assertEquals(-15.0f, ConfigParser.parse(data).tmpBatCUTPR, 0.1f)
    }

    @Test
    fun testTmpMosOT() {
        val data = makeData { FieldEncoder.writeI32(it, 100, 850) }
        assertEquals(85.0f, ConfigParser.parse(data).tmpMosOT, 0.1f)
    }

    @Test
    fun testTmpMosOTPR() {
        val data = makeData { FieldEncoder.writeI32(it, 104, 800) }
        assertEquals(80.0f, ConfigParser.parse(data).tmpMosOTPR, 0.1f)
    }

    // --- Integer control fields ---

    @Test
    fun testCellCount() {
        val data = makeData { FieldEncoder.writeU32(it, 108, 20L) }
        assertEquals(20L, ConfigParser.parse(data).cellCount)
    }

    @Test
    fun testBatChargeEn() {
        val data = makeData { FieldEncoder.writeU32(it, 112, 1L) }
        assertEquals(1L, ConfigParser.parse(data).batChargeEn)
    }

    @Test
    fun testBatDischargeEn() {
        val data = makeData { FieldEncoder.writeU32(it, 116, 1L) }
        assertEquals(1L, ConfigParser.parse(data).batDischargeEn)
    }

    @Test
    fun testBalanEn() {
        val data = makeData { FieldEncoder.writeU32(it, 120, 0L) }
        assertEquals(0L, ConfigParser.parse(data).balanEn)
    }

    @Test
    fun testCapBatCell() {
        val data = makeData { FieldEncoder.writeU32(it, 124, 100000L) } // 100 Ah
        assertEquals(100.0f, ConfigParser.parse(data).capBatCell, 0.001f)
    }

    @Test
    fun testScpDelay() {
        val data = makeData { FieldEncoder.writeU32(it, 128, 10L) }
        assertEquals(10L, ConfigParser.parse(data).scpDelay)
    }

    @Test
    fun testVolStartBalan() {
        val data = makeData { FieldEncoder.writeU32(it, 132, 3350L) }
        assertEquals(3.35f, ConfigParser.parse(data).volStartBalan, 0.001f)
    }

    // --- cellConWireRes array (32 × U32 * 0.001f, offsets 136–264) ---

    @Test
    fun testCellConWireResArray() {
        val data = makeData { buf ->
            for (i in 0 until 32) {
                FieldEncoder.writeU32(buf, 136 + i * 4, (i * 1000).toLong())
            }
        }
        val result = ConfigParser.parse(data).cellConWireRes
        assertEquals(32, result.size)
        for (i in 0 until 32) {
            assertEquals(i.toFloat(), result[i], 0.001f)
        }
    }

    // --- Tail fields (offsets 264–291) ---

    @Test
    fun testDevAddr() {
        val data = makeData { FieldEncoder.writeU32(it, 264, 1L) }
        assertEquals(1L, ConfigParser.parse(data).devAddr)
    }

    @Test
    fun testDischrgPreChrgT() {
        val data = makeData { FieldEncoder.writeU32(it, 268, 5L) }
        assertEquals(5L, ConfigParser.parse(data).dischrgPreChrgT)
    }

    @Test
    fun testCurrentRange() {
        val data = makeData { FieldEncoder.writeU32(it, 272, 200000L) } // 200 A
        assertEquals(200.0f, ConfigParser.parse(data).currentRange, 0.001f)
    }

    @Test
    fun testSwitchStatus() {
        val data = makeData { buf ->
            // set bit 0 and bit 3 in the 16-bit bitmap at offset 276
            FieldEncoder.writeBitmap(buf, 276, BooleanArray(16).also {
                it[0] = true
                it[3] = true
            })
        }
        val result = ConfigParser.parse(data).switchStatus
        assertEquals(16, result.size)
        assertTrue(result[0])
        assertFalse(result[1])
        assertFalse(result[2])
        assertTrue(result[3])
    }

    @Test
    fun testTmpStartHeating() {
        val data = makeData { FieldEncoder.writeI8(it, 278, -10) }
        assertEquals(-10, ConfigParser.parse(data).tmpStartHeating)
    }

    @Test
    fun testTmpStopHeating() {
        val data = makeData { FieldEncoder.writeI8(it, 279, 5) }
        assertEquals(5, ConfigParser.parse(data).tmpStopHeating)
    }

    @Test
    fun testTimeSmartSleep() {
        val data = makeData { FieldEncoder.writeU8(it, 280, 30) }
        assertEquals(30, ConfigParser.parse(data).timeSmartSleep)
    }

    @Test
    fun testEnableFlags() {
        val data = makeData { buf ->
            buf[281] = 0x01.toByte()
            buf[282] = 0x02.toByte()
            buf[288] = 0xFF.toByte()
        }
        val flags = ConfigParser.parse(data).enableFlags
        assertEquals(9, flags.size)
        assertEquals(0x01.toByte(), flags[0])
        assertEquals(0x02.toByte(), flags[1])
        assertEquals(0xFF.toByte(), flags[7])
    }

    @Test
    fun testTmpBatDCHUT() {
        val data = makeData { FieldEncoder.writeI8(it, 290, -20) }
        assertEquals(-20, ConfigParser.parse(data).tmpBatDCHUT)
    }

    @Test
    fun testTmpBatDCHUTPR() {
        val data = makeData { FieldEncoder.writeI8(it, 291, -15) }
        assertEquals(-15, ConfigParser.parse(data).tmpBatDCHUTPR)
    }

    // --- Guard ---

    @Test(expected = IllegalArgumentException::class)
    fun testRequireGuard() {
        ConfigParser.parse(ByteArray(100))
    }
}
```

- [ ] **Step 2: Run the tests**

```bash
cd JkBmsApp
./gradlew test --tests "com.horse.jk_bms.protocol.ConfigParserTest"
```

Expected: `BUILD SUCCESSFUL` with all tests passing.

- [ ] **Step 3: Commit**

```bash
git add JkBmsApp/app/src/test/java/com/horse/jk_bms/protocol/ConfigParserTest.kt
git commit -m "test: add exhaustive ConfigParser unit tests"
```

---

### Task 4: RuntimeDataParserTest

**Files:**
- Create: `JkBmsApp/app/src/test/java/com/horse/jk_bms/protocol/RuntimeDataParserTest.kt`

- [ ] **Step 1: Create the test file**

```kotlin
package com.horse.jk_bms.protocol

import org.junit.Assert.*
import org.junit.Test

class RuntimeDataParserTest {

    private fun makeData(setup: (ByteArray) -> Unit): ByteArray {
        val data = ByteArray(293)
        setup(data)
        return data
    }

    // --- Cell voltage array (32 × U16 * 0.001f, offsets 0–64) ---

    @Test
    fun testCellVoltagesArray() {
        val data = makeData { buf ->
            for (i in 0 until 32) {
                FieldEncoder.writeU16(buf, i * 2, 3300 + i)
            }
        }
        val result = RuntimeDataParser.parse(data).cellVoltages
        assertEquals(32, result.size)
        for (i in 0 until 32) {
            assertEquals((3300 + i) * 0.001f, result[i], 0.001f)
        }
    }

    // --- cellStatus bitmap (32 bits at 64) ---

    @Test
    fun testCellStatus() {
        val data = makeData { buf ->
            buf[64] = 0b10101010.toByte()
            buf[65] = 0x00.toByte()
            buf[66] = 0x00.toByte()
            buf[67] = 0x00.toByte()
        }
        val result = RuntimeDataParser.parse(data).cellStatus
        assertEquals(32, result.size)
        assertFalse(result[0])
        assertTrue(result[1])
        assertFalse(result[2])
        assertTrue(result[3])
    }

    // --- Average / delta / min / max cell ---

    @Test
    fun testCellVolAve() {
        val data = makeData { FieldEncoder.writeU16(it, 68, 3350) }
        assertEquals(3.35f, RuntimeDataParser.parse(data).cellVolAve, 0.001f)
    }

    @Test
    fun testMaxVoltDelta() {
        val data = makeData { FieldEncoder.writeU16(it, 70, 15) }
        assertEquals(0.015f, RuntimeDataParser.parse(data).maxVoltDelta, 0.001f)
    }

    @Test
    fun testCelMaxVol() {
        val data = makeData { FieldEncoder.writeU8(it, 72, 7) }
        assertEquals(7, RuntimeDataParser.parse(data).celMaxVol)
    }

    @Test
    fun testCelMinVol() {
        val data = makeData { FieldEncoder.writeU8(it, 73, 3) }
        assertEquals(3, RuntimeDataParser.parse(data).celMinVol)
    }

    // --- Cell wire resistance array (32 × U16 * 0.001f, offsets 74–138) ---

    @Test
    fun testCellWireResArray() {
        val data = makeData { buf ->
            for (i in 0 until 32) {
                FieldEncoder.writeU16(buf, 74 + i * 2, 100 + i)
            }
        }
        val result = RuntimeDataParser.parse(data).cellWireRes
        assertEquals(32, result.size)
        for (i in 0 until 32) {
            assertEquals((100 + i) * 0.001f, result[i], 0.001f)
        }
    }

    // --- tempMos (I16 at 138, * 0.1f) ---

    @Test
    fun testTempMos() {
        val data = makeData { FieldEncoder.writeI16(it, 138, 350) } // 35.0°C
        assertEquals(35.0f, RuntimeDataParser.parse(data).tempMos, 0.1f)
    }

    @Test
    fun testTempMosNegative() {
        val data = makeData { FieldEncoder.writeI16(it, 138, -50) } // -5.0°C
        assertEquals(-5.0f, RuntimeDataParser.parse(data).tempMos, 0.1f)
    }

    // --- batVol (I32 at 144, * 0.001f) ---

    @Test
    fun testBatVol() {
        val data = makeData { FieldEncoder.writeI32(it, 144, 52400) }
        assertEquals(52.4f, RuntimeDataParser.parse(data).batVol, 0.001f)
    }

    @Test
    fun testBatVolNegative() {
        val data = makeData { FieldEncoder.writeI32(it, 144, -100) }
        assertEquals(-0.1f, RuntimeDataParser.parse(data).batVol, 0.001f)
    }

    // --- batWatt (U32 at 148, * 0.001f) ---

    @Test
    fun testBatWatt() {
        val data = makeData { FieldEncoder.writeU32(it, 148, 5000000L) } // 5000 W
        assertEquals(5000.0f, RuntimeDataParser.parse(data).batWatt, 0.001f)
    }

    // --- batCurrent (I32 at 152, * 0.001f) ---

    @Test
    fun testBatCurrentCharging() {
        val data = makeData { FieldEncoder.writeI32(it, 152, 20000) } // 20 A charging
        assertEquals(20.0f, RuntimeDataParser.parse(data).batCurrent, 0.001f)
    }

    @Test
    fun testBatCurrentDischarging() {
        val data = makeData { FieldEncoder.writeI32(it, 152, -30000) } // 30 A discharging
        assertEquals(-30.0f, RuntimeDataParser.parse(data).batCurrent, 0.001f)
    }

    // --- batTemp1, batTemp2 (I16 at 156, 158, * 0.1f) ---

    @Test
    fun testBatTemp1() {
        val data = makeData { FieldEncoder.writeI16(it, 156, 250) } // 25.0°C
        assertEquals(25.0f, RuntimeDataParser.parse(data).batTemp1, 0.1f)
    }

    @Test
    fun testBatTemp1Negative() {
        val data = makeData { FieldEncoder.writeI16(it, 156, -100) } // -10.0°C
        assertEquals(-10.0f, RuntimeDataParser.parse(data).batTemp1, 0.1f)
    }

    @Test
    fun testBatTemp2() {
        val data = makeData { FieldEncoder.writeI16(it, 158, 300) }
        assertEquals(30.0f, RuntimeDataParser.parse(data).batTemp2, 0.1f)
    }

    // --- sysAlarm bitmap (32 bits at 160) ---

    @Test
    fun testSysAlarm() {
        val data = makeData { buf ->
            buf[160] = 0x01.toByte() // bit 0 set
            buf[161] = 0x00.toByte()
            buf[162] = 0x00.toByte()
            buf[163] = 0x00.toByte()
        }
        val result = RuntimeDataParser.parse(data).sysAlarm
        assertEquals(32, result.size)
        assertTrue(result[0])
        assertFalse(result[1])
    }

    // --- equCurrent, equStatus ---

    @Test
    fun testEquCurrent() {
        val data = makeData { FieldEncoder.writeI16(it, 164, 500) } // 0.5 A
        assertEquals(0.5f, RuntimeDataParser.parse(data).equCurrent, 0.001f)
    }

    @Test
    fun testEquCurrentNegative() {
        val data = makeData { FieldEncoder.writeI16(it, 164, -200) }
        assertEquals(-0.2f, RuntimeDataParser.parse(data).equCurrent, 0.001f)
    }

    @Test
    fun testEquStatus() {
        val data = makeData { FieldEncoder.writeU8(it, 166, 3) }
        assertEquals(3, RuntimeDataParser.parse(data).equStatus)
    }

    // --- soc (U8 at 167) ---

    @Test
    fun testSoc() {
        val data = makeData { FieldEncoder.writeU8(it, 167, 85) }
        assertEquals(85, RuntimeDataParser.parse(data).soc)
    }

    @Test
    fun testSocFull() {
        val data = makeData { FieldEncoder.writeU8(it, 167, 100) }
        assertEquals(100, RuntimeDataParser.parse(data).soc)
    }

    // --- SOC capacity fields ---

    @Test
    fun testSocCapabilityRemain() {
        val data = makeData { FieldEncoder.writeU32(it, 168, 85000L) } // 85 Ah
        assertEquals(85.0f, RuntimeDataParser.parse(data).socCapabilityRemain, 0.001f)
    }

    @Test
    fun testSocFullChargeCapacity() {
        val data = makeData { FieldEncoder.writeU32(it, 172, 100000L) }
        assertEquals(100.0f, RuntimeDataParser.parse(data).socFullChargeCapacity, 0.001f)
    }

    @Test
    fun testSocCycleCount() {
        val data = makeData { FieldEncoder.writeU32(it, 176, 42L) }
        assertEquals(42L, RuntimeDataParser.parse(data).socCycleCount)
    }

    @Test
    fun testSocCycleCapacity() {
        val data = makeData { FieldEncoder.writeU32(it, 180, 50000L) }
        assertEquals(50.0f, RuntimeDataParser.parse(data).socCycleCapacity, 0.001f)
    }

    // --- soh (U8 at 184) ---

    @Test
    fun testSoh() {
        val data = makeData { FieldEncoder.writeU8(it, 184, 97) }
        assertEquals(97, RuntimeDataParser.parse(data).soh)
    }

    // --- Boolean fields ---

    @Test
    fun testPreDischargeTrue() {
        val data = makeData { FieldEncoder.writeU8(it, 185, 1) }
        assertTrue(RuntimeDataParser.parse(data).preDischarge)
    }

    @Test
    fun testPreDischargeFalse() {
        val data = makeData { FieldEncoder.writeU8(it, 185, 0) }
        assertFalse(RuntimeDataParser.parse(data).preDischarge)
    }

    @Test
    fun testUserAlarm() {
        val data = makeData { FieldEncoder.writeU16(it, 186, 0x0003) }
        assertEquals(0x0003, RuntimeDataParser.parse(data).userAlarm)
    }

    // --- runtime (U32 at 188) ---

    @Test
    fun testRuntime() {
        val data = makeData { FieldEncoder.writeU32(it, 188, 3600L) } // 1 hour
        assertEquals(3600L, RuntimeDataParser.parse(data).runtime)
    }

    // --- chargeStatus, dischargeStatus (U8 at 192, 193) ---

    @Test
    fun testChargeStatusTrue() {
        val data = makeData { FieldEncoder.writeU8(it, 192, 1) }
        assertTrue(RuntimeDataParser.parse(data).chargeStatus)
    }

    @Test
    fun testChargeStatusFalse() {
        val data = makeData { FieldEncoder.writeU8(it, 192, 0) }
        assertFalse(RuntimeDataParser.parse(data).chargeStatus)
    }

    @Test
    fun testDischargeStatusTrue() {
        val data = makeData { FieldEncoder.writeU8(it, 193, 1) }
        assertTrue(RuntimeDataParser.parse(data).dischargeStatus)
    }

    // --- userAlarm2 bitmap (16 bits at 194) ---

    @Test
    fun testUserAlarm2() {
        val data = makeData { buf ->
            buf[194] = 0x05.toByte() // bits 0 and 2 set
            buf[195] = 0x00.toByte()
        }
        val result = RuntimeDataParser.parse(data).userAlarm2
        assertEquals(16, result.size)
        assertTrue(result[0])
        assertFalse(result[1])
        assertTrue(result[2])
    }

    // --- Recovery timer fields (U16) ---

    @Test
    fun testTimeDcOCPR() {
        val data = makeData { FieldEncoder.writeU16(it, 196, 30) }
        assertEquals(30, RuntimeDataParser.parse(data).timeDcOCPR)
    }

    @Test
    fun testTimeDcSCPR() {
        val data = makeData { FieldEncoder.writeU16(it, 198, 60) }
        assertEquals(60, RuntimeDataParser.parse(data).timeDcSCPR)
    }

    @Test
    fun testTimeCOCPR() {
        val data = makeData { FieldEncoder.writeU16(it, 200, 90) }
        assertEquals(90, RuntimeDataParser.parse(data).timeCOCPR)
    }

    @Test
    fun testTimeCSCPR() {
        val data = makeData { FieldEncoder.writeU16(it, 202, 120) }
        assertEquals(120, RuntimeDataParser.parse(data).timeCSCPR)
    }

    @Test
    fun testTimeUVPR() {
        val data = makeData { FieldEncoder.writeU16(it, 204, 150) }
        assertEquals(150, RuntimeDataParser.parse(data).timeUVPR)
    }

    @Test
    fun testTimeOVPR() {
        val data = makeData { FieldEncoder.writeU16(it, 206, 180) }
        assertEquals(180, RuntimeDataParser.parse(data).timeOVPR)
    }

    // --- tempSensorAbsent bitmap (8 bits at 208) ---

    @Test
    fun testTempSensorAbsent() {
        val data = makeData { it[208] = 0b00000011.toByte() } // sensors 0 and 1 absent
        val result = RuntimeDataParser.parse(data).tempSensorAbsent
        assertEquals(8, result.size)
        assertTrue(result[0])
        assertTrue(result[1])
        assertFalse(result[2])
    }

    // --- heatingStatus (U8 at 209) ---

    @Test
    fun testHeatingStatusTrue() {
        val data = makeData { FieldEncoder.writeU8(it, 209, 1) }
        assertTrue(RuntimeDataParser.parse(data).heatingStatus)
    }

    @Test
    fun testHeatingStatusFalse() {
        val data = makeData { FieldEncoder.writeU8(it, 209, 0) }
        assertFalse(RuntimeDataParser.parse(data).heatingStatus)
    }

    // --- timeEmerg (U16 at 212) ---

    @Test
    fun testTimeEmerg() {
        val data = makeData { FieldEncoder.writeU16(it, 212, 10) }
        assertEquals(10, RuntimeDataParser.parse(data).timeEmerg)
    }

    // --- dischrgCurCorrect (U16 at 214) ---

    @Test
    fun testDischrgCurCorrect() {
        val data = makeData { FieldEncoder.writeU16(it, 214, 1000) }
        assertEquals(1000, RuntimeDataParser.parse(data).dischrgCurCorrect)
    }

    // --- volChargCur, volDischargCur (U16 * 0.001f at 216, 218) ---

    @Test
    fun testVolChargCur() {
        val data = makeData { FieldEncoder.writeU16(it, 216, 4200) }
        assertEquals(4.2f, RuntimeDataParser.parse(data).volChargCur, 0.001f)
    }

    @Test
    fun testVolDischargCur() {
        val data = makeData { FieldEncoder.writeU16(it, 218, 3800) }
        assertEquals(3.8f, RuntimeDataParser.parse(data).volDischargCur, 0.001f)
    }

    // --- batVolCorrect (F32 at 220) ---

    @Test
    fun testBatVolCorrect() {
        val data = makeData { FieldEncoder.writeF32(it, 220, 1.05f) }
        assertEquals(1.05f, RuntimeDataParser.parse(data).batVolCorrect, 0.0001f)
    }

    // --- chargPWM, dischargPWM (U16 at 224, 226) ---

    @Test
    fun testChargPWM() {
        val data = makeData { FieldEncoder.writeU16(it, 224, 512) }
        assertEquals(512, RuntimeDataParser.parse(data).chargPWM)
    }

    @Test
    fun testDischargPWM() {
        val data = makeData { FieldEncoder.writeU16(it, 226, 768) }
        assertEquals(768, RuntimeDataParser.parse(data).dischargPWM)
    }

    // --- totalBatVol (U16 * 0.01f at 228) ---

    @Test
    fun testTotalBatVol() {
        val data = makeData { FieldEncoder.writeU16(it, 228, 5240) } // 52.4 V
        assertEquals(52.4f, RuntimeDataParser.parse(data).totalBatVol, 0.01f)
    }

    // --- heatCurrent (I16 * 0.001f at 230) ---

    @Test
    fun testHeatCurrent() {
        val data = makeData { FieldEncoder.writeI16(it, 230, 2000) } // 2 A
        assertEquals(2.0f, RuntimeDataParser.parse(data).heatCurrent, 0.001f)
    }

    @Test
    fun testHeatCurrentNegative() {
        val data = makeData { FieldEncoder.writeI16(it, 230, -500) }
        assertEquals(-0.5f, RuntimeDataParser.parse(data).heatCurrent, 0.001f)
    }

    // --- accStatus (U8 at 233) ---

    @Test
    fun testAccStatusTrue() {
        val data = makeData { FieldEncoder.writeU8(it, 233, 1) }
        assertTrue(RuntimeDataParser.parse(data).accStatus)
    }

    // --- specialChargerSta (U8 at 234) ---

    @Test
    fun testSpecialChargerStaTrue() {
        val data = makeData { FieldEncoder.writeU8(it, 234, 1) }
        assertTrue(RuntimeDataParser.parse(data).specialChargerSta)
    }

    // --- startupFlag (U8 at 235) ---

    @Test
    fun testStartupFlag() {
        val data = makeData { FieldEncoder.writeU8(it, 235, 2) }
        assertEquals(2, RuntimeDataParser.parse(data).startupFlag)
    }

    // --- volC (U8 at 236) ---

    @Test
    fun testVolC() {
        val data = makeData { FieldEncoder.writeU8(it, 236, 48) }
        assertEquals(48.0f, RuntimeDataParser.parse(data).volC, 0.001f)
    }

    // --- mcuid (U8 at 237) ---

    @Test
    fun testMcuid() {
        val data = makeData { FieldEncoder.writeU8(it, 237, 5) }
        assertEquals(5, RuntimeDataParser.parse(data).mcuid)
    }

    // --- chargePlugged (U8 at 239) ---

    @Test
    fun testChargePluggedTrue() {
        val data = makeData { FieldEncoder.writeU8(it, 239, 1) }
        assertTrue(RuntimeDataParser.parse(data).chargePlugged)
    }

    @Test
    fun testChargePluggedFalse() {
        val data = makeData { FieldEncoder.writeU8(it, 239, 0) }
        assertFalse(RuntimeDataParser.parse(data).chargePlugged)
    }

    // --- sysRunTicks (U32 * 0.1f at 240) ---

    @Test
    fun testSysRunTicks() {
        val data = makeData { FieldEncoder.writeU32(it, 240, 36000L) } // 3600.0 ticks
        assertEquals(3600.0f, RuntimeDataParser.parse(data).sysRunTicks, 0.1f)
    }

    // --- pvdTrigTimestamps (U32 * 0.1f at 244) ---

    @Test
    fun testPvdTrigTimestamps() {
        val data = makeData { FieldEncoder.writeU32(it, 244, 100L) }
        assertEquals(10.0f, RuntimeDataParser.parse(data).pvdTrigTimestamps, 0.1f)
    }

    // --- batTemp3, batTemp4, batTemp5 (I16 * 0.1f at 248, 250, 252) ---

    @Test
    fun testBatTemp3() {
        val data = makeData { FieldEncoder.writeI16(it, 248, 280) }
        assertEquals(28.0f, RuntimeDataParser.parse(data).batTemp3, 0.1f)
    }

    @Test
    fun testBatTemp4() {
        val data = makeData { FieldEncoder.writeI16(it, 250, 290) }
        assertEquals(29.0f, RuntimeDataParser.parse(data).batTemp4, 0.1f)
    }

    @Test
    fun testBatTemp5() {
        val data = makeData { FieldEncoder.writeI16(it, 252, 300) }
        assertEquals(30.0f, RuntimeDataParser.parse(data).batTemp5, 0.1f)
    }

    // --- chrgCurCorrect (U16 at 254) ---

    @Test
    fun testChrgCurCorrect() {
        val data = makeData { FieldEncoder.writeU16(it, 254, 1100) }
        assertEquals(1100, RuntimeDataParser.parse(data).chrgCurCorrect)
    }

    // --- rtcCounter (U32 at 256) ---

    @Test
    fun testRtcCounter() {
        val data = makeData { FieldEncoder.writeU32(it, 256, 86400L) }
        assertEquals(86400L, RuntimeDataParser.parse(data).rtcCounter)
    }

    // --- detailLogsCount (U32 at 260) ---

    @Test
    fun testDetailLogsCount() {
        val data = makeData { FieldEncoder.writeU32(it, 260, 99L) }
        assertEquals(99L, RuntimeDataParser.parse(data).detailLogsCount)
    }

    // --- timeEnterSleep (U32 at 264) ---

    @Test
    fun testTimeEnterSleep() {
        val data = makeData { FieldEncoder.writeU32(it, 264, 7200L) }
        assertEquals(7200L, RuntimeDataParser.parse(data).timeEnterSleep)
    }

    // --- pclModule (U8 at 268) ---

    @Test
    fun testPclModuleTrue() {
        val data = makeData { FieldEncoder.writeU8(it, 268, 1) }
        assertTrue(RuntimeDataParser.parse(data).pclModule)
    }

    @Test
    fun testPclModuleFalse() {
        val data = makeData { FieldEncoder.writeU8(it, 268, 0) }
        assertFalse(RuntimeDataParser.parse(data).pclModule)
    }

    // --- batteryType (U8 at 269) ---

    @Test
    fun testBatteryType() {
        val data = makeData { FieldEncoder.writeU8(it, 269, 1) } // Li-ion
        assertEquals(1, RuntimeDataParser.parse(data).batteryType)
    }

    // --- chargeStatusTime (U16 at 272) ---

    @Test
    fun testChargeStatusTime() {
        val data = makeData { FieldEncoder.writeU16(it, 272, 600) }
        assertEquals(600, RuntimeDataParser.parse(data).chargeStatusTime)
    }

    // --- chargeStatus2 (U8 at 274) ---

    @Test
    fun testChargeStatus2() {
        val data = makeData { FieldEncoder.writeU8(it, 274, 2) }
        assertEquals(2, RuntimeDataParser.parse(data).chargeStatus2)
    }

    // --- switchStatus bitmap (3 bits at 275) ---

    @Test
    fun testSwitchStatus() {
        val data = makeData { buf ->
            buf[275] = 0b00000101.toByte() // bits 0 and 2 set
        }
        val result = RuntimeDataParser.parse(data).switchStatus
        assertEquals(3, result.size)
        assertTrue(result[0])
        assertFalse(result[1])
        assertTrue(result[2])
    }

    // --- Guard ---

    @Test(expected = IllegalArgumentException::class)
    fun testRequireGuard() {
        RuntimeDataParser.parse(ByteArray(100))
    }
}
```

- [ ] **Step 2: Run the tests**

```bash
cd JkBmsApp
./gradlew test --tests "com.horse.jk_bms.protocol.RuntimeDataParserTest"
```

Expected: `BUILD SUCCESSFUL` with all tests passing.

- [ ] **Step 3: Commit**

```bash
git add JkBmsApp/app/src/test/java/com/horse/jk_bms/protocol/RuntimeDataParserTest.kt
git commit -m "test: add exhaustive RuntimeDataParser unit tests"
```

---

### Task 5: DeviceInfoParserTest

**Files:**
- Create: `JkBmsApp/app/src/test/java/com/horse/jk_bms/protocol/DeviceInfoParserTest.kt`

- [ ] **Step 1: Create the test file**

```kotlin
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

    // --- String fields ---

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
            writeString(buf, 3, "EXTRA") // should be ignored
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

    // --- Numeric fields ---

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
        val data = makeData { FieldEncoder.writeU8(it, 260, 25) } // 2.5 hours
        assertEquals(2.5f, DeviceInfoParser.parse(data).rcvTime, 0.1f)
    }

    @Test
    fun testRfvTime() {
        val data = makeData { FieldEncoder.writeU8(it, 261, 30) } // 3.0 hours
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

    // --- Byte array fields ---

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

    // --- Guard ---

    @Test(expected = IllegalArgumentException::class)
    fun testRequireGuard() {
        DeviceInfoParser.parse(ByteArray(100))
    }
}
```

- [ ] **Step 2: Run the tests**

```bash
cd JkBmsApp
./gradlew test --tests "com.horse.jk_bms.protocol.DeviceInfoParserTest"
```

Expected: `BUILD SUCCESSFUL` with all tests passing.

- [ ] **Step 3: Commit**

```bash
git add JkBmsApp/app/src/test/java/com/horse/jk_bms/protocol/DeviceInfoParserTest.kt
git commit -m "test: add exhaustive DeviceInfoParser unit tests"
```

---

### Task 6: FaultInfoParserTest (includes SystemLogParser)

**Files:**
- Create: `JkBmsApp/app/src/test/java/com/horse/jk_bms/protocol/FaultInfoParserTest.kt`

- [ ] **Step 1: Create the test file**

```kotlin
package com.horse.jk_bms.protocol

import org.junit.Assert.*
import org.junit.Test

class FaultInfoParserTest {

    private fun makeData(setup: (ByteArray) -> Unit): ByteArray {
        val data = ByteArray(293)
        setup(data)
        return data
    }

    // ─── FaultInfoParser ────────────────────────────────────────────────────

    @Test
    fun testZeroRecords() {
        val data = makeData { buf ->
            FieldEncoder.writeU16(buf, 0, 0)  // beginIndex
            FieldEncoder.writeU8(buf, 2, 0)   // count = 0
        }
        val result = FaultInfoParser.parse(data)
        assertEquals(0, result.count)
        assertTrue(result.records.isEmpty())
    }

    @Test
    fun testBeginIndexAndCount() {
        val data = makeData { buf ->
            FieldEncoder.writeU16(buf, 0, 42)  // beginIndex
            FieldEncoder.writeU8(buf, 2, 1)    // count = 1 (need at least 1 to parse)
        }
        val result = FaultInfoParser.parse(data)
        assertEquals(42, result.beginIndex)
        assertEquals(1, result.count)
    }

    @Test
    fun testSingleRecordAllFields() {
        // Record at offset 3, 24 bytes total
        val data = makeData { buf ->
            FieldEncoder.writeU8(buf, 2, 1)               // count = 1
            FieldEncoder.writeU32(buf, 3, 123456L)        // rtcCount
            FieldEncoder.writeU8(buf, 7, 0x05)            // logCode
            // switchSta bitmap at offset 8 (1 byte, 4 bits)
            buf[8] = 0b00001001.toByte()                  // bits 0 and 3 set
            FieldEncoder.writeU8(buf, 9, 7)               // maxVolCellNo
            FieldEncoder.writeU8(buf, 10, 2)              // minVolCellNo
            FieldEncoder.writeU16(buf, 11, 3650)          // volCellMax (* 0.001f) = 3.65V
            FieldEncoder.writeU16(buf, 13, 3100)          // volCellMin (* 0.001f) = 3.1V
            FieldEncoder.writeU16(buf, 15, 5240)          // volBat (* 0.01f) = 52.4V
            FieldEncoder.writeU16(buf, 17, 300)           // curBat (* 0.1f) = 30.0A
            FieldEncoder.writeU16(buf, 19, 850)           // socCapRemain (* 0.1f) = 85.0
            FieldEncoder.writeU16(buf, 21, 1000)          // socFullChargeCap (* 0.1f) = 100.0
            FieldEncoder.writeU8(buf, 23, 45)             // maxTemp
            FieldEncoder.writeU8(buf, 24, 20)             // minTemp
            FieldEncoder.writeU8(buf, 25, 38)             // tempMos
            FieldEncoder.writeU8(buf, 26, 15)             // heatCurrent (* 0.1f) = 1.5A
        }
        val result = FaultInfoParser.parse(data)
        assertEquals(1, result.records.size)
        val r = result.records[0]
        assertEquals(123456L, r.rtcCount)
        assertEquals(0x05, r.logCode)
        assertTrue(r.switchSta[0])
        assertFalse(r.switchSta[1])
        assertFalse(r.switchSta[2])
        assertTrue(r.switchSta[3])
        assertEquals(7, r.maxVolCellNo)
        assertEquals(2, r.minVolCellNo)
        assertEquals(3.65f, r.volCellMax, 0.001f)
        assertEquals(3.1f, r.volCellMin, 0.001f)
        assertEquals(52.4f, r.volBat, 0.01f)
        assertEquals(30.0f, r.curBat, 0.1f)
        assertEquals(85.0f, r.socCapRemain, 0.1f)
        assertEquals(100.0f, r.socFullChargeCap, 0.1f)
        assertEquals(45, r.maxTemp)
        assertEquals(20, r.minTemp)
        assertEquals(38, r.tempMos)
        assertEquals(1.5f, r.heatCurrent, 0.1f)
    }

    @Test
    fun testMultipleRecords() {
        // First record at offset 3, second at offset 3+24=27
        val data = makeData { buf ->
            FieldEncoder.writeU8(buf, 2, 2)           // count = 2
            FieldEncoder.writeU32(buf, 3, 1000L)      // first record rtcCount
            FieldEncoder.writeU32(buf, 27, 2000L)     // second record rtcCount
        }
        val result = FaultInfoParser.parse(data)
        assertEquals(2, result.records.size)
        assertEquals(1000L, result.records[0].rtcCount)
        assertEquals(2000L, result.records[1].rtcCount)
    }

    @Test
    fun testScaleFactors() {
        val data = makeData { buf ->
            FieldEncoder.writeU8(buf, 2, 1)
            // volCellMax at offset 3+8=11, U16 * 0.001f
            FieldEncoder.writeU16(buf, 11, 4000)   // 4.000V
            // volBat at offset 3+12=15, U16 * 0.01f
            FieldEncoder.writeU16(buf, 15, 4800)   // 48.00V
            // curBat at offset 3+14=17, U16 * 0.1f
            FieldEncoder.writeU16(buf, 17, 500)    // 50.0A
            // heatCurrent at offset 3+23=26, U8 * 0.1f
            FieldEncoder.writeU8(buf, 26, 25)      // 2.5A
        }
        val r = FaultInfoParser.parse(data).records[0]
        assertEquals(4.000f, r.volCellMax, 0.001f)
        assertEquals(48.00f, r.volBat, 0.01f)
        assertEquals(50.0f, r.curBat, 0.1f)
        assertEquals(2.5f, r.heatCurrent, 0.1f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testFaultInfoRequireGuard() {
        FaultInfoParser.parse(ByteArray(100))
    }

    // ─── SystemLogParser ─────────────────────────────────────────────────────

    @Test
    fun testSystemLogLogCount() {
        val data = makeData { FieldEncoder.writeU32(it, 0, 999L) }
        assertEquals(999L, SystemLogParser.parse(data).logCount)
    }

    @Test
    fun testSystemLogCheck() {
        val data = makeData { FieldEncoder.writeU8(it, 4, 0xAB) }
        assertEquals(0xAB, SystemLogParser.parse(data).check)
    }

    @Test
    fun testSystemLogAlarmLogContent() {
        val data = makeData { buf ->
            buf[5] = 0x11.toByte()
            buf[6] = 0x22.toByte()
            buf[254] = 0xFF.toByte() // last byte of 250-byte alarmLog
        }
        val log = SystemLogParser.parse(data).alarmLog
        assertEquals(250, log.size)
        assertEquals(0x11.toByte(), log[0])
        assertEquals(0x22.toByte(), log[1])
        assertEquals(0xFF.toByte(), log[249])
    }

    @Test(expected = IllegalArgumentException::class)
    fun testSystemLogRequireGuard() {
        SystemLogParser.parse(ByteArray(100))
    }
}
```

- [ ] **Step 2: Run all protocol tests together to confirm nothing is broken**

```bash
cd JkBmsApp
./gradlew test --tests "com.horse.jk_bms.protocol.*"
```

Expected: `BUILD SUCCESSFUL` with all tests passing (existing `ChecksumTest`, `FrameDecoderTest`, `FrameEncoderTest` plus the 6 new files).

- [ ] **Step 3: Commit**

```bash
git add JkBmsApp/app/src/test/java/com/horse/jk_bms/protocol/FaultInfoParserTest.kt
git commit -m "test: add exhaustive FaultInfoParser and SystemLogParser unit tests"
```
