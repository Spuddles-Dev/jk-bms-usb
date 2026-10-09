# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Android app (Kotlin + Jetpack Compose) that communicates with a **JK-B2A20S20P BMS** over **USB-OTG serial** (115200 baud, 8N1). Offline monitoring preview. Hardware compatibility is not yet validated and configuration writes remain disabled.

All source lives under `JkBmsApp/`. Run all commands from `JkBmsApp/`.

## Build & Test

```bash
cd JkBmsApp

# Debug APK
.\gradlew assembleDebug        # Windows
./gradlew assembleDebug        # Unix/macOS

# Install on connected device
.\gradlew installDebug

# Unit tests (all)
.\gradlew test

# Single test class
.\gradlew testDebugUnitTest --tests "com.horse.jk_bms.protocol.ChecksumTest"

# Single test method
.\gradlew testDebugUnitTest --tests "com.horse.jk_bms.protocol.ChecksumTest.testValidChecksum"

# Instrumented tests (requires device/emulator)
.\gradlew connectedAndroidTest

# Clean
.\gradlew clean
```

Requires a complete JDK 21 (including jlink), SDK platform 36, and `JkBmsApp/local.properties` with `sdk.dir=<Android-SDK-path>` (not committed).

APK outputs: `app/build/outputs/apk/debug/app-debug.apk`

## Architecture

Clean layered architecture — each layer only depends on layers below it:

```
USB hardware
    └── usb/UsbSerialManager          — mik3y usb-serial-for-android, enumerate/connect/read/write
    └── usb/UsbEventReceiver          — BroadcastReceiver for USB attach/detach
    └── protocol/                     — pure Kotlin, no Android deps
         ├── FrameEncoder/Decoder     — 300-byte frame build/validate (header + code + counter + 293B data + sum8)
         ├── FieldDecoder/Encoder     — typed reads/writes (u8/u16/u32/i8/i16/i32/f32/arrays/bitmaps)
         ├── ConfigFieldValidator     — per-field min/max validation rules for config writes
         └── *Parser                  — RuntimeDataParser, ConfigParser, DeviceInfoParser, FaultInfoParser
    └── model/                        — pure data classes (BmsRuntimeData, BmsConfig, BmsDeviceInfo)
    └── data/
         ├── local/                   — Room database, entities, DAOs, TypeConverters
         ├── repository/DataLogRepository — auto-logging to Room on each poll response
         └── export/                  — CsvFormatter, JsonFormatter, DataExporter
    └── connection/BmsConnection      — connect/disconnect, poll cycle, StateFlows, query+write, circuit breaker, auto-log
    └── repository/BmsRepository      — facade over BmsConnection for ViewModels
    └── viewmodel/                    — MVVM, @HiltViewModel, expose StateFlow
    └── ui/                           — Compose screens (connection, dashboard, cells, settings, device, faults, logs)
```

DI: Hilt with KSP (`@HiltAndroidApp` on `JkBmsApp.kt`, `@AndroidEntryPoint` on `MainActivity`, `@HiltViewModel` on all VMs).

Navigation: single `MainActivity` → `AppNavHost` → nine application routes via `androidx.navigation:navigation-compose`.

## Protocol Facts

Full spec: `protocol-complete.md` (repo root).

- Every frame is exactly **300 bytes**: `55 AA EB 90` (4B magic) + frame code (1B) + counter (1B) + data (293B) + sum8 checksum (1B)
- Host sends all-zeros 300-byte query → BMS responds with same frame code
- Frame codes: `0x01`=config read, `0x02`=runtime, `0x03`=device info, `0x04`=config write, `0x05`=sys log, `0x06`=faults
- All multi-byte values are **little-endian** with scale factors (0.001 for mV/mA, 0.1 for deci-degrees)
- Device/config on connect, runtime polling with 250ms delay, faults every 20 cycles, logs on demand. Every exchange shares one mutex.
- Counter increments per sent frame, wraps at 255

## Current State

See `docs/implementation-status-2026-10-09.md` for verification and remaining work. This is a monitoring preview with configuration writes gated by `CONFIG_WRITES_VERIFIED=false` until capture-backed hardware validation.

The complete Gradle 8.13 wrapper and ProGuard rules are present. Run `test lintDebug assembleDebug assembleRelease`, and `connectedDebugAndroidTest` on a device/emulator. Windows/Linux CI is configured; do not claim a remote CI result from a local run.

The app includes serialized USB sessions, permission handling, bounded framing/logging, Room v2 migration and session identity, paged exports, configuration validation/backups/diffs, offline history, cell sorting/pinning, optional connected-device foreground monitoring and bounded capture/replay. Credentials are excluded from routine stored snapshots and traces. Fault event identity across reconnects and raw sensor/wiring bit semantics still require hardware evidence. Energy integration is deferred.

## Code Conventions

- `Result<T>` for failable operations; `StateFlow<T>` for VM state; `SharedFlow<T>` for events
- Validate byte array length before any parsing; use `require()` for preconditions
- Apply scale factors immediately when reading fields (e.g., `* 0.001f // mV`)
- No wildcard imports; 4-space indent; 120-char soft line limit
- Test naming: `test<Feature><Scenario>` (e.g., `testInvalidChecksum`)
- Protocol tests use synthetic byte arrays — no mocking needed for pure parsing logic
