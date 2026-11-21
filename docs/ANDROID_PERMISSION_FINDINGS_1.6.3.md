# Android authorization findings — StudyNest 1.6.3

## Supplied physical-phone evidence

Inspected the supplied 104.05-second recording, including sampled frames across
the entire recording. This is user-supplied real-device evidence, not an assistant
test of the new APK. No exact phone model, Android version or Nothing OS build is
shown. The report focuses on this specific authorization flow; it does not claim
the app generally fails on newer Android.

- About 18–23s: StudyNest 1.6.2 opens its own **App usage data** page successfully.
  The switch says **Controlled by restricted setting**. Tapping it shows Android's
  **App was denied access** dialog.
- About 24–32s: the user follows Android's **Learn how to allow access** link.
  Google Android Help displays **Learn about restricted settings**. It instructs
  the user to open Settings → Apps → the app → More → Allow restricted settings.
- About 37–43s: precise foreground location is granted through the legitimate
  runtime dialog and StudyNest's status refreshes on return.
- About 48–78s: background location opens StudyNest App info, then the user opens
  Permissions → Location and selects Allow all the time. StudyNest refreshes.
- About 83–91s: the Do Not Disturb action opens the OEM **Modes access** list,
  then StudyNest's page. The user allows access. These routes are working.
- About 94–103s: notifications are granted, setup finishes and Home is usable.

## Cause and supported remedy

The primary Usage Access intent is already reaching the right per-app page. The
browser is launched by Android's denied-access help link, not by StudyNest. Android
15's compatibility requirements explicitly include Usage Access in restricted
settings for downloaded/local APKs and require confirmation on the app's AppInfo
page. The exact OS build in this recording remains unknown.

StudyNest retains public ACTION_USAGE_ACCESS_SETTINGS with its package URI, then
the exact Usage Access category if an OEM rejects that route, then App info.
It now offers **Open StudyNest app info** whenever Usage Access is missing on
Android 13+. Clear instructions explain More → Allow restricted settings, user
confirmation, return and Grant Access again. Missing access is not falsely called
active. No inaccessible ECM/System API, tap simulation, browser launch, hidden
component or automatic approval is used. A third-party app cannot open or execute
Android's internal confirmation directly through a supported public API.

Existing lifecycle/runtime-result refreshes are retained. App Monitoring rechecks
the real AppOps authorization, syncs the measurement mode on resume, and preserves
the saved enable flag, selections and explicit Save Changes behavior. Administrator
or OEM policy may block approval; no success is claimed until Usage Access is allowed.

## Public references

- [Google restricted-settings steps](https://support.google.com/android/answer/12623953?hl=en)
- [Android 15 restricted-settings requirements, section 2.2.5](https://source.android.com/docs/compatibility/15/android-15-cdd#2_2_5_security_model)
- [Public Settings actions](https://developer.android.com/reference/android/provider/Settings)

## Added foreground/background location requirement

Background location is needed by the existing geofence registration and library
detection while the app is closed/locked. Manual timing remains usable without it.
The manifest already declares the required permission; no new permission was added.

The foreground action requests COARSE and FINE together. Android 8–9 uses that
grant for background access too. Android 10 requests BACKGROUND separately after
foreground succeeds. Android 11+ first explains why background access is needed,
then uses Android's public background runtime request so the permission controller
can present its own supported Location approval route. StudyNest never combines
foreground and background in one request or invents an initial “Allow all the time”
dialog. The educational step uses Android's localized background-option label.

“Set up later” dismisses the second step. “Open app settings” is a public per-app
App info fallback, with Permissions → Location → Allow all the time directions.
After a previous blocked request, that fallback avoids repeating a dead-end prompt.
Background status is re-read from the real permission result and on Activity resume;
it is never inferred from launching Settings. OEMs control the final page/dialog.
The hidden ACTION_MANAGE_APP_PERMISSION action is not used: it is not a supported
third-party route. The educational dialog scrolls on smaller/larger-font screens.

Automated checks cover request ordering, separate optional background approval,
Skip/later, App info package routing and resume status on APIs 26, 29, 30, 33–36.
The final report records their results. Actual permission-controller rendering and
restricted confirmation on the user's phone still need testing with the new APK.

- [Background location approval](https://developer.android.com/develop/sensors-and-location/location/permissions/background)
- [Android 11 foreground/background request separation](https://developer.android.com/about/versions/11/privacy/location)
- [Foreground precise/approximate permissions](https://developer.android.com/develop/sensors-and-location/location/permissions/runtime)
