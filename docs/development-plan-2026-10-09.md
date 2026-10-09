# JK-BMS next stage development plan

Reviewed 9 October 2026 against commit `f7fb5e6` on local `main`.

Implementation follow-up: see [implementation status](implementation-status-2026-10-09.md) for the changes and current validation. The audit below records the checkout before implementation.

The September development plan has not been implemented in this checkout. The next stage should turn the existing application into a dependable offline USB monitor, establish verified configuration writes, and then add history and diagnostics that help explain what the battery is doing.

My working assumption is that you want a practical tool for your own JK-B2A20S20P: connect reliably, understand cell behaviour over time, retain useful records, and change settings with confidence. That follows the project's existing USB, local storage, and vendor-independent direction; it is an inference about your priorities, not a previously stated requirement.

**What was implemented and what was not**

The application already contains seven Compose screens, protocol encoders and parsers, USB serial access, Room logging, CSV/JSON exporters, settings editing, and initial recovery logic. There are 267 `@Test` annotations across nine protocol test files. Those are real foundations, but their presence does not establish complete end-to-end behaviour.

The [uncommitted September plan](C:/Users/Horse/Documents/GitHub/jk-bms-usb/docs/development-plan-2026-09-10.md) reviewed this same commit. At the start of this review, it was the only untracked file; tracked files had no changes. The only local branch and the locally cached `origin/main` reference both point to `f7fb5e6`. No remote fetch or review of other machines was performed. Accordingly, **none of the September remediation work is evidenced here, and all 16 findings remain open**.

The September document remains unchanged as the detailed audit. This plan supplies the current status, revised delivery order, and proposed product backlog.

| Finding | Current status and evidence | Delivery |
|---|---|---|
| F01 Build reproducibility | Open; build fails on the missing wrapper JAR. The JAR is ignored, the POSIX launcher is mode 100644, and no distribution checksum is configured. | M0 |
| F02 Configuration write layout | Open; [encoder](C:/Users/Horse/Documents/GitHub/jk-bms-usb/JkBmsApp/app/src/main/java/com/horse/jk_bms/protocol/FrameEncoder.kt:72) still writes capability bytes before the two discharge temperature fields. Both archived frame-04 schemas put those temperatures immediately after smart-sleep time, followed by reserved bytes. Actual firmware behaviour still needs captures. | M3 |
| F03 Incomplete validation | Open; [validateAll](C:/Users/Horse/Documents/GitHub/jk-bms-usb/JkBmsApp/app/src/main/java/com/horse/jk_bms/protocol/ConfigFieldValidator.kt:73) covers 19 values; the connection write method has no validation boundary. | M3 |
| F04 Numeric truncation | Open; [scaled encoding](C:/Users/Horse/Documents/GitHub/jk-bms-usb/JkBmsApp/app/src/main/java/com/horse/jk_bms/protocol/FrameEncoder.kt:26) still divides floats then truncates. Exact preservation of untouched wire values is not guaranteed. | M3 |
| F05 False write success | Open; [writeConfig](C:/Users/Horse/Documents/GitHub/jk-bms-usb/JkBmsApp/app/src/main/java/com/horse/jk_bms/connection/BmsConnection.kt:201) accepts a decoded response without checking its frame type or performing a separate readback comparison. | M3 |
| F06 Concurrent serial exchanges | Open; polling cancellation does not join, and no common transaction queue or mutex owns all requests. See [stopPolling](C:/Users/Horse/Documents/GitHub/jk-bms-usb/JkBmsApp/app/src/main/java/com/horse/jk_bms/connection/BmsConnection.kt:130). | M1 |
| F07 USB permissions | Open; [connect](C:/Users/Horse/Documents/GitHub/jk-bms-usb/JkBmsApp/app/src/main/java/com/horse/jk_bms/usb/UsbSerialManager.kt:50) calls openDevice directly without a permission request flow. | M1 |
| F08 Session lifetime | Open; Disconnect only navigates in [AppNavHost](C:/Users/Horse/Documents/GitHub/jk-bms-usb/JkBmsApp/app/src/main/java/com/horse/jk_bms/ui/navigation/AppNavHost.kt). The USB receiver belongs to the connection viewmodel, which is removed after successful navigation; detach handling only refreshes devices. | M1 |
| F09 Receive recovery | Open; [transport](C:/Users/Horse/Documents/GitHub/jk-bms-usb/JkBmsApp/app/src/main/java/com/horse/jk_bms/usb/UsbSerialManager.kt:91) uses a new fixed-size receive buffer per exchange and discards partial reads on timeout. | M1 |
| F10 Settings draft accuracy | Open; [draft state](C:/Users/Horse/Documents/GitHub/jk-bms-usb/JkBmsApp/app/src/main/java/com/horse/jk_bms/viewmodel/SettingsViewModel.kt:22), identity equality, and [input parsing](C:/Users/Horse/Documents/GitHub/jk-bms-usb/JkBmsApp/app/src/main/java/com/horse/jk_bms/ui/screen/settings/SettingsScreen.kt:254) can leave the submitted values different from visible input or fresh device values. | M3 |
| F11 Stale indication | Open; [dashboard](C:/Users/Horse/Documents/GitHub/jk-bms-usb/JkBmsApp/app/src/main/java/com/horse/jk_bms/ui/screen/dashboard/DashboardScreen.kt:37) updates a timer state that composition never reads. | M1 |
| F12 Duplicate and unbounded logging | Open; [logging repository](C:/Users/Horse/Documents/GitHub/jk-bms-usb/JkBmsApp/app/src/main/java/com/horse/jk_bms/data/repository/DataLogRepository.kt) inserts every fault/config/device snapshot; cleanup covers only runtime and system logs. | M2 |
| F13 Failure containment | Open; partial USB setup lacks reliable cleanup, and background database jobs have no error handling. | M1 and M2 |
| F14 Export flow and CSV | Open; [dialog](C:/Users/Horse/Documents/GitHub/jk-bms-usb/JkBmsApp/app/src/main/java/com/horse/jk_bms/ui/screen/dashboard/ExportDialog.kt:26) dismisses on retained completion/error state; reset has no caller. [CSV](C:/Users/Horse/Documents/GitHub/jk-bms-usb/JkBmsApp/app/src/main/java/com/horse/jk_bms/data/export/CsvFormatter.kt:14) labels the highest-cell index as active_cells. | M2 |
| F15 Logs and diagnostics | Open; [LogsViewModel](C:/Users/Horse/Documents/GitHub/jk-bms-usb/JkBmsApp/app/src/main/java/com/horse/jk_bms/viewmodel/OtherViewModels.kt:35) never requests logs. Runtime parsing still discards wiring-status and capability fields. | M2 |
| F16 Older Android persistence | Open; [converters](C:/Users/Horse/Documents/GitHub/jk-bms-usb/JkBmsApp/app/src/main/java/com/horse/jk_bms/data/local/TypeConverters.kt:50) use java.util.Base64 with minSdk 21 and no configured core-library desugaring. | M0 |

**Verification and limits**

On 9 October, running the following from the Android project directory failed immediately with `Unable to access jarfile ... gradle-wrapper.jar`:

```powershell
.\gradlew.bat test assembleDebug lintDebug assembleRelease --console=plain
```

No tests, lint analysis, or APK builds completed in this review. The September audit reports 267 passing debug tests and 267 passing release tests in a temporary repaired build copy, plus failing lint; those are historical results, not a fresh verification of this checkout. No Android UI or physical BMS was exercised here.

Source inspection reconfirmed the defect paths above. Protocol uncertainty remains material: the [protocol reference](C:/Users/Horse/Documents/GitHub/jk-bms-usb/protocol-complete.md:600) still leaves ACK format, counter behaviour, and other hardware semantics unresolved. Synthetic tests alone cannot settle those questions.

**Recommended delivery sequence**

Each milestone should produce a reviewable change and evidence for its exit criteria. Estimates are rough engineering days for one developer familiar with Kotlin/Compose, including focused tests. They exclude waiting for hardware, major protocol discoveries, and distribution-account setup.

| Milestone | Deliverable | Depends on | Estimate |
|---|---|---|---|
| M0 Build baseline | Reproducible builds, passing compatibility checks, CI, accurate project status | None | 1–2 days |
| M1 Reliable monitoring | One USB session owner, resilient framing, correct status, initial real-device captures | M0 | 4–6 days |
| M2 Useful stored data | Bounded logging, working Logs/export, device/session identity | M1 | 3–5 days |
| M3 Verified configuration | Exact encoding, complete validation, truthful editor, capture-backed writes and readback | M1 and protocol evidence; M2 for persisted audit history | 4–7 days |
| M4 History and diagnostics | Session browser, trend charts, cell comparisons, event context | M2 and read-only hardware validation | 3–5 days |
| M5 Background sessions and alerts | Explicit background monitoring, status notification, useful local alerts | M1, M2 and hardware validation | 3–5 days |
| M6 Release candidate | Device matrix, soak tests, minified APK checks, documented compatibility | Selected preceding milestones | 2–4 days |

Full proposed scope: roughly **20–34 engineering days**, plus hardware discovery and access. M0–M3 account for 12–20 days. Re-estimate after M1; M4 and M5 can be separate releases. Configuration protocol uncertainty should not prevent shipping a qualified monitoring preview.

**M0 Build baseline**

Regenerate the complete wrapper bundle for the intended pinned Gradle version, including compatible scripts, the tracked JAR, the executable bit, and a narrow ignore exception. Add the distribution SHA-256 and validate the wrapper. Gradle documents both [distribution verification and wrapper integrity](https://docs.gradle.org/current/userguide/gradle_wrapper.html#sec:verification-of-downloaded-gradle-distributions).

Resolve the JDK documentation conflict: daemon criteria currently request JetBrains JDK 21 while compilation targets Java 17. Document and reproduce the actual toolchain before considering upgrades.

Replace or properly support the Base64 API while preserving existing stored values. Android documents [java.util.Base64 as API 26](https://developer.android.com/reference/java/util/Base64), above this application's declared minimum. Supply the referenced ProGuard file, configure an instrumentation runner, and establish Windows/Linux CI for tests, lint, debug assembly, and minified release assembly.

Correct the README and contributor documents where they claim complete implementation or verified writes. Keep the existing Kotlin/Compose architecture and initially pinned dependency versions; assess upgrades individually after a baseline passes.

Exit criteria: fresh checkouts build on both platforms; unit tests and lint pass; codec round-trip and legacy-row checks pass on API 21/25 and a modern API; CI retains reports and a debug APK.

**M1 Reliable monitoring and early hardware evidence**

Introduce an injectable serial transport and a session owner that owns permission, port resources, USB events, request scheduling, receive buffering, and connection state. Every poll, refresh, and write must use that same transaction path. Handle cancellation without swallowing it; close partially acquired resources on setup failure.

Support permission pending, connecting, live, stale, recovering, and disconnected states. Preserve the user's intent to disconnect, identify the specific detached adapter, and avoid reconnecting to an arbitrary adapter solely because its VID/PID matches. Android's [USB host guide](https://developer.android.com/develop/connectivity/usb/host) requires permission before communication, including explicit requests where an attach intent has not granted it.

Use a bounded receive buffer across reads, with resynchronization after noise and invalid frames. Keep transport response deadlines separate from the UI's stale-data age. Use a monotonic clock for freshness.

Prioritize runtime polling; read device information on connection and config on connection, explicit refresh, or completed writes. Poll faults at an evidence-based slower cadence and request system logs on demand. Choose intervals after measuring response times on the actual hardware.

Move the first hardware session here. Record phone/OS, adapter and port, BMS model/firmware, raw request/response bytes, and timestamps. Start with read-only operations. Add replay fixtures so late responses, noise, and disconnections can be reproduced without the battery connected.

Exit criteria:

- Grant, deny, detach during permission, active/unrelated detach, double-connect, navigation, cancel, and manual disconnect have deterministic outcomes.
- A fake transport records at most one active exchange and no new requests after shutdown completes.
- Fragmented headers, prefix noise, corruption, late tails, and multiple frames recover without unbounded buffering.
- With the current two-second stale threshold, a clock-controlled test shows stale status by the next one-second tick even if no data arrives.
- Read-only telemetry is compared with the vendor display on one recorded hardware combination; unsupported behaviour remains explicit.

**M2 Useful stored data and complete existing flows**

Add device and session identity before building charts. Record config/device snapshots only when their contents change. Use content-aware comparisons; identity equality is not a valid change detector for array-bearing models.

Deduplicate repeated fault history using validated event identity. An identical measurement is not necessarily the same event, and a device counter may reset. Keep the observed time separate from any BMS event time and test reconnect/reset cases.

Enable Room schema export and versioned migrations before changing tables. Index range queries, bound the logging queue, and define retention for all tables. Show a recoverable logging error when storage fails, while keeping monitoring usable. Exclude credentials from routine snapshots, shared diagnostics, and backup as appropriate; the existing device entity persists both Bluetooth and settings passwords.

Wire Logs entry/refresh into the session queue with loading, error, empty, and populated states. Carry known wiring and capability diagnostics through parser, model, persistence, and UI; leave unknown bits uninterpreted.

Fix repeat exports and visible retry. Define a versioned CSV/JSON schema with units, UTC timestamps, device/session identity, correct active-cell count, and per-cell values. Stream paged data into distinct output filenames with correct MIME types.

Exit criteria: a repeated 12-event snapshot does not create duplicates; different devices remain separate; migrations preserve history; retention covers every table; simulated storage failure leaves live telemetry working; two consecutive exports and a failed/retried export work; a 72-hour dataset exports without loading its entirety into memory. Measure time and peak memory on the intended phone.

**M3 Verified configuration and practical backups**

Treat settings writes as a transaction: obtain a fresh baseline, resolve conflicts, validate the complete intended configuration, encode, send, interpret the documented response, read config again, and compare the applied values. When the outcome is uncertain, show that explicitly and refresh before offering another write. Do not automatically retry an uncertain write.

Separate read/write payload layouts. Reconcile both archived schemas with captures before claiming firmware compatibility; do not assume ACK counter echo. Preserve untouched raw values where possible and use tested rounding for edited scaled values.

Create a typed field schema for units, supported capabilities, range, signedness, array length, and relevant protection/recovery relationships. Validate at the write boundary as well as the editor. Retain raw text and its errors independently of parsed values. Merge fresh device values into clean fields, detect conflicts in dirty fields, and show an exact before/after comparison.

Save a versioned configuration backup tied to model, firmware, device identity, and time. Add backup comparison first; restore must reuse the same validation and verified-write path. Never apply a saved configuration automatically on connection, and do not treat a generic battery chemistry preset as verified for a particular pack.

Exit criteria: independent golden frames verify the write tail; exact-byte tests preserve untouched values; invalid, non-finite, out-of-width, malformed-array, and conflicting drafts are rejected; unrelated responses cannot produce success; readback mismatches identify the affected fields; a controlled real-device change verifies the intended value and unchanged settings.

**New features ranked for your likely use**

| Rank | Feature and user benefit | Smallest useful scope | Dependency |
|---|---|---|---|
| 1 | Session history and trend charts: see what happened while charging or under load | Offline session list; current, pack voltage, SOC, temperatures, cell delta, and selected-cell plots; obvious gaps where samples are missing | M2; deliver in M4 |
| 2 | Cell comparison: see which cells repeatedly diverge | Sort by voltage; pin cells; mark minimum/maximum; display balance state and validated wiring diagnostics alongside trends | M4 |
| 3 | Configuration backups and differences: remember what changed | Named snapshots, current-versus-saved comparison, exact pre-write changes, verified restore | M3 |
| 4 | Background monitoring and local alerts: keep a session useful with the screen off | User-started recording, persistent status, Stop action, disconnect/stale/BMS-alarm notifications with deduplication and cooldown | M5 |
| 5 | Capture, replay, and diagnostic export: reproduce a problem without waiting for it to recur | Replay mode with an obvious demo label; bounded protocol trace; user-exported diagnostics with credentials removed | Capture foundation in M1; polish after M4 |
| 6 | Session energy summaries: understand observed charge/discharge activity | Integrate signed current and power over valid timestamped samples; show Ah/Wh separately, observed duration and missing-data coverage | After current-sign and sampling semantics are validated; separate follow-up |
| 7 | Named battery profiles: recognize different packs | Local names and history filters bound to validated identity; separate histories without automatic setting application | M2 foundation; separate follow-up |

M4 should start with a useful selected-cell chart rather than dozens of simultaneous traces. Include local history browsing after disconnect. Test that chart downsampling preserves short extrema and that missing intervals are not drawn as continuous measured data.

For M5, design user-started continuous USB monitoring around the applicable Android lifecycle and permission rules. The [connectedDevice foreground-service type](https://developer.android.com/develop/background-work/services/fgs/service-types#connected-device) explicitly covers USB interaction. Implement the required declarations, visible notification and Stop action, then test backgrounding, screen-off, process termination, and restart behaviour on the intended phone. Do not promise survival after force-stop or hardware removal.

Start alerts with BMS-reported alarms, lost connection, and stale data. Add user-defined thresholds with duration and hysteresis after the basic pipeline is trustworthy. Distinguish an app alert from a BMS protection event, and show missing sensors as unavailable rather than zero.

**Release gates and practical defaults**

A monitoring preview requires M0, M1, essential M2 retention/error handling, correct stale status, and successful read-only hardware validation. Keep configuration writes unavailable in that preview until M3 is proven for the supported firmware.

A configuration-enabled build additionally requires M3's capture-backed layout, full validation, serialized transactions, and successful readback. Persist the before/after result so a user can later answer what was changed.

A broader release requires a documented phone/Android/adapter/firmware matrix; a 24-hour monitoring soak; a 72-hour history/export check; unplug/replug and permission tests; accessibility, font scaling, rotation, and small-screen checks; and installation/testing of the actual minified release APK. Record pass/fail evidence and artifact versions. Publish only the combinations actually tested.

Proposed defaults are local storage, manual exports, foreground-only monitoring for the first preview, and one active BMS per session. Preserve the current minimum Android version through M0; revisit its support cost using the user's actual device and a passing compatibility baseline. Capture phone/adapter/firmware details during M1 rather than blocking build work on those decisions.

Defer Bluetooth, cloud accounts, remote control, a framework rewrite, broad support for other BMS families, and automatic battery tuning. They do not address the demonstrated gaps or the likely immediate value. Also defer predictive health scores and time-to-empty claims until data quality and estimation accuracy can be evaluated.

**First implementation batch**

1. Fix the wrapper bundle and documented toolchain; run the real build/test/lint baseline.
2. Fix the compatibility error, release configuration, and CI reporting; update misleading status claims.
3. Introduce the transport seam and serialized session lifecycle, with permission and disconnect tests.
4. Complete a read-only hardware capture and replay it through the decoder.
5. Deliver the monitoring preview after stale-state, logging containment, and retention criteria pass.

Track each milestone with its linked finding IDs, acceptance evidence, supported hardware, and remaining uncertainties. Mark findings complete only after the corresponding regression check and any required device evidence pass.
