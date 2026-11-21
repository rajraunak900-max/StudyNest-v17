# StudyNest 1.6.12 implementation and verification report

## User-visible changes from 1.6.11

- Actual Study now has the same all-location meaning on Home, the daily summary, History, Calendar, week/month chart, session totals and the ongoing notification: valid tracked study from library sessions plus valid Manual timer periods outside the library. Existing distraction, pause and unknown-coverage exclusions remain. Overlapping study windows are not double counted.
- “Library Time” has been renamed “Sitting Time” across user-facing summaries, reports, notification and help text. Sitting Time is the union of library-presence time and Manual timer intervals, so overlapping minutes count once. Distraction time is included in Sitting Time while Actual Study excludes it. Automatic-only timer time outside a detected library does not add Sitting Time.
- The configured minimum session length now filters both Manual and Automatic timer periods equally, at home and in the library. Shorter periods remain in daily totals but do not appear as separate Session Information entries. Library visit qualification continues to use the saved per-visit threshold.
- Manual, Manual-Automatic and Automatic timer entries use the same History card structure, with their timer type shown consistently.
- The About section now has a brief feature overview and credits **RAUNAK RAJ** as the app author.

## Implementation notes

The work is based on stable signed 1.6.11. The package ID remains `com.studynest.app`, versionCode is 22, versionName is 1.6.12, and Room schema remains 8. No schema change, database repair, data rewrite, permission change, dependency change, timer lifecycle change or Wi-Fi departure/grace-period change was made.

`domain/HistoryAnalytics.kt` adds union-based Sitting calculations and Sitting aggregates. `domain/StudyPeriods.kt` applies the shared timer-session threshold only to separate period presentation, retaining shorter valid study in aggregates. `automation/NotificationTiming.kt` and `automation/Automation.kt` show independent Sitting and Actual totals. Home and History UI, the chart, calendar, onboarding/settings help and About copy now use the corresponding definitions. History timer rows share one card layout.

Existing 90-second departure confirmation and sound restoration remain independent of the 30-minute visit-return grace period. Existing background/locked-screen tracking, notification actions, midnight logic, permissions, streak rules, tasks, app monitoring, sound and vibration behavior are inherited from 1.6.11. These inherited behaviors were not modified as part of the Sitting/Actual changes.

## Automated verification

The final full regression passed **645/645 tests**, with zero failures, errors or skips. Android Lint completed with **28 warnings and zero errors**. The earlier full run had 9 failures; these were investigated and corrected as test fixture/selector issues: an invalid simulated elapsed-time anchor, selectors using the old “Library session” label, an exact millisecond assertion on a live clock, a Compose tag lookup using merged semantics, and a JUnit test method with an inferred non-Unit return type. The corrected targeted LocalNavigation test and phone-scale Post169 Home, Redesign History and Simplification UI test classes passed.

Robolectric and Compose results are automated/simulated, not physical-device tests. The real-device library departure/re-entry test remains for the user to perform after installation.

The regression suite includes coverage for Sitting interval union and overlap, manual time at home, automatic-only outside time, distraction inclusion/exclusion rules, daily and period totals, all-location Actual in notification/Home/History/Calendar/chart, shared minimum duration, common History card structure, session lifecycle and visit accounting. The UI navigation fixture uses a ten-minute session because the default separate-session threshold is ten minutes.

## Compatibility and release verification

The final signed release APK is `StudyNest_1.6.12.apk` (12,059,163 bytes; SHA-256 `810C30237C13C3254B13860626B4FD739224865C0BB8C2AEF0A85F71698CFCBE`). Its one-file ZIP is `StudyNest_1.6.12_APK.zip` (SHA-256 `A44BF999DE09192FA236B8DD6EE31689F7E972C78D4A4DD85C8D8BCA52BA39F0`). `aapt` confirms package `com.studynest.app`, versionCode 22 and versionName 1.6.12; `zipalign -c` passes. `apksigner verify` passes and reports one v2 signer with SHA-256 `1a1fd08ef47edfbc56c21d5d0a60205092885ba4705d662b8967a91b34c75d17`, identical to the 1.6.11 stable APK. Same package ID, a higher versionCode, unchanged signer and Room schema support in-place update with existing app data retained. The APK has not been physically installed over 1.6.11 in this environment, so installation and actual data retention are not claimed as device-tested.

All artifacts in `outputs/StudyNest_1.6.11` are preserved. Final APK, APK ZIP and source ZIP names and checksums are recorded after archive creation.

## Known historical issue

The exact historical display cause for the 3 October 2026 visit remains unresolved. Diagnostic evidence established the evening 6:54–8:49 PM visit existed and qualified, with the configured minimum threshold applied. No data repair is justified. This old incident is not proof of a 1.6.12 defect and does not warrant added polling or background work.
