# AGENTS.md — JK-BMS Android App

Android app (Kotlin + Jetpack Compose) communicating with a JK-B2A20S20P BMS over USB-OTG serial (115200 baud, 8N1). All source under `JkBmsApp/`. Run all commands from `JkBmsApp/`.

## Build & Test Commands

```bash
# Build debug APK
.\gradlew assembleDebug          # Windows
./gradlew assembleDebug          # Unix/macOS

# Install on connected device
.\gradlew installDebug

# Run all unit tests
.\gradlew test

# Run a single test class
.\gradlew test --tests "com.horse.jk_bms.protocol.ChecksumTest"

# Run a single test method
.\gradlew test --tests "com.horse.jk_bms.protocol.ChecksumTest.testCalculateValidFrame"

# Instrumented tests (requires device/emulator)
.\gradlew connectedAndroidTest

# Clean
.\gradlew clean
```

Requires `local.properties` with `sdk.dir=<Android-SDK-path>` (not committed).
APK output: `app/build/outputs/apk/debug/app-debug.apk`

There is no ktlint or detekt configured. There is no lint command. Verify code by running `.\gradlew test`.

## Project Structure

```
app/src/main/java/com/horse/jk_bms/
├── protocol/          # Wire protocol engine (pure Kotlin, no Android deps)
│   ├── BmsConstants.kt, FrameCode.kt, Checksum.kt
│   ├── FrameEncoder.kt, FrameDecoder.kt
│   ├── FieldEncoder.kt, FieldDecoder.kt
│   ├── ConfigFieldValidator.kt      # Per-field min/max rules for config writes
│   └── RuntimeDataParser, ConfigParser, DeviceInfoParser, FaultInfoParser, SystemLogParser
├── model/             # Data classes: BmsRuntimeData, BmsConfig, BmsDeviceInfo, BmsFaultAndLog
├── usb/               # UsbSerialManager, UsbEventReceiver (USB attach/detach)
├── connection/        # BmsConnection — poll cycle, StateFlows, query+write, circuit breaker, auto-log
├── data/
│   ├── local/         # Room database: JkBmsDatabase, entities, DAOs, TypeConverters
│   ├── repository/    # DataLogRepository — auto-logging + 7-day cleanup
│   └── export/        # CsvFormatter, JsonFormatter, DataExporter (+ FileProvider share)
├── repository/        # BmsRepository — facade over BmsConnection for ViewModels
├── di/                # Hilt modules: UsbModule, DatabaseModule
├── viewmodel/         # @HiltViewModel, expose StateFlow (7 VMs)
└── ui/                # Compose screens (7 screens)
    ├── theme/         # Color.kt, Theme.kt (Material3 dynamic colors)
    ├── navigation/    # Screen sealed class (7 routes), AppNavHost
    └── screen/        # Per-feature Compose screens
```

Tests: `app/src/test/java/com/horse/jk_bms/protocol/` (9 test files, ~150+ tests)

## Architecture

Clean layered architecture — each layer only depends on layers below:
`protocol/` + `usb/` → `connection/` → `repository/` → `viewmodel/` → `ui/`

- **DI**: Hilt with KSP. `@HiltAndroidApp` on `JkBmsApp.kt`, `@AndroidEntryPoint` on `MainActivity`, `@HiltViewModel` on all VMs.
- **Navigation**: Single `MainActivity` → `AppNavHost` → 7 `Screen` routes via navigation-compose.
- **State**: `StateFlow<T>` for VM state, `SharedFlow<T>` for one-shot events.
- **Async**: `Dispatchers.IO` for heavy operations, `Dispatchers.Main` for UI.
- **Database**: Room with 5 entities, auto-logs every poll response, 7-day cleanup every ~600 polls.
- **Export**: CSV + JSON formatters, FileProvider share intent.

## Code Style

### Naming
- **Classes/Interfaces**: PascalCase (`BmsRuntimeData`, `FrameDecoder`)
- **Functions/Variables**: camelCase (`decode()`, `frameCode`)
- **Constants**: `const val` in `object`, UPPER_SNAKE_CASE (`FRAME_SIZE`, `HEADER_MAGIC`)
- **Composables**: PascalCase (`DashboardScreen()`, `StatusRow()`)
- **Test methods**: `test<Feature><Scenario>` (`testValidFrame`, `testInvalidChecksum`)

### Formatting
- 4-space indent, no tabs
- 120-char soft line limit
- K&R braces (opening brace on same line)
- Single blank line between top-level declarations

### Imports
- **No wildcard imports** — always import specific types
- Remove unused imports

### Types & Patterns
- `Result<T>` for failable operations
- `StateFlow<T>` for reactive state in ViewModels/Connection layer
- `SharedFlow<T>` for events
- `data class` for all models, `sealed class` for limited hierarchies
- `object` for singletons and utilities (`Checksum`, `FieldDecoder`, `BmsConstants`)
- Prefer explicit types over inference when it aids readability

### Error Handling
- `Result<T>` for success/failure returns
- `require()` for precondition checks (`require(data.size >= 293)`)
- Return early on errors (`if (!valid) return null`)
- Graceful degradation — never crash on protocol errors
- Emit errors via `SharedFlow` in connection/VM layers
- Circuit breaker in polling: 5 consecutive failures → 2s pause → retry

### Comments
- Do not add comments unless explicitly asked
- Exception: scale factor inline comments (`* 0.001f // mV`) and protocol offset references

## Protocol Reference

Full spec: `../protocol-complete.md`

- Every frame is exactly **300 bytes**: `55 AA EB 90` header (4B) + frame code (1B) + counter (1B) + data (293B) + sum8 checksum (1B)
- Host sends all-zeros 300-byte query → BMS responds with data frame of same frame code
- Config write: frame code `0x04` with values in 293-byte data, BMS echoes back
- All multi-byte values are **little-endian** with scale factors (0.001 for mV/mA, 0.1 for deci-degrees)
- Frame codes: `0x01`=config read, `0x02`=runtime, `0x03`=device info, `0x04`=config write, `0x05`=sys log, `0x06`=faults
- Polling cycle: Runtime → Config → DeviceInfo → Faults, 100ms gap between queries
- Counter increments per sent frame, wraps at 255

### Protocol/Field Decoding Conventions
- Always validate byte array length before parsing
- Use `FieldDecoder` helpers: `readU8()`, `readU16()`, `readU32()`, `readI32()`, `readF32()`
- Apply scale factors immediately when reading (`* 0.001f // mV`)
- Parse all fields even if currently unused — return complete data class instances, never partial

## Testing Conventions

- Protocol tests use synthetic byte arrays — no mocking needed for pure parsing logic
- Use JUnit 4 (`org.junit.Test`), MockK, kotlinx-coroutines-test, Turbine
- Test location mirrors source: `app/src/test/java/com/horse/jk_bms/...`
- Test class naming: `<ClassUnderTest>Test` (`ChecksumTest`, `FrameDecoderTest`)
- 9 test files: ChecksumTest, FrameDecoderTest, FrameEncoderTest, FieldDecoderTest, FieldEncoderTest, ConfigParserTest, RuntimeDataParserTest, DeviceInfoParserTest, FaultInfoParserTest

## Dependencies (key)

- Kotlin 2.1.0, Compose BOM 2024.12.01, Material3
- Hilt 2.54 (KSP), Room 2.6.1 (KSP)
- mik3y/usb-serial-for-android v3.8.1 (JitPack)
- Navigation Compose 2.8.5
