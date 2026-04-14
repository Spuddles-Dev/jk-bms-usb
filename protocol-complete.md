# JK-BMS Serial Protocol — Complete Specification

**Source:** Reverse-engineered from `protocore.dll` (v4.13.1) and decrypted `en_US.json` / `zh_CN.json` protocol definitions.

---

## Physical Layer

| Parameter | Value |
|---|---|
| Interface | UART / TTL Serial (3.3V) |
| Baud rate | **115200** |
| Data bits | 8 |
| Stop bits | 1 |
| Parity | None |
| Flow control | None |

P5 connector pinout (standard JK-B2A20S20P):
- GND → adapter GND
- RX → adapter TX
- TX → adapter RX
- VGPS → leave disconnected

---

## Frame Structure

**Every frame is exactly 300 bytes.**

```
Offset  Size   Field        Description
------  ----   -----        -----------
0       4      Header       Fixed magic: 55 AA EB 90
4       1      FrameCode    Command/response type (0x01–0x06)
5       1      Counter      Frame sequence counter
6       293    Data         Frame payload (fixed size, padded)
299     1      Checksum     sum8 checksum
```

Total: 300 bytes.

---

## Checksum Algorithm

**Type:** `sum8`

```
checksum = 0
for i in range(0, 299):       # bytes 0 through 298
    checksum += frame[i]
checksum = checksum & 0xFF     # low byte only
frame[299] = checksum
```

- The checksum is the sum of bytes 0–298, truncated to 8 bits.
- Byte 299 is the checksum itself and is NOT included in the sum.
- Confirmed by SSE2 `paddb` (packed byte-add) instructions in `protocore.dll` at `0x180093300`.

---

## Frame Codes

| Code | Direction | Name | Description |
|---|---|---|---|
| `0x01` | BMS→Host | Configuration Read | 46 fields (293 bytes data) |
| `0x02` | BMS→Host | Runtime Data | 74 fields (293 bytes data) |
| `0x03` | BMS→Host | Device Info | 45 fields (293 bytes data) |
| `0x04` | Host→BMS | Configuration Write | 45 fields (293 bytes data) |
| `0x05` | BMS→Host | System Log | 4 fields (293 bytes data) |
| `0x06` | BMS→Host | Fault Info | 5 fields (293 bytes data) |

---

## Communication Flow

### Query (Host → BMS)

The host sends a 300-byte frame to request data:

```
55 AA EB 90 [frame_code] [counter] [293 bytes of 0x00] [sum8_checksum]
```

- `frame_code`: which data to request (0x01–0x06)
- `counter`: incrementing sequence number (wraps at 255)
- Data section: all zeros for read requests
- Checksum: sum8 of bytes 0–298

### Response (BMS → Host)

The BMS responds with a 300-byte frame containing the requested data:

```
55 AA EB 90 [frame_code] [counter] [293 bytes of data] [sum8_checksum]
```

### Config Write (Host → BMS)

The host sends frame code `0x04` with configuration values in the data section:

```
55 AA EB 90 04 [counter] [293 bytes of config data] [sum8_checksum]
```

The BMS responds with an ACK frame (likely frame code 0x04 with the written values echoed back).

### Timing

| Parameter | Value |
|---|---|
| Send interval | 20ms between queries |
| Query timeout | 350ms |
| Receive interval | 5ms polling |
| Device address code | 1 |
| Frame address offset | 0x1000 |

### Polling Sequence

The host cycles through frame codes:
1. Send query for 0x02 (runtime data)
2. Wait up to 350ms for response
3. Send query for 0x01 (config read)
4. Wait up to 350ms for response
5. Send query for 0x03 (device info)
6. Wait up to 350ms for response
7. Send query for 0x06 (fault info)
8. Repeat

---

## Data Types

All multi-byte values are **little-endian**.

| JSON type | Size | C type | Description |
|---|---|---|---|
| `nt: "u8"` | 1 byte | uint8_t | Unsigned 8-bit |
| `nt: "u16"` | 2 bytes | uint16_t | Unsigned 16-bit LE |
| `nt: "u32"` | 4 bytes | uint32_t | Unsigned 32-bit LE |
| `nt: "i8"` | 1 byte | int8_t | Signed 8-bit |
| `nt: "i16"` | 2 bytes | int16_t | Signed 16-bit LE |
| `nt: "i32"` | 4 bytes | int32_t | Signed 32-bit LE |
| `nt: "f32"` | 4 bytes | float | IEEE 754 LE |
| `t: "bv"` | 1 byte | uint8_t | Bit value (0/1 or enum) |
| `t: "bv"` with `c` | (c+7)//8 | - | Bit value with bit count |
| `t: "bm"` | (c+7)//8 | - | Bitmap with c bits |
| `t: "a"` with `c` and `at` | c * sizeof(at) | - | Array of c elements |
| `t: "a"` with `c` no `at` | c bytes | - | Byte array |
| `t: "cx"` | sum of inner | - | Complex (nested struct) |

### Scaling

Most numeric fields have a scale factor (`s`) in the JSON. The raw value is multiplied by the scale to get the physical value:

```
physical_value = raw_value × scale
```

Examples:
- `s: 0.001` → millivolt/milliamp resolution (raw in mV/mA)
- `s: 0.1` → deci-degree for temperatures
- No `s` → raw integer value (1:1)

### Display Decimals

The `dm` field specifies decimal places for display:
- `dm: 0` → integer display
- `dm: 1` → 1 decimal place
- `dm: 2` → 2 decimal places
- `dm: 3` → 3 decimal places

---

## Frame 0x01 — Configuration Read (Uplink, 293 bytes)

| Offset | Size | Field | Name | Type | Scale | Unit | Range |
|--------|------|-------|------|------|-------|------|-------|
| 0 | 4 | volSmartSleep | Vol. Smart Sleep | u32 | 0.001 | V | 1.2–4.4 |
| 4 | 4 | volCellUV | Cell UVP | u32 | 0.001 | V | 1.2–4.4 |
| 8 | 4 | volCellUVPR | Cell UVPR | u32 | 0.001 | V | 1.2–4.4 |
| 12 | 4 | volCellOV | Cell OVP | u32 | 0.001 | V | 1.2–4.4 |
| 16 | 4 | volCellOVPR | Cell OVPR | u32 | 0.001 | V | 1.2–4.4 |
| 20 | 4 | volBalanTrig | Balance Trig Volt | u32 | 0.001 | V | 0.003–1.0 |
| 24 | 4 | volSOCP100 | SOC-100% Volt | u32 | 0.001 | V | 1.2–4.4 |
| 28 | 4 | volSOCP0 | SOC-0% Volt | u32 | 0.001 | V | 1.2–4.4 |
| 32 | 4 | volCellRCV | Vol. Cell RCV | u32 | 0.001 | V | 1.2–4.4 |
| 36 | 4 | volCellRFV | Vol. Cell RFV | u32 | 0.001 | V | 1.2–4.4 |
| 40 | 4 | volSysPwrOff | Power Off Volt | u32 | 0.001 | V | 1.2–4.4 |
| 44 | 4 | timBatCOC | Cont. Charge Curr | u32 | 0.001 | A | 1–600 |
| 48 | 4 | timBatCOCPDly | Charge OCP Delay | u32 | 1 | s | 2–600 |
| 52 | 4 | timBatCOCPRDly | Charge OCPR Time | u32 | 1 | s | 2–600 |
| 56 | 4 | timBatDcOC | Cont. Discharge Curr | u32 | 0.001 | A | 1–1200 |
| 60 | 4 | timBatDcOCPDly | Discharge OCP Delay | u32 | 1 | s | 2–600 |
| 64 | 4 | timBatDcOCPRDly | Discharge OCPR Time | u32 | 1 | s | 2–600 |
| 68 | 4 | timBatSCPRDly | SCPR Time | u32 | 1 | s | 2–600 |
| 72 | 4 | curBalanMax | Max Balance Cur | u32 | 0.001 | A | 0.3–15.0 |
| 76 | 4 | tmpBatCOT | Charge OTP | i32 | 0.1 | ℃ | 30–80 |
| 80 | 4 | tmpBatCOTPR | Charge OTPR | i32 | 0.1 | ℃ | 30–80 |
| 84 | 4 | tmpBatDcOT | Discharge OTP | i32 | 0.1 | ℃ | 30–80 |
| 88 | 4 | tmpBatDcOTPR | Discharge OTPR | i32 | 0.1 | ℃ | 30–80 |
| 92 | 4 | tmpBatCUT | Charge UTP | i32 | 0.1 | ℃ | -45–20 |
| 96 | 4 | tmpBatCUTPR | Charge UTPR | i32 | 0.1 | ℃ | -45–20 |
| 100 | 4 | tmpMosOT | MOS OTP | i32 | 0.1 | ℃ | 50–110 |
| 104 | 4 | tmpMosOTPR | MOS OTPR | i32 | 0.1 | ℃ | 50–110 |
| 108 | 4 | cellCount | Cell Count | u32 | 1 | | 2–32 |
| 112 | 4 | batChargeEn | Charge Enabled | u32 | 1 | | 0–1 |
| 116 | 4 | batDischargeEn | Discharge Enabled | u32 | 1 | | 0–1 |
| 120 | 4 | balanEn | Balance Enabled | u32 | 1 | | 0–1 |
| 124 | 4 | capBatCell | Battery Capacity | u32 | 0.001 | Ah | 2–20000 |
| 128 | 4 | scpDelay | SCP Delay | u32 | 1 | μs | 0–1000000 |
| 132 | 4 | volStartBalan | Start Balance Volt | u32 | 0.001 | V | 1.2–4.25 |
| 136 | 128 | cellConWireRes | Con. Wire Res [32] | u32[32] | 0.001 | mΩ | 0–2000 |
| 264 | 4 | devAddr | Device Addr | u32 | 1 | | 0–65535 |
| 268 | 4 | dischrgPreChrgT | Dischrg Pre Chrg T | u32 | 1 | s | 0–300 |
| 272 | 4 | currentRange | Current Range | u32 | 0.001 | A | 100–2000 |
| 276 | 2 | switchStatus | Switch Status | bm16 | - | - | bitmap |
| 278 | 1 | tmpStartHeating | TMP Start Heating | u8 | 1 | ℃ | -40–100 |
| 279 | 1 | tmpStopHeating | TMP Stop Heating | u8 | 1 | ℃ | -40–100 |
| 280 | 1 | timeSmartSleep | Time Smart Sleep | u8 | 1 | h | 1–100 |
| 281 | 9 | enableFlags | Enable Flags | byte[9] | - | - | flags |
| 290 | 1 | tmpBatDCHUT | Discharge UTP | u8 | 1 | ℃ | -40–100 |
| 291 | 1 | tmpBatDCHUTPR | Discharge UTPR | u8 | 1 | ℃ | -40–100 |
| 292 | 1 | reserved | Reserved | u8 | - | - | - |
| **Total** | **293** | | | | | | |

### switchStatus bitmap (frame 0x01, 16 bits):

| Bit | Name | Values |
|-----|------|--------|
| 0 | Heating Status | OFF / ON |
| 1 | Disable Temp Sensor | OFF / ON |
| 2 | GPS Heartbeat | OFF / ON |
| 3 | Multiplexed Port | CAN / RS485 |
| 4 | Display Always On | OFF / ON |
| 5 | Special Charger | OFF / ON |
| 6 | Smart Sleep | OFF / ON |
| 7 | Disable PCL Module | OFF / ON |
| 8 | Timed Stored Data | OFF / ON |
| 9 | Charging Float Mode | OFF / ON |
| 10 | Button Trg. Emerg. | OFF / ON |
| 11 | DRY ARM Intermittent | OFF / ON |
| 12 | Discharge OCP Ⅱ | OFF / ON |
| 13 | Discharge OCP Ⅲ | OFF / ON |
| 14 | GPS Locked CHG | OFF / ON |
| 15 | GPS Locked DCH | OFF / ON |

---

## Frame 0x02 — Runtime Data (Uplink, 293 bytes)

| Offset | Size | Field | Name | Type | Scale | Unit |
|--------|------|-------|------|------|-------|------|
| 0 | 64 | cellVol | Cell Voltage [32] | u16[32] | 0.001 | V |
| 64 | 4 | cellStatus | Cell Status | bm32 | - | - |
| 68 | 2 | cellVolAve | Ave. Cell Volt | u16 | 0.001 | V |
| 70 | 2 | maxVoltDelta | Cell Volt. Diff | u16 | 0.001 | V |
| 72 | 1 | celMaxVol | Highest Cell No. | u8 | - | - |
| 73 | 1 | celMinVol | Lowest Cell No. | u8 | - | - |
| 74 | 64 | cellWireRes | Cell Wire Res [32] | u16[32] | 0.001 | Ω |
| 138 | 2 | tempMos | MOS Temp. | i16 | 0.1 | ℃ |
| 140 | 4 | cellWireResStat | Wire Res Status | bm32 | - | - |
| 144 | 4 | batVol | Battery Volt. | i32 | 0.001 | V |
| 148 | 4 | batWatt | Battery Power | u32 | 0.001 | W |
| 152 | 4 | batCurrent | Battery Current | i32 | 0.001 | A |
| 156 | 2 | batTemp1 | Battery Temp. 1 | i16 | 0.1 | ℃ |
| 158 | 2 | batTemp2 | Battery Temp. 2 | i16 | 0.1 | ℃ |
| 160 | 4 | sysAlarm | System Alarm | bm32 | - | - |
| 164 | 2 | equCurrent | Balance Curr. | i16 | 0.001 | A |
| 166 | 1 | equStatus | Balance Status | bv | - | - |
| 167 | 1 | socRelativeStateOfCharge | SOC % | u8 | 1 | % |
| 168 | 4 | socCapabilityRemain | Remain Capacity | u32 | 0.001 | Ah |
| 172 | 4 | socFullChargeCapacity | Battery Capacity | u32 | 0.001 | Ah |
| 176 | 4 | socCycleCount | Cycle Count | u32 | 1 | - |
| 180 | 4 | socCycleCapacity | Cycle Capacity | u32 | 0.001 | Ah |
| 184 | 1 | sOCSOH | SOH | u8 | 1 | % |
| 185 | 1 | preDischarge | Pre-Discharge | bv | - | - |
| 186 | 2 | userAlarm | User Alarm | u16 | 1 | - |
| 188 | 4 | runtime | Runtime | u32 | 1 | s |
| 192 | 1 | chargeStatus | Charge | bv | - | - |
| 193 | 1 | dischargeStatus | Discharge | bv | - | - |
| 194 | 2 | userAlarm2 | User Alarm 2 | bm16 | - | - |
| 196 | 2 | timeDcOCPR | Dch OCPR Time | u16 | 1 | s |
| 198 | 2 | timeDcSCPR | Dch SCPR Time | u16 | 1 | s |
| 200 | 2 | timeCOCPR | Chg OCPR Time | u16 | 1 | s |
| 202 | 2 | timeCSCPR | Chg SCPR Time | u16 | 1 | s |
| 204 | 2 | timeUVPR | UVPR Time | u16 | 1 | s |
| 206 | 2 | timeOVPR | OVPR Time | u16 | 1 | s |
| 208 | 1 | tempSensorAbsent | Sensor Absent | bm | - | - |
| 209 | 1 | heatingStatus | Heating Status | bv | - | - |
| 210 | 1 | reserved1 | Reserve 1 | u8 | - | - |
| 211 | 1 | reserved2 | Reserve 2 | u8 | - | - |
| 212 | 2 | timeEmerg | Time Emerg. | bv16 | - | - |
| 214 | 2 | dischrgCurCorrect | Dch Cur. Correct | u16 | 1 | - |
| 216 | 2 | volChargCur | Volt. Charg. Cur. | u16 | 0.001 | V |
| 218 | 2 | volDischargCur | Volt. Disch. Cur. | u16 | 0.001 | V |
| 220 | 4 | batVolCorrect | Bat. Volt. Correct | f32 | - | - |
| 224 | 2 | chargPWMDutyCyle | Charg. PWM | u16 | 1 | % |
| 226 | 2 | dischargPWMDutyCyle | Discharg. PWM | u16 | 1 | % |
| 228 | 2 | totalBatVol | Battery Volt. 2 | u16 | 0.01 | V |
| 230 | 2 | heatCurrent | Heat Current | i16 | 0.001 | A |
| 232 | 1 | reserved4 | Reserve 4 | u8 | - | - |
| 233 | 1 | aCCStatus | ACC Status | bv | - | - |
| 234 | 1 | specialChargerSta | Special Charger | bv | - | - |
| 235 | 1 | startupFlag | Startup Flag | u8 | 1 | - |
| 236 | 1 | volC | Vol C- | u8 | - | V |
| 237 | 1 | mCUID | MCUID | u8 | - | - |
| 238 | 1 | reserved7 | Reserve 7 | u8 | - | - |
| 239 | 1 | chargePlugged | Charg. Plugged | bv | - | - |
| 240 | 4 | sysRunTicks | Sys Run Ticks | u32 | 0.1 | s |
| 244 | 4 | pVDTrigTimestamps | PVD Trig. Time | u32 | 0.1 | s |
| 248 | 2 | batTemp3 | Battery Temp. 3 | i16 | 0.1 | ℃ |
| 250 | 2 | batTemp4 | Battery Temp. 4 | i16 | 0.1 | ℃ |
| 252 | 2 | batTemp5 | Battery Temp. 5 | i16 | 0.1 | ℃ |
| 254 | 2 | chrgCurCorrect | Chg Cur. Correct | u16 | 1 | - |
| 256 | 4 | rtcCounter | RTC Counter | u32 | 1 | - |
| 260 | 4 | detailLogsCount | Detail Logs Count | u32 | 1 | - |
| 264 | 4 | timeEnterSleep | Time Enter Sleep | u32 | 1 | s |
| 268 | 1 | pclModule | PCL Module | bv | - | - |
| 269 | 1 | batteryType | Cell Type | bv | - | LFP/Lion/LTO |
| 270 | 1 | reserved8 | Reserve 8 | u8 | - | - |
| 271 | 1 | reserved9 | Reserve 9 | u8 | - | - |
| 272 | 2 | chargeStatusTime | Charge Status Time | u16 | 1 | S |
| 274 | 1 | chargeStatus2 | Charge Status | bv | - | Bulk/Abs/Float |
| 275 | 1 | switchStatus | Switch Status | bm | - | - |
| 276 | 5 | reserved10 | Reserve 10 | byte[5] | - | - |
| 281 | 12 | enableFlags | Enable Flags | byte[12] | - | - |
| **Total** | **293** | | | | | |

### sysAlarm bitmap (frame 0x02, 32 bits):

| Bit | Name |
|-----|------|
| 0 | Balance wire resistance abnormal |
| 1 | MOS over temp protection |
| 2 | Cell count mismatch |
| 3 | Current sensor anomaly |
| 4 | Battery fully charged |
| 5 | Battery over voltage |
| 6 | Charge over current |
| 7 | Charge short circuit |
| 8 | Charge over temp |
| 9 | Charge under temp |
| 10 | CPUAUX anomaly |
| 11 | Cell under voltage |
| 12 | Battery under voltage |
| 13 | Discharge over current |
| 14 | Discharge short circuit |
| 15 | Discharge over temp |
| 16 | Charging MOS abnormal |
| 17 | Discharging MOS abnormal |
| 18 | GPS disconnected |
| 19 | Modify PWD in time |
| 20 | Discharge on failed |
| 21 | Battery over temp alarm |
| 22 | Temp sensor abnormal |
| 23 | PL module abnormal |
| 24 | SCP release failed |
| 25 | DCHOCP2 |
| 26 | DCHOCP3 |
| 27 | Alarm Dch UTP |
| 28 | GPS Remote Lock |

### batteryType values:
- 0 = LFP (LiFePO4)
- 1 = Lion (Li-ion)
- 2 = LTO (Li-Titanate)

### chargeStatus2 values:
- 0 = Bulk
- 1 = Absorption
- 2 = Float

---

## Frame 0x03 — Device Info (Uplink, 293 bytes)

| Offset | Size | Field | Name | Type |
|--------|------|-------|------|------|
| 0 | 15 | manuDeviceID | Vendor ID | i8[15] |
| 15 | 1 | maxCells | Max Cells | u8 |
| 16 | 8 | hardwareVersion | Hardware Ver | i8[8] |
| 24 | 8 | softwareVersion | Software Ver | i8[8] |
| 32 | 4 | oddRunTime | Total Time | u32 (seconds) |
| 36 | 4 | pwrOnTimes | Power-on Times | u32 |
| 40 | 16 | bluetoothName | BLE Name | i8[16] |
| 56 | 16 | bluetoothPwd | BLE PWD | i8[16] |
| 72 | 8 | manufactureDate | First On Date | i8[8] |
| 80 | 16 | deviceSN | Serial Number | i8[16] |
| 96 | 16 | userData | User Private Data | i8[16] |
| 112 | 16 | settingPassword | Setting Password | i8[16] |
| 128 | 16 | userData2 | User Data 2 | i8[16] |
| 144 | 32 | cmdSupportFlags | Cmd Support Flags | byte[32] |
| 176 | 2 | agencyId | Agency ID | u16 |
| 178 | 1 | uart1ProtoNo | UART1 Protocol No. | u8 |
| 179 | 1 | canProtoNo | CAN Protocol No. | u8 |
| 180 | 4 | uart1ProtoEnabled | UART1 Proto Enabled | byte[4] |
| 184 | 12 | hardwareOption | Hardware Option | i8[12] |
| 196 | 4 | canProtoEnabled | CAN Proto Enabled | byte[4] |
| 200 | 12 | reserved1 | Reserve 1 | i8[12] |
| 212 | 1 | uart2ProtoNo | UART2 Protocol No. | u8 |
| 213 | 4 | uart2ProtoEnabled | UART2 Proto Enabled | byte[4] |
| 217 | 11 | reserved2 | Reserve 2 | i8[11] |
| 228 | 1 | lcdBuzzerTrigger | LCD Buzzer Trigger | u8 |
| 229 | 1 | dry1Trigger | DRY 1 Trigger | u8 |
| 230 | 1 | dry2Trigger | DRY 2 Trigger | u8 |
| 231 | 1 | uartMPTLVer | UART MPT L Ver | u8 |
| 232 | 4 | lcdBuzzerTriggerVal | LCD Buzzer Trig Val | i32 |
| 236 | 4 | lcdBuzzerReleaseVal | LCD Buzzer Rel Val | i32 |
| 240 | 4 | dry1TriggerVal | DRY 1 Trig Val | i32 |
| 244 | 4 | dry1ReleaseVal | DRY 1 Rel Val | i32 |
| 248 | 4 | dry2TriggerVal | DRY 2 Trig Val | i32 |
| 252 | 4 | dry2ReleaseVal | DRY 2 Rel Val | i32 |
| 256 | 4 | dataStoredPeriod | Data Stored Period | u32 (seconds) |
| 260 | 1 | rcvTime | RCV Time | u8 (0.1H) |
| 261 | 1 | rfvTime | RFV Time | u8 (0.1H) |
| 262 | 1 | canMPTLVer | CAN MPT L Ver | u8 |
| 263 | 1 | emergencyTime | Emerg. Time | u8 (minutes) |
| 264 | 1 | uart3ProtoNo | UART3 Protocol No. | u8 |
| 265 | 7 | uart3ProtoEnabled | UART3 Proto Enabled | byte[7] |
| 272 | 1 | reBulkSOC | Re-Bulk SOC | u8 (%) |
| 273 | 8 | reserved | Reserve | byte[8] |
| 281 | 11 | enableFlags | Enable Flags | byte[11] |
| 292 | 1 | protocolVer | Protocol Version | u8 |
| **Total** | **293** | | | |

---

## Frame 0x04 — Configuration Write (Downlink, 293 bytes)

Same layout as Frame 0x01 but with the following differences:
- Frame code is `0x04`
- 45 fields instead of 46 (no reserved byte at end, replaced by 10-byte reserved array)
- Last field is `Reserved1` (byte[10]) instead of the individual reserved byte

Fields 0–43 are identical to Frame 0x01 fields 0–43.
Field 44 is `Reserved1` (byte[10]).

---

## Frame 0x05 — System Log (Uplink, 293 bytes)

| Offset | Size | Field | Name | Type |
|--------|------|-------|------|------|
| 0 | 4 | logCount | Log Count | u32 |
| 4 | 1 | check | Checksum | u8 |
| 5 | 250 | alarmLog | Alarm Log | byte[250] |
| 255 | 38 | _reserved1 | Reserve | byte[38] |
| **Total** | **293** | | | |

---

## Frame 0x06 — Fault Info (Uplink, 293 bytes)

| Offset | Size | Field | Name | Type |
|--------|------|-------|------|------|
| 0 | 2 | beginIndex | Start Index | u16 |
| 2 | 1 | count | Record Count | u8 |
| 3 | 24 | record1 | Record 1 | complex struct |
| 27 | 264 | record2_12 | Records 2–12 | byte[264] |
| 291 | 2 | reserved1 | Reserve | byte[2] |
| **Total** | **293** | | | |

### record1 structure (24 bytes):

| Offset | Size | Field | Name | Type | Scale | Unit |
|--------|------|-------|------|------|-------|------|
| 0 | 4 | rtcCount | RTC Timestamp | u32 | 1 | S (from 2020-01-01) |
| 4 | 1 | logCode | Log Code | u8 | - | - |
| 5 | 1 | switchSta | Switch Status | bm4 | - | - |
| 6 | 1 | maxVolCellNo | Max Volt Cell No. | u8 | - | - |
| 7 | 1 | minVolCellNo | Min Volt Cell No. | u8 | - | - |
| 8 | 2 | volCellMax | Max Cell Volt | u16 | 0.001 | V |
| 10 | 2 | volCellMin | Min Cell Volt | u16 | 0.001 | V |
| 12 | 2 | volBat | Battery Volt | u16 | 0.01 | V |
| 14 | 2 | curBat | Battery Curr | u16 | 0.1 | A |
| 16 | 2 | socCapRemain | Remain Capacity | u16 | 0.1 | AH |
| 18 | 2 | socFullChargeCap | Actual Capacity | u16 | 0.1 | AH |
| 20 | 1 | maxTemp | Max Temp | u8 | 1 | ℃ |
| 21 | 1 | minTemp | Min Temp | u8 | 1 | ℃ |
| 22 | 1 | tempMos | MOS Temp | u8 | 1 | ℃ |
| 23 | 1 | hearCurrent | Heating Curr | u8 | 0.1 | A |

---

## Query Frame Example

To request runtime data (frame 0x02):

```python
import struct

def build_query(frame_code, counter=0):
    frame = bytearray(300)
    
    # Header
    frame[0:4] = bytes([0x55, 0xAA, 0xEB, 0x90])
    
    # Frame code
    frame[4] = frame_code
    
    # Counter
    frame[5] = counter
    
    # Data section: all zeros (already initialized)
    
    # Checksum: sum8 of bytes 0-298
    checksum = sum(frame[0:299]) & 0xFF
    frame[299] = checksum
    
    return bytes(frame)

# Request runtime data
query = build_query(0x02, counter=0)
serial_port.write(query)

# Wait for response
response = serial_port.read(300)
```

### Parsing the response:

```python
def parse_runtime_data(frame):
    # Verify header
    assert frame[0:4] == bytes([0x55, 0xAA, 0xEB, 0x90])
    
    # Verify checksum
    assert sum(frame[0:299]) & 0xFF == frame[299]
    
    frame_code = frame[4]
    counter = frame[5]
    data = frame[6:299]
    
    # Parse cell voltages (32 x u16)
    cell_voltages = []
    for i in range(32):
        raw = struct.unpack_from('<H', data, i * 2)[0]
        cell_voltages.append(raw * 0.001)  # scale to V
    
    # Skip cellStatus bitmap (offset 64, 4 bytes)
    
    avg_cell_v = struct.unpack_from('<H', data, 68)[0] * 0.001
    volt_delta = struct.unpack_from('<H', data, 70)[0] * 0.001
    max_cell_no = data[72]
    min_cell_no = data[73]
    
    # Skip cellWireRes (offset 74, 64 bytes)
    
    mos_temp = struct.unpack_from('<h', data, 138)[0] * 0.1
    bat_voltage = struct.unpack_from('<i', data, 144)[0] * 0.001
    bat_power = struct.unpack_from('<I', data, 148)[0] * 0.001
    bat_current = struct.unpack_from('<i', data, 152)[0] * 0.001
    bat_temp1 = struct.unpack_from('<h', data, 156)[0] * 0.1
    bat_temp2 = struct.unpack_from('<h', data, 158)[0] * 0.1
    
    soc = data[167]
    
    return {
        'cell_voltages': cell_voltages,
        'avg_cell_v': avg_cell_v,
        'volt_delta': volt_delta,
        'max_cell': max_cell_no,
        'min_cell': min_cell_no,
        'mos_temp': mos_temp,
        'voltage': bat_voltage,
        'power': bat_power,
        'current': bat_current,
        'temp1': bat_temp1,
        'temp2': bat_temp2,
        'soc': soc,
    }
```

---

## Check Type Reference (from protocore.dll)

The DLL supports the following check types:

| Type | Description |
|------|-------------|
| sum8 | Byte-wise sum, result truncated to 8 bits |
| sum16 | Byte-wise sum, result truncated to 16 bits |
| sum32 | Byte-wise sum, result truncated to 32 bits |
| sum64 | Byte-wise sum, result truncated to 64 bits |
| xorsum8 | Byte-wise XOR, result truncated to 8 bits |
| xorsum16 | Byte-wise XOR, result truncated to 16 bits |
| xorsum32 | Byte-wise XOR, result truncated to 32 bits |
| xorsum64 | Byte-wise XOR, result truncated to 64 bits |
| crc8 | CRC-8 with configurable params |
| crc16 | CRC-16 with configurable params |
| crc32 | CRC-32 with configurable params |
| crc64 | CRC-64 with configurable params |

This protocol uses **sum8** only.

---

## Open Questions for Hardware Testing

1. **Write ACK format** — Does the BMS echo back frame 0x04 after a successful write?
2. **Counter behavior** — Does the BMS copy the host's counter or use its own?
3. **Error responses** — What happens on checksum failure or invalid frame code?
4. **Variable-length frames** — Are all frames really 300 bytes, or can shorter frames be sent for queries?
5. **Frame address offset** — How does `addrCode=1` and `frameAddrOffset=0x1000` affect the frame?
