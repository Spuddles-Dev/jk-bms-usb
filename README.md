# JK-BMS USB

An offline Android USB-OTG monitor for JK-B2A20S20P battery management systems, built with Kotlin and Jetpack Compose. This is a monitoring preview; physical BMS compatibility has not yet been validated.

## Features

- Live pack voltage, current, power, SOC, cell voltages, temperatures and alarms, with monotonic freshness and explicit connection state.
- USB permission handling, one serialized request path, bounded frame resynchronization, adapter-specific detach handling and conservative reconnection.
- Cell sorting and pinning, wiring flags, fault history and on-demand system logs.
- Local sessions, battery names, offline history and extrema-preserving charts for pack voltage, current, SOC, MOS temperature, cell delta and a selected cell.
- Room schema migration, changed-snapshot recording, fault deduplication within a session, a bounded recording queue and seven-day retention for every history table.
- Paged CSV/JSON exports with units, UTC time, session/device identity, per-cell values and distinct share files.
- Named configuration backups and snapshot selection/comparison, complete numeric validation, raw input errors and dirty-field conflict detection.
- User-enabled background USB monitoring with a persistent status notification, Stop action and deduplicated stale/disconnect/BMS-alarm alerts.
- Bounded protocol capture export and a clearly labelled, separate offline replay viewer. Device-info frames containing credentials are excluded from traces; stored credentials are blanked.

Configuration writes are disabled by `BuildConfig.CONFIG_WRITES_VERIFIED`. The implementation includes fresh-baseline checks, the archived write layout and separate readback comparison, but actual ACK/layout behaviour must be established with hardware captures before enabling writes. Numeric fields are editable for comparison; wire-resistance arrays and multiplex switches are preserved or restored from a matching backup.

Temperature sensor and wiring flags are exposed as raw diagnostics pending validation of their firmware semantics. Background monitoring requires an active USB connection and explicit opt-in; it does not restart after reboot or force-stop.

## Build and test

Use a complete JDK **21** (including `jlink`) and Android SDK platform **36**. Gradle 8.13 is pinned with its complete wrapper and distribution checksum; source/bytecode target Java 17. On Windows, Android Studio's bundled `jbr` is suitable. Set `JAVA_HOME` to that JDK and create ignored `JkBmsApp/local.properties` containing your `sdk.dir`.

From `JkBmsApp`:

```powershell
.\gradlew.bat test lintDebug assembleDebug assembleRelease --console=plain
.\gradlew.bat connectedDebugAndroidTest
```

On Linux/macOS use `./gradlew`. Instrumented tests require an emulator or device. The Windows/Linux CI workflow retains reports and APKs; a local run does not establish that CI has run remotely.

Debug APK: `JkBmsApp/app/build/outputs/apk/debug/app-debug.apk`. Gradle's minified release output is unsigned. On Windows, `scripts/Build-TesterRelease.ps1` creates a signed tester APK and ZIP using a dedicated local key kept outside Git. See the [tester release guide](docs/tester-release.md). Do not commit keystores or `local.properties`.

Tests cover protocol framing and encoding, session serialization/shutdown, storage failure, legacy Base64, version-one database migration, deduplication, retention, paged repeat exports, settings reconciliation, chart extrema/gaps and alert cooldowns. See [implementation status](docs/implementation-status-2026-10-09.md) for the exact verification results and remaining hardware gates.

## Connecting

Use an Android device with USB host/OTG support and a serial adapter supported by usb-serial-for-android (FTDI, CP210x, CH340/CH341, PL2303 or CDC/ACM). Driver support is not a validated phone/adapter/BMS compatibility matrix.

Connect adapter GND to BMS GND, TX to RX and RX to TX at the P5 UART connector, then attach it through USB-OTG. Scan, select the correct adapter/port, connect and grant Android's USB permission. Serial parameters are 115200 baud, 8N1. Confirm electrical levels and the connector pinout for your hardware before connecting.

The app disconnects when it leaves the foreground unless background monitoring was explicitly enabled. The notification's Stop action ends the session; disabling background monitoring while the app is open keeps foreground monitoring available.

## Architecture and protocol

`protocol/` and `model/` hold pure Kotlin parsing, encoding and validation. Injectable `usb/BmsTransport` sits below the session owner in `connection/`; repositories feed ViewModels and Compose screens. Room stores five telemetry/snapshot tables plus sessions and write audit. Background monitoring and diagnostics have separate components.

The archived protocol reference is [protocol-complete.md](protocol-complete.md). Frames are 300 bytes: four-byte magic header, code, counter, 293-byte payload and sum8 checksum. Multi-byte values are little-endian. Device/config are read on connection; runtime polling waits 250 ms between cycles and faults are requested every twenty cycles. Five consecutive runtime failures trigger a two-second recovery pause. These are initial intervals pending hardware measurements, not guaranteed sample rates.

The archived September audit is preserved at [development-plan-2026-09-10.md](docs/development-plan-2026-09-10.md). The next-stage plan is [development-plan-2026-10-09.md](docs/development-plan-2026-10-09.md). Hardware captures, controlled writes, screen-off USB behaviour, a device compatibility matrix and a long recording/export soak remain release requirements. Energy integration is deferred until current-sign and sampling semantics are validated.

## Contributing

Run unit tests, lint and both APK builds before submitting changes. Export Room schemas and add explicit migrations for database changes. Keep configuration writes gated while compatibility evidence is missing.

## License

Provided as-is for educational and personal use. This independent project is not affiliated with or endorsed by the BMS manufacturer.
