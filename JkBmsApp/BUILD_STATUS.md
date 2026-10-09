# Android build status — 9 October 2026

The complete Gradle 8.13 wrapper is restored, including its JAR, Windows/POSIX launchers, executable Git mode and pinned distribution SHA-256. The wrapper JAR checksum matches Gradle's published 8.13 checksum: `81a82aaea5abcc8ff68b3dfcb58b3c3c429378efd98e7433460610fecd7ae45f`.

Use a complete JDK 21 with `jlink` and Android SDK platform 36. Compilation targets Java 17. The obsolete daemon-download configuration is removed; `JAVA_HOME` selects the build JDK. Windows `local.properties` drive colons must be escaped, for example `sdk.dir=C\:/Users/you/AppData/Local/Android/Sdk`.

From this directory run:

```powershell
.\gradlew.bat test lintDebug assembleDebug assembleRelease assembleDebugAndroidTest --console=plain
.\gradlew.bat connectedDebugAndroidTest
```

The first command checks both debug/release unit variants, lint and minified release assembly. The second requires a connected device/emulator. Windows/Linux CI is configured but has not been run remotely in this task.

ProGuard rules, an instrumentation runner, API-21-compatible Base64, exported Room v1/v2 schemas and an explicit database migration are present. Configuration writes remain disabled pending capture-backed BMS compatibility tests.

See [implementation status](../docs/implementation-status-2026-10-09.md) for final validation results and outstanding hardware release evidence. Gradle assembles an unsigned minified release. The Windows [tester packaging script](../scripts/Build-TesterRelease.ps1) signs a separate distributable APK with a dedicated local key and creates a testing bundle; see the [testing guide](../docs/tester-release.md).
