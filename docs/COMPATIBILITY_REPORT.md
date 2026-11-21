# StudyNest 1.5.0 — Android compatibility report

Date: 1 October 2026. Source: StudyNest_V4_source.zip supplied by the user.

## Configuration

| Setting | Original | Updated |
|---|---|---|
| Application/package ID | com.studynest.app | unchanged |
| Version code/name | 6 / 1.4.0 | 7 / 1.5.0 |
| minSdk | 33 | 26 |
| targetSdk | 33 | 36 |
| compileSdk | 34 | 36 |
| Android Gradle Plugin | 8.5.2 | 8.10.1 |
| Gradle | 8.7 | 8.11.1 (checksum pinned) |
| Kotlin / Compose compiler | 1.9.24 / 1.5.14 | unchanged |
| Java source/bytecode | 17 | unchanged |
| WorkManager | 2.9.0 | 2.10.5 |
| Robolectric (tests only) | 4.12.2 | 4.16 |
| Room | 2.6.1, schema 4 | unchanged |

The intended platform range is Android 8.0 through Android 16 (API 26–36),
including 8.1 and 12L. minSdk does not set a maximum Android version. Compatibility
on future releases still needs validation. SDK 36 was selected to meet the requested
Android 16 target without unnecessary migration to a newer platform/toolchain.

## Preserved functionality and storage

The existing app and screens were modified, not replaced. Manual and automatic
timers, pause/resume/finish actions, elapsed-time accounting, library Wi-Fi lists,
geofences, vibration automation, Normal Sound restoration, both manual-override
options, current-session notifications, history/details, daily/week/month totals,
calendar/chart, streaks, task editing/completion/priority/due dates, quotes, themes,
settings/navigation, launcher artwork and the StudyNest name remain in source.

The package ID, Room database `studynest.db`, tables `sessions`, `segments`,
`tasks`, `automation`, and SharedPreferences file `settings` and its keys are
unchanged. Schema 4 and migrations 1→2→3→4 are retained. There is no destructive
migration or data reset. Streaks continue to derive from stored study segments and
the saved streak threshold; no new streak database was introduced.

The existing development.keystore signer is retained for the generated APK.
In-place updates require the installed app to have this same certificate and the
same package ID. A different previously-used production certificate cannot be
replaced by this key. Do not uninstall or clear data to work around a signature
mismatch: rebuild with the matching original key instead. The development key is
bundled in the supplied source and is not suitable as a private production signing
secret. The updated release build is signed with this existing key.

## Implementation changes

### APIs and permissions

* API 26/27 use provider checks instead of API 28 LocationManager.isLocationEnabled.
* A separate background-location grant is required only on API 29+. API 26–28
  correctly use the existing foreground location grant for background access.
* POST_NOTIFICATIONS checks and requests apply only on API 33+. Android's
  notification-enabled state is checked on all supported versions.
* Runtime fine/coarse location requests are available in the existing access
  panel; API 29 can request background location separately, and API 30+ gives the
  required settings instructions after foreground access.
* Dynamic receivers use ContextCompat registration, including flags on modern
  Android. Geofence PendingIntent mutability is explicit from API 31, with the
  older compatible flags below it. Notification intents remain immutable.
* The network callback's API 31 constructor is isolated behind an SDK check.
  Earlier phones use the no-argument constructor and event-triggered legacy
  connectionInfo reads. Modern callbacks request location-bearing WifiInfo.
* No nearby-device permission, exact-alarm permission or new invasive runtime
  permission was added. SSID-based library detection and the Wi-Fi picker derive
  location, so fine/background location remains appropriate; nearby Wi-Fi access
  does not substitute for SSID/scan location permission.

### Foreground services and background execution

The existing StudyService remains the lifecycle owner for live Wi-Fi monitoring,
session notification actions and active timer checkpoints. Manifest types are
`location|specialUse`, with FOREGROUND_SERVICE_LOCATION and
FOREGROUND_SERVICE_SPECIAL_USE and an explanatory special-use property.
API 34+ starts use an explicit type; API 29–33 uses the applicable legacy location
type, and API 26–28 uses two-argument startForeground. Timer-only operation needs
no location permission. Notification actions use foreground-service PendingIntents.

Location monitoring is selected only with precise/background permission and
enabled device Location. An idle service without usable location access stops
unless an active session or pending restoration still needs it. Monitoring remains
enabled after removal from Recents (`stopWithTask=false`), with START_STICKY
recovery and existing boot/package-update recovery. Geofence-only idle operation
uses Play services geofences instead of keeping a service alive outside the library.

Wi-Fi automation requires a user-visible service while that setting is enabled:
Android cannot guarantee immediate arbitrary Wi-Fi arrivals to a killed process
through periodic work. The service stops when neither Wi-Fi monitoring, a session,
presence/departure deadline nor pending sound restoration needs it. It is not used
for task lists, history, statistics, quotes or ordinary settings.

Google Play submission requires foreground-service use-case declarations and
review, particularly specialUse. The manifest declaration does not guarantee
Play approval. The service does not claim dataSync, mediaProcessing, shortService
or systemExempted types to avoid their limits.

### Wi-Fi, timers and screen-off deadlines

Callbacks cover Wi-Fi capabilities changes/loss and network switching; a Wi-Fi
state receiver handles disabling the radio. SSID redaction on an existing network
is not treated as proof of departure. The redundant recurring 30-second SSID poll
has been removed. The picker still scans only while open, with Android scan
throttling/errors shown in the existing UI.

The original rules are preserved: sound restores after a 30-second Wi-Fi loss
grace; confirmed absence from all enabled detection sources pauses counting after
90 seconds, closing the segment at the observed departure time. It does not erase
or finalize the session history. Automatic sessions resume the same session on
return; a manually started session requires explicit resume. A present geofence
can intentionally keep the timer running after Wi-Fi loss, while sound follows
the original Wi-Fi-priority rule. Short reconnections cancel pending departure.

Arrival and departure processing stays independent of Activity lifecycle.
Persisted Room deadlines and bounded event wake protection cover screen-off
settling. The existing five-minute restore option now also has a bounded wake lock
while its deadline is pending, released on completion/cancellation. WorkManager
remains a process-loss fallback and is not represented as exact or immediate:
Android can defer it, including with Android 16 job quotas. The worker now goes
through BackgroundEvents so pending deadlines are recovered consistently.

Running-timer checkpoints remain lightweight and periodic to bound lost time on
process/reboot recovery. Idle Wi-Fi monitoring avoids repeating Room state writes
and WorkManager cancellation every tick. No continuous GPS, repeated background
Wi-Fi scans, wake lock for an entire session, exact alarm or polling CPU loop was
introduced. Occasional 90-second departure and five-minute restoration wake windows
trade a small amount of battery use for deadline reliability; no measured battery
benchmark or radio-delivery timing claim is made.

### Sound and notifications

The existing AudioManager ringer control, DND access check, fixed-volume handling,
1.5-second acknowledgement and visible error/status reporting are retained.
Normal Sound restoration and five-minute restore / one-reminder options retain
their original semantics. StudyNest does not alter media volume or call global
setInterruptionFilter/setNotificationPolicy. On Android 15+, a separate DND rule
or manufacturer routine can still suppress sound despite Normal ringer mode;
the access panel explains this limitation.

Existing badge-free channels/IDs are preserved, including channel migration,
silent reminder behavior, current-session elapsed chronometer and pause/finish
actions. Permission denial does not block foreground-service promotion, although
Android may hide the notification from the drawer. Allow notifications to see it.
Safe-drawing insets and enableEdgeToEdge keep existing screens clear of status,
navigation and display-cutout areas under API 35/36 edge-to-edge enforcement.
Existing Compose BackHandler navigation is retained for modern back dispatch.

## Required setup and limitations

1. Open StudyNest once after installation and choose/configure library automation.
2. Allow precise location and enable device Location for SSID access. From Android
   10, also grant background/Allow all the time access. On Android 11+, do this in
   the separate system settings step shown by StudyNest.
3. On Android 13+, allow notifications for the current session and sound reminder.
4. Grant Do Not Disturb/Notification Policy Access for sound automation.
5. For location automation, Google Play services and saved coordinates are needed.
6. Review the existing Background operation page if the phone puts the app into
   sleeping/deep-sleeping/restricted battery mode. StudyNest does not automatically
   exempt itself from battery optimization.

Locked-screen delivery, OEM service survival and radio transition timing require
real-device testing. Force-stop, Android's active-app Stop control, revoked access,
restricted battery state and some manufacturer task cleaners can stop monitoring.
Android 12+ can refuse non-exempt background foreground-service starts; StudyNest
reports a restart/access problem instead of repeatedly trying to evade it. Boot
recovery depends on permission and Android/OEM background-start allowances.
Geofence responsiveness can be delayed by Android and is not an exact arrival
clock. No universal always-on/force-stop-survival guarantee is claimed.

## Validation

Original source baseline: assembleDebug succeeded with original SDK values and
dependencies. Windows build-environment path resolution needed a temporary drive
alias to the authorized workspace; this did not change the Android project.

Final clean command: `clean :app:assembleRelease :app:lintDebug`.
**BUILD SUCCESSFUL** (2m 37s, 78 tasks). Release vital lint also passed.
Debug lint: **zero errors, 27 warnings**: 14 dependency-update notices, one
AGP update notice, one KAPT-to-KSP suggestion, two obsolete SDK-check notices,
two unused resources, six KTX suggestions and one implicit SAM warning on
stopService(Intent). No unsupported-API or missing-FGS-permission lint errors.
Warnings are retained in the delivered lint report.

The final release APK verifies with APK Signature Scheme v2 (supported by API 26+).
Certificate SHA-256: `1a1fd08ef47edfbc56c21d5d0a60205092885ba4705d662b8967a91b34c75d17`.
It matches the signer checked from the supplied previous APK. APK metadata confirms
package `com.studynest.app`, version 7 / 1.5.0, minSdk 26, target/compileSdk 36.

The final automated regression run passed **151 tests, zero failures/errors**.
This includes 66 framework compatibility tests: six on each API 26, 27, 28, 29,
30, 31, 32, 33, 34, 35 and 36. Checks exercise version-dependent permissions,
notification intents, callback construction, foreground-service promotion,
automatic departure/return and manual session accounting. MainActivity startup
also passed on API 26, 33 and 36. Existing timer, sound override, history math,
preferences, theme/navigation, picker and notification regressions passed.

Robolectric 4.16 ran on JDK 21. Each API uses an isolated test process to keep
Android framework resource memory bounded. Initial all-SDK runs exhausted the
runner heap; the isolated final run passed without dropping any SDK coverage.
Framework runtime download interruptions were resolved before the final run.

All four Room schema JSON files were compared with the original and match in
content. Package/version/SDK metadata, release signature and archive contents
are checked separately. The APK has no bundled native .so libraries, so native
16 KB page-size alignment does not apply to this artifact.

These are JVM Android-framework simulations and build/static checks, not physical
phone or emulator tests. No device installation, real Wi-Fi/geofence delivery,
locked-screen vibration/sound, Doze/OEM service survival, measured battery drain,
or in-place upgrade on an existing phone was tested. These acceptance checks
remain in docs/DEVICE_CHECKLIST.md. Background restrictions described above
remain applicable even when every automated test passes.

## Platform references

* [AGP 8.10 compatibility](https://developer.android.com/build/releases/agp-8-10-0-release-notes)
* [Foreground service types](https://developer.android.com/develop/background-work/services/fgs/service-types)
* [Background foreground-service restrictions](https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start)
* [Background location requests](https://developer.android.com/develop/sensors-and-location/location/permissions/background)
* [Wi-Fi permission requirements](https://developer.android.com/develop/connectivity/wifi/wifi-permissions)
* [Android 15 target behavior](https://developer.android.com/about/versions/15/behavior-changes-15)
* [Android 16 target behavior](https://developer.android.com/about/versions/16/behavior-changes-16)
* [Android 16 changes for all apps](https://developer.android.com/about/versions/16/behavior-changes-all)
* [WorkManager release notes](https://developer.android.com/jetpack/androidx/releases/work)
* [Robolectric SDK support](https://robolectric.org/compatibility_table/)


