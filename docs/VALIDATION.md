# Current validation — StudyNest 1.6.2

See [IMPLEMENTATION_1.6.2.md](IMPLEMENTATION_1.6.2.md) for the final setup,
permission/navigation, Home/History and live notification changes and limitations.

Final code bcdadf9: testDebugUnitTest, assembleRelease and lintDebug succeeded
in 12m10s. All 260 tests passed, with zero failures/errors/skips; lint zero
errors/fatal, nine retained baseline warnings and no new findings.
Simulated UI checks cover both themes, three display sizes, 1.5x small-screen
fonts, all onboarding Skip paths, permission-return status, existing-user data,
monitoring Save/draft behavior and native compact/expanded notification layouts.

Version 10 / 1.6.2, package com.studynest.app, min/target/compile 26/36/36.
APK v2 signature, original certificate, packaged manifest and ZIP alignment
verified. APK/download/source archives have CRC and byte-integrity checks.
Data/schema/migrations, dependencies, existing resources, core logic, service
callbacks and reconciliation intervals remain unchanged. Earlier release hashes
still match. Final documentation is the only change after tested code bcdadf9.

Live notification clocks project the last observed state between the existing
roughly 15-second usage reconciliations; late app events can correct the display.
No per-second expensive query loop or guessed recorded study is introduced.
No physical-device/OEM installation, system navigation, radio, Doze, call/sound
or background behavior has been verified by the assistant. Use the report's
phone acceptance checklist. Evidence: outputs/Verification_1.6.2.

The following entries are retained historical validation records.

# Previous validation — StudyNest 1.6.1

See [IMPLEMENTATION_1.6.1.md](IMPLEMENTATION_1.6.1.md) for the completed UI update,
regression results, signing evidence and physical-device acceptance checks.

Final run: testDebugUnitTest, assembleRelease, lintDebug; BUILD SUCCESSFUL 9m45s.
205 tests passed, zero failures/errors/skips. Lint zero errors/fatal, 12 retained
baseline warnings with no new findings. Timer-first Home, consolidated daily
History and App Monitoring draft/Save/discard behavior passed native simulated
UI checks in Light/Dark at 320x640 dp (1.5x fonts), 411x891 dp and 600x960 dp.

APK v2 signature and zip alignment verified; original certificate retained.
Version 9 / 1.6.1, package com.studynest.app, min/target/compile 26/36/36.
Domain/data/automation, manifest, resources, schemas, key and dependencies remain
unchanged from 1.6.0. Original 1.5.0 and 1.6.0 artifact checksums still match.
No database migration/data reset introduced. Install over 1.6.0 without uninstalling.

Physical installation/update, OEM background/locked-screen/radio/sound and real
app/call behavior remain untested by the assistant. The user reports 1.6.0 works
on their phone; that does not establish physical testing of this 1.6.1 release.
Final logs, screenshots, signature and all checksums are in outputs/Verification_1.6.1.

The following entries are retained historical validation records.

# Previous validation — StudyNest 1.6.0

See [IMPLEMENTATION_1.6.md](IMPLEMENTATION_1.6.md) for current build/tests and
physical-device acceptance limits. The entries below validate earlier releases.

Final run: testDebugUnitTest, assembleRelease, lintDebug; BUILD SUCCESSFUL 9m55s.
192 tests passed, zero failures/errors/skips. Lint zero errors/fatal, 12 retained
baseline warnings. APK v2 signed with original certificate; version 8 / 1.6.0,
package com.studynest.app, min/target/compile 26/36/36. Physical phone tests remain
pending; see DEVICE_1.6_CHECKLIST.md. Original 1.5.0 artifacts are unchanged.

# Current compatibility validation — StudyNest 1.5.0

See [COMPATIBILITY_REPORT.md](COMPATIBILITY_REPORT.md) for the current upgrade's
151 passing tests, final build checks and untested physical-device acceptance.

The records below are historical evidence supplied with version 1.4.0.

# StudyNest1.4.0 verification — 30 September2026

Clean command: `clean assembleDebug testDebugUnitTest lintDebug`.
**BUILD SUCCESSFUL**,11m14s,59 tasks.79 tests passed;0 failed,0 errors,0 skipped.
Lint:0 errors;5 warnings (older target as requested, redundant SDK guard,
KAPT vs KSP suggestion, two unused resources). No fatal lint findings.

Tests: SessionEngine29 (including Room1/2/3→4), SoundAutomation19,
BackgroundEvents5, StudyMath6, StudyPeriods6, WifiScan6, AccessTheme2,
PickerIntegration2, Notification2, Preferences1, LocalNavigation1.
Background tests use RobolectricAPI33 with no Activity: departure cutoff,
sound restoration, reconnect, dual detection, manual pause and elapsed-duration
notification. Navigation, custom goals, theme, task creation and history passed.
Adaptive normal/round/monochrome resources resolve; rendered icon and new Settings
visually inspected. Test render images are included in docs, not phone screenshots.

Package:com.studynest.app;version1.4.0/code6;min/target33;compile34.
APK signature verified. Certificate SHA256:
`1a1fd08ef47edfbc56c21d5d0a60205092885ba4705d662b8967a91b34c75d17`.
APK SHA256:`b8310af017bbd603205931b946ee65b1103d2d8d14b13df366bd9e5301f47447`.
Original key and Room schema4 retained; no new migration needed.
Manifest includes required notification, location/background-location, sound
policy, foreground-service and bounded-wake permissions.

No physical Android13 device testing was performed. Real radio/geofence delivery,
locked-phone behaviour, OEM battery restrictions, actual vibration and battery
usage still need the device checklist. Force-stop/deep sleep may block delivery;
the bounded wake protection acts after an event is received. No per-second
notification redraw loop or new continuous GPS polling was added.

Install as an update. Do not uninstall/clear app data to refresh a launcher icon.

## Historical1.3.0 evidence


# StudyNest 1.3.0 validation — 30 September 2026

## Verified implementation

- 73 automated tests pass on JVM/Robolectric API 33. Sound engine tests simulate
  ringer changes and time; they are not measurements on the user's phone.
- Clean `assembleDebug testDebugUnitTest lintDebug` passed after the database
  update. Final grouping refinement also passed assemble/tests/lint in 1m 22s, exit 0.
  Lint: zero errors, four non-blocking warnings (retained in verification/lint.txt).
- 29 engine/database tests, 19 sound tests, 6 time/streak tests, 6 grouping tests,
  6 scan-policy tests, 2 permission/theme tests, 2 picker/icon integration tests,
  1 preference test, 1 notification test and 1 expanded UI navigation test.
- Grouping checks: three hours across Wi-Fi/manual fragments appears as one;
  later visits separate; explicit Finish separates; short gaps exclude breaks;
  sub-minute groups stay hidden; midnight allocations remain correct.
- UI navigation opens Session 1 details and returns to calendar/history.
  Rendered details inspected, with start/end, actual study and break time.
- Real Compose Wi-Fi picker with simulated WifiManager results selects and saves
  the exact SSID without typing. Denied/revoked access, Wi-Fi/Location off,
  duplicate SSIDs, throttled, empty and timed-out scans are covered in logic tests.
- Attached icon copied unchanged to adaptive-icon foreground. Android drawable
  render inspected. Actual launcher/Recents/App Info surfaces not tested.

## Sound investigation and corrections

The code excluded geofence-only presence from manual override. It also lost the
restore deadline for Vibrate→Silent→Sound and required saving the option elsewhere.
These are corrected; override selection saves immediately. A live coroutine
countdown complements persisted WorkManager scheduling; the system ringer receiver
is exported for protected platform broadcasts and periodic mode checks remain.
Settings shows detected countdown/reminder status, and disabled reminder channels
or notification permission generate visible feedback. App's own changes remain
serialized and do not cause reminder loops. Wi-Fi still takes priority over stale
geofence presence when restoring Normal Sound after departure.

Both modes pass simulated tests, including geo-only restoration/reminder,
Silent→Sound, five-minute timing, departure cancellation, restart, repeated
manual events and once-only notifications. The precise cause of the user's
physical-phone failure was NOT independently reproduced; confirm on that phone.

## Database and data preservation

Room schema 4 adds sessions.endedExplicitly (default false) through migration
3→4. Upgrades from versions 1, 2 and 3 preserve sessions/tasks; no destructive
migration. Finish marks a boundary; stopping automatic tracking to switch modes
does not. Existing tracking records remain untouched, except schema addition.
Legacy records use a 30-minute gap heuristic because old Finish intent is unknown.
Under-one-minute groups are hidden only in presentation; totals retain them.
All existing goals, tasks, networks, sound settings, key and package are retained.

## Phone acceptance still required

No physical phone/full emulator installation or in-place upgrade was executed
by the assistant. Real Wi-Fi scans/connections, geofence timing, OEM background
restrictions, sound transitions and notification appearance must be checked on
Android 13 using DEVICE_CHECKLIST.md. Android scheduling can delay background
work and scan throttling can return cached results; the UI explains these states.
Do not uninstall or clear data. Install the same-key APK directly as an update.

## API references consulted

https://developer.android.com/develop/connectivity/wifi/wifi-scan
https://developer.android.com/develop/background-work/background-tasks/broadcasts

Wi-Fi scan APIs require precise location and Location enabled on this target;
CHANGE_WIFI_STATE is added. Nearby Wi-Fi permission is not requested for APIs
that do not require it. Scans occur only during foreground picker use; the app
never connects to networks automatically. No INTERNET/ads/analytics permission.

## Final APK identity

Package com.studynest.app, versionCode 5 / versionName 1.3.0, min/target API33,
compile API34. apksigner verification successful; certificate SHA256 matches prior
versions: 1a1fd08ef47edfbc56c21d5d0a60205092885ba4705d662b8967a91b34c75d17.
APK SHA256: d2b950bb12f552361567027dcb91fa3d240907c585c494eac10b18aca0433a26
Attached launcher artwork is byte-identical to the uploaded PNG.


