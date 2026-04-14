# AGENTS.md — JK-BMS Android App

## Project Summary

Custom Android app (Kotlin + Jetpack Compose) that communicates with a **JK-B2A20S20P BMS** over **USB-OTG serial** (UART, 115200 baud, 8N1). No Bluetooth. Full replacement for the official JK-BMS app — real-time monitoring, config read/write, fault viewing, data logging.

## Build & Run

```bash
# Prerequisites: Android SDK, JDK 17+, Android Studio
# Set JAVA_HOME to Android Studio's JBR or any JDK 17+

# First run — gradle wrapper will download Gradle 8.11.1 automatically
./gradlew assembleDebug

# APK output
app/build/outputs/apk/debug/app-debug.apk

# Install on device
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Gradle Properties Required (`gradle.properties`)
```
android.useAndroidX=true
kotlin.code.style=official
android.nonTransitiveRClass=true
android.suppressUnsupportedCompileSdk=36
org.gradle.jvmargs=-Xmx2g -XX:MaxMetaspaceSize=512m
```

### `local.properties` (machine-specific, NOT committed)
```
sdk.dir=<path-to-Android-SDK>
```

## Known Build Issue — Hilt + Kotlin 2.1 + kapt

**Current blocker:** kapt + Hilt 2.50 + Kotlin 2.1.0 fails with:
```
Provided Metadata instance has version 2.1.0, while maximum supported version is 2.0.0.
```

**Fix options (pick one):**
1. **Migrate from kapt to KSP** (recommended). Update `build.gradle.kts`:
   - Replace `kapt("com.google.dagger:hilt-compiler:2.50")` with `ksp("com.google.dagger:hilt-compiler:2.50")`
   - Add KSP plugin: `id("com.google.devtools.ksp")` version matching Kotlin
   - Room also needs KSP: `ksp("androidx.room:room-compiler:2.6.1")` instead of kapt
2. **Downgrade Kotlin to 1.9.x** — set `kotlin = "1.9.22"` in root `build.gradle.kts` and use matching compose compiler plugin
3. **Upgrade Hilt to 2.54+** which has better Kotlin 2.x support

Also: `kotlinOptions { jvmTarget = "17" }` in `app/build.gradle.kts` triggers a deprecation warning. Can migrate to `compilerOptions` block.

## Project Structure

```
JkBmsApp/
├── app/src/main/java/com/horse/jk_bms/
│   ├── JkBmsApp.kt              # Hilt Application class
│   ├── MainActivity.kt           # Single activity, Compose + NavHost
│   │
│   ├── protocol/                 # Wire protocol engine (COMPLETE)
│   │   ├── BmsConstants.kt       # Frame size, offsets, timing
│   │   ├── FrameCode.kt          # 6 frame codes enum
│   │   ├── Checksum.kt           # sum8 calculation/validation
│   │   ├── FrameEncoder.kt       # Build query + config write frames
│   │   ├── FrameDecoder.kt       # Validate header/checksum, extract RawFrame
│   │   ├── FieldDecoder.kt       # Read u8/u16/u32/i8/i16/i32/f32/arrays/bitmaps/strings
│   │   ├── FieldEncoder.kt       # Write typed values to byte arrays
│   │   ├── RuntimeDataParser.kt  # Parse frame 0x02 → BmsRuntimeData (74 fields)
│   │   ├── ConfigParser.kt       # Parse frame 0x01 → BmsConfig (46 fields)
│   │   ├── DeviceInfoParser.kt   # Parse frame 0x03 → BmsDeviceInfo (45 fields)
│   │   ├── FaultInfoParser.kt    # Parse frame 0x06 → BmsFaultInfo + FaultRecord
│   │   └── SystemLogParser.kt    # Parse frame 0x05 → BmsSystemLog
│   │
│   ├── model/                    # Data classes
│   │   ├── BmsRuntimeData.kt     # 74-field runtime model
│   │   ├── BmsConfig.kt          # 46-field config model
│   │   ├── BmsDeviceInfo.kt      # 45-field device info model
│   │   └── BmsFaultAndLog.kt     # FaultRecord, BmsFaultInfo, BmsSystemLog
│   │
│   ├── usb/                      # USB serial layer
│   │   └── UsbSerialManager.kt   # Enumerate/connect/send+receive via mik3y usb-serial-for-android
│   │
│   ├── connection/               # Connection + polling state machine
│   │   └── BmsConnection.kt      # Connect/disconnect, poll cycle, query/write config, StateFlows
│   │
│   ├── repository/
│   │   └── BmsRepository.kt      # Facade over BmsConnection for ViewModels
│   │
│   ├── viewmodel/
│   │   ├── ConnectionViewModel.kt
│   │   ├── DashboardViewModel.kt
│   │   ├── SettingsViewModel.kt
│   │   └── OtherViewModels.kt    # Cells, DeviceInfo, Faults, Logs VMs
│   │
│   └── ui/
│       ├── theme/
│       │   ├── Color.kt
│       │   └── Theme.kt          # JkBmsTheme (Material3 dynamic colors)
│       ├── navigation/
│       │   ├── Screen.kt         # Sealed class: 7 routes
│       │   └── AppNavHost.kt     # NavHost wiring
│       └── screen/
│           ├── connection/       # USB device picker + connect button
│           ├── dashboard/        # Main monitoring (voltage, current, SOC, temps, alarms)
│           ├── cells/            # Per-cell voltage bars + wire resistance
│           ├── settings/         # Config read-only display + write with confirmation dialog
│           ├── device/           # Device info (serial, versions, protocols)
│           ├── faults/           # Fault history records
│           └── logs/             # System log hex dump
```

## Protocol Reference

Full byte-level spec: `../protocol-complete.md` (parent directory)

Key facts:
- Every frame is exactly **300 bytes**: `55 AA EB 90` header (4B) + frame code (1B) + counter (1B) + data (293B) + sum8 checksum (1B)
- Host sends all-zeros query frame → BMS responds with data frame of same frame code
- Config write: frame code 0x04 with values in 293-byte data section, BMS echoes back
- All multi-byte values are **little-endian**
- Numeric fields have scale factors (0.001 for mV/mA, 0.1 for deci-degrees, etc.)
- Frame codes: 0x01=config read, 0x02=runtime, 0x03=device info, 0x04=config write, 0x05=sys log, 0x06=faults

## USB Serial Library

Uses **mik3y/usb-serial-for-android v3.8.1** via JitPack.
- Package: `com.hoho.android.usbserial.driver.*`
- Supports: FTDI, CP210x, CH34x, PL2303, CDC/ACM
- Enumerate: `UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)`
- Port API: `open(connection)`, `setParameters(baud, dataBits, stopBits, parity)`, `read(buf, timeout)`, `write(buf, timeout)`

## Dependency Injection

Uses **Hilt** with `@AndroidEntryPoint` on `MainActivity` and `@HiltViewModel` on all ViewModels.
`JkBmsApp.kt` is the `@HiltAndroidApp` Application class.

## What's Done

- ✅ Protocol engine fully implemented (encode/decode/parse all 6 frame types)
- ✅ USB serial manager
- ✅ Connection + polling state machine
- ✅ Repository
- ✅ All ViewModels
- ✅ All UI screens (Connection, Dashboard, Cells, Settings, Device Info, Faults, Logs)
- ✅ Navigation graph
- ✅ Theme (Material3 with dynamic colors)
- ✅ AndroidManifest with USB host + device filter

## What's NOT Done (prioritized)

1. **Fix kapt/Hilt build issue** (KSP migration or Kotlin downgrade) — blocking compilation
2. **Write unit tests** for protocol engine (FrameEncoder, FrameDecoder, all parsers with synthetic byte arrays)
3. **Implement Room database** for data logging (entities, DAO, database class — deps already in build.gradle.kts)
4. **Edit-capable settings screen** — currently read-only; add editable fields with validation
5. **Hardware testing** — UART adapter is in the mail, no real BMS testing yet
6. **Error recovery** — reconnection logic, stale data handling, USB detach events
7. **Data export** — CSV/JSON export of logged data

## Key Decisions

- `minSdk = 21` (broad compatibility)
- No Bluetooth — USB serial only
- Confirmation dialogs before any BMS write operation
- Read-back verification after config write
- Polling cycle: Runtime → Config → DeviceInfo → Faults, 100ms gap between queries
- Counter increments per frame sent, wraps at 255

## Open Questions (need hardware)

1. Does all-zeros query frame trigger a response? (inferred from DLL, never tested)
2. Config write ACK format — does BMS echo back frame 0x04?
3. How addrCode=1 / frameAddrOffset=0x1000 affects wire protocol
4. Error response behavior on bad checksum or invalid frame code
5. Whether 300-byte all-zeros query is correct, or if shorter frames work

## Analysis Documents (parent directory)

- `protocol-complete.md` — complete protocol spec (key reference)
- `apk-build-plan.md` — original 10-phase build plan
- `apk-investig.md` — official Android APK analysis
- `win-app-analysis.md` — Windows app analysis
- `protocol-decode.md` — original encryption/decryption discovery
- `en_US.json` / `zh_CN.json` — decrypted protocol field definitions from DLL
