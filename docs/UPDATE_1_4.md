# StudyNest 1.4.0 implementation checkpoint

Status: clean compilation, APK generation,79 automated tests and lint passed.
Version1.4.0/code6 signed with the original key. Physical-device checks remain.

## Changes
- Adaptive icon uses transparent clock/book foreground and full navy background,
  plus round/monochrome resources. Android applies the final mask. API33 remains
  the minimum supported OS; fallback resources do not add older OS support.
- Settings grouped into automation, sound, tracking, notification/access and app
  rows. Configuration pages keep the existing values; immediate switches/radios.
  Service-running status is set only after successful foreground promotion.
- Direct Wi-Fi OFF receiver, guarded network callback handoff, serialized presence
  processing, persisted departure deadlines with bounded partial wake protection.
  No additional periodic polling or continuous GPS. Existing service check remains
  every30s while awake; event deadlines no longer depend on that check.
- Notification chronometer uses the active session's study segments. Paused
  duration freezes; old sessions/pauses are excluded. No per-second app updates.

## Preserved behaviour
Wi-Fi sound exit grace30s; timer departure grace90s. Timer pauses at the first
detected exit time, so the grace is excluded after a confirmed departure.
Wi-Fi OR location presence keeps timing when both are enabled. Departure-paused
automatic timers resume on return; manual timers wait for Resume. These rules
preserve the existing requested auto-pause behaviour. Finish stays explicit.
Wi-Fi sound policy remains primary even if location says inside.
No Room migration: only a session-segment read query was added. Package/key kept.

## Verification
Clean assembleDebug/testDebugUnitTest/lintDebug passed;79 tests, zero failures,
zero lint errors. New Settings and adaptive icon renders inspected. See
VALIDATION.md for signature, hash, test inventory and remaining device checks.

On Android13: install over existing app (do not clear data/uninstall), check
Settings values and permissions, lock screen, connect savedWi-Fi, verify vibration
and running notification; turn Wi-Fi OFF and wait90s, verify paused time matches
exit. Repeat out-of-range, rapid reconnect, dual detection and geo-only modes.
Test manual session departure and return, notification-denied access, sound
override modes, process restart/reboot, disabled Location, battery restrictions.
Measure real battery behaviour; no quantitative battery claim has been made.
Force-stop/OEM deep-sleep can prevent event delivery; wake protection only applies
after Android delivers an event. No physical Android13 testing performed here.
