# StudyNest 1.6.11 implementation and test report

## Changes

Automatic tracking no longer turns itself off when the user starts the Manual timer. If Wi-Fi/geofence presence is active when a manual timer starts, or is detected while a manual timer is running, StudyNest adopts the existing session as `Manual-Auto`; it keeps the same session ID and already-recorded segments. The detection event opens or continues the existing presence and focus records, so no second overlapping timer is created.

The adopted timer follows the existing 90-second departure confirmation: the study segment pauses at the original disconnect cutoff, independently of sound restoration. A Wi-Fi return during its existing 30-minute grace resumes the same timer and visit. At grace expiry, the timer and visit close at the departure cutoff; a later entry creates a new timer and visit. User-paused sessions stay paused. Turning off automatic tracking converts the adopted manual-origin session to ordinary Manual behavior. Schema 8 and historical records are unchanged.

Actual Study continues to come from the existing focus-window accounting and includes valid manual and automatic study. Library Time continues to come from detected presence. The exact simulated case—manual timer started inside library, 1 hour study, 2 hours away, return, 2 hours study—produced 3h Actual Study and 3h Library, with no away time credited. A within-grace return reused the same session/visit and excluded the confirmed-away gap.

Resource audit found `app/src/main/res/drawable-nodpi/study_icon.png` (2,056,073 bytes) had no direct or indirect code/XML reference. The manifest uses the adaptive `mipmap/ic_launcher` resources, and the adaptive art XML references `study_foreground.png`. The unused image was removed. No dependency was removed because each declared runtime dependency maps to active framework, Room, WorkManager, location or coroutine APIs. No unrelated refactor or upgrade was made.

## Validation

- Focused `ManualAutomaticTransitionTest`: 7/7 passed.
- Full `:app:testDebugUnitTest`: 620/620 passed; 0 failed, 0 errored, 0 skipped. This includes all 577 baseline 1.6.9 cases, the full 1.6.10 coverage, and the seven new transition cases. One legacy session test was renamed and its expectation updated to match automatic adoption.
- `:app:lintDebug`: passed, 0 Fatal, 0 Error, 29 Warnings. Baseline 1.6.10 had 30 warnings; one `UnusedResources` occurrence was resolved.
- The test suite uses Robolectric and simulated Room/usage data. It does not prove physical Wi-Fi/geofence delivery, screen-locked background execution, sound/vibration behavior, package installation or real user-data retention.

## Release identity and data

Target version is 1.6.11 (versionCode 21), package/application ID `com.studynest.app`, minimum SDK 26, target SDK 36, Room schema 8. The configured signing key is unchanged; expected certificate SHA-256 is `1a1fd08ef47edfbc56c21d5d0a60205092885ba4705d662b8967a91b34c75d17`. VersionCode 21 is greater than stable 1.6.10's 20. These checks establish compatible update identity, but no physical device installation or data comparison is claimed.

The signed release APK is 12,052,259 bytes; stable 1.6.10 was 13,215,551 bytes, a reduction of 1,163,292 bytes (about 8.8%). Resource-table inspection confirms the old APK packaged the unused `drawable/study_icon` as obfuscated `res/_o.png` (1,162,807 bytes raw), accounting for essentially all of the reduction. The active launcher icon remains the adaptive mipmap icon using `study_foreground.png`. No required feature resource was removed.

## Final release verification

The signed 1.6.11 APK is `outputs/StudyNest_1.6.11.apk` (12,052,259 bytes; SHA-256 `7eabf76997d1b57ceeb9b34be1ba95f9f3d46fa1883f34927f9359af7c9c998d`). Its package is `com.studynest.app`, versionCode 21/versionName 1.6.11, and signer SHA-256 matches 1.6.10 exactly: `1a1fd08ef47edfbc56c21d5d0a60205092885ba4705d662b8967a91b34c75d17`. `zipalign` verification succeeded. The packaged manifest matches 1.6.10 after removing the two version attributes. These static identity checks support update installation over 1.6.10; a physical installation and retained-data comparison remain pending on the user's device.

`outputs/StudyNest_1.6.11_APK.zip` contains only `StudyNest_1.6.11.apk`. A complete source ZIP and SHA-256 manifest are delivered alongside this report. All 68 files tracked by the 1.6.10 protected-output baseline remain unchanged. No physical-device tests were run; the transition sequence and automated suite were exercised using Robolectric simulations.
