# StudyNest 1.6.6 — fresh-conversation handoff

Current status: final signed normal1.6.6 release completed; release outputs and verified complete source archive supplied. New physical-device session acceptance is pending. Continue this source project; do not restart the app or repeat completed migrations. The user explicitly accepted that the October 3 historical incident's exact cause remains unresolved and authorized finishing the final release on October 4, 2026. The final report must retain that limitation.

## Identity, compatibility and preservation

- Final target: versionName **1.6.6**, versionCode **16**, application/namespace **com.studynest.app**, minSdk26 (Android8), compile/targetSdk36. Stable1.6.5 used code13; installed read-only diagnostic used14; an undistributed temporary History test project used15. Code16 is a forward update over these compatible versions.
- Original signing certificate SHA256: **1a1fd08ef47edfbc56c21d5d0a60205092885ba4705d662b8967a91b34c75d17**. Keep the included development.keystore and the existing signing configuration for this personal app. It uses the original public Android debug credentials; it is not a private production-distribution secret. Changing the certificate/package requires a different installation and breaks the intended update path.
- Stable1.6.5 source checkpoint: dbd978e5bc94de1f2ddce71868177ecc3bede5f3. Stable release files and earlier versions are separate outputs and must never be overwritten. Preserve user database/preferences; never require uninstall, Clear data, destructive migration or synthetic history repair.
- This source is the normal app, not the stripped read-only diagnostic or the temporary instrumented History test. Final source must not contain their diagnostic activity, extra Settings button or memory logger.

## Architecture and files

Kotlin/Jetpack Compose/Material3 native app. MainActivity hosts ui/StudyNest.kt with Home, Tasks, History and Settings; responsive shared components and System/Light/Dark themes. StudyNestApp owns Room, Preferences, SessionEngine, UsageMonitor and BackgroundEvents. No login, backend, ads, analytics or network service is used.

data/Database.kt defines timer sessions, segments, tasks and automation state; data/FocusData.kt defines library visits, focus windows and selected distraction intervals; data/BreakData.kt defines manual breaks and coverage. data/Preferences.kt persists Config in SharedPreferences("settings") and exposes StateFlow. DAO flows drive live Compose/lifecycle updates.

domain/SessionEngine.kt serializes state changes with a mutex/Room transactions. FocusRecorder keeps library-presence visits distinct from timer identities and eligible focus windows; BreakRecorder records pauses. FocusMath, StudyMath, HistoryIndex/HistoryTrackingIndex, StudyPeriods and VisitQualification compute intervals, reports and qualification. UI formatting must not alter stored timestamps or classification.

automation/Automation.kt contains Android monitoring/service/receiver/worker integration; BackgroundEvents uses event/deadline handling with existing bounded event wake protection and a persisted fallback. AppUsage.kt classifies selected app/category usage. PlatformAccess.kt and AccessNavigation.kt manage Android permission/settings flows; WifiDiscovery.kt provides available/current network discovery. BreakReminder.kt and NotificationTiming.kt manage reminders and notification actions.

## Major features

Home shows tracked study, goal progress, current/best streak, real task counts/due/overdue status, timer controls and offline motivational quotes. Manual start/pause/resume/finish and automatic Wi-Fi/geofence tracking remain available. Tasks support add/edit/delete/complete, due dates and priority. Settings preserve daily goal, minimum daily streak time, minimum library-session duration, Wi-Fi networks, library location/radius, sound override, monitoring selections, notifications, permissions, background operation, theme and About.

Library Wi-Fi supports saved named networks, rename/remove/add/current-network selection and explicit configuration saving. Any configured matching network counts as library presence; passwords are not stored and the app does not connect on the user's behalf. Preserve stored network lists/legacy SSID migration and existing multiple-network support; screenshots are UI references, not embedded assets or arbitrary limits.

Focus reports distinguish total Library Time, library Actual Study, library Distraction, manual Break and Efficiency. Actual Study outside the library is separately measured when applicable. Unclassified/permission-limited historical measurements remain unavailable rather than being fabricated as study. Timer tracked-study totals exclude pauses and are distinct from distraction-adjusted Actual Study. Goal/streak behavior uses the existing tracked-study semantics, not a newly invented metric.

## Session and minimum-duration rules

LibraryVisit records begin at detected arrival and are independent of StudySession timer identity. A same-day automatic timer may resume its existing identity, while each actual separate library visit keeps its own ID. Combined enabled Wi-Fi/geofence sources preserve presence until both report outside. Existing departure grace is90seconds; brief reconnect cancels it and keeps one continuous visit. A real departure eventually closes at the recorded departure cutoff, without adding the grace as library study time.

MinimumSessionMinutes defaults10, accepts custom1–1440, is captured at arrival and never retroactively reclassifies older visits when settings change. Qualification uses the continuous library visit end-start with **>=** threshold. Actual Study, breaks, distractions, foreground usage and current UI timer do not reduce qualifying duration. Exact10m qualifies;9m59s does not. Separate5m+6m visits never combine into an11m qualifying visit. Short visits still contribute legitimate time to totals. Completed visits are counted/displayed once per underlying visit ID.

Schema7 nullable minimumMinutes/qualified mark pre-1.6.5 legacy rows. Keep original earlier-study-period presentation for these; do not reinterpret NULL as failed or fabricate new qualification. History excludes coverage of all new visits (including short unqualified ones) from legacy timer-period cards to prevent double counting. Manual/earlier timer periods retain established grouping and sub-minute display rules; their source time remains intact.

Continuous midnight visits keep one identity/qualification over the full duration; daily metrics split at local midnight. Running or paused library visits and all manual sessions retain identity across midnight. A departed old automatic timer resets/reconciles on the new day from persisted timestamps; old records and continuous overnight study must not be cleared. Streaks preserve yesterday's run while working toward today's minimum; pauses do not count.

Two reproduced lifecycle defects fixed in1.6.6 must stay protected:

1. An expired departure could be erased by a delayed same-day re-entry before the tick finalized it, merging real visits. SessionEngine.signal now confirms old expired departure before applying new presence, inside its existing transaction.
2. Recovery during departure grace could close at a checkpoint beyond real exit and create a phantom visit from grace alone. FocusRecorder recovery uses the earlier persisted exit/checkpoint; grace may hold an existing visit but only actual enabled detection can create a new one.

Process recovery remains conservative: do not infer unobserved presence across a dead process/reboot. These proven corrections do not establish the cause of the October3 phone incident.

## Completed 1.6.6 UI work

History top navigation is **Tracked study | Calendar**. Removed redundant top Session info tab; detailed Session Information remains accessible through date-filtered View all sessions. Main layout keeps Actual Study/Library Time week/month chart, five daily metrics, optional genuine app breakdown, concise session report/count, optional expandable Break & Distraction two-series trend and unique longer-period information. Avoid repeated single-day graphs/dashboards.

Calendar has seven measured responsive columns, full A•Actual Study / L•Library Time legend, compact display-only values such as3.8h/52m, exact underlying intervals, empty cells showing only dates and future dates inactive. Normal phone widths fit seven columns; very large text can scroll a complete readable grid. Selected date shows the five metrics, optional recorded per-app distraction breakdown, dated report/count and View all sessions; no extra selected-day/month-summary graph repetition. Month analytics remain accessible through the main chart.

GoalFields uses equal preset widths/heights/gaps and centered incomplete rows. Normal daily presets4+4; streak3+3; session minimum3+2. Smaller screens/large fonts reduce columns. Custom goal/streak/minimum inputs, validation, Clear, save/state restoration and onboarding Skip remain working.

An existing active qualifying visit is shown separately as **In progress**, without inserting or finalizing data. Completed count stays separate. Room completion changes the same visit's card from active to completed and updates reports/list count. Back/navigation preserves selected-date context. Open-visit UI caching avoids repeatedly searching old completed records on the existing visible-screen clock.

## Permissions, background and battery

Onboarding/welcome and Settings keep Skip/Set up later, feature-specific explanations and status rechecks after return from system settings. Foreground location is obtained first. Background location follows the existing Android-version-aware second step: Android10 runtime background request where permitted, Android11+ app settings/manual Allow all the time, no security bypass or forced initial-dialog option. App Usage Access has its own settings/guidance, including Restricted settings guidance when applicable. Notification/DND/battery access remains separate and optional according to the feature.

StudyService uses the existing foreground-service types/notification and monitors through saved Wi-Fi/location settings. Boot/package replacement and persisted deadlines restore supported operation. Preserve API-specific service-start restrictions, configured access checks and normal outside-library idle behavior. Finalization is tested independently of opening History or an Activity; physical Samsung/OEM behavior still requires device validation.

No new1-second background polling, GPS polling, Wi-Fi scans, UsageStats query loops, wake locks, aggressive workers or diagnostic logging were added by this update. The existing1-second UI display clock only runs while visible/lifecycle STARTED; it is not a new background engine. Existing monitoring/reminder intervals and bounded event wakes remain unchanged. Pause/break excludes timer study; explicit manual break inside library can produce the existing one-time45-minute vibration reminder. Sound override restores after5minutes or reminds once according to settings; access/platform refusal must be reported accurately.

## Database and migrations

Room file **studynest.db**, schema **7**, tables sessions, segments, tasks, automation, library_visits, focus_windows, distraction_periods, manual_breaks, break_tracking. Historical migrations1→2→3→4→5→6→7 and schema JSON1–7 are retained; no schema/migration changes in1.6.6 and no destructive fallback.6→7 only added nullable visit minimum/qualification fields; legacy content preserved. Settings remain in the existing preference file/keys. Existing database/private-storage content must be left in place during update.

## Verification and build

Earlier ordinary full run:466/466 passed; later UI-only cache confirmation9/9 covers existing cases. Three new actual-phone structural replay cases passed; total469 distinct ordinary cases are represented before the final full469 run. Final full regression/lint/release/signature/archive results will be recorded below. Keep raw failures/reproductions and case provenance; do not sum repeated confirmations as new coverage.

Tests cover threshold boundaries/custom values, separate/short visits, expired re-entry, process recovery/departure grace,60m and locked-screen90m Wi-Fi finalization with no UI, combined presence/manual behavior/midnight, DAO/date filters, active→completed live refresh, calendar/goal geometry and accessibility, migrations1–7, task summaries, themes/onboarding/permissions, selected app/call usage, sound/reminders and notifications. Native Robolectric renderings simulate Android; none are physical-device installation/UI tests. Real exported rows are evidence, not proof that emulation reproduces earlier phone state.

Build with Gradle8.11.1, JDK17, Android platform36/build-tools35.0.0; Robolectric API36 needs JDK21 (STUDYNEST_TEST_JAVA_HOME can point to Android Studio's JBR). Supply local SDK location or ANDROID_HOME. Run testDebugUnitTest separately from lintDebug and assembleRelease to avoid resource contention. Windows: gradlew.bat; other systems: ./gradlew. Dependency versions remain unchanged. Signed output: app/build/outputs/apk/release/app-release.apk. Source archive must contain all tracked source/docs/tests/wrapper/schemas/key while excluding .git, build caches and local.properties; verify against committed contents.

## October 3 evidence and limitations

User reported missing evening visit after leaving20:49 onOct3 and checking approximately08:00 onOct4. Read-only diagnostic snapshot at09:46 onOct4 (SamsungSM-A528B/API33,Asia/Kolkata) contains visit13 from18:54:58.404→20:49:42.624:6,884,220ms, capturedminimum10, qualified=true, completed and included by the unchanged3October filter. Library data was not lost; no repair is justified. Earlier legacy visits10/11 preserve two earlier-study-period cards; shortvisit12 correctly fails its10m minimum but retains time.

Archived1.6.5 History logic and native1.6.6 History both display13 once with3October report3=one qualifying visit+two earlier periods. The archived screen also refreshes on normal Room completion. The7:07PM screenshot was during the active visit, explaining its then-zero completed count; that does not explain the later8AM observation. Snapshot lacks completion-write timing and past displayed date/lifecycle/scroll state. **Exact historical cause remains unresolved**, explicitly accepted by the user. No pretend repair/root-cause claim.

The read-only diagnostic code14 paused normal tracking; final normalcode16 restores normal app components/features with existing settings. Separate exploratory HistoryTestcode15 passed27targeted checks but its lint failed on a temporary numeric view ID; the user redirected work to finalization before it was packaged/delivered. It is not part of this final source. Do not continue spending time on it unless asked. Preserve its saved checkpoint/evidence rather than replacing stable releases.

After installing final1.6.6, the user will perform a **new real-device qualifying Wi-Fi visit**, leave, wait for existing departure grace, then check History/report/View all sessions and Calendar on the correct date. Do not claim that check has already passed. OEM/background/permission behavior, reboot, locked-screen monitoring and sound should be assessed on the user's phone. Keep all reports honest about automated versus physical evidence.

## Final validation record

Final full run executed469cases:465passed/4fixture failures. Corrected Onboarding/Redesign cleanup/timing fixtures passed30/30; production code unchanged. Combined case-by-case evidence validates469distinct cases, all418 stable identities retained,0unresolved failures. This is not a claim of a green initial full command. Preserve raw full failures and confirmation evidence.

LintDebug0fatal/0error/14warnings; assembleRelease succeeded after pre-packaging checkpointcc1261d24aa82655e909e0644c5640d35d06d060. Signature verified and matches165:1a1fd08ef47edfbc56c21d5d0a60205092885ba4705d662b8967a91b34c75d17. Packagecom.studynest.app/code16/name1.6.6/min26/target36;16KB alignment and APK-only ZIP integrity passed. APK SHA256:20C2C3DF11694150FCAA7903362C94B8A6872E9D15ADBB336DCCA7A6B456B7C1;13,207,771bytes. All22 earlier artifact hashes unchanged; stable165/diagnostic source preserved. Database/schema/preferences/migrations unchanged; simulated existing-data tests pass. Physical update/data-retention/new-session test remains unperformed; user will test. No proven October3 historical root cause.

Complete source ZIP is generated from final committed tracked files and verified byte-for-byte, including this guide, all sources/tests/docs, schemas1–7, wrapper and original key. Exact final source commit and all artifact hashes/sizes are in adjacent Verification_1.6.6/final/validation.json, SHA256SUMS_1.6.6.txt and delivered report. Read docs/IMPLEMENTATION_1.6.6.md for exact test provenance; historical journals are retained separately. Normal1.6.6 development/packaging is complete; next required evidence is the user's fresh physical-device acceptance.
