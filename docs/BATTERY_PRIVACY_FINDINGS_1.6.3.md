# Battery and privacy investigation — StudyNest 1.6.3

## Evidence and interpretation

The user reports roughly 59% app battery usage after charging to 100%, with phone
level around 95%. This is not proof that StudyNest consumed 59 percentage points.
The supplied video does not display that 59% battery detail; App info instead
shows 2% usage since last full charge around 49–76s and status level falls 95→94%
during recording. These observations refer to different snapshots and cannot
establish a measured steady drain rate, cause or exact percentage denominator.

Android battery figures are estimated attribution over the selected reporting
window. If 59% represents a share of a 5-point loss, its illustrative equivalent
is about 2.95 points, not 59; this is an interpretation example, not a measured
StudyNest result. Battery stats/reporting windows need confirmation on the actual
Nothing Phone. No before/after physical power measurement was performed.

## Source review and improvements

- Existing bounded event wake locks protect presence/departure/sound callbacks
  (normally 10s, up to 100s departure and 310s sound override). There is no
  session-long wake lock. These protections are preserved for locked-screen work.
- One foreground study service retains callbacks/receivers and 15-second Usage
  reconciliation during applicable active sessions. Screen-off selected-app
  replays are skipped when state remains locked; optional calls retain checks.
- Existing 30-second service recovery/checkpoints are retained. Wi-Fi presence
  uses network callbacks, not repeated active scans. The Wi-Fi picker scans only
  while the user configures it. Geofences use Play services, dwell/responsiveness
  delays, with no continuous GPS request loop.
- No one-second UsageStats/database/Wi-Fi/GPS/foreground-app query or notification
  repost loop exists. Visible UI timestamps update at one second for display only.
  Android renders notification chronometers from elapsed-time bases.
- Actual unnecessary work found: sound reconciliation repeatedly upserted the
  same automation row, cancelled the same sound WorkManager job, and replaced an
  unchanged override deadline at successive ticks. State is now saved only when
  changed; persisted sound work is cleaned once per engine recreation, then only
  when a new pending reminder/deadline exists. An unchanged deadline is scheduled
  once. Sound restoration, override semantics and event wake protection remain.
- New manual-break checkpoints use existing engine/service boundaries. Exactly
  one delayed coroutine and unique WorkManager fallback per break are armed,
  without a long wake lock, alarm or new polling loop. UI closure does not cancel
  this app-scope task. Doze, OEM killing and force-stop can delay/prevent delivery.

## Privacy audit

Reviewed all app-owned main source, dependencies, manifest, extraction rules and
storage paths. No advertising/analytics/telemetry/crash upload/cloud database/
cloud synchronization SDK or app-owned network client/upload API is present.
The app has no INTERNET permission. Room and preferences store study history,
tasks, selected-app identifiers/timing and settings locally; Android backup and
data extraction are disabled by the existing manifest/rules.

UsageStats reads activity/package timestamps; persisted activity identifiers are
numeric hashes, not content. It does not access messages, conversations, typed
text, photos or screen contents. Installed launcher apps/icons are queried only
for local selection UI. No Accessibility service, screen scrape or keylogger.

Important scope: optional library location uses Google Play services geofencing
and Android location services. Their platform/provider processing is outside
StudyNest's own storage/upload implementation. The welcome therefore claims
local **study records** and no StudyNest study-data uploads, and discloses those
optional location services. It does not claim every platform service keeps all
information offline. The previous overbroad “Everything stays on your phone”
wording has been replaced.

## Phone comparison procedure

Compare equal-length screen-off sessions with the same network/location/settings
and similar starting charge; record phone model, OS/build, observation window,
actual battery level drop, app CPU/wake duration, foreground/background time and
whether calls/screen recording were active. Repeat with app monitoring off/on.
Preserve locked-screen detection, timer accuracy and sound restoration while
measuring. The report does not claim a numerical battery improvement yet.

References: [Android persistent work](https://developer.android.com/develop/background-work/background-tasks/persistent-work),
[wake-lock best practices](https://developer.android.com/develop/background-work/background-tasks/awake/wakelock),
[Android battery diagnostics](https://developer.android.com/topic/performance/power/setup-battery-historian),
[Google battery troubleshooting](https://support.google.com/pixelphone/answer/6090599?hl=en).
