# Additional location flow requirement — 2 October 2026

Incorporated into the current update after checkpoint d6e210d, preserving welcome/
permission checkpoint 8f1daab and manual-break checkpoint 11bb159. No restart or
undo of completed work.

Background location is necessary for the existing library geofence and reliable
SSID detection paths while closed/locked. Foreground approval alone is not shown
as full background approval. Android 8/9 uses the same foreground permission;
Android 10 requests background separately; Android 11–16 uses the separately
approved background-location flow with settings-based Allow all the time.

When library detection is selected, foreground success offers a second explanatory
step, rather than silently leaving the user at Allow while using the app. The
Background location action also guides foreground first when missing. A public
ACCESS_BACKGROUND_LOCATION request lets Android's permission controller open its
own supported background flow after foreground approval. App info is an explicit
fallback when Android will not present that flow again. The restricted/system-only
MANAGE_APP_PERMISSION action and internal Location screen components are not used.

Set up later dismisses the second step; setup Skip remains available. Status is
rechecked on runtime result and lifecycle resume. The exact Location destination
and OEM behavior still require the user's physical-phone acceptance check.

Evidence: the supplied 1:44 video shows the initial while-in-use dialog and the
longer App info → Permissions → Location path. It also shows Allow all the time
being granted later; this update makes that necessary second step clearer and
uses the public runtime background request to facilitate the system flow.

[Android background location guidance](https://developer.android.com/develop/sensors-and-location/location/permissions/background)
and [Android 11 separate requests](https://developer.android.com/about/versions/11/privacy/location).
