# Implementation status — 9 October 2026

This records implementation of the October development plan. The original September audit remains unchanged. These changes have not been exercised against a physical BMS. A signed monitoring-preview APK can be packaged for external testing using the [tester release guide](tester-release.md).

## Delivered software

| Milestone | Implemented | Remaining exit evidence |
|---|---|---|
| M0 | Complete Gradle 8.13 wrapper/checksum, JDK 21 build documentation, API-21-compatible legacy Base64, ProGuard rules, instrumentation runner, exported Room schemas, Windows/Linux CI | Remote clean-checkout CI runs and device matrix |
| M1 | Injectable transport; serialized transactions; permission flow with bounded wait; joined polling shutdown; application-owned USB events; matching detach and conservative reconnect; bounded fragmented-frame recovery; monotonic live/stale/recovering status | Real permission/cable/adapter trials, timing/counter behaviour and comparison with the vendor display |
| M2 | Room v2 migration, device/session identity, changed snapshots, bounded recording actor with visible storage errors, within-session fault deduplication, retention for all history tables, working on-demand Logs, credentials omitted from stored snapshots, streaming CSV/JSON export | Fault identity across reconnect/counter reset; 72-hour export and memory measurements on the intended phone |
| M3 | Typed numeric schema and validation, read/write layout separation, exact untouched wire preservation and rounded edits, truthful raw drafts/conflicts, before/after review, versioned named configuration backups and snapshot selection, fresh-baseline/ACK/readback transaction and audit recording | Capture-backed ACK/layout verification and controlled real-device writes. Writes remain disabled |
| M4 | Offline session browser, persistent local battery names while retained history exists, bounded extrema charts, gaps, observed fault context, selected-cell trends, cell sorting/pinning and raw wiring diagnostics | Physical read-only validation, sensor/wiring semantics and phone accessibility checks |
| M5 | Explicit opt-in connected-device foreground service, status notification and Stop action, notification permission, stale/lost/BMS-bit alerts with transition deduplication and cooldown | Screen-off/background/termination trials with a USB adapter on supported OS versions |
| M6 | Regression suite, device compatibility tests, debug/minified APK builds, minified emulator launch/replay and hardware checklist | Production signing, physical matrix, long soak and minified behaviour on intended phones |

Capture export keeps at most 256 frames and excludes device-info frames containing passwords. The replay viewer imports bounded captures and is labelled as a demo; it cannot send USB requests or writes. It currently replays runtime samples at 250 ms intervals rather than reproducing original timing. Traces contain reconstructed validated receive frames, not arbitrary raw serial noise.

## Verification

The final local validation completed successfully on Windows with JDK 21:

```powershell
.\gradlew.bat test lintDebug assembleDebug assembleRelease connectedDebugAndroidTest --console=plain
```

| Check | Result |
|---|---|
| Debug unit tests | 310 passed; zero failures or errors |
| Release unit tests | 310 passed; zero failures or errors |
| Android 7.1.1 / API 25 instrumentation | Five passed; zero failures or errors |
| Debug lint | Zero errors; 21 dependency-version/target-SDK upgrade warnings |
| Debug APK | Built successfully, 18,061,487 bytes |
| Minified unsigned release APK | Built successfully, 1,693,575 bytes |
| Minified API 25 smoke check | Installed a separate copy signed with the local debug key; launch, offline navigation and file-picker capture replay passed, with no AndroidRuntime errors |
| Original September plan | SHA-256 unchanged: `BC37E57A0C1F2F4FA03232210A326CDE3CDC4C4369FBD10298EEE29AAA981D6C` |

The suite covers framing noise/corruption/fragmentation, exact scaling and write-tail bytes, complete validation, baseline conflicts, unrelated ACKs and readback mismatch, serialized session shutdown including cancellation while connecting, storage failure containment, legacy conversion, populated v1 migration, deduplication/retention, paged repeat exports, editor reconciliation, chart extrema/gaps and alert transitions. API-specific persistence tests run under Robolectric for API 21, 25 and 35. Instrumentation covers codec compatibility, an empty legacy database migration, app launch without USB, named configuration backup identity/round-trip and capture import/replay.

The minified replay displayed the synthetic fixture's final sample: SOC 78%, approximately 52.952 V, −10 A and 16 cells. Its screenshot is in `JkBmsApp/app/build/verification/release-replay.png`. Reports and APKs are under `JkBmsApp/app/build`; these are ignored build artifacts. The release output remains unsigned; the debug-key smoke copy is only a local verification artifact. Remote Windows/Linux CI, native API 21/modern device tests, physical USB/BMS tests and soak measurements have not been performed.

## Important limits

- `CONFIG_WRITES_VERIFIED=false` is deliberate. Enabling it is a code change that requires hardware evidence; the UI has no bypass. A timeout after sending a write has an uncertain outcome and is never automatically retried.
- Configuration switch bits and wire-resistance arrays are preserved or loaded from a matching backup; they do not have manual editors. Numeric fields are editable for comparison. Backup restore uses the same validation and transaction path. Capability bits remain read-only. The editor supports named snapshot selection, filtered by device/hardware/firmware identity.
- Fault keys combine snapshot index and complete event content inside each session. A reconnect starts another session; identical historic faults may appear again. Do not claim global deduplication until event-counter reset and wrap semantics are established.
- No ACK counter echo is assumed. Same-code late replies cannot be reliably attributed until hardware establishes correlation semantics. The transport accepts the first validated expected-code frame and discards unrelated complete frames while retaining partial bytes across reads.
- Temperature sensor and wiring status bits remain raw. Zero temperature is displayed rather than silently hidden; it may represent an absent sensor. Their polarity must be checked against real hardware before interpreting them or using temperature thresholds.
- Seven-day retention covers all database tables, including write audit; backups retain the latest 50 internal snapshots. Battery names survive reconnects while an identified session remains retained. A permanent profile store and energy summaries are separate follow-ups.
- Background operation requires a user-started live USB session. There is no boot, force-stop or process-death resumption. Stop ends the USB session; disabling background mode while foregrounded preserves foreground monitoring. OEM USB power policy remains unverified.
- History is paged and downsampled to at most 1,200 chart samples. Refresh reloads the selection; it does not stream an ever-growing live chart. Fault markers show observation time, separate from the BMS RTC count.
- Dependency/SDK upgrades are intentionally deferred until this pinned baseline is validated. Gradle's release output is unsigned; the Windows tester packaging script creates a separate signed APK using a dedicated local key. Store publication and production release qualification are not configured.

## Hardware release checklist

Record a row for every tested combination: phone model, Android API, adapter VID/PID/serial/port, BMS model, hardware and firmware, date, capture files and outcome.

1. Compare read-only runtime/config/device/fault/log values with the vendor display, including current sign, genuine zero and absent sensor values, cell indexing, alarm bits and wiring flags.
2. Exercise permission grant/denial, detach during permission, unrelated detach, cable removal during polling, two adapters, reconnect with/without accessible serial, manual disconnect, navigation, rotation and app backgrounding.
3. Measure response latency, fragmentation, late replies, counter behaviour and sustained polling rate before choosing final deadlines/cadence. Retain anonymized capture fixtures in the repository after reviewing their contents.
4. Establish ACK success/rejection and the frame-04 downstream tail for the exact firmware. Only then enable writes in a development build, choose a controlled reversible change, save a backup and verify both changed and untouched settings by independent readback.
5. Validate background notification/Stop, denied notification permission, screen off, USB power loss, app termination and intentional restart. Do not promise recovery after force-stop.
6. Record 72 hours, test storage exhaustion and retry, and export the dataset twice in both formats. Record elapsed time, peak memory, database growth, gaps and retention behaviour.
7. Install a separately signed minified build on the intended phones. Exercise backup serialization, database upgrade, capture import/replay and export/share in that build before declaring a release candidate.

The permission callback uses the authoritative `UsbManager.hasPermission()` result rather than relying on immutable PendingIntent fill-in extras. See Android's [PendingIntent reference](https://developer.android.com/reference/android/app/PendingIntent) and [USB permission implementation](https://android.googlesource.com/platform/frameworks/base/+/master/services/usb/java/com/android/server/usb/UsbUserPermissionManager.java).

A synthetic demo fixture is provided at `JkBmsApp/app/src/test/resources/fixtures/demo-runtime-capture.json`. It contains three runtime frames and is explicitly synthetic; it is not hardware evidence. Unit and device tests both replay this shared fixture.
