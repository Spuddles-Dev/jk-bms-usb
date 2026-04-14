# Windows App Analysis: jk-bms-monitor v3.4.0.34

## Summary

The Windows desktop application **JK-BMS-MONITOR** communicates with the BMS exclusively over **serial port (UART)**. It is a Qt5 C++ application with a structured protocol engine. There is no Bluetooth support in the desktop version.

---

## Installation Location

```
C:\JKTech\JK-BMS-MONITOR\3.4.0\jk-bms-monitor\
```

### Key Files

| File | Role |
|---|---|
| `bin\jk-bms-monitor.exe` | Main application |
| `bin\protocore.dll` | Core protocol engine (channels, frames, tables, parser) |
| `bin\protometa.dll` | Protocol metadata (frame/table definitions, serialization) |
| `bin\protowidget.dll` | Protocol UI widgets (serial/BLE/CAN/UDP settings panels) |
| `bin\jwt.dll` | Chart/visualization library (not JWT auth) |
| `config\main.json` | Active configuration (channel + device list + global settings) |
| `config\en_US.jsonds` | Encrypted protocol definition (English) |
| `config\zh_CN.jsonds` | Encrypted protocol definition (Chinese) |

### Architecture

- **Framework:** Qt 5 (Widgets-based, not QML)
- **Database:** SQLite via SOCI library — table name `JK_BXAXS_XP`
- **XML/JSON:** pugixml + RapidJSON
- **Serialization:** Custom `.jsonds` format (encrypted/compressed, not plain JSON)
- **Protocol format:** Custom binary — `.eproto` files with `EProtoParser`

---

## Channel Configuration

### From `config\main.json`

```json
{
  "channel": {
    "config": " --id=5913c96c62254e26b71e835d8176b9f8 --type=serial --name= --mark= --autoSend=1 --sendInterval=20 --recvInterval=5 --sendCount=-1 --portName=COM3 --baudRate=115200 --dataBits=8 --stopBits=1 --parit=N"
  },
  "global": {
    "addrCode": 1,
    "frameAddrOffset": "0x1000",
    "language": "Chinese (China)",
    "locale": "zh_CN",
    "parallelTimeout": 10000,
    "queryTimeout": 350,
    "simulate": false,
    "theme": "darkblue"
  }
}
```

### Serial Port Settings

| Parameter | Default Value | Description |
|---|---|---|
| `--type` | `serial` | Channel type |
| `--portName` | `COM3` | Serial port |
| `--baudRate` | `115200` | Baud rate |
| `--dataBits` | `8` | Data bits |
| `--stopBits` | `1` | Stop bits |
| `--parit` | `N` | Parity (None) |
| `--autoSend` | `1` | Auto-send enabled |
| `--sendInterval` | `20` | Send interval (ms) |
| `--recvInterval` | `5` | Receive interval (ms) |
| `--sendCount` | `-1` | Unlimited sends |

### Global Protocol Settings

| Parameter | Value | Description |
|---|---|---|
| `addrCode` | `1` | Device address code |
| `frameAddrOffset` | `0x1000` | Register offset for frame addressing |
| `queryTimeout` | `350` ms | Query response timeout |
| `parallelTimeout` | `10000` ms | Parallel operation timeout |
| `simulate` | `false` | Simulation mode flag |

---

## Supported Channel Types

From `protocore.dll` string analysis, the engine supports these channel types (abbreviations listed in the binary):

| Type Key | Abbreviation | Class | Description |
|---|---|---|---|
| `serial` | `s` | `SerialChannel` | USB-UART serial |
| `ble` | `b` | `BleChannel` | Bluetooth Low Energy |
| `udp` | `u` | `UdpChannel` | UDP network |
| `canbus` | `canbus` | `CanBusChannel` | CAN bus |
| `arinc429` | `arinc429` | `Arinc429Channel` | ARINC 429 (aviation) |
| `file` | `f` | `FileChannel` | File-based (log replay) |

The Windows app (`jk-bms-monitor.exe`) only instantiates `SerialChannel` and `UdpChannel`. The `BleChannel` class exists in `protocore.dll` but is only used by the mobile APK.

---

## Protocol Architecture

### Class Hierarchy

The protocol engine uses a structured frame/table system:

```
Channel (base)
├── SerialChannel    — serial port I/O
├── BleChannel       — BLE I/O
├── UdpChannel       — UDP I/O
├── CanBusChannel    — CAN bus I/O
├── FileChannel      — file I/O (replay)
└── Arinc429Channel  — ARINC 429 I/O

WorkerSend / WorkerRecv  — async send/receive threads
FrameCode               — frame command definition (read/write)
Frame                   — individual data frame
Table                   — data table with items/matches
EProtoParser            — protocol definition file parser
EProtoHeader            — protocol file header
EProtoSection           — protocol section
EProtoTable             — protocol table definition
EProtoMatcher           — field matcher pattern
EProtoMatch             — match result
```

### Frame/Command System

The app uses a **register-based command system** with frame codes:

- Frame addresses use offset `0x1000` (configurable via `frameAddrOffset`)
- Commands reference `frame/03/deviceSN`, `frame/03/settingPassword`, `frame/03/cmdSupportFlags` etc.
- The `03` likely refers to Modbus function code 03 (Read Holding Registers)
- `FrameCode` objects support: `cyclicSend`, `updateSend`, `updateRecv`, `createBuffer`, `dataFromBuffer`, `setDataToBuffer`
- Frame code types include: read, write, and variable-length frames

### Known Frame References (from exe strings)

| Frame Path | Description |
|---|---|
| `frame/03/deviceSN` | Device serial number |
| `frame/03/settingPassword` | Settings password |
| `frame/03/cmdSupportFlags` | Supported command flags |
| `frame/03/manuDeviceID` | Manufacturer device ID |
| `frame/03/softwareVersion` | Firmware version |

### Data Fields Read from BMS

| Field | Description |
|---|---|
| `deviceSN` | Device serial number |
| `softwareVersion` | Firmware version |
| `hardwareVersion` | Hardware version |
| `manuDeviceID` | Manufacturer device ID |
| `chargeStatus` | Charging status |
| `chargeStatus2` | Secondary charge status |
| `chargeStatusTime` | Charge status timestamp |
| `dischargeStatus` | Discharge status |
| `equStatus` | Equalization status |
| `heatingStatus` | Heating status |
| `aCCStatus` | CC status |
| `switchStatus` | Switch status |
| `cmdSupportFlags` | Supported commands bitmap |
| `settingsPwd` | Settings password |
| `emergencyTime` | Emergency mode timer |

---

## UI Workflow

### Connection Flow

1. App starts → shows `DisconnectedWindow`
2. User configures serial port via `SerialConfigDlg` (port name, baud rate, etc.)
3. App opens serial channel → status bar shows: `Comm. Status: Connected`
4. App auto-sends read frames at 20ms intervals
5. Status bar shows: `Device Id:`, `Port Name:`, version info

### Configuration Storage

- Config stored in Qt `QSettings` format
- Registry path: `JKTech/JK_BMS/JK-BXAXS-XP`
- Record files saved to: `C:/jk-bms-monitor/record` or `D:/jk-bms-monitor/record`
- Device definitions loaded from: `:/desktop/others/devices.json` (Qt resource)

### Settings Hierarchy

- **Basic Settings** — standard user parameters
- **Advance Settings** — advanced parameters
- **Config Params** — protocol-level configuration
- **Con. Wire Res. Settings** — connection wire resistance settings
- **One Key Settings** — quick preset
- **Factory setting LION** — lithium-ion defaults
- **Factory setting LFP** — LiFePO4 defaults
- **Factory setting LTO** — lithium titanate defaults

### Security

- Settings changes require password verification (`VerifySettingsDlg`)
- Password can be changed via `SettingsPwdDlg`
- Authorization settings via `VerifyTmpSettingsDlg`

---

## How to Interact with the BMS via UART

### Required Hardware

- USB to 3.3V UART adapter (FTDI / CP2102 / CH340)
- Wiring: GND→GND, TX→RX, RX→TX

### Serial Configuration

```
Port:      COM3 (or whatever your adapter assigns)
Baud:      115200
Data bits: 8
Stop bits: 1
Parity:    None
Flow ctrl: None
```

### Communication Protocol

Based on the analysis, the protocol uses:

- **Function code 03** (Read Holding Registers) for reading data
- Register offset `0x1000` as base address
- Auto-send at 20ms intervals, receive check at 5ms intervals
- Query timeout of 350ms
- Device address code `1`
- Frame structure likely follows Modbus-like format with custom extensions

### Manual Testing Approach

You can test raw communication using a serial terminal (e.g., PuTTY, RealTerm, or Python `pyserial`):

1. Set serial port to 115200 8N1
2. The app auto-sends frames — you may see periodic data from the BMS if it's in auto-respond mode
3. To query, construct Modbus-like read commands targeting register range starting at `0x1000`
4. The exact frame format is defined in the encrypted `.jsonds` files — without decrypting these, you'd need to sniff the traffic using a port monitor (e.g., Portmon, HDD Software Serial Monitor) while the official app is running

---

## Differences: Windows App vs Android APK

| Aspect | Windows App | Android APK |
|---|---|---|
| Framework | Qt5 Widgets (C++) | Qt6 QML (C++ native lib) |
| Connection | Serial / UART only | BLE only |
| Baud rate | 115200 | N/A (BLE) |
| Channel classes | `SerialChannel`, `UdpChannel` | `BleChannel` |
| Protocol engine | `protocore.dll` + `protometa.dll` | Embedded in `libenjpower_arm64-v8a.so` |
| Protocol definitions | `.jsonds` (encrypted) | `.rcc` bundle (Qt resources) |
| Protocol format | Same underlying protocol | Same underlying protocol |
| Settings UI | `SerialConfigDlg` | BLE scan + connect |

Both apps share the same protocol engine design (same class names: `Frame`, `FrameCode`, `Table`, `EProtoParser`, etc.) and the same `.jsonds` protocol definition files. The core difference is the **transport channel** — serial vs BLE.
