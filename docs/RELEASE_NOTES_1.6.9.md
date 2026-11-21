# StudyNest 1.6.9 release notes

Six narrow changes over stable 1.6.8:
- A manually paused automatic timer ends when its old Wi-Fi library visit's full 30-minute grace expires. A later arrival starts a fresh timer and visit. Returning within grace preserves the existing pause and visit.
- Local App Monitoring search ranks visible-name prefixes before name substrings and package matches. Draft selections and Save Changes remain intact.
- Only Efficiency's percentage text changes colour: below 50% red, 50% to below 75% amber, 75% and above green. Unknown remains neutral; light/dark palettes are readable. Calculation unchanged.
- Week & Month Report retains all five metrics and period switching, removing duplicate tracked/all-time/average/streak text and its redundant explanation.
- Foreground Library and Actual clocks show today's accumulated library measurements, across visits, clipped to local midnight and excluding confirmed away time. Actual uses History's inside-library Actual Study; outside manual tracked study remains a separate existing measurement.
- Existing once-per-continuous-break 45-minute vibration also posts a clear, silent, badge-free Break Reminder. It uses the existing event/channel and a separate notification ID, opening Home when tapped.

Package, original signer, Room schema 8/migrations, settings/history and all unrelated features preserved. No new services, background timers, alarms, polling, wake locks or permissions. Physical update, sound/vibration, locked-screen notification delivery and battery acceptance remain user tests. Do not uninstall or clear data.
