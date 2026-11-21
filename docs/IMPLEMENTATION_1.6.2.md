# StudyNest 1.6.2 — setup, organization and live notifications

Continued from the authoritative 1.6.1 source checkpoint `34fb3d7` in a separate
`outputs/StudyNest_1.6.2` checkout. Earlier source projects, APKs and ZIPs are
preserved. Their SHA-256 checksums were verified before this update.

## User-facing changes

* Five setup pages introduce manual study, optional library Wi-Fi, optional library
  location, feature access, and daily goals. Progress, clear page icons, primary
  actions, Back and an obvious Skip are provided. Actions remain outside the
  scrollable body. Invalid optional detection remains disabled when skipped;
  its configuration can be added later. Setup saves at completion and the existing
  `setup` preference keeps configured users out of onboarding after an upgrade.
* Manual timing, tasks, history, quotes and Settings remain available when setup
  and optional access are skipped. Home offers Library Wi-Fi not configured / Set up,
  opening the Wi-Fi configuration page directly. Library settings include an
  explicit detection switch and Save configuration.
* Permissions & System Access groups the actual access used by StudyNest into
  study tracking, library automation, sound automation, background reliability and
  notifications. Cards show readable status, feature-specific requirements,
  explanations and Grant/Manage actions. Status refreshes on Android lifecycle
  resume and runtime-permission results; there is no Verify button or polling.
* Notification settings target StudyNest. Usage Access first attempts the public
  settings action with StudyNest's package URI, then the access list on unsupported
  phones. Precise location/notification grants use Android runtime dialogs when
  appropriate. Permanently denied access can be managed in the app's settings.
  Background location on Android 10 has its separate runtime request; Android 11+
  uses StudyNest's app details with Permissions → Location → Allow all the time
  instructions. Do Not Disturb and battery optimization open their legitimate
  system lists with short instructions. Device Location and Wi-Fi open their
  specific pages. Unsupported destinations fall back safely with instructions.
* No hidden settings components or APIs are used. No exact-alarm, notification
  listener, Nearby Wi-Fi, battery-exemption-request or other extra permission is
  added. Nearby Wi-Fi is not a substitute for the location-sensitive SSID APIs
  used by the working baseline. Battery exemption remains optional/recommended.
* Home order: Today's Tracked Study Session → Study Timer → Current Library Visit
  → Motivational Quote. Goal, session count, task shortcut and streak information
  are compacted into the first card; duplicate duration wording is removed.
* History order: Line Chart → Today's/Selected Day Session Report → Week & Month
  Report → Calendar → Session Information. The Tracked Study Overview card is
  removed. Its unique all-time total and daily average remain in the period report;
  streak information and existing charts, calendar and session details remain.
  Daily visit consolidation still uses the original weighted FocusMath totals;
  raw visit records are never deleted or merged.
* Settings retains simple grouped navigation and all existing options. The 1.6.1
  App Monitoring icons, app search, selected states, access status, explicit Save
  Changes, retained selections and unsaved-draft discard behavior are preserved.

## Live notification architecture and limits

The foreground notification displays Library Time and Actual Study using two
Android RemoteViews Chronometer widgets, with standard notification decorations
and the existing Pause/Resume and Finish actions. Stopped values use ordinary text
so delayed notification inflation cannot advance a paused timer. Library presence
continues through manual breaks; Actual Study freezes during pauses or a confirmed
selected-app/call distraction. A stopped session cannot advance its study clock.
Unknown measurements remain unavailable. Actual Study is scoped to the current
session, while Library Time follows the current visit/departure rules.

Each snapshot uses the authoritative existing FocusMath records and the existing
UsageTimeline classification, including lock-screen/call state and switch grace.
Android advances clock widgets from elapsedRealtime bases. The display writes no
records and never substitutes its projection for database totals. Existing
15-second usage reconciliation, 30-second service checks and event-driven updates
remain. No one-second app loop, UsageStats/database query, Wi-Fi scan, GPS request,
foreground-app query, notification repost or new wake lock is introduced.

Between reconciliations the study clock projects the **last observed state**.
Android does not provide this app an immediate foreground-app-change broadcast.
Newly delivered usage events can therefore correct the projection at the existing
reconciliation interval (normally about 15 seconds). An app switch delivered late
can revise the displayed value backward by the newly identified distraction time;
the display must honor authoritative totals rather than hide that correction or
record guessed study. OEM delays may be longer. Existing stale/unknown-coverage
rules remain; exact instantaneous distraction detection is not claimed. Screen
lock/unlock uses the existing event path and does not require per-second queries.

Android/OEM notification rendering, Doze delivery, process stopping and system
access restrictions still apply. An OS-rendered clock is efficient; it does not
bypass those restrictions or prove that the process remains alive.

## Preserved baseline

Package `com.studynest.app`, original signing key, SDK 26/36/36 and all dependency
versions are retained. Room remains version 5 with schemas and migrations 1-5.
No destructive migration, history reset or preference reset is introduced.
SessionEngine, FocusRecorder/FocusMath, UsageMonitor, Wi-Fi/geofence callbacks,
sound restoration, service scheduling and data storage remain unchanged. Required
notification changes are limited to read-only presentation/snapshot construction.
Existing launcher assets, theme, tasks, quotes, goals and streak calculations remain.
Version is 1.6.2 / code 10 for installation over 1.6.1 without uninstalling.

## Validation

Final validation on 1 October 2026: `testDebugUnitTest`, `assembleRelease` and
`lintDebug` completed successfully in **12m 10s** against code checkpoint
`bcdadf9964097ef112cf9d93c162298a2e7656c4`. All **260 tests passed**, with zero
failures, errors or skips. This retains the 205 baseline cases (two UI fixtures
adapted to the new screens) and adds 55 cases. Lint has zero errors/fatal findings,
nine retained baseline warnings and **no new findings**.

| Coverage | Passed cases |
| --- | ---: |
| Android API 26 through 36 compatibility | 66 |
| Session engine | 30 |
| Focus integration and weighted daily totals | 22 |
| Sound automation | 19 |
| Usage platform and timeline | 17 |
| Background events | 6 |
| Wi-Fi discovery and presence | 8 |
| Study math and periods | 12 |
| Preserved App Monitoring Save/draft behavior | 5 |
| Home/History organization and layout | 9 |
| Startup, navigation, preferences, picker, access themes, legacy notifications | 11 |
| New notification clock states/reconciliation on APIs 26, 33 and 36 | 30 |
| New public system-settings routes and unsupported-handler fallback | 12 |
| New onboarding Skip, setup later, permission return and existing-user cases | 12 |
| New compact/expanded native notification layout | 1 |
| **Total** | **260** |

Simulated UI checks cover 320x640 dp with 1.5x fonts, 411x891 dp and 600x960 dp,
including Light/Dark Home, History and monitoring screens. Every setup Skip stays
reachable; a skipped permission changes status on return when granted later.
Configured users bypass setup and retain preferences, tasks and session records.
The daily library fixture retains three raw visits while reporting 7h presence,
5h actual study, 2h distraction and the weighted 71.4% focus. Native notification
layouts were measured at 260 dp width and 1.5x text in both themes; the collapsed
content fits 48 dp and expanded values remain readable, including Unavailable.
Final screenshots were inspected. These are Robolectric simulations, not physical
phone evidence. Test teardown emitted some Android CloseGuard resource warnings;
there were no failed assertions, and this does not establish a real-device leak.

Source comparison confirms the original domain/data code, schemas and migrations,
manifest, dependencies, signing key and existing resources are unchanged. Service
callbacks and reconciliation loops from `object Automation` onward are unchanged.
Earlier 1.5.0, 1.6.0 and 1.6.1 release artifact SHA-256 checksums still match.

The signed release APK is **13,113,303 bytes** (about 12.51 MiB), with SHA-256:
`1FFAA41A150744AE348EF99C927A4DFC35BB18211E6F27F637E82ECBB1399249`.
APK v2 signature and ZIP alignment passed. The certificate matches 1.6.1:
`1a1fd08ef47edfbc56c21d5d0a60205092885ba4705d662b8967a91b34c75d17`.
Package `com.studynest.app`, version code 10 / 1.6.2, minimum API 26 and target
API 36 were checked from the packaged APK. The existing development signing key
is retained. This meets package/version/signature prerequisites for upgrading
1.6.1 without uninstalling; actual installation has not been tested by the assistant.

The APK archive passes CRC checks. Its download ZIP contains only the same final
APK, verified byte-for-byte. The source ZIP contains every committed source file,
wrapper, original key, schemas and documentation, excludes generated build output,
and is checked against committed bytes with CRC validation. The final source
checkpoint differs from the tested code only in completed documentation.

No assistant physical-device or OEM testing has occurred. The phone acceptance
checks below remain necessary for real Wi-Fi/geofence transitions, permission
destinations, sound/call behavior, background delivery and actual upgrades.

## Final outputs and evidence

All files are in the same accessible `outputs` directory as earlier releases:

* `StudyNest_1.6.2.apk`: final signed APK.
* `StudyNest_1.6.2_APK.zip`: download fallback containing only that APK.
* `StudyNest_1.6.2_source.zip`: complete updated source project.
* `StudyNest_1.6.2_Implementation_Report.md`: this implementation/testing report.
* `SHA256SUMS_1.6.2.txt`: hashes for the three release artifacts.
* `Verification_1.6.2`: final build log, JUnit XML, lint XML/HTML, screenshots,
  APK signature, packaged manifest, alignment and `validation.json`.

`validation.json` records the tested code checkpoint, final documentation/source
checkpoint, archive checks and hashes. The completed Git checkout and all earlier
release projects remain preserved.

## Saved checkpoints

* `34fb3d7`: preserved 1.6.1 baseline.
* `1454550`: skippable onboarding, grouped access cards and public system routes;
  debug build succeeded.
* `66bfb9b`: requested Home/History organization and direct library setup;
  debug build succeeded.
* `5cfab62`: read-only live notification clocks; debug build succeeded.
* `bcdadf9`: completed regression coverage, final layout fixes and SDK guards;
  full 260-test, lint and signed release run succeeded.
* The final documentation/source checkpoint is recorded in `validation.json`.

## Physical-phone acceptance

1. Install over 1.6.1 without uninstalling. Confirm no onboarding appears for an
   already configured user. Verify history, visits, tasks, monitored apps, saved
   Wi-Fi/location, goal/streak, theme, permissions and version 1.6.2.
2. On a separate fresh test installation, skip every page with no library Wi-Fi
   and no optional access. Reach Home, start/pause/stop a manual session, use Tasks,
   History and quotes, then use Set up / Settings to configure a library later.
3. Complete setup with library Wi-Fi available. Try skipping location, denying a
   runtime prompt, granting access later and returning from each system page.
   Status should update automatically. Verify OEM fallback instructions if a
   package-specific access page is unsupported. Every setup Skip remains reachable.
4. Review Home/History order, both themes and larger text. Confirm one daily
   weighted library summary and old-date readability. Check all chart metrics,
   calendar, week/month totals, all-time figures and session details.
5. Preserve App Monitoring Save/discard/search behavior. Test real selected-app
   transitions and observe the notification clocks: Library continues, study
   freezes after existing reconciliation observes distraction. Return to study;
   measured totals should reconcile without double counting. Pause manually,
   lock/unlock, leave the library and stop the session. Check collapsed and expanded
   notification readability and actions. Note legitimate delayed OS event delivery.
6. Repeat the retained `DEVICE_1.6_CHECKLIST.md` for actual Wi-Fi/geofence events,
   vibration/Normal Sound restoration, Android DND rules, background/Doze behavior
   and real call audio. Record phone/Android version and expected/observed results.

## Android references used

* [Public settings actions](https://developer.android.com/reference/android/provider/Settings)
* [AOSP package-specific Usage Access handler](https://android.googlesource.com/platform/packages/apps/Settings/+/refs/heads/android14-d1-release/AndroidManifest.xml)
* [Background location flow](https://developer.android.com/develop/sensors-and-location/location/permissions/background)
* [RemoteViews monotonic chronometer bases](https://developer.android.com/reference/android/widget/RemoteViews#setChronometer(int,%20long,%20java.lang.String,%20boolean))
* [Decorated custom notification layouts and size limits](https://developer.android.com/develop/ui/views/notifications/custom-notification)
