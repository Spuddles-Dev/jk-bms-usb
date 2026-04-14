# Protocol Unit Tests — Design Spec

**Date:** 2026-04-14
**Scope:** Exhaustive JUnit4 unit tests for all untested protocol-layer classes in `JkBmsApp/app/src/test/java/com/horse/jk_bms/protocol/`

---

## Context

The protocol engine is fully implemented. Three test files already exist (`ChecksumTest`, `FrameDecoderTest`, `FrameEncoderTest`). Six classes have no tests:

- `FieldDecoder` — primitive byte-array reads
- `FieldEncoder` — primitive byte-array writes
- `ConfigParser` — parses 293-byte data section → `BmsConfig` (46 fields)
- `RuntimeDataParser` — parses 293-byte data section → `BmsRuntimeData` (74 fields)
- `DeviceInfoParser` — parses 293-byte data section → `BmsDeviceInfo` (45 fields)
- `FaultInfoParser` / `SystemLogParser` — parse fault records and system log

All classes are pure Kotlin with no Android or coroutine dependencies. Tests run on the JVM with JUnit4. No mocking required.

---

## Decisions

- **Coverage:** Exhaustive — every field tested individually.
- **Test data:** Hand-crafted per test — each test creates a `ByteArray(293)` (or 300 for frame tests), writes only the bytes relevant to the field under test (everything else stays zero), parses, and asserts the single field.
- **Structure:** One test file per source class, mirroring the existing layout.
- **Float equality:** Use `assertEquals(expected, actual, delta)` with delta matching the scale factor (e.g., 0.001f for mV fields, 0.1f for temperature fields).
- **Array fields:** Assert length and each element individually (or use `assertArrayEquals` for byte arrays).

---

## Files to Create

All under `JkBmsApp/app/src/test/java/com/horse/jk_bms/protocol/`:

| File | Source | Tests |
|---|---|---|
| `FieldDecoderTest.kt` | `FieldDecoder.kt` | All 13 read methods |
| `FieldEncoderTest.kt` | `FieldEncoder.kt` | All 9 write methods |
| `ConfigParserTest.kt` | `ConfigParser.kt` | All 46 fields + require guard |
| `RuntimeDataParserTest.kt` | `RuntimeDataParser.kt` | All 74 fields + require guard |
| `DeviceInfoParserTest.kt` | `DeviceInfoParser.kt` | All 45 fields + require guard |
| `FaultInfoParserTest.kt` | `FaultInfoParser.kt` + `SystemLogParser` | Fault records + system log + require guards |

---

## FieldDecoderTest

One test method per read type. Each sets known bytes in a `ByteArray(8)` (enough for all types) and asserts the decoded value.

| Test | Method | Key assertions |
|---|---|---|
| `readU8Zero` | `readU8` | `0x00` → `0` |
| `readU8Max` | `readU8` | `0xFF` → `255` (unsigned, not -1) |
| `readI8Positive` | `readI8` | `0x7F` → `127` |
| `readI8Negative` | `readI8` | `0xFF` → `-1` |
| `readU16LittleEndian` | `readU16` | `[0x34, 0x12]` → `0x1234` |
| `readU16Max` | `readU16` | `[0xFF, 0xFF]` → `65535` |
| `readI16Negative` | `readI16` | `[0x00, 0x80]` → `-32768` |
| `readU32LittleEndian` | `readU32` | `[0x04, 0x03, 0x02, 0x01]` → `0x01020304L` |
| `readU32Max` | `readU32` | `[0xFF,0xFF,0xFF,0xFF]` → `4294967295L` (unsigned) |
| `readI32Negative` | `readI32` | `[0x00,0x00,0x00,0x80]` → `Int.MIN_VALUE` |
| `readF32KnownValue` | `readF32` | known IEEE754 bit pattern → expected float |
| `readU16Array` | `readU16Array` | count=3, verify each element and little-endian order |
| `readU32Array` | `readU32Array` | count=2, verify each element |
| `readBytes` | `readBytes` | offset+length slice matches source |
| `readStringNullTerminated` | `readString` | stops at embedded `0x00` |
| `readStringFullLength` | `readString` | no null byte, uses full length |
| `readBitmapAllZeros` | `readBitmap` | all bits false |
| `readBitmapAllOnes` | `readBitmap` | all bits true, correct count |
| `readBitmapAlternating` | `readBitmap` | `0b10101010` → alternating false/true pattern |

---

## FieldEncoderTest

Every write method tested via round-trip: write a known value into a `ByteArray`, read it back with the corresponding `FieldDecoder` method, assert equality. Multi-byte types also assert raw byte order explicitly.

| Test | Method | Key assertions |
|---|---|---|
| `writeU8RoundTrip` | `writeU8` | read back == written value; byte at offset == value |
| `writeI8Negative` | `writeI8` | `-1` round-trips correctly |
| `writeU16LittleEndian` | `writeU16` | `0x1234` → `[0x34, 0x12]` raw bytes |
| `writeI16Negative` | `writeI16` | negative value round-trips |
| `writeU32LittleEndian` | `writeU32` | `0x01020304L` → `[0x04,0x03,0x02,0x01]` raw bytes |
| `writeI32Negative` | `writeI32` | `Int.MIN_VALUE` round-trips |
| `writeF32KnownValue` | `writeF32` | known float round-trips via `readF32` |
| `writeU32Array` | `writeU32Array` | each element at correct offset |
| `writeBitmapAllTrue` | `writeBitmap` | 8-bit all-ones → `0xFF.toByte()` |
| `writeBitmapAlternating` | `writeBitmap` | alternating pattern → correct byte value |
| `writeBitmapPartialLastByte` | `writeBitmap` | 3-bit bitmap uses only low 3 bits of byte |

---

## ConfigParserTest

Each test: `val data = ByteArray(293)`, set bytes for one field, `val result = ConfigParser.parse(data)`, assert that field.

### Voltage fields (U32 * 0.001f, offsets 0–44 step 4)
| Field | Offset | Test value (raw) | Expected (float) |
|---|---|---|---|
| `volSmartSleep` | 0 | `45500` | `45.5f` |
| `volCellUV` | 4 | `2800` | `2.8f` |
| `volCellUVPR` | 8 | `2600` | `2.6f` |
| `volCellOV` | 12 | `3650` | `3.65f` |
| `volCellOVPR` | 16 | `3700` | `3.7f` |
| `volBalanTrig` | 20 | `3300` | `3.3f` |
| `volSOCP100` | 24 | `3600` | `3.6f` |
| `volSOCP0` | 28 | `3000` | `3.0f` |
| `volCellRCV` | 32 | `3100` | `3.1f` |
| `volCellRFV` | 36 | `3550` | `3.55f` |
| `volSysPwrOff` | 40 | `44000` | `44.0f` |

### Time/current fields (U32 * 0.001f or raw Long)
| Field | Offset | Type |
|---|---|---|
| `timBatCOC` | 44 | U32 * 0.001f |
| `timBatCOCPDly` | 48 | U32 raw Long |
| `timBatCOCPRDly` | 52 | U32 raw Long |
| `timBatDcOC` | 56 | U32 * 0.001f |
| `timBatDcOCPDly` | 60 | U32 raw Long |
| `timBatDcOCPRDly` | 64 | U32 raw Long |
| `timBatSCPRDly` | 68 | U32 raw Long |
| `curBalanMax` | 72 | U32 * 0.001f |

### Temperature fields (I32 * 0.1f, offsets 76–108)
All 8 temperature fields tested individually with both positive and negative values where applicable (`tmpBatCUT`, `tmpBatDCHUT`, etc.).

### Integer control fields
| Field | Offset | Type |
|---|---|---|
| `cellCount` | 108 | U32 raw Long |
| `batChargeEn` | 112 | U32 raw Long |
| `batDischargeEn` | 116 | U32 raw Long |
| `balanEn` | 120 | U32 raw Long |
| `capBatCell` | 124 | U32 * 0.001f |
| `scpDelay` | 128 | U32 raw Long |
| `volStartBalan` | 132 | U32 * 0.001f |

### cellConWireRes array (offsets 136–264, 32 × U32 * 0.001f)
One test sets all 32 elements to distinct values (`i * 1000` raw → `i.toFloat()`), asserts each.

### Tail fields (offsets 264–291)
| Field | Offset | Type |
|---|---|---|
| `devAddr` | 264 | U32 raw Long |
| `dischrgPreChrgT` | 268 | U32 raw Long |
| `currentRange` | 272 | U32 * 0.001f |
| `switchStatus` | 276 | bitmap 16 bits |
| `tmpStartHeating` | 278 | I8 |
| `tmpStopHeating` | 279 | I8 |
| `timeSmartSleep` | 280 | U8 |
| `enableFlags` | 281 | 9 bytes |
| `tmpBatDCHUT` | 290 | I8 |
| `tmpBatDCHUTPR` | 291 | I8 |

### Guard
`testRequireGuard` — `ConfigParser.parse(ByteArray(100))` throws `IllegalArgumentException`.

---

## RuntimeDataParserTest

Same pattern: one test per field (or per array). Key groupings:

**Cell voltage array** — set all 32 U16 values at offsets 0–64, assert `cellVoltages[i] == raw * 0.001f` for each.

**Cell wire resistance array** — set all 32 U16 values at offsets 74–138, assert `cellWireRes[i]`.

**Scalar numeric fields** — one test each:
- `batVol` (I32 at 144, * 0.001f) — test with negative value (discharging)
- `batWatt` (U32 at 148, * 0.001f)
- `batCurrent` (I32 at 152, * 0.001f) — test negative
- `batTemp1`/`batTemp2` (I16 at 156/158, * 0.1f) — test negative (sub-zero temp)
- `tempMos` (I16 at 138, * 0.1f)
- `soc` (U8 at 167, 0–100)
- `soh` (U8 at 184)
- `socCapabilityRemain` (U32 at 168, * 0.001f)
- `socFullChargeCapacity` (U32 at 172, * 0.001f)
- `socCycleCount` (U32 at 176, raw)
- `socCycleCapacity` (U32 at 180, * 0.001f)
- `runtime` (U32 at 188, raw)
- `equCurrent` (I16 at 164, * 0.001f)
- `equStatus` (U8 at 166)
- `userAlarm` (U16 at 186)
- `batTemp3`/`batTemp4`/`batTemp5` (I16 at 248/250/252, * 0.1f)
- `totalBatVol` (U16 at 228, * 0.01f)
- `heatCurrent` (I16 at 230, * 0.001f)

**Boolean fields** — each: set raw byte to 1, assert `true`; set to 0, assert `false`:
- `preDischarge` (U8 at 185)
- `chargeStatus` (U8 at 192)
- `dischargeStatus` (U8 at 193)
- `heatingStatus` (U8 at 209)
- `chargePlugged` (U8 at 239)
- `accStatus` (U8 at 233)
- `pclModule` (U8 at 268)

**Bitmap fields** — `sysAlarm` (32 bits at 160), `cellStatus` (32 bits at 64), `userAlarm2` (16 bits at 194), `tempSensorAbsent` (8 bits at 208), `switchStatus` (3 bits at 275): set known byte, assert specific bit positions.

**Remaining scalar fields** — `rtcCounter`, `detailLogsCount`, `timeEnterSleep`, `chrgCurCorrect`, `dischrgCurCorrect`, `chargPWM`, `dischargPWM`, `startupFlag`, `volC`, `mcuid`, `batteryType`, `chargeStatus2`, `sysRunTicks`, `pvdTrigTimestamps`, `timeDcOCPR`, `timeDcSCPR`, `timeCOCPR`, `timeCSCPR`, `timeUVPR`, `timeOVPR`, `timeEmerg`, `volChargCur`, `volDischargCur`, `batVolCorrect` (F32), `specialChargerSta`, `chargeStatusTime`.

**Guard** — `ByteArray(100)` throws `IllegalArgumentException`.

---

## DeviceInfoParserTest

**String fields** — tested for correct content:
- `manuDeviceID` (offset 0, 15 bytes)
- `hardwareVersion` (16, 8), `softwareVersion` (24, 8)
- `bluetoothName` (40, 16), `bluetoothPwd` (56, 16)
- `manufactureDate` (72, 8), `deviceSN` (80, 16)
- `userData` (96, 16), `settingPassword` (112, 16), `userData2` (128, 16)
- `hardwareOption` (184, 12)

**Null termination** — one test writes `"ABC\u0000XYZ"` into a string field and asserts the result is `"ABC"`.

**Numeric fields:**
- `maxCells` U8 at 15
- `oddRunTime` / `pwrOnTimes` U32 at 32/36
- `agencyId` U16 at 176
- `uart1ProtoNo` / `canProtoNo` / `uart2ProtoNo` / `uart3ProtoNo` U8
- `lcdBuzzerTriggerVal` / `lcdBuzzerReleaseVal` / `dry1TriggerVal` / `dry1ReleaseVal` / `dry2TriggerVal` / `dry2ReleaseVal` I32 — test with negative values
- `dataStoredPeriod` U32 at 256
- `rcvTime` (U8 at 260, * 0.1f)
- `rfvTime` (U8 at 261, * 0.1f)
- `canMPTLVer` / `emergencyTime` / `uart3ProtoNo` / `reBulkSOC` / `protocolVer` / `uartMPTLVer` / `lcdBuzzerTrigger` / `dry1Trigger` / `dry2Trigger` U8
- `uart1ProtoEnabled` / `canProtoEnabled` / `uart2ProtoEnabled` / `uart3ProtoEnabled` byte arrays — assert length and content
- `cmdSupportFlags` (32 bytes at 144), `enableFlags` (11 bytes at 281) — assert length and content

**Guard** — `ByteArray(100)` throws `IllegalArgumentException`.

---

## FaultInfoParserTest

**FaultInfoParser:**

| Test | Description |
|---|---|
| `testZeroRecords` | `count=0` → `records` is empty |
| `testSingleRecordAllFields` | One record at offset 3: set all 15 fields to known values, assert each after scaling |
| `testMultipleRecords` | `count=2`: verify first record at offset 3, second at offset 27 (3 + 24) |
| `testScaleFactors` | `volCellMax` (* 0.001f), `volBat` (* 0.01f), `curBat` (* 0.1f), `heatCurrent` (* 0.1f) |
| `testBeginIndexAndCount` | `beginIndex` U16 at 0, `count` U8 at 2 parsed correctly |
| `testRequireGuard` | `ByteArray(100)` throws `IllegalArgumentException` |

**SystemLogParser (same file):**

| Test | Description |
|---|---|
| `testLogCountAndCheck` | `logCount` U32 at 0, `check` U8 at 4 |
| `testAlarmLogContent` | 250-byte `alarmLog` at offset 5: assert length=250 and known bytes |
| `testSystemLogRequireGuard` | `ByteArray(100)` throws `IllegalArgumentException` |

---

## Testing Conventions (consistent with existing tests)

- Package: `com.horse.jk_bms.protocol`
- Imports: `org.junit.Assert.*`, `org.junit.Test`
- Float delta: match scale factor of the field (0.001f for mV/mA, 0.1f for temperature/time)
- No mocking, no coroutines — pure synchronous JVM tests
- Test names: `test<FieldName>` for single-field tests, `test<FeatureScenario>` for multi-field
