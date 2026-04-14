# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Android app (Kotlin + Jetpack Compose) that communicates with a **JK-B2A20S20P BMS** over **USB-OTG serial** (115200 baud, 8N1). No Bluetooth. Full replacement for the official JK-BMS app.

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
.\gradlew test --tests "com.horse.jk_bms.protocol.ChecksumTest"

# Single test method
.\gradlew test --tests "com.horse.jk_bms.protocol.ChecksumTest.testValidChecksum"

# Instrumented tests (requires device/emulator)
.\gradlew connectedAndroidTest

# Clean
.\gradlew clean
```

Requires `JkBmsApp/local.properties` with `sdk.dir=<Android-SDK-path>` (not committed).

APK outputs: `app/build/outputs/apk/debug/app-debug.apk`

## Architecture

Clean layered architecture — each layer only depends on layers below it:

```
USB hardware
    └── usb/UsbSerialManager          — mik3y usb-serial-for-android, enumerate/connect/read/write
    └── protocol/                     — pure Kotlin, no Android deps
         ├── FrameEncoder/Decoder     — 300-byte frame build/validate (header + code + counter + 293B data + sum8)
         ├── FieldDecoder/Encoder     — typed reads/writes (u8/u16/u32/i8/i16/i32/f32/arrays/bitmaps)
         └── *Parser                  — RuntimeDataParser, ConfigParser, DeviceInfoParser, FaultInfoParser
    └── model/                        — pure data classes (BmsRuntimeData 74 fields, BmsConfig 46, BmsDeviceInfo 45)
    └── connection/BmsConnection      — connect/disconnect, poll cycle, StateFlows, query+write
    └── repository/BmsRepository      — facade over BmsConnection for ViewModels
    └── viewmodel/                    — MVVM, @HiltViewModel, expose StateFlow
    └── ui/                           — Compose screens (connection, dashboard, cells, settings, device, faults, logs)
```

DI: Hilt with KSP (`@HiltAndroidApp` on `JkBmsApp.kt`, `@AndroidEntryPoint` on `MainActivity`, `@HiltViewModel` on all VMs).

Navigation: single `MainActivity` → `AppNavHost` → 7 `Screen` routes via `androidx.navigation:navigation-compose`.

## Protocol Facts

Full spec: `protocol-complete.md` (repo root).

- Every frame is exactly **300 bytes**: `55 AA EB 90` (4B magic) + frame code (1B) + counter (1B) + data (293B) + sum8 checksum (1B)
- Host sends all-zeros 300-byte query → BMS responds with same frame code
- Frame codes: `0x01`=config read, `0x02`=runtime, `0x03`=device info, `0x04`=config write, `0x05`=sys log, `0x06`=faults
- All multi-byte values are **little-endian** with scale factors (0.001 for mV/mA, 0.1 for deci-degrees)
- Polling cycle order: Runtime → Config → DeviceInfo → Faults, 100ms gap between queries
- Counter increments per sent frame, wraps at 255

## Current State

**Build is working** — migrated to KSP (was blocked on kapt + Kotlin 2.1 incompatibility).

**Done:** Protocol engine, USB layer, connection/polling state machine, repository, all ViewModels, all UI screens, navigation, theme, AndroidManifest.

**Not done (priority order):**
1. Unit tests for remaining parsers (`FieldDecoder`, `FieldEncoder`, `ConfigParser`, `RuntimeDataParser`, `DeviceInfoParser`, `FaultInfoParser`) — plan at `docs/superpowers/plans/2026-04-14-protocol-unit-tests.md`; `Checksum`, `FrameDecoder`, `FrameEncoder` tests already exist
2. Room database for data logging (deps already in `build.gradle.kts`)
3. Edit-capable settings screen (currently read-only)
4. Hardware testing (UART adapter not yet tested against real BMS)
5. Error recovery / reconnection logic / USB detach handling

## Code Conventions

- `Result<T>` for failable operations; `StateFlow<T>` for VM state; `SharedFlow<T>` for events
- Validate byte array length before any parsing; use `require()` for preconditions
- Apply scale factors immediately when reading fields (e.g., `* 0.001f // mV`)
- No wildcard imports; 4-space indent; 120-char soft line limit
- Test naming: `test<Feature><Scenario>` (e.g., `testInvalidChecksum`)
- Protocol tests use synthetic byte arrays — no mocking needed for pure parsing logic
