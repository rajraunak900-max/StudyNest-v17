# StudyNest 1.1.0

Requested updates:
- History: selectable week/month line graph, earlier/later period navigation,
  daily points using the same midnight-split durations as statistics. Rest days
  are zero and future days blank. Tap a point to select its sessions below.
- Home: choose Manual or Automatic. Manual start turns off automatic timing;
  automatic start arms library detection, including re-arming after a finished
  session. Stop automatic saves the current automatic session and disables
  automatic timing until started again. An active session must be stopped
  before switching modes. Pause/resume remain available.
- Use the uploaded clock/book artwork as the adaptive launcher icon, preserving
  the original PNG; the artwork is inset to fit Android icon masks.
- Goal presets up to 10h, -1h/+1h controls, custom hours (including fractions)
  up to the existing 24-hour daily maximum. Apply custom goal then Save settings.
- Theme choice saves/applies immediately. System follows device theme and is
  expected to match Dark when the device is dark. Light/dark card colours have
  been explicitly defined. Other settings still require Save settings.
- Granted background location and sound access buttons are disabled/grey like
  the existing granted notification and precise-location controls. Permission
  state refreshes on return from Android settings.

Sound behaviour is unchanged: enabled library presence keeps vibration active
regardless of timer choice. Manual timing keeps vibration active away from the
library while unpaused. An explicit sound override is respected.

User reported on 29 September 2026 that the 1.0.1 sound update works on their
phone. Departure/restoration was explained, but not yet confirmed by the user.
This is user-reported device feedback, not assistant-run physical testing.

Install 1.1.0 over the existing installation. Same signing key and database
schema; do not uninstall or clear app data.

## Additional requested auto-pause

After confirmed departure (90 seconds outside all enabled sources), either
running timer pauses. Study time ends at the original disconnect/exit timestamp.
Automatic timers resume on re-entry only if departure caused their pause.
Manual timers wait for Resume; user-paused automatic timers also stay paused.
A short Wi-Fi drop within the grace period does not interrupt the timer.
Sound restoration can now occur after auto-pausing a manual timer on departure.
Location delivery may be delayed by Android. Without configured detection,
manual timers cannot infer departure. Schema 2 persists the pause reason using
an explicit migration from version 1; migration is covered by a data-preservation
test using the exported original schema.
