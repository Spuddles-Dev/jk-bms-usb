# Custom JK-BMS Android App — Build Plan

## Overview

Build a native Android APK (Kotlin + Jetpack Compose) that communicates with the JK-B2A20S20P BMS over **USB-OTG serial** (no Bluetooth). The app will be a full replacement for the official app: real-time monitoring, configuration read/write, fault viewing, and data logging.

**Protocol source:** `protocol-decode.md` and `en_US.json` (fully decoded).
**Physical layer:** UART 115200 baud, 8N1, via USB-OTG + USB-to-UART adapter (FTDI/CP2102/CH340).

---

## Architecture

```
┌─────────────────────────────────────────────┐
│                  UI Layer                    │
│          Jetpack Compose (Material 3)        │
│  ┌─────────┬──────────┬──────────┬────────┐ │
│  │Dashboard│  Cells   │ Settings │  Logs  │ │
│  └────┬────┴────┬─────┴────┬─────┴───┬────┘ │
│       │         │          │         │       │
│  ┌────▼─────────▼──────────▼─────────▼────┐ │
│  │           ViewModel Layer               │ │
│  │     (StateFlow / lifecycle-aware)       │ │
│  └────────────────┬───────────────────────┘ │
│                   │                          │
│  ┌────────────────▼───────────────────────┐ │
│  │          Repository Layer               │ │
│  │  BmsRepository (protocol + DB bridge)   │ │
│  └───┬──────────────────────────┬─────────┘ │
│      │                          │           │
│  ┌───▼───────────┐  ┌──────────▼─────────┐ │
│  │ Protocol Layer │  │   Room Database     │ │
│  │ Frame encode/  │  │  (data logging)     │ │
│  │ decode engine  │  │                     │ │
│  └───┬───────────┘  └────────────────────┘ │
│      │                                       │
│  ┌───▼─────────────────────────────────────┐ │
│  │        USB Serial Transport              │ │
│  │  usb-serial-for-android (mik3y library)  │ │
│  └─────────────────────────────────────────┘ │
└─────────────────────────────────────────────┘
```

---

## Tech Stack

| Component | Choice | Reason |
|---|---|---|
| Language | Kotlin | Native Android, user preference |
| Min SDK | 21 (Android 5.0) | USB host support starts at API 12; 21 covers 99%+ devices |
| Target SDK | 34 (Android 14) | Current stable |
| UI | Jetpack Compose + Material 3 | Modern, declarative |
| Architecture | MVVM + Repository pattern | Standard Android architecture |
| USB Serial | `usb-serial-for-android` v3.x | De facto standard, supports FTDI/CP2102/CH340/PL2303 |
| Async | Kotlin Coroutines + Flow | Native async, fits serial streaming |
| DI | Hilt | Standard Android DI |
| Database | Room | Local data logging |
| Navigation | Compose Navigation | Fragment-free navigation |
| Build | Gradle Kotlin DSL | Standard |

---

## Dependencies

```kotlin
// Core
implementation("androidx.core:core-ktx:1.12.0")
implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")

// Compose
implementation(platform("androidx.compose:compose-bom:2024.01.00"))
implementation("androidx.compose.ui:ui")
implementation("androidx.compose.material3:material3")
implementation("androidx.compose.ui:ui-tooling-preview")

// USB Serial
implementation("com.github.mik3y:usb-serial-for-android:3.7.0")

// DI
implementation("com.google.dagger:hilt-android:2.50")
kapt("com.google.dagger:hilt-compiler:2.50")

// Room
implementation("androidx.room:room-runtime:2.6.1")
implementation("androidx.room:room-ktx:2.6.1")
kapt("androidx.room:room-compiler:2.6.1")

// Navigation
implementation("androidx.navigation:navigation-compose:2.7.6")
implementation("androidx.hilt:hilt-navigation-compose:1.1.0")

// Charts
implementation("com.patrykandpatrick.vico:compose-m3:1.13.1")

// Data
implementation("com.google.code.gson:gson:2.10.1")
```

---

## Project Structure

```
app/src/main/java/com/example/jkbms/
├── JkBmsApp.kt                    // Application class (@HiltAndroidApp)
├── MainActivity.kt                 // Single activity
│
├── usb/
│   ├── UsbSerialManager.kt         // USB device discovery, connection lifecycle
│   └── UsbPermissionReceiver.kt    // Handle USB attach/detach broadcasts
│
├── protocol/
│   ├── BmsFrame.kt                 // Frame data classes (header, code, counter, payload, checksum)
│   ├── FrameCode.kt                // Enum: CONFIG_READ, RUNTIME_DATA, DEVICE_INFO, CONFIG_WRITE, SYSLOG, FAULT
│   ├── FrameEncoder.kt             // Build outgoing frames (host → BMS)
│   ├── FrameDecoder.kt             // Parse incoming frames (BMS → host)
│   ├── FieldDecoder.kt             // Extract typed fields from frame payload (u8/u16/u32/i16/i32/f32 + scale)
│   ├── ConfigFields.kt             // Data classes for Frame 0x01 (43 params)
│   ├── RuntimeFields.kt            // Data classes for Frame 0x02 (52 params + cell array)
│   ├── DeviceInfoFields.kt         // Data classes for Frame 0x03 (25 params)
│   ├── ConfigWriteFields.kt        // Data classes for Frame 0x04 (42 params)
│   └── Checksum.kt                 // Frame checksum calculation
│
├── connection/
│   ├── BmsConnection.kt            // High-level connection state machine (CONNECTED, POLLING, DISCONNECTED)
│   ├── BmsPoller.kt                // Periodic query loop (20ms send interval, 350ms timeout)
│   └── BmsResponseDispatcher.kt    // Route decoded frames to the right Flow
│
├── model/
│   ├── BmsRuntimeData.kt           // UI-friendly runtime data model
│   ├── BmsConfig.kt                // UI-friendly config model
│   ├── BmsDeviceInfo.kt            // UI-friendly device info model
│   ├── CellVoltage.kt              // Cell number + voltage
│   └── BmsFault.kt                 // Fault info model
│
├── repository/
│   └── BmsRepository.kt            // Single source of truth, bridges protocol ↔ DB ↔ UI
│
├── data/local/
│   ├── BmsDatabase.kt              // Room database
│   ├── dao/
│   │   ├── RuntimeSnapshotDao.kt   // Log runtime data periodically
│   │   └── ConfigSnapshotDao.kt    // Log config changes
│   └── entity/
│       ├── RuntimeSnapshotEntity.kt
│       └── ConfigSnapshotEntity.kt
│
├── ui/
│   ├── navigation/
│   │   └── NavGraph.kt             // Compose navigation
│   ├── theme/
│   │   ├── Theme.kt
│   │   └── Color.kt
│   ├── screen/
│   │   ├── connection/
│   │   │   ├── ConnectionScreen.kt // USB device picker, connect/disconnect
│   │   │   └── ConnectionViewModel.kt
│   │   ├── dashboard/
│   │   │   ├── DashboardScreen.kt  // Main monitoring view (voltages, current, SOC, temps)
│   │   │   └── DashboardViewModel.kt
│   │   ├── cells/
│   │   │   ├── CellsScreen.kt      // Individual cell voltages bar chart
│   │   │   └── CellsViewModel.kt
│   │   ├── settings/
│   │   │   ├── SettingsScreen.kt   // Config read/write with validation
│   │   │   └── SettingsViewModel.kt
│   │   ├── device/
│   │   │   ├── DeviceInfoScreen.kt // Hardware/firmware info
│   │   │   └── DeviceInfoViewModel.kt
│   │   ├── faults/
│   │   │   ├── FaultsScreen.kt     // Active faults & history
│   │   │   └── FaultsViewModel.kt
│   │   └── logs/
│   │       ├── DataLogScreen.kt    // Historical data charts
│   │       └── DataLogViewModel.kt
│   └── component/
│       ├── VoltageGauge.kt         // Circular gauge for pack voltage
│       ├── CurrentIndicator.kt     // Charge/discharge current with direction
│       ├── SocBar.kt               // Battery SOC percentage bar
│       ├── TempCard.kt             // Temperature display card
│       ├── CellBarChart.kt         // Cell voltage bar chart
│       └── ConfigEditor.kt         // Reusable config parameter editor with validation
│
└── util/
    ├── ByteExtensions.kt           // ByteArray → typed reads (readU16LE, readU32LE, readF32LE, etc.)
    └── ValueFormatter.kt           // Scale + unit formatting
```

---

## Development Phases

### Phase 1 — Project Skeleton + USB Connection (Day 1-2)

**Goal:** App launches, detects USB serial adapters, can open/close a connection.

1. Create Android Studio project (Kotlin, Compose, Hilt)
2. Add `usb-serial-for-android` dependency
3. Add `android.hardware.usb.host` feature to manifest
4. Implement `UsbSerialManager`:
   - Enumerate connected USB serial devices
   - Request USB permission via system dialog
   - Open connection at 115200/8N1
   - Expose connection state as `StateFlow`
5. Build `ConnectionScreen`:
   - List detected USB serial devices
   - Connect/disconnect button
   - Show connection status

**Deliverable:** App sees the USB-UART adapter, opens it, shows raw byte counter.

---

### Phase 2 — Protocol Engine (Day 3-4)

**Goal:** Encode and decode all 6 frame types against the spec in `protocol-decode.md`.

1. Implement `BmsFrame`, `FrameCode`, `Checksum`
2. Implement `FrameDecoder`:
   - Sync on header magic `55 AA EB 90`
   - Extract frame code, counter, data, validate checksum
   - Handle partial reads / byte-by-byte streaming
3. Implement `FrameEncoder`:
   - Build query frames for 0x01, 0x02, 0x03, 0x05, 0x06
   - Build write frames for 0x04
4. Implement `FieldDecoder`:
   - Parse payload bytes → typed fields using scale factors from `en_US.json`
   - All types: u8, u16, u32, i16, i32, f32
5. Implement data classes: `ConfigFields`, `RuntimeFields`, `DeviceInfoFields`, `ConfigWriteFields`
6. Write unit tests for encoder/decoder using known byte sequences

**Deliverable:** Protocol layer compiles, unit tests pass with synthetic data.

---

### Phase 3 — Connection + Polling (Day 5-6)

**Goal:** Maintain a live connection to the BMS, poll data at regular intervals.

1. Implement `BmsConnection` state machine:
   - States: DISCONNECTED, CONNECTING, CONNECTED, POLLING, ERROR
2. Implement `BmsPoller`:
   - Send read queries in rotation (02 → 01 → 03 → 06)
   - 20ms inter-frame gap
   - 350ms timeout per query
   - Run on a dedicated coroutine + serial dispatch queue
3. Implement `BmsResponseDispatcher`:
   - Route decoded frames to typed `SharedFlow<RuntimeFields>`, `SharedFlow<ConfigFields>`, etc.
4. Wire `BmsRepository` to combine connection + poller + dispatcher
5. Test with the actual BMS hardware (UART adapter required)

**Deliverable:** App connects to BMS, continuously receives and decodes all frame types.

---

### Phase 4 — Dashboard Screen (Day 7-8)

**Goal:** Display real-time BMS data.

1. `DashboardViewModel`:
   - Collect runtime data Flow from repository
   - Transform to UI state (format values, compute derived fields)
2. `DashboardScreen`:
   - Pack voltage (large gauge)
   - Current (with charge/discharge indicator and direction)
   - Power (W)
   - SOC percentage bar
   - SOH percentage
   - Temperature cards (MOS, battery 1-5)
   - Cycle count
   - Balance current
   - Runtime counter
   - Alert/fault indicator (tap → FaultsScreen)
3. Auto-refresh via Flow collection
4. Handle connection loss gracefully

**Deliverable:** Live dashboard with all runtime data updating in real time.

---

### Phase 5 — Cell Voltages Screen (Day 9)

**Goal:** Show individual cell voltages.

1. `CellsViewModel`:
   - Extract cell voltage array from runtime data
   - Compute min, max, delta, average
2. `CellsScreen`:
   - Bar chart (Vico library) showing all cell voltages
   - Highlight highest and lowest cells
   - Show voltage delta
   - Color-code: green (normal), yellow (near threshold), red (violation)
   - Toggle between absolute voltage and deviation from average

**Deliverable:** Cell voltage visualization with bar chart.

---

### Phase 6 — Device Info Screen (Day 10)

**Goal:** Show hardware and firmware information.

1. `DeviceInfoViewModel`:
   - Collect device info from repository
2. `DeviceInfoScreen`:
   - Max cells supported
   - Total runtime
   - Power-on count
   - UART1/2/3 protocol numbers
   - CAN protocol number
   - Protocol version
   - All trigger/release values

**Deliverable:** Static info screen populated from Frame 0x03.

---

### Phase 7 — Configuration Read/Write Screen (Day 11-14)

**Goal:** Read and safely write BMS configuration parameters.

1. `SettingsViewModel`:
   - Load current config from repository (Frame 0x01)
   - Build editable form state with validation (min/max from `en_US.json`)
   - Write changes via Frame 0x04
   - Read-back verification after write
2. `SettingsScreen`:
   - Grouped parameters (voltage, current, temperature, balancing, system)
   - Each parameter shows: name, current value, unit, allowed range
   - Inline validation (red border when out of range)
   - "Apply" button with confirmation dialog
   - "Reset to defaults" option
   - Undo support (remember previous value)
3. Safety features:
   - Confirmation dialog before any write
   - Show warning for critical parameters (cell count, chemistry)
   - Log all config changes to Room DB
   - Read-back after write to verify

**Deliverable:** Full config editor with validation and safety guards.

---

### Phase 8 — Faults Screen (Day 15)

**Goal:** Display active and historical fault information.

1. `FaultsViewModel`:
   - Collect fault data from Frame 0x06
   - Parse `userAlarm` bitmask from runtime data
2. `FaultsScreen`:
   - Active alarms with severity indicators
   - Alarm bitmask breakdown (individual flags)
   - Fault history if available from syslog (Frame 0x05)

**Deliverable:** Fault display with alarm flag decoding.

---

### Phase 9 — Data Logging (Day 16-17)

**Goal:** Log data to local database and visualize history.

1. Room entities and DAOs for runtime snapshots
2. Periodic logger (configurable interval, e.g., every 60s)
3. `DataLogScreen`:
   - Time-series charts (pack voltage, current, SOC, temperatures)
   - Time range selector (1h, 6h, 24h, 7d, all)
   - Export to CSV
4. Config change audit log

**Deliverable:** Historical data charts and CSV export.

---

### Phase 10 — Polish + Release (Day 18-20)

1. App icon and splash screen
2. Dark mode support
3. Landscape layout optimization
4. USB device auto-reconnect on resume
5. Error handling and user-friendly error messages
6. Settings persistence (last connected device, polling interval, log settings)
7. Build release APK (signed)
8. Test on multiple screen sizes

**Deliverable:** Release-ready APK.

---

## Protocol Implementation Notes

### Frame Encoding (Host → BMS)

```
Query frame:
55 AA EB 90 [frame_code] [counter] [data] [checksum]

- Frame codes to query: 01, 02, 03, 05, 06
- Device address: 1
- Frame address offset: 0x1000
```

### Frame Decoding (BMS → Host)

```
Response frame:
55 AA EB 90 [frame_code] [counter] [N bytes data] [checksum]

- Sync on 4-byte header
- Read frame code (1 byte)
- Read counter (1 byte)
- Read data length (implicit from frame code or needs discovery)
- Validate checksum
- Parse fields according to frame code definition
```

### Cell Voltage Array

After the main Frame 0x02 payload, individual cell voltages follow as sequential `u16` values (scale 0.001 V). The count matches the `cellCount` from config.

### Data Types and Byte Order

All multi-byte values are **little-endian**.

| Type | Size | Read |
|---|---|---|
| u8 | 1 byte | direct |
| u16 | 2 bytes | readU16LE |
| u32 | 4 bytes | readU32LE |
| i16 | 2 bytes | readI16LE |
| i32 | 4 bytes | readI32LE |
| f32 | 4 bytes | readF32LE (IEEE 754) |

### Checksum Algorithm

To be confirmed during hardware testing. Likely one of:
- XOR of all bytes excluding header
- Sum of all bytes modulo 256
- CRC-8

The Windows app binary (`protocore.dll`) contains the implementation — can be extracted if needed before hardware arrives.

---

## Manifest Requirements

```xml
<manifest>
    <uses-feature android:name="android.hardware.usb.host" android:required="true" />
    <uses-permission android:name="android.permission.USB_PERMISSION" />
    
    <application
        android:name=".JkBmsApp"
        ...>
        
        <activity android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.hardware.usb.action.USB_DEVICE_ATTACHED" />
            </intent-filter>
            <meta-data
                android:name="android.hardware.usb.action.USB_DEVICE_ATTACHED"
                android:resource="@xml/device_filter" />
        </activity>
    </application>
</manifest>
```

`res/xml/device_filter.xml` — USB vendor/product IDs for known serial chips:
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- FTDI FT232 -->
    <usb-device vendor-id="1027" product-id="24577" />
    <!-- CP2102 -->
    <usb-device vendor-id="4292" product-id="60000" />
    <!-- CH340 -->
    <usb-device vendor-id="6790" product-id="21795" />
    <!-- CH341 -->
    <usb-device vendor-id="6790" product-id="21970" />
    <!-- PL2303 -->
    <usb-device vendor-id="1659" product-id="8963" />
</resources>
```

---

## Risk Mitigation

| Risk | Mitigation |
|---|---|
| Checksum algorithm unknown | Reverse from `protocore.dll` before hardware testing |
| Frame data length unknown per code | Probe from Windows app traffic; or test with hardware |
| Protocol differences between BMS firmware versions | Read `protocolVer` from Frame 0x03 and adapt |
| USB adapter not recognized by Android | Support all common chips (FTDI, CP2102, CH340, PL2303) |
| Accidental bad config write to BMS | Confirmation dialogs, read-back verification, change logging |
| BMS stops responding mid-session | Timeout + auto-reconnect + user notification |

---

## Hardware Prerequisites

- Android phone with USB-OTG support
- USB-OTG adapter (USB-C or micro-USB)
- USB-to-3.3V-UART adapter (FTDI/CP2102/CH340)
- JK-B2A20S20P BMS with P5 UART port accessible
- 3-wire connection: GND, TX→RX, RX→TX

---

## Open Questions (resolve during development)

1. **Checksum algorithm** — need to extract from `protocore.dll` or test with hardware
2. **Frame data length** — is it fixed per frame code, or included in the frame?
3. **Query frame format** — does the host send an empty data section for reads, or a specific query payload?
4. **Cell voltage count** — how does the host know how many u16 values follow Frame 0x02?
5. **Write response** — does the BMS ACK a config write, and what format?
