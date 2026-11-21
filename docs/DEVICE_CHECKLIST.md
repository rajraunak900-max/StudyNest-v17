# StudyNest 1.5.0 phone acceptance checks — API 26–36

Not executed on a physical phone by the assistant. Automated evidence is in
COMPATIBILITY_REPORT.md. Perform these checks on representative Android 8,
8.1, 9, 10, 11, 12, 12L, 13, 14, 15 and 16 phones/emulators and the actual library;
Robolectric cannot establish OEM background, radios, launcher or sound behaviour.

1. Install over the existing app without uninstalling. Confirm history, tasks,
   goals, settings and saved library Wi-Fi remain. Check Settings says 1.5.0.
   Update checks apply on devices that could install the original API 33-minimum
   APK. On Android 8–12L, test a fresh installation of this build instead.
2. Grant precise/background location, notification and sound access. Check each
   green AND grey permission control still opens the relevant Android settings.
   API 26–28 must not request a nonexistent separate background permission.
   API 29 requests background access after foreground access; API 30+ guides
   Allow all the time in settings. Only API 33+ requests notification permission.
3. Save four Wi-Fi names. Connect to each: vibration. Switch between saved names
   or reconnect within 30 seconds: no sound departure or duplicate timer.
4. Disconnect all names for 30 seconds: Normal Sound. Repeat while geofence
   says inside, and while manual timer is running. Sound should still be Normal.
5. Disable geofencing, leave Wi-Fi on, repeat arrival/departure. Reboot and check
   saved geofencing/override/theme/network preferences persist.
6. RESTORE option: on library Wi-Fi, manually select Sound; wait five minutes,
   still connected: vibration returns. Repeat and leave early; no late vibration.
   Repeat and manually return to vibration; pending override should be cancelled.
7. NOTIFY option: manually choose Sound on library Wi-Fi: one silent reminder.
   Wait beyond five minutes: no repeating reminder. Choose vibration then Sound:
   a new single reminder is allowed. Departure resets the event.
8. Verify clock/book status icon and absence of launcher notification badge;
   required ongoing monitoring/timer notification remains usable.
9. Repeat detection and override checks with screen off, main UI closed and
   after removing from Recents. Force-stop intentionally blocks Android callbacks
   until reopened. Power restrictions can delay background deadlines.
10. With both detection methods enabled, timers auto-pause only when BOTH are
    outside for 90 seconds. With geofencing off, Wi-Fi departure alone is enough.
    Study duration cuts off at detected departure. Manual stays paused on return;
    departure-paused automatic resumes. User-paused automatic stays paused.
11. Test timer selection/start/pause/resume/finish, restart and midnight totals.
    Verify streak threshold, week/month graph and calendar date selection.
12. Select Light, Dark, then System with Android Light and Dark; change Android
    theme while System is selected. Confirm live response and persistence.
13. Create/edit/complete tasks of each priority: High red, Medium yellow,
    Normal green, readable in both themes. History type appears once per session.
    Refresh dashboard quote and confirm goals above five hours save correctly.

For a failure record phone model/Android version, settings, exact sequence,
observed vs expected result and the app's Sound/Detection status. Do not clear
application data to troubleshoot, because all history is local.

## Additional 1.3.0 phone checks

- Choose Wi-Fi Network: grant precise location; verify nearby SSIDs, select one,
  save settings, restart, and confirm exact name remains. Verify discovery does
  not connect automatically. Deny permission, disable Wi-Fi/Location and repeat;
  useful messages must appear. Rapid refresh may legitimately be throttled.
- Start/pause/resume a study period and switch tracking with short gaps; inspect
  numbered grouped History and details. Confirm break time is excluded. Finish,
  then start another period: Session 2. A later return after 30 minutes separates
  even a departure-paused automatic timer. Verify existing history survives update.
- Check new red/blue book/clock launcher icon in launcher, App Info and Recents.
  These actual system surfaces have not been tested by the assistant.
- Sound override: choose each setting (now saved immediately), observe Settings
  sound status after changing Vibrate→Sound. RESTORE should show remaining seconds
  and restore at about five minutes while still in-library; NOTIFY sends one.
  Repeat in Wi-Fi-only and geofence-only modes. Repeat Vibrate→Silent→Sound.
  Leave before the deadline; Normal Sound must remain after departure confirmation.
  If a failure remains, record phone model, Android version, selected mode,
  Sound status text, detection status and notification policy/channel access.
