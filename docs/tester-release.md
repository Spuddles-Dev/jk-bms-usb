# JK-BMS USB 1.1.0-preview.1 — testing guide

This signed, minified release build is a monitoring preview for JK-B2A20S20P USB systems. Configuration writes are disabled. Compatibility with a physical BMS, USB adapter and phone has not yet been established.

## Install

Send the APK directly or the testing ZIP. Extract the ZIP before installing `jk-bms-usb-1.1.0-preview.1.apk`. On the phone, allow installation from the file/browser app used to open it, then install. Android 5.0/API 21 or newer and USB host/OTG support are required.

Updates must use the same release signing key. An existing debug build uses a different key: export any wanted history before uninstalling it to install this release. Uninstalling removes local data. Future releases signed with this tester key can update this APK normally.

## Test without a battery

Open **Browse session history → Diagnostics and replay → Replay a capture** and select the supplied `demo-runtime-capture.json`. It is synthetic test data. The final sample should show a **DEMO REPLAY** label, SOC 78%, approximately 52.952 V, −10 A and 16 cells. Replay does not communicate with USB hardware.

## Test your system

Record the phone model, Android version, adapter model/VID/PID and port, BMS model, hardware and firmware version. Use a correctly wired USB-OTG UART adapter and confirm the electrical levels and connector pinout for that hardware.

1. Scan for adapters, select the correct port, connect and grant USB permission. Note permission denial or failures and whether retry works.
2. Compare pack voltage, current/sign, SOC, cells and temperatures with the vendor display. Temperature/wiring flags are raw pending hardware verification; a zero temperature may be an absent sensor.
3. Open cells, faults, logs and device details. Try cell sorting/pinning and Logs refresh. Leave credentials masked when sharing screenshots.
4. Disconnect/reconnect the cable and check that the app reports lost/stale data and handles reconnects. Test rotation and navigation. Foreground-only mode disconnects when the app leaves the foreground.
5. Explicitly enable background monitoring for a connected session. Test screen-off behaviour, notifications and Stop. It does not resume after reboot, force-stop or process death.
6. Browse recorded sessions after disconnect, name the pack, select chart metrics/cells and export CSV/JSON twice. Check timestamps, units, gaps and cell counts. History is retained for seven days.
7. Save a named configuration backup and compare a selected snapshot. Editing and comparison are available; applying settings is intentionally unavailable.

For a problem, send the version, hardware details, exact steps, expected/actual values and a screenshot. Export a diagnostic capture shortly after the problem; it holds the latest 256 valid frames and excludes device-info frames containing credentials. Review telemetry/history exports before sharing them. A long recording test should also report elapsed time, gaps, battery/storage usage and export time.

## Building another signed test release on Windows

Set `JAVA_HOME` to a complete JDK 21 and configure Android SDK platform 36/build-tools 36.0.0. From the repository root:

```powershell
.\scripts\Build-TesterRelease.ps1
```

The script builds the minified release, signs and verifies it, and creates an APK and ZIP under ignored `dist/`. It retains a dedicated key in `%LOCALAPPDATA%\jk-bms-usb\signing`, with its password encrypted using Windows DPAPI for the current Windows user. Keep the signing directory backed up securely and retain access to that Windows user's DPAPI keys; do not send the key/password to testers or commit them. The script reuses this key for updates and refuses an incomplete key/password pair. `-SkipBuild` packages an already built release; only use it when the build matches the current committed source. Update the version/code in the app and packaging script together for a subsequent release.
