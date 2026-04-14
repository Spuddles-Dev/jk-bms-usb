# JK-BMS USB

Android app for real-time monitoring and configuration of **JK-B2A20S20P Battery Management Systems** over USB-OTG serial. Built with Kotlin and Jetpack Compose.

A full replacement for the official JK-BMS Android app — no Bluetooth, no cloud, no vendor lock-in. Just a USB serial cable and your phone.

## Features

**Live Monitoring**
- Per-cell voltages (up to 32 cells) with visual bars and delta highlighting
- Pack voltage, current, power
- State of charge (SOC%), state of health (SOH%), cycle count
- MOS temperature + up to 5 battery temperature probes
- Capacity tracking: remaining, full, and cycle capacity in Ah
- Real-time status: charging, discharging, balancing, heating indicators
- Active alarm count at a glance

**Configuration**
- Read and write all 49 BMS parameters: voltage/current/temperature protections, delays, cell count, capacity, balance settings, switch enables
- Confirmation dialog before any write operation
- Automatic read-back verification after config changes

**Diagnostics**
- Device info: hardware/software version, serial number, manufacture date, BLE credentials, protocol versions
- Fault history with per-record details (voltage, current, temperatures, cell data)
- System log viewer with hex dump

**Hardware Support**
- Works with common USB-serial adapters: FTDI, CP210x, CH340/CH341, PL2303, CDC/ACM
- 115200 baud, 8N1 — matches the JK-BMS P5 connector UART

## Screenshots

| Connection | Dashboard | Cells |
|---|---|---|
| *USB device picker* | *Live telemetry overview* | *Per-cell voltage bars* |

| Settings | Device Info | Faults |
|---|---|---|
| *BMS configuration read/write* | *Hardware identification* | *Fault history records* |

> Screenshots to be added after hardware testing.

## Getting Started

### Prerequisites

- Android device with **USB-OTG** support and a USB-OTG adapter cable
- USB-serial adapter (FTDI, CP210x, CH340, PL2303, or CDC/ACM)
- JK-B2A20S20P BMS with P5 connector access
- Android Studio (for building from source), or download a pre-built APK from [Releases](../../releases)

### Building from Source

```bash
# Clone
git clone https://github.com/<user>/jk-bms-usb.git
cd jk-bms-usb/JkBmsApp

# Set your Android SDK path
echo "sdk.dir=/path/to/Android/Sdk" > local.properties

# Build and install
./gradlew installDebug
```

Requires JDK 17+ and Android SDK with compileSdk 36.

### Connecting

1. Connect the USB-serial adapter to the BMS P5 connector:
   - GND → GND
   - Adapter TX → BMS RX
   - Adapter RX → BMS TX
2. Plug the adapter into your phone via USB-OTG
3. Open the app and tap **Scan** to discover USB devices
4. Select your adapter and tap **Connect**

## Architecture

```
┌─────────────────────────────────────────┐
│  UI Layer (Jetpack Compose + Material3) │
│  7 screens: Connection, Dashboard,      │
│  Cells, Settings, DeviceInfo, Faults,   │
│  Logs                                   │
├─────────────────────────────────────────┤
│  ViewModel Layer (@HiltViewModel)       │
│  StateFlow for reactive state,          │
│  SharedFlow for one-shot events         │
├─────────────────────────────────────────┤
│  Repository Layer (BmsRepository)       │
│  Facade over connection for ViewModels  │
├─────────────────────────────────────────┤
│  Connection Layer (BmsConnection)       │
│  Poll cycle state machine, query/write  │
├──────────────┬──────────────────────────┤
│  Protocol    │  USB Serial              │
│  Frame       │  (mik3y                  │
│  encode/     │   usb-serial-for-        │
│  decode/     │   android v3.8.1)        │
│  parse       │                          │
└──────────────┴──────────────────────────┘
```

- **Dependency Injection**: Hilt with KSP
- **Navigation**: Single-activity, Navigation Compose with 7 routes
- **Async**: Kotlin coroutines — `Dispatchers.IO` for serial I/O, `Dispatchers.Main` for UI
- **Protocol**: Pure Kotlin, no Android dependencies — testable with synthetic byte arrays

## Protocol

The JK-BMS serial protocol was reverse-engineered from `protocore.dll` v4.13.1 and decrypted protocol definition files. The complete 606-line specification is in [`protocol-complete.md`](protocol-complete.md).

Key facts:
- Every frame is exactly **300 bytes**: `55 AA EB 90` header + frame code + counter + 293-byte data payload + sum8 checksum
- Query protocol: host sends all-zeros frame with desired frame code, BMS responds with data
- All multi-byte values are **little-endian** with numeric scale factors (0.001 for mV/mA, 0.1 for temperature)
- Frame codes: `0x01` config read, `0x02` runtime data, `0x03` device info, `0x04` config write, `0x05` system log, `0x06` faults
- Polling cycle: Runtime → Config → DeviceInfo → Faults, 100ms gap between queries

## Testing

```bash
cd JkBmsApp

# Run all unit tests
./gradlew test

# Run a single test class
./gradlew test --tests "com.horse.jk_bms.protocol.ChecksumTest"

# Run a specific test method
./gradlew test --tests "com.horse.jk_bms.protocol.ChecksumTest.testCalculateValidFrame"

# Run instrumented tests (requires device/emulator)
./gradlew connectedAndroidTest
```

Protocol layer tests use synthetic byte arrays — no mocking needed. Current coverage includes `Checksum`, `FrameEncoder`, `FrameDecoder`, and `FieldDecoder`. Parser tests (`RuntimeDataParser`, `ConfigParser`, `DeviceInfoParser`, `FaultInfoParser`) are planned.

## Tech Stack

| Component | Technology |
|---|---|
| Language | Kotlin 2.1 |
| UI | Jetpack Compose + Material3 |
| DI | Hilt 2.51 (KSP) |
| Navigation | Navigation Compose 2.8.5 |
| USB Serial | [usb-serial-for-android](https://github.com/mik3y/usb-serial-for-android) v3.8.1 |
| Database | Room 2.6.1 (planned, deps present) |
| Async | Kotlin Coroutines + Flow |
| Min SDK | 21 (Android 5.0) |
| Target SDK | 36 |

## Project Status

**Working build, pending hardware testing.**

- [x] Protocol engine (encode/decode/parse all 6 frame types)
- [x] USB serial layer with multi-chip adapter support
- [x] Connection and polling state machine
- [x] All 7 UI screens with live data binding
- [x] Configuration read/write with confirmation
- [x] Unit tests for protocol primitives
- [ ] Room database for data logging
- [ ] Editable settings screen (currently read-only)
- [ ] Hardware validation against real BMS
- [ ] Error recovery and USB detach handling
- [ ] CSV/JSON data export

## Contributing

Contributions welcome. Please run `./gradlew test` before submitting PRs.

## License

This project is provided as-is for educational and personal use. The JK-BMS protocol was reverse-engineered independently and is not affiliated with or endorsed by the BMS manufacturer.
