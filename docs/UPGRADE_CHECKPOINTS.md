# Android compatibility upgrade checkpoints

2026-10-01: Extracted StudyNest_V4_source.zip and inspected all production components,
UI/navigation, storage, migrations, existing regression tests and build configuration.
Original source saved in Git before compatibility edits.

Baseline: com.studynest.app, versionCode 6, versionName 1.4.0,
min/target/compile SDK 33/33/34, AGP 8.5.2, Gradle 8.7,
Kotlin 1.9.24, JVM bytecode 17. Room schema 4 (studynest.db),
SharedPreferences settings, existing migrations 1→2→3→4.

Baseline build attempts are logged in the surrounding workspace work directory.
Initial environment failures: missing writable Android user directory; missing SDK.
A workspace-local SDK and Android user directory are being prepared.

Findings: unguarded API 28 LocationManager.isLocationEnabled, API 29 typed
startForeground, API 31 callback constructor and PendingIntent mutability,
API 33 registerReceiver flags and notification permission checks. Modern FGS
type permissions are absent; timer-only service starts use type 0. Wi-Fi is
event-driven with redundant 30-second SSID resampling. Room-backed departure
deadlines already exist (sound 30 seconds; timer pause 90 seconds), plus deferred
WorkManager fallback. Manual override is five-minute restoration or one reminder.
No exact alarms, continuous GPS, DataStore or separate streak database.

Identity, feature screens, database schema and preference keys will be preserved.

Baseline build succeeded (fresh Gradle process via temporary S: alias) on 2026-10-01.
The alias resolves a Windows sandbox parent-directory enumeration issue that affects
Java Path.toRealPath even for readable workspace files. No Android source change
was required to build version 1.4.0. Logs: baseline-drive-alias-fresh.log.

Compatibility checkpoints saved: SDK 26/36/36, version 1.5.0 (7), AGP 8.10.1,
Gradle 8.11.1 with checksum, WorkManager 2.10.5, Robolectric 4.16 for API 36 tests.
Guarded location/callback/foreground-service/PendingIntent APIs; compatible
receiver registration; modern FGS types and permissions; explicit permission
requests and Android-version setup guidance; edge-to-edge safe drawing padding.
Removed periodic SSID polling and avoided idle Room/WorkManager churn. Preserved
30/90-second departure deadlines and added bounded wake protection for the existing
five-minute sound override deadline. API 26–36 regression matrix is saved in source.

Final regression checkpoint: 151 tests passed, zero failures/errors. All APIs 26–36
received six compatibility checks each; MainActivity startup passed on 26/33/36.
The matrix uses one isolated process per API to bound Robolectric resource memory.
All original Room schema JSON content is unchanged. Physical-device acceptance remains open.

Final clean release/lint checkpoint: BUILD SUCCESSFUL, 2m37s, 78 tasks.
Lint zero errors, 27 warnings. Final APK v2 signature and package/SDK metadata
verified; no bundled native libraries. Signer matches the supplied previous APK.
