# jsonds Decryption & JK-BMS Protocol

## Decryption Method

The `.jsonds` files in the Windows app config directory are encrypted protocol definitions.

### Encryption Scheme

- **Algorithm:** AES-256-CBC
- **Key:** `2B10F23AC94C4910AE8BCCE19E4D485B` (32-byte ASCII string, used as AES-256 key)
- **IV:** All zeros (`00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00`)
- **File format:** `[4-byte LE uint32 decompressed size][zlib-compressed JSON][AES padding]`

### How the key was found

The key is returned by `FileParser::encryptKey()` in `protocore.dll`. It was extracted by loading the DLL via Python `ctypes` and calling the function directly — it returns a static C string that the application uses to encrypt/decrypt all `.jsonds` protocol definition files.

### Decryption script

```python
from Crypto.Cipher import AES
import zlib, struct

key = b'2B10F23AC94C4910AE8BCCE19E4D485B'

def decrypt_jsonds(filepath):
    data = open(filepath, 'rb').read()

    # Manual AES-256-CBC decryption (IV = 0)
    cipher = AES.new(key, AES.MODE_ECB)
    ecb_dec = cipher.decrypt(data)

    cbc_plain = bytearray(len(data))
    for i in range(0, len(data), 16):
        block = bytearray(ecb_dec[i:i+16])
        if i > 0:
            prev_ct = data[i-16:i]
            block = bytes(a ^ b for a, b in zip(block, prev_ct))
        cbc_plain[i:i+16] = block

    size = struct.unpack('<I', cbc_plain[:4])[0]
    result = zlib.decompress(bytes(cbc_plain[4:]))

    assert len(result) == size
    return result
```

---

## JK-BMS Serial Protocol (JK-BXAXS-XP)

### Physical Layer

| Parameter | Value |
|---|---|
| Interface | UART / TTL Serial (3.3V) |
| Baud rate | 115200 |
| Data bits | 8 |
| Stop bits | 1 |
| Parity | None |

### Frame Structure

Every frame sent to/from the BMS follows this layout:

```
Offset  Size    Field       Description
------  ----    -----       -----------
0       4       Header      Fixed magic: 55 AA EB 90
4       1       Frame Code  Command/response type (01-06)
5       1       Counter     Frame counter/sequence number
6       N       Frame Data  Payload (variable length, depends on frame code)
6+N     1       Checksum    Frame checksum
```

### Frame Codes

| Code | Direction | Name | Description |
|---|---|---|---|
| `01` | BMS→Host | Configuration Read | 43 parameters (all config values) |
| `02` | BMS→Host | Runtime Data | 52 parameters (live readings) |
| `03` | BMS→Host | Device Info | 25 parameters (hardware/firmware info) |
| `04` | Host→BMS | Configuration Write | 42 parameters (write config to BMS) |
| `05` | BMS→Host | System Log | Log entries |
| `06` | BMS→Host | Fault Info | Fault/error records |

### Frame 0x01 — Configuration Parameters (Read/Uplink)

| # | Field | Name | Type | Scale | Unit | Range |
|---|---|---|---|---|---|---|
| 1 | volSmartSleep | Vol. Smart Sleep | u32 | 0.001 | V | 1.2–4.4 |
| 2 | volCellUV | Cell UVP | u32 | 0.001 | V | 1.2–4.4 |
| 3 | volCellUVPR | Cell UVPR | u32 | 0.001 | V | 1.2–4.4 |
| 4 | volCellOV | Cell OVP | u32 | 0.001 | V | 1.2–4.4 |
| 5 | volCellOVPR | Cell OVPR | u32 | 0.001 | V | 1.2–4.4 |
| 6 | volBalanTrig | Balance Trig. Volt. | u32 | 0.001 | V | 0.003–1.0 |
| 7 | volSOCP100 | SOC-100% Volt. | u32 | 0.001 | V | 1.2–4.4 |
| 8 | volSOCP0 | SOC-0% Volt. | u32 | 0.001 | V | 1.2–4.4 |
| 9 | volCellRCV | Vol. Cell RCV | u32 | 0.001 | V | 1.2–4.4 |
| 10 | volCellRFV | Vol. Cell RFV | u32 | 0.001 | V | 1.2–4.4 |
| 11 | volSysPwrOff | Power Off Vol. | u32 | 0.001 | V | 1.2–4.4 |
| 12 | timBatCOC | Continued Charge Curr. | u32 | 0.001 | A | 1–600 |
| 13 | timBatCOCPDly | Charge OCP Delay | u32 | 1 | s | 2–600 |
| 14 | timBatCOCPRDly | Charge OCPR Time | u32 | 1 | s | 2–600 |
| 15 | timBatDcOC | Continued Discharge Curr. | u32 | 0.001 | A | 1–1200 |
| 16 | timBatDcOCPDly | Discharge OCP Delay | u32 | 1 | s | 2–600 |
| 17 | timBatDcOCPRDly | Discharge OCPR Time | u32 | 1 | s | 2–600 |
| 18 | timBatSCPRDly | SCPR Time | u32 | 1 | s | 2–600 |
| 19 | curBalanMax | Max Balance Cur. | u32 | 0.001 | A | 0.3–15.0 |
| 20 | tmpBatCOT | Charge OTP | i32 | 0.1 | ℃ | 30–80 |
| 21 | tmpBatCOTPR | Charge OTPR | i32 | 0.1 | ℃ | 30–80 |
| 22 | tmpBatDcOT | Discharge OTP | i32 | 0.1 | ℃ | 30–80 |
| 23 | tmpBatDcOTPR | Discharge OTPR | i32 | 0.1 | ℃ | 30–80 |
| 24 | tmpBatCUT | Charge UTP | i32 | 0.1 | ℃ | -45–20 |
| 25 | tmpBatCUTPR | Charge UTPR | i32 | 0.1 | ℃ | -45–20 |
| 26 | tmpMosOT | MOS OTP | i32 | 0.1 | ℃ | 50–110 |
| 27 | tmpMosOTPR | MOS OTPR | i32 | 0.1 | ℃ | 50–110 |
| 28 | cellCount | Cell Count | u32 | 1 | | 2–32 |
| 29 | batChargeEn | Charge Enabled | u32 | 1 | | 0–1 |
| 30 | batDischargeEn | Discharge Enabled | u32 | 1 | | 0–1 |
| 31 | balanEn | Balance Enabled | u32 | 1 | | 0–1 |
| 32 | capBatCell | Battery Capacity | u32 | 0.001 | Ah | 2–20000 |
| 33 | scpDelay | SCP Delay | u32 | 1 | μs | 0–1000000 |
| 34 | volStartBalan | Start Balance Volt. | u32 | 0.001 | V | 1.2–4.25 |
| 35 | devAddr | Device Addr. | u32 | 1 | | 0–65535 |
| 36 | dischrgPreChrgT | Dischrg. Pre. Chrg. T | u32 | 1 | s | 0–300 |
| 37 | currentRange | Current Range | u32 | 0.001 | A | 100–2000 |
| 38 | tmpStartHeating | TMP Start Heating | | 1 | ℃ | -40–100 |
| 39 | tmpStopHeating | TMP Stop Heating | | 1 | ℃ | -40–100 |
| 40 | timeSmartSleep | Time Smart Sleep | u8 | 1 | h | 1–100 |
| 41 | tmpBatDCHUT | Discharge UTP | | 1 | ℃ | -40–100 |
| 42 | tmpBatDCHUTPR | Discharge UTPR | | 1 | ℃ | -40–100 |
| 43 | (reserved) | Reserve | u8 | | | |

### Frame 0x02 — Runtime Data (Read/Uplink)

| # | Field | Name | Type | Scale | Unit |
|---|---|---|---|---|---|
| 1 | cellVolAve | Ave. Cell Volt. | u16 | 0.001 | V |
| 2 | maxVoltDelta | Cell Volt. Diff. | u16 | 0.001 | V |
| 3 | celMaxVol | Highest Cell No. | u8 | | |
| 4 | celMinVol | Lowest Cell No. | u8 | | |
| 5 | tempMos | MOS Temp. | i16 | 0.1 | ℃ |
| 6 | batVol | Battery Volt. | i32 | 0.001 | V |
| 7 | batWatt | Battery Power | u32 | 0.001 | W |
| 8 | batCurrent | Battery Current | i32 | 0.001 | A |
| 9 | batTemp1 | Battery Temp. 1 | i16 | 0.1 | ℃ |
| 10 | batTemp2 | Battery Temp. 2 | i16 | 0.1 | ℃ |
| 11 | equCurrent | Balance Curr. | i16 | 0.001 | A |
| 12 | socRelativeStateOfCharge | Remain Battery | u8 | | % |
| 13 | socCapabilityRemain | Remain Capacity | u32 | 0.001 | Ah |
| 14 | socFullChargeCapacity | Battery Capacity | u32 | 0.001 | Ah |
| 15 | socCycleCount | Cycle Count | u32 | | |
| 16 | socCycleCapacity | Cycle Capacity | u32 | 0.001 | Ah |
| 17 | sOCSOH | SOC SOH | u8 | | % |
| 18 | userAlarm | User Alarm | u16 | | |
| 19 | runtime | Runtime | u32 | | s |
| 20-25 | timeDcOCPR etc. | Protection Release Times | u16 | | s |
| 26-27 | (reserved) | Reserve 1/2 | u8 | | |
| 28 | dischrgCurCorrect | Discharg. Curr. Correct | u16 | | |
| 29 | volChargCur | Volt. Charg. Curr. | u16 | 0.001 | V |
| 30 | volDischargCur | Volt. Discharg. Curr. | u16 | 0.001 | V |
| 31 | batVolCorrect | Bat. Volt. Correct | f32 | | |
| 32 | chargPWMDutyCyle | Charg. PWM | u16 | | % |
| 33 | dischargPWMDutyCyle | Discharg. PWM | u16 | | % |
| 34 | totalBatVol | Battery Volt. 2 | u16 | 0.01 | V |
| 35 | heatCurrent | Heat Current | i16 | 0.001 | A |
| 36 | (reserved) | Reserve 4 | u8 | | |
| 37 | startupFlag | Startup Flag | u8 | | |
| 38 | volC | Vol C- | | | V |
| 39 | mCUID | MCUID | u8 | | |
| 40 | (reserved) | Reserve 7 | u8 | | |
| 41 | sysRunTicks | Sys Run Ticks | u32 | 0.1 | s |
| 42 | pVDTrigTimestamps | PVD Trig. Time | u32 | 0.1 | s |
| 43-45 | batTemp3-5 | Battery Temp. 3-5 | i16 | 0.1 | ℃ |
| 46 | chrgCurCorrect | Charg. Curr. Correct | u16 | | |
| 47 | rtcCounter | RTC Counter | u32 | | |
| 48 | detailLogsCount | Detail Logs Count | u32 | | |
| 49 | timeEnterSleep | Time Enter Sleep | u32 | | s |
| 50-51 | (reserved) | Reserve 8/9 | u8 | | |
| 52 | chargeStatusTime | Charge Status Time | u16 | | S |

### Frame 0x03 — Device Info (Read/Uplink)

| # | Field | Name | Type |
|---|---|---|---|
| 1 | maxCells | Max Cells | u8 |
| 2 | oddRunTime | Total Time | u32 |
| 3 | pwrOnTimes | Power-on Times | u32 |
| 4 | agencyId | Agency ID | u16 |
| 5 | uart1ProtoNo | UART1 Protocol No. | u8 |
| 6 | canProtoNo | CAN Protocol No. | u8 |
| 7 | uart2ProtoNo | UART2 Protocol No. | u8 |
| 8 | lcdBuzzerTrigger | LCD Buzzer Trigger | u8 |
| 9 | dry1Trigger | DRY 1 Trigger | u8 |
| 10 | dry2Trigger | DRY 2 Trigger | u8 |
| 11 | uartMPTLVer | UART MPT L Ver | u8 |
| 12 | lcdBuzzerTriggerVal | LCD Buzzer Trigger Val | i32 |
| 13 | lcdBuzzerReleaseVal | LCD Buzzer Release Val | i32 |
| 14 | dry1TriggerVal | DRY 1 Trigger Val | i32 |
| 15 | dry1ReleaseVal | DRY 1 Release Val | i32 |
| 16 | dry2TriggerVal | DRY 2 Trigger Val | i32 |
| 17 | dry2ReleaseVal | DRY 2 Release Val | i32 |
| 18 | dataStoredPeriod | Data Stored Period | u32 |
| 19 | rcvTime | RCV Time | u8 |
| 20 | rfvTime | RFV Time | u8 |
| 21 | canMPTLVer | CAN MPT L Ver | u8 |
| 22 | emergencyTime | Emerg. Time | u8 |
| 23 | uart3ProtoNo | UART3 Protocol No. | u8 |
| 24 | reBulkSOC | Re-Bulk SOC | u8 |
| 25 | protocolVer | Protocol Version | u8 |

### Frame 0x04 — Configuration Write (Downlink)

Same 42 parameters as Frame 0x01 (minus the reserved field), sent from host to BMS to write configuration.

---

## Communication Flow

1. Host sends a request frame: `55 AA EB 90 [frame_code] [counter] [data] [checksum]`
2. BMS responds with: `55 AA EB 90 [frame_code] [counter] [data] [checksum]`
3. App auto-sends read requests every 20ms
4. Query timeout: 350ms
5. Device address code: 1
6. Frame address offset: `0x1000`

### Cell Voltage Array

In addition to the fixed fields above, the BMS also transmits individual cell voltages as a sequential array after the main frame data. Each cell voltage is a `u16` with scale `0.001` (millivolts).
