# StudyNest 1.6.1 UI refinement

Built from the completed 1.6.0 checkpoint cf714fe. The original project and release
artifacts are preserved separately. The user reports that 1.6.0 works on their
physical phone; the assistant has not physically tested 1.6.1.

## What changed

* Home now presents the Study Timer first, immediately followed by the live
  library/session card. Goal, streak, task and quote information follows them.
  Timer mode selection, status, clock, Pause/Resume and Stop actions are retained.
* Live and historical focus statistics use balanced metric tiles. The grid uses
  two columns when space permits and one column on narrow screens/large text.
  Timer text and action buttons also adapt to available space.
* Home's developer-style sampling paragraph was removed. A concise optional help
  section in App Monitoring explains update timing, privacy and call limitations.
* History presents one consolidated summary for the selected calendar day. It uses
  the existing FocusMath day-range calculations: total library presence, measured
  study, distraction and weighted efficiency. Efficiency is study inside the
  library divided by total library presence, never an average of visit percentages.
  No visits are deleted or merged in the database. Raw records remain intact.
* Charts, calendar, tracked totals, session details and app breakdown remain.
  Week/month focus figures and app breakdown expand when requested. Chart titles
  now identify the selected metric; calendar cells grow with text.
* App Monitoring has consistent icon/name/toggle rows, obvious selected states,
  search with a clear button, system filtering, selected count and compact Usage
  Access status. Installed package IDs appear only to disambiguate duplicate app
  names; unavailable selections show an identifier so they can be managed.
* App selections, master ON/OFF and optional call audio are temporary drafts until
  **Save Changes**. This one explicit save uses the existing saveMonitoring method,
  preserving measurement boundaries and unrelated settings. The button remains
  visible above navigation. A Material snackbar confirms success or reports a
  save failure. Turning monitoring OFF retains selected apps.
* Back and bottom navigation discard unsaved drafts. Rotation and a return from
  Android's Usage Access settings can retain the on-screen draft; neither commits
  it. Usage Access itself is a separate Android permission and refreshes on return.
  An explicitly requested save is allowed to finish even if the page is then left.
* Shared card/page spacing, calmer headings, wrapping task filters, accessible
  control labels and adequately sized primary touch targets preserve the existing
  green Material theme, light/dark appearance, launcher identity and navigation.

## What remains unchanged

Package com.studynest.app, SDK 26/36/36, all dependency/toolchain versions, signing
key, permissions and manifest, resources, Room version 5 and migrations 1-5.
No database migration is needed. SessionEngine, FocusRecorder/FocusMath,
UsageMonitor/timeline/access APIs, preference storage, Wi-Fi/geofence callbacks,
services, notifications, ringer automation and recovery are unchanged.

Library Time, Actual Study, distraction, efficiency and outside-library manual
study retain their 1.6 meanings. Missing coverage/older measurements remain
unavailable. Goals and streaks still use tracked study. Tasks, quotes and all
existing settings are retained. Version increases to 9 / 1.6.1 for an update over
1.6.0; install without uninstalling or clearing application data.

## Validation

Final run on 1 October 2026: `:app:testDebugUnitTest :app:assembleRelease
:app:lintDebug`. BUILD SUCCESSFUL in 9m45s. Tested code checkpoint:
`731527c1e4c74fdab12a65eae425514f1d28caa4`. Subsequent changes only finish documentation.

**205 tests passed; zero failures, errors or skipped tests.** Lint: zero errors
or fatal findings, 12 warnings identical to the 1.6.0 baseline; no new findings.
The old monitoring UI test was updated for explicit saving and expanded to five
tests. Nine additional responsive dashboard tests cover three simulated layouts.
All other existing regression test sources were retained.

| Regression coverage | Passing cases |
| --- | ---: |
| Framework compatibility, APIs 26 through 36 | 66 |
| Session engine, manual/automatic timers and database migrations | 30 |
| Focus integration, live measurements and configuration boundaries | 22 |
| Sound/vibration automation and restoration | 19 |
| Usage timelines and platform monitoring APIs 26/29/33/36 | 17 |
| Background events | 6 |
| Wi-Fi presence and scanning | 8 |
| Study mathematics and periods/streak calculations | 12 |
| Monitoring UI: Save, discard, persistence, OFF retention and Usage Access return | 5 |
| Responsive Home, daily History and App Monitoring in both themes | 9 |
| Startup, navigation/tasks, notifications, picker, preferences and theme | 11 |
| **Total** | **205** |

The daily-summary fixture preserves all three visits and displays 7 hours of
library presence, 5 hours of actual study, 2 hours of distraction and 71.4%
efficiency. This proves aggregation uses totals rather than the 66.7% average of
the three visit efficiencies. Existing FocusMath tests retain zero-duration,
cross-midnight, incomplete coverage and historical measurement behavior.

Native Robolectric rendering and text-fit assertions cover 320x640 dp with 1.5x
font scale, 411x891 dp and 600x960 dp. Home, History and App Monitoring render in
Light and Dark; narrow layouts stack timer controls and metric tiles. Screenshots
were visually reviewed. Scrolling reaches off-screen library values and Save
Changes stays visible. Usage Access is exercised with a simulated Android settings
intent and permission change; this does not prove real OEM behavior.

The domain, data, automation and StudyNestApp sources, manifest, resources, schemas
and keystore compare byte-for-byte with the completed 1.6.0 baseline. The Gradle
configuration differs only in versionCode/versionName. Original 1.5.0 and 1.6.0
APK/source artifacts match their preserved SHA-256 files.

## Release and preserved outputs

* `StudyNest_1.6.1.apk`: 13,098,815 bytes, approximately 12.49 MiB / 13.10 MB.
* `StudyNest_1.6.1_APK.zip`: contains only `StudyNest_1.6.1.apk`; extracted bytes
  match the signed release exactly.
* `StudyNest_1.6.1_source.zip`: complete committed source, Gradle wrapper, original
  signing key, Room schemas 1-5, tests and documentation. Build caches and local
  machine configuration are excluded. Archive CRC and every committed file are
  verified; source export preserves committed line endings.
* `StudyNest_1.6.1_Implementation_Report.md`: this report outside the project.
* `Verification_1.6.1`: final build log, JUnit results, lint reports, simulated
  screenshots, signature, APK manifest/alignment evidence and validation.json.
* `SHA256SUMS_1.6.1.txt`: checksums for the APK and both ZIPs.

APK ZIP integrity, Android package metadata, v2 signature and zip alignment passed.
Package is `com.studynest.app`, versionCode 9 / versionName 1.6.1, minimum API 26,
target/compile API 36. The certificate matches 1.6.0 and the original supplied key:

`1a1fd08ef47edfbc56c21d5d0a60205092885ba4705d662b8967a91b34c75d17`

APK SHA-256:

`B501DB9634F4091FF51BAC86612DE14BCE34C0EC2F6AB72DADAE45539689184C`

Install as an update over 1.6.0 without uninstalling or clearing app data.
Existing data-storage architecture is retained; no migration or data reset is
introduced. Actual installation over a populated physical device remains pending.

## Saved checkpoints

The preserved 1.6.0 project remains at `cf714fe`. Work continued in a separate
`outputs/StudyNest_1.6.1` Git checkout, retaining the original history.

| Saved group | Checkpoint |
| --- | --- |
| Timer-first Home and responsive live metrics | ae6348b |
| Daily History and redesigned App Monitoring with draft/Save behavior | 0531064 |
| General polish, responsive controls and regression coverage | 25e2627 |
| Correct scrolling-position assertions and simulated UI cleanup | 731527c |
| Final signed release, verification and completed report | final source HEAD in validation.json |

No assistant physical-phone or OEM testing was performed. Real installation/update,
Doze/background delivery, lock-screen timing, Wi-Fi/geofence arrival/departure,
ringer policy, sound/vibration, real app usage and call audio remain device
acceptance checks. The user-reported working phone baseline is 1.6.0. Repeat
`DEVICE_1.6_CHECKLIST.md` plus the UI checks below for 1.6.1.

## Phone acceptance checks

1. Install over 1.6.0; verify saved history/visits, tasks, app selections, library
   settings, theme, goal/streak and permission state remain. Check About: 1.6.1.
2. Check Home order in Light/Dark and with larger text. Start manual/automatic
   sessions, pause/resume/stop, and confirm the clock and live library figures.
3. On a day with several visits, verify one daily library summary, total durations
   and weighted efficiency. Check old dates, calendar, every chart metric, session
   details, breakdown and week/month comparisons; no historical records disappear.
4. Select several apps, toggle master/calls, search/clear, then leave without saving:
   the old configuration must remain. Reopen, edit and Save Changes: confirmation
   appears and reopening/restarting retains the saved values. OFF must keep selections.
5. Grant/revoke Usage Access and return; status must update without committing
   unsaved app selections. Exercise system filtering, duplicate labels and an
   uninstalled app where practical. Try keyboard-open search and the save button.
6. Repeat selected-app timing, Wi-Fi arrival/departure, vibration/Normal Sound,
   locked-screen/background, notification actions, tasks, quotes and streak checks.
   Record phone model, Android version and expected versus observed results for
   any issue. Android 8-16 remains the supported framework range.
