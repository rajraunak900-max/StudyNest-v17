# StudyNest 1.6.3 — completed update and validation

Release 1.6.3 / versionCode 11 continues the completed 1.6.2 project. It preserves
the package, original signing key, saved history/settings/tasks and Android 8–16
architecture. Earlier completed projects and release archives remain untouched.
The user's later location requirement was integrated after saving the existing
permission/welcome and battery/break checkpoints; no previous work was restarted.

## Authorization diagnosis and changes

The supplied 104.05-second phone video shows the existing Usage Access action
already opening StudyNest's App usage data page. Android disables its switch with
“Controlled by restricted setting”; tapping it produces “App was denied access”.
The browser help page is opened by Android's help link. This is restricted-setting
approval, not evidence of a generally broken Android app or missing Usage intent.
The exact model/Android/Nothing OS build is not shown in the recording.

Public per-app Usage Access, category fallback and App info fallback remain.
Missing Usage Access on Android 13+ now offers **Open StudyNest app info**, explains
More → Allow restricted settings and returning to Grant Access. The user must
approve it. No security bypass, hidden system component, tap automation, Accessibility
service or browser launch is added. OEM/admin policy can still prevent approval.
Actual AppOps access is checked on resume; no launch is treated as a successful grant.
App Monitoring selections/master/call tracking retain explicit Save Changes, draft
discard and search behavior. Permission changes do not silently save a draft.

### Additional foreground/background location flow

The existing automatic geofence/library feature requires background location for
closed-app or locked-screen detection. The declaration was already present.

* Android 8–9: foreground location also supplies the relevant background grant.
* Android 10: request foreground COARSE/FINE first, then BACKGROUND separately.
* Android 11–16: obtain foreground first, explain optional background access with
  Android's localized “Allow all the time” label, then make the public background
  runtime request. Android's permission controller owns the supported approval page.
* If Android/OEM does not present that route, **Open app settings** opens StudyNest's
  App info with Permissions → Location → Allow all the time directions. A previous
  permanently denied request uses this fallback instead of repeating a dead end.

The second step keeps **Set up later**, every setup page keeps **Skip**, and actual
permissions refresh on callback and resume. Foreground/background are never bundled
in one request. No unsupported action forces “Allow all the time” into a first dialog.
The supplied video is evidence of the old navigation; new controller rendering still
requires a phone test. See [permission findings](ANDROID_PERMISSION_FINDINGS_1.6.3.md).

## Welcome and permission organization

The preserved skippable setup explains Library Time, Actual Study, distractions,
efficiency, tasks, history and streaks, with a Continue action. Permissions are grouped
by Study tracking, Library automation, Sound automation, Background reliability and
Notifications, with actual configured status and context-specific instructions.
Manual timing/tasks/history remain usable without optional detection/Usage Access.
Precise location, background location and the phone's Location toggle are distinct.

Privacy was checked against app-owned source, dependencies, manifest and storage.
No ads, analytics, cloud study database, upload client or INTERNET permission is
present. Study records are in Room/preferences with backup/extraction disabled.
Usage Access reads app timing, not messages, conversations, text, photos or screens.
Optional Android/Google Play services location is disclosed; the welcome claims
local study records rather than making an overbroad promise about provider processing.

## Explicit Break records and reminder

**Break means an explicit manual Pause during an active session inside the detected
library.** It ends on Resume, stop/end, leaving, disabled detection or interrupted
presence recovery. Idle library time, gaps, selected-app distraction and outside
pauses do not become breaks. Returning while still paused does not invent a new Pause.
Library Time continues through a break. Break does not subtract study twice.

Room schema 6 adds only manual_breaks and break_tracking through migration 5→6.
Old tables/entities, schemas 1–5 and migrations 1–5 are unchanged. There is no
destructive migration, reset or inferred backfill. Old History says **Not recorded**;
partially covered days disclose when Break measurement began.

A continuous eligible break reaching 45 minutes requests one **2000 ms vibration**.
It does not repeat at 50/60/90 minutes. A new Pause after Resume has fresh eligibility.
Leaving or stopping cancels it. The attempt is committed before vibrating, so retries,
duplicate ticks and process recreation cannot repeat it. A crash between that commit
and vibration can omit an attempt; at-most-once delivery is deliberate.

One app-scope delay and one unique WorkManager fallback are armed per break; repeated
ticks do not replace them. There is no new polling loop, alarm, exact-alarm permission,
45-minute wake lock, sound or repeated notification. UI closure alone does not cancel
the task. The deadline checks current pause/presence and samples configured Wi-Fi
once. Existing service reconciliation also checks eligibility.

Doze/OEM scheduling can delay it; vibration/DND/device policy and force-stop remain
authoritative. Interrupted process recovery closes unobserved presence conservatively.
Exact 45:00 delivery in deep sleep is not guaranteed by ordinary Android APIs.
See [break architecture](BREAK_ARCHITECTURE_1.6.3.md).

## Local midnight and History

Existing timestamp-based local-day intersection splits ongoing Library Time, Actual
Study, Distraction and the new explicit Break intervals at the device's local midnight.
No Finish Day action, Activity-open dependency or midnight alarm is needed. The session
can remain continuous; dates receive their own totals when later reconciled. Daylight
saving boundaries use the selected device time zone instead of assuming 24-hour days.
An open Today History report follows midnight automatically; a chosen past day stays
selected for review. Session information and original records remain available.

History uses five full pastel icon rows: blue Library Time (Includes breaks), green
Actual Study, red Distraction, amber Break (Manual Pause inside the library), purple
Efficiency. The **Actual Study outside library** row was removed only from presentation;
outside study remains in stored measurements and overall study reports. Library
Distraction and its expandable breakdown now share the same library-only scope.
Old inferred session gaps are labeled **Untracked gap**, not explicit Break. The
calendar/chart, weighted efficiency, week/month/all-time totals and streaks remain.

## Home and timer

The green Today's Tracked Study Session card keeps its compact structure, daily
goal, current/best streaks and now derives task totals/completed/due-today/overdue
from real saved tasks. Completed tasks never count as overdue. **View Tasks** opens
the existing Tasks tab. Small screens/larger fonts stack the summary without clipping.

The larger primary clock sits below the timer heading/settings gear and stays visible
as **00:00:00** when idle. Automatic/Manual is a compact segmented selector; changing
mode is disabled during an active session. All start/re-arm/Pause/Resume/stop controls
retain their behavior. Current library metrics and quotes follow the timer. Both themes,
320×640 dp at 1.5× font, 411×891 dp and 600×960 dp are covered by native simulated UI.
Screenshots supplied by the user guided design; they were not bug evidence.

## Battery and notification review

The reported 59% figure near 95% charge is not proof of a 59-point battery loss. The
video instead shows a separate 2%-since-charge snapshot. No denominator, steady rate
or numerical improvement can be established from those observations alone.

Actual repeated work was found in sound reconciliation: unchanged automation writes,
repeated cancellations and rescheduling an unchanged override deadline. Identical
state is now saved only when changed; sound fallback work is cleaned once after
engine recreation and cancelled/scheduled again when its state actually changes.
The original restoration/override behavior passes its existing regression suite.

Existing Wi-Fi callbacks, configuration-only scans, geofences, bounded event wake
protection, 15-second applicable Usage reconciliation and 30-second service checks
remain. No one-second expensive-query or notification-repost loop is introduced.
Android-rendered notification clocks and their read-only timestamp projections from
1.6.2 are unchanged. Real power improvement needs a controlled phone comparison;
see [battery/privacy findings](BATTERY_PRIVACY_FINDINGS_1.6.3.md).

## Automated and package verification

Final full test, signed-release and lint run succeeded on 2 October 2026 against
code checkpoint `b6f95f72ea9d1df42f799b27e03133c7f94035f7` in **26m 13s**. **346 tests passed**, with zero failures,
errors or skips. This includes denial → Settings grant → resume with accurate
status and no stale background-denial message, plus actual migrations from old
database versions 1, 2, 3, 4 and 5 through the complete schema-6 upgrade path.
Lint has **0 errors**, **0 fatal findings**
and **9 warnings**. No new lint findings compared with 1.6.2.

| Test suite | Passed cases |
| --- | ---: |
| AccessNavigationTest | 20 |
| AccessThemeTest | 2 |
| BackgroundEventsTest | 6 |
| BreakPlatformTest | 10 |
| CompatibilityApi26Test | 6 |
| CompatibilityApi27Test | 6 |
| CompatibilityApi28Test | 6 |
| CompatibilityApi29Test | 6 |
| CompatibilityApi30Test | 6 |
| CompatibilityApi31Test | 6 |
| CompatibilityApi32Test | 6 |
| CompatibilityApi33Test | 6 |
| CompatibilityApi34Test | 6 |
| CompatibilityApi35Test | 6 |
| CompatibilityApi36Test | 6 |
| DashboardLargeScreenTest | 7 |
| DashboardPhoneTest | 7 |
| DashboardSmallPhoneTest | 7 |
| DataMigrationAndBatteryTest | 2 |
| FocusIntegrationTest | 22 |
| LocalNavigationTest | 1 |
| LocationFlowTest | 21 |
| MainActivityStartupTest | 3 |
| ManualBreakTest | 12 |
| MidnightMetricsTest | 3 |
| MonitoringUiTest | 20 |
| NotificationLayoutTest | 1 |
| NotificationTest | 2 |
| NotificationTimingTest | 30 |
| OnboardingLargeScreenTest | 4 |
| OnboardingPhoneTest | 4 |
| OnboardingSmallPhoneTest | 4 |
| PickerIntegrationTest | 2 |
| PlatformMonitoringApi26Test | 2 |
| PlatformMonitoringApi29Test | 2 |
| PlatformMonitoringApi33Test | 2 |
| PlatformMonitoringApi36Test | 2 |
| PreferencesTest | 1 |
| SessionEngineTest | 30 |
| SoundAutomationTest | 19 |
| StudyMathTest | 6 |
| StudyPeriodsTest | 6 |
| TaskSummaryTest | 3 |
| UsageTimelineTest | 9 |
| WifiPresenceTest | 2 |
| WifiScanTest | 6 |
| **Total** | **346** |

New coverage includes manual-break threshold/resume/departure/stop/reboot/duplicate
delivery; scheduling/cancellation and 2000ms vibration API checks without an Activity
on APIs 26/33–36; separate location/later/resume paths on 26/29/30/33–36; restricted
App info and Usage status on 33–36; old-database upgrade with populated original
tables; stable sound write/job counts; midnight/DST scope; task counts/navigation;
idle timer; explicit Break rendering and History midnight/selected-date behavior.
All retained session/sound/background/usage/notification/compatibility suites pass.

Original manifest/resources/key/dependency versions, core Usage/Focus recording,
service callbacks and notification timing were compared with 1.6.2 and are unchanged.
Schemas 1–5 and old entities are byte/content checked; schema 6 is additive. Every
previous release artifact checksum still matches. The APK ZIP passes CRC and contains
only the exact final APK; the source ZIP is checked against every committed file and
includes wrapper, key, schemas 1–6 and documents, excluding generated build/cache files.

APK size: **13,138,487 bytes**. SHA-256:
`0F6005EAE509856F73DB70D65A2EEC626C0E1360691143543C91819CAD99AF1D`.
Package com.studynest.app, code 11 / 1.6.3, min API 26 / target 36; APK signature
and ZIP alignment are separately verified. Certificate matches the existing release:
`1a1fd08ef47edfbc56c21d5d0a60205092885ba4705d662b8967a91b34c75d17`.
These satisfy same-package, higher-version, same-signer upgrade prerequisites.
Install over the current app; **do not uninstall or clear data** to keep records.
The included original development key remains for this personal release.

## Saved checkpoints and outputs

* 1f94415: preserved completed 1.6.2 baseline.
* 8f1daab: welcome/privacy and restricted Usage Access help; compiled.
* 11bb159: explicit breaks, additive migration and reminder; compiled.
* d6e210d: battery write/scheduling review, saved before the added location request.
* 61c6ea9: separate foreground/background location; focused break/sound checks passed.
* 21962f5: reference dashboards/task summary/primary timer/midnight follow; compiled.
* 7c22635: expanded permission, migration, platform and dashboard checks.
* fed6d10: background-denial message disappears after a real Settings grant.
* The following test checkpoint adds migration 5→6 to the retained legacy test
  builders and verifies no fabricated Break data after every old-version upgrade.
* Tested code and final documentation/source commits are recorded in validation.json.

Outputs: StudyNest_1.6.3.apk, StudyNest_1.6.3_APK.zip, StudyNest_1.6.3_source.zip,
StudyNest_1.6.3_Implementation_Report.md, StudyNest_1.6.3_Permission_Findings.md,
StudyNest_1.6.3_Battery_Privacy_Findings.md and SHA256SUMS_1.6.3.txt. Verification_1.6.3
contains full logs, JUnit XML, lint reports, native screenshots, signature, badging,
alignment and machine-readable validation. All earlier source/release work is preserved.

## Physical-phone acceptance still required

No assistant physical-device/OEM test or measured power comparison has occurred.
The supplied video documents the old APK. Automated tests do not prove real Wi-Fi,
geofence, permission-controller rendering, DND/vibration or Doze behavior on your phone.

1. Install 1.6.3 over the current app. Confirm history/tasks/streak/goal/theme/library
   networks/location/monitoring settings remain, and configured users bypass setup.
2. On the affected phone, Grant Usage Access. If the switch says restricted, open
   StudyNest App info → More → Allow restricted settings, personally confirm, return
   and Grant Access. Check the real status, Save selections, lock/unlock and selected
   distracting apps. If OEM/admin policy blocks confirmation, record the exact page.
3. For location, allow precise foreground first, continue the optional background
   step and select Allow all the time on Android's page. Check automatic status on
   return. Try denial and Set up later too; manual study must stay usable. Verify the
   own-App-info fallback where the direct controller route is unavailable.
4. Inside a library pause for 20 min/44:59 (no reminder), 45 min (one 2-sec vibration),
   then 90 min (no second vibration). Resume/new Pause resets eligibility. Leave or
   stop at 30 min (cancel). Repeat with the phone locked and UI closed; record Doze
   delays/device policy. Confirm Library Time includes Break, with no double counting.
5. Keep a session/break running over local midnight, screen off. Verify both dates'
   clipped totals, correct efficiency and continuous session records. Test an open
   Today report and an explicitly chosen historical date. Older Break is Not recorded.
6. Check both themes and larger text, real Home task values, View Tasks, idle clock,
   existing timer actions, all History reports, quotes and settings. Repeat the retained
   DEVICE_1.6_CHECKLIST.md for actual sound restoration, calls, radios and notifications.
7. Compare equal-duration background sessions with similar charge/network/settings
   and record actual level drop plus app CPU/wake/activity attribution. No numerical
   battery improvement is claimed until those matched observations are available.

## Official Android references

* [Restricted settings](https://support.google.com/android/answer/12623953?hl=en)
* [Android 15 security model](https://source.android.com/docs/compatibility/15/android-15-cdd#2_2_5_security_model)
* [Public Settings actions](https://developer.android.com/reference/android/provider/Settings)
* [Background location](https://developer.android.com/develop/sensors-and-location/location/permissions/background)
* [Android 11 location changes](https://developer.android.com/about/versions/11/privacy/location)
* [Runtime location requests](https://developer.android.com/develop/sensors-and-location/location/permissions/runtime)
* [Persistent background work](https://developer.android.com/develop/background-work/background-tasks/persistent-work)
