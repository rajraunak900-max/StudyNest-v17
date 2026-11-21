# Sound-mode investigation — 29 September 2026

The user tested v1.0.0 on their phone: a Wi-Fi session was running while the
quick-settings tile still showed Sound. A later screenshot confirmed precise,
background location and Notification Policy (sound) access granted. The user
reported following the toggle/save and permission steps without improvement.

The exact phone-side cause is NOT yet established. Do not claim that missing
permissions caused this report, or that the updated APK has fixed it on-device.

Version 1.0.1 adds:
- Sound requests on the main dispatcher, with acknowledgement checks every
  250 ms for up to 1.5 seconds instead of a single immediate read.
- A fixed-volume check and concrete failure messages with actual ringer mode.
- A Sound status panel in Settings (active, disabled, waiting, blocked, or an
  external override detected).
- Apply vibration now: explicit user retry clears external-override suppression
  without ending or restarting a session. Existing restoration ownership is
  preserved while StudyNest still owns the active vibrate mode.
- Four engine regression tests: retry after override, no-session retry, retry
  after permission recovery, and preservation of the original restoration mode.

Install v1.0.1 as an update over the existing app; do not uninstall or clear data.
While connected to the selected library Wi-Fi, open Settings > Sound status,
press Apply vibration now, wait two seconds, and compare the status with the
phone's sound tile. If unsuccessful, collect the exact status text/screenshot,
phone model, Android version, and whether any phone routine changes sound.
An active Vibrate report with an audible phone needs device logs/behaviour
verification; do not repeatedly add unrelated permissions or force DND.
