# StudyNest 1.6.11 physical-device checklist

Automated regression is simulation only. After release, install the APK over 1.6.10 without uninstalling or clearing data, then confirm StudyNest opens with its existing tasks, Wi-Fi settings, goals and history intact.

For the requested lifecycle test, enable Automatic Study Tracking and configure the library Wi-Fi. Start the Manual timer while inside the library with automatic timer currently stopped (if an automatic timer starts on arrival, finish it, select Manual, and start the manual timer while still connected). Study validly for 1 hour, disconnect from library Wi-Fi and leave for 2 hours, then reconnect and study another 2 hours. Expected: the first timer/visit ends at the departure cutoff after grace, return starts a new library timer/visit automatically, Actual Study totals 3 hours, Library Time totals 3 hours, and the 2-hour away period is excluded. Check that normal sound is restored at the existing approximately 90-second sound deadline, without waiting for the 30-minute visit deadline.

Also test a return within 30 minutes: the same session and visit should continue without duplication, and away time should not count. Test the timer paused by the user, Auto-Pause from a selected app, call pause if supported, screen locked during arrival/departure, Wi-Fi loss while geofence still says inside, midnight crossing, and a second session later the same day. Confirm History, Session Information, Home Actual and Library values update as designed.

Record phone model, Android version, permission status, app version, event times, visible session IDs/details and whether the device was locked. Do not clear app data between update installation and the data-preservation check.
