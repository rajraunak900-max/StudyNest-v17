# StudyNest 1.6.0 implementation report

Date: 1 October 2026. Built from the completed StudyNest 1.5.0 source checkpoint
a7f0e40. The original 1.5.0 project, APK and source ZIP are preserved separately.

## Version and compatibility

Package `com.studynest.app`, minSdk 26, targetSdk 36, compileSdk 36, AGP 8.10.1,
Gradle 8.11.1, Kotlin/Compose toolchain and all product dependency versions remain
unchanged from 1.5.0. Version increases from 7/1.5.0 to 8/1.6.0. The existing
development.keystore signing configuration is retained for an in-place update.
Android 8-16 remains the intended/tested framework range; real-phone verification
is still needed. This update does not repeat the compatibility migration.

## Library Sitting Time

A library visit starts when the existing enabled Wi-Fi/geofence presence rules
report inside. If both are enabled, either can keep the visit present. Losing all
enabled sources invokes the existing 90-second departure confirmation; a confirmed
departure closes the visit at the original observed loss timestamp, excluding the
grace interval. A quick reconnect within grace keeps one continuous visit.

Library Time is independent of the active study session, pause/resume controls,
automatic tracking selection, selected apps and call audio. It continues through
manual study pauses and distracting-app use. Disabling detection ends the relevant
presence measurement; no new GPS or Wi-Fi polling architecture was introduced.

Presence is checkpointed while a visit is being measured. After a dead process or
reboot, an unobserved visit is conservatively closed at its last saved checkpoint;
the existing presence/recovery rules start a separate current visit. Interrupted
automatic study also closes its focus window at the original timer's checkpoint.
StudyNest cannot prove physical presence
while Android has stopped its monitoring process.

## Actual Study, Distraction and efficiency

The original SessionEngine and tracked segments remain authoritative for manual
start/pause/resume/finish and automatic tracking. New focus windows represent the
times those sessions are counting. A user pause closes a focus window, but does
not close a library visit. Using a selected distracting app changes classification
inside a focus window; it does not pause, delete or finish the original session.
Returning to an allowed app/home resumes actual-study counting automatically.

App Monitoring defaults OFF and no apps/call categories are automatically selected.
When OFF, no app time is subtracted. ON with no apps and call audio disabled also
subtracts nothing. With monitoring enabled, actual study is eligible tracked time
minus selected-app/call distraction intervals. Missing or unclassifiable usage
coverage is marked unavailable, never invented as verified productive study.

During a library visit:

`Library Time = recorded actual study + classified distraction + manual breaks/not studying + unavailable coverage`

`Efficiency = actual study inside library / Library Time * 100`

Efficiency is unavailable for zero-duration or incompletely classified library
coverage. Manual study outside the library can also subtract selected apps; it is
shown separately and never inflates the library-efficiency numerator above 100%.
App/call overlap is one distraction interval, with call audio taking attribution
priority. Consecutive monitored apps never create overlapping deductions.

Android event timestamps determine app interval boundaries; the live display is
an estimate until the next query. A one-second background/switch grace avoids tiny
resumes between selected apps or screens. An allowed-app resume ends distraction
immediately; a background event without a replacement clears it after one second.
This can add up to one second to a background-only departure. Normal activity
changes are identified using a numeric hash of public usage-event activity metadata;
no messages, page contents or private app files are accessed.

## App selection and permissions

Settings > App Monitoring provides:

* Master ON/OFF, saved immediately.
* Installed launchable apps with icon, label and selection checkbox.
* Search by label/package and an OFF-by-default Show system apps option.
* Selection storage by package ID; no forced popular-app defaults.
* Usage Access status, clear privacy explanation and Grant/Review Usage Access.
* Optional Pause during call audio, OFF by default.

The launcher-intent `<queries>` declaration makes launchable apps visible on newer
Android versions. No QUERY_ALL_PACKAGES permission was added. Non-launchable
internal system components are intentionally not shown. Selected system apps stay
visible even when the system filter is off. Labels/icons are refreshed on return
to the page or Refresh; package IDs survive app renames and updates. Uninstalled
selections remain removable and are retained for a reinstall. Missing icons/labels
fall back safely. Setting writes are serialized to preserve rapid independent
selections.

The only newly declared permission is `PACKAGE_USAGE_STATS`, controlled by
Android's special Usage Access settings, not a normal runtime dialog. AppOps is
checked on all supported versions. Returning to settings and active monitoring
refreshes access status. Missing/revoked access preserves the library and original
timer but marks selected-app actual-study coverage unavailable.

No AccessibilityService, screen scraping, message/keyboard/photo/browser-content
access, phone-state/call-log/contact permission, Internet permission, new server,
new GPS request or new foreground-service instance was introduced. Data stays in
the existing on-device Room database and SharedPreferences.

## Phone calls and Android limits

The optional call-audio setting reads AudioManager's exposed IN_CALL or
IN_COMMUNICATION mode. It can cover cellular or VoIP audio where the OS exposes
that state, but is best effort: some phones hide or delay it, and communication
mode is not an authoritative telephony call record. No READ_PHONE_STATE/call-log
permission is requested. Unexposed calls cannot be reliably subtracted. The user
can additionally choose their launchable Phone/dialer app as a distraction app.

Call boundaries use observed mode changes, approximately the sampling interval;
they cannot be reconstructed from UsageStats history. Long unobserved call gaps
are marked unavailable rather than inventing their duration. Doze can delay
sampling and background call-end detection. No always-on reliability claim is made.

UsageStats is not a push callback for each app switch. Android can batch, delay or
omit events. The latest resumed foreground activity is the classification model;
split-screen, picture-in-picture, overlays, multiple simultaneous resumed apps,
work profiles and multiple instances of the same activity can reduce accuracy.
No app content is inspected to work around these limits. Recent usage replay is
bounded to 24 hours; missing older coverage is unavailable. Clock/time-zone changes
and interrupted monitoring require care when interpreting records.

Screen-off/keyguard events clear selected-app distraction. The lock screen is not
a distracting app. Known locked time remains productive by the configured rules
unless call audio is classified. Existing location/background/sound permissions,
force-stop behavior, Doze, OEM task cleaners and Android restrictions from 1.5.0
still apply to Wi-Fi/geofences and ringer automation.

## Battery and notifications

One recent UsageStats event stream is queried about every 15 seconds during an
active monitored session while the device is interactive. A bounded lookback
initializes a window's foreground state once. Installed apps are enumerated only
for the selector, not every second or per monitored package.

The 15-second sampling loop sleeps while locked unless optional call audio needs
sampling; the original service's 30-second checkpoints still save measured time.
Repeated usage queries are skipped for known locked state. No permanent wake lock,
exact alarm, rapid WorkManager job, extra foreground service or GPS/Wi-Fi scan was
added. Existing bounded presence/sound wake windows are preserved. No measured
physical battery benchmark was possible.

The existing silent active-session notification can display Library/Study time
and retains Pause/Resume/Finish actions and channel IDs. Monitoring does not post
per-app-switch notifications. When monitoring is enabled, the original tracked
chronometer is hidden so it cannot imply verified productive time while an app is
classified as distracting.

## UI, history and preserved features

Home adds a compact live library/actual-study/distraction/efficiency panel. The
original tracked timer and its controls remain. The main daily goal label says
Tracked Study; existing goal/streak semantics are preserved rather than silently
rewriting historical totals with measurements that did not exist before 1.6.

History reuses the chart/calendar with selectable Tracked Study, Actual Study,
Library Time and Distraction series. New selected-day/week/month panels and visit
arrival/departure results show comparison and package/category breakdown. Session
details include the new focus measurements where available. Midnight/day-range
splitting uses the existing time-zone/date architecture. Older history remains
readable and displays new measurements as unavailable.

Wi-Fi lists/picker, geofences, automatic/manual timers, pause/departure/return
behavior, vibration and Normal Sound restoration, both sound override modes,
history/grouping, streaks/goals, tasks/priority/due dates, quotes, navigation,
themes, notifications, launcher artwork and package ID are retained.

## Database migration and recoverability

Room version 4 -> 5 uses an additive, non-destructive migration:

* Nullable `sessions.measurementStart` distinguishes newly measured time from
  old sessions. Existing values remain null; an ongoing upgraded session begins
  new measurement at observation, not at an invented historical arrival.
* `library_visits` saves presence start/end/checkpoint/elapsed/boot metadata.
* `focus_windows` saves eligible session intervals, monitoring/selection snapshots
  and replay progress/state, including a numeric activity identifier.
* `distraction_periods` saves only selected package/category start/end intervals
  or explicit unclassified coverage.

Durations and efficiency are derived consistently from these persisted intervals,
not from a volatile UI counter. Daily/month/session figures and breakdowns can be
recomputed after restart. Existing sessions, segments, tasks, automation and
SharedPreferences file/keys remain. Migrations 1->2->3->4 remain and chain to 5;
there is no destructive fallback, database reset or clearing of history/settings.

Git checkpoints preserve the 1.5.0 baseline and each major 1.6 data/usage/UI change.
The final source includes wrapper, development signing key, all five schemas,
tests, docs and build files. Local SDK/cache/build outputs are excluded from its ZIP.

## Validation

Final run: `:app:testDebugUnitTest :app:assembleRelease :app:lintDebug`,
**BUILD SUCCESSFUL**, 9m55s. **192 tests passed: 0 failures, 0 errors, 0 skipped.**
All 151 baseline cases remain, with 40 monitoring/focus/UI cases and one additional
version-4 migration case. Lint: **0 errors/fatal findings, 12 warnings**; every
warning signature already existed in the baseline. They include redundant SDK
guards, preserved unused resources, KAPT/KTX suggestions and a false SAM warning
on Android's Intent-based stopService call. No warning was hidden for this update.

* 66 existing compatibility cases: six on each API 26-36, including service
  promotion/receiver registration, permissions, departure/return, timers and settings.
* 22 integration cases: independent visits, manual pauses/outside study, selected
  app subtraction/resume, master OFF, future-only selection changes, missing/granted
  access, screen lock/missed unlock, calls/overlap/unobserved gaps, dual presence,
  grace reconnect/departure cutoff, restart checkpoint, midnight, old history,
  zero-duration efficiency and preference persistence.
* Nine timeline cases: exact example, consecutive apps, one-second grace across
  chunks, allowed apps, lock, calls without overlap deductions, unknown coverage
  and ignoring a previous activity's late pause.
* Eight public-API cases across API 26/29/33/36: denied/granted Usage Access,
  event parsing and installed-app catalog with duplicate labels/system apps.
* Native Compose selection/search/persistence test plus retained navigation/task/
  goal/theme UI test; retained native MainActivity startup tests on API 26/33/36.
* Room migrations 1/2/3/4 -> 5 preserve seeded sessions, segments, tasks and
  automation; new measurementStart is null and new tables start empty. Existing
  timer/sound/Wi-Fi/history/math/notification regression cases also pass.

The supplied example produces **4h library, 35m distraction, 3h25m study and
85.4167% efficiency**, with app breakdown 15m + 20m.

Signed APK: `StudyNest_1.6.0.apk`, **13,075,635 bytes (12.47 MiB)**. ZIP CRC,
manifest and alignment checks pass. It is versionCode 8 / versionName 1.6.0,
package com.studynest.app, minSdk 26, target/compileSdk 36. APK Signature Scheme v2
verifies with the same certificate as 1.5.0:

`1a1fd08ef47edfbc56c21d5d0a60205092885ba4705d662b8967a91b34c75d17`

APK SHA-256:

`0C03109DBDD75AAD812C96D0208E4819606D6F68561D76774FC9083B793C4F2B`

The APK-only ZIP contains exactly that APK. The source ZIP is exported from the
final Git checkpoint, with no build/cache directories. The original 1.5.0 APK
and source ZIP still match their original SHA-256 checksums; its source checkout
is unchanged. SDK settings, dependency versions and launcher resources are retained.

Evidence is saved beside the outputs in `Verification_1.6`: build/test/lint log,
individual JUnit XML results, lint XML/HTML, simulated UI screenshots, validation
JSON, APK manifest and certificate verification. Tested application code is
checkpoint cdf5bf8; final documentation/packaging has its own checkpoint. Major
recoverable commits: 8391fb1, 62388cf, 327d069, 63e0fd3 and cdf5bf8.

Physical device/emulator installation, real app switches, calls, OEM/Doze/locked-
screen survival, battery measurements, actual reboot and update installation on
a phone were **not performed**. Preference reopening, framework simulations and
database migration tests do not prove those device behaviors. The source includes
`docs/DEVICE_1.6_CHECKLIST.md` for A-Q device acceptance and the retained baseline
checklist. Install over 1.5.0 without uninstalling/clearing data, then enable App
Monitoring, select apps and grant Usage Access when ready to test. Optional call
audio remains best effort and OFF by default.

## Android references

* [UsageStatsManager](https://developer.android.com/reference/android/app/usage/UsageStatsManager)
* [Usage events](https://developer.android.com/reference/android/app/usage/UsageEvents.Event)
* [Package visibility declarations](https://developer.android.com/training/package-visibility/declaring)
* [AudioManager](https://developer.android.com/reference/android/media/AudioManager)
