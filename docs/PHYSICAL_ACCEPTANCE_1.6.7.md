# StudyNest1.6.7 — phone acceptance checklist

These are physical tests for RAUNAK RAJ's phone. Automated Android/Robolectric tests exercise logic and UI; they do not prove real Wi-Fi callbacks, OEM battery/Doze timing, permissions navigation, sound access or private-device data retention.

## Update and preserved data

Install the signed1.6.7 APK over the current compatible1.6.6 installation. Do not uninstall or Clear data. Check About shows1.6.7 and RAUNAK RAJ. Check prior History/session records, tasks/completion/dates, saved Wi-Fi networks, selected distraction apps, goals/streak minimum/session threshold and theme remain. If Android rejects an update, retain the installed app/data and report the exact message rather than uninstalling.

## Independent90s and30min behavior

Enable configured Library Wi-Fi detection, automatic tracking and desired sound automation. For an isolated Wi-Fi test, leave Location/geofence detection disabled; if enabled and still inside, combined presence legitimately keeps study running.

1. Enter the saved library network; check one active visit/timer starts and library vibration activates when sound access is granted. Study at least10min (or your configured minimum). Lock the phone for an extended portion, ideally90min.
2. Disconnect at a recorded time. For the first90s the existing confirmation tolerance remains. After approximately90s check sound has returned to Normal Sound and tracked/Actual Study is paused. The visit must still be pending/In progress rather than completed. Confirmed study time uses the actual disconnect cutoff.
3. Return to ANY saved library network after5min,10min or29:59; test these as separate episodes with study between them. The SAME visit ID/original start continues; timer resumes and library vibration reactivates as appropriate. No second visit is created. After10min away then10min further study, Actual Study should add only the10min further study. Library Time keeps the continuous visit span on a canceled departure; confirmed away time is not Actual Study or a manual Break.
4. On a final departure remain disconnected for the full30min. Study stays paused after90s; visit then completes once with end time at the original disconnect, not30min later. A genuinely short9:59 visit must not qualify simply because finalization waited30min. A visit exactly at the configured minimum qualifies.
5. Open History, Today's Session Report, Calendar for that date and View all sessions. Verify the completed visit appears once and the count/list refreshes. A later genuine new arrival creates a new visit; same-day timer identity may be reused under existing rules.
6. Where practical, repeat a temporary departure across local midnight and return within30min: one visit/timer, no away Actual Study, daily allocation correct. Do not change the device clock merely to manufacture this condition.

Android/OEM scheduling can delay delivery while deeply sleeping; check stored cutoff and absence accounting even if observed UI updates occur later. Turning off Wi-Fi then reconnecting within90s remains a brief interruption and preserves the prior tolerance behavior. Explicit Manual timer/location-only departure behavior remains90s.

## Search, chart and reports

-Settings → App Monitoring: type `Whats`, `Pool`, mixed case and padded whitespace. Results update by app name; clearing restores appropriate rows. Select from filtered results and check count/state after another query. Show system apps must work; selected system rows remain selected. Search does not save drafts. Save Changes persists selections; Back discards temporary edits.
-History current Week: today's complete day/bar is immediately visible. Month: today's date is visible, including near month end. Manually swipe afterward; it must stay where you leave it through normal data refresh and rotation. Previous periods remain historical. Reopen History/current period to expose today again.
-Bottom Week & Month Report: expand This week/This month; check tracked study, five focus metrics and longer-period information with real data. No top Session info tab has returned.
-Actual Study period total/daily average/best day: a fully measured period must display sum, elapsed-calendar-day average including rest days, and largest known day. Mixed older unmeasured days can intentionally make all three Unavailable while today's exact values/recorded Library time remain visible; do not expect missing past measurements to be invented.
-Check ordinary manual pause/resume/finish, app/call distraction exclusion,45min explicit Break reminder, notifications/actions, tasks, goals/streaks, Wi-Fi settings, optional permission setup/settings return and appearance still work.

## Historical October3 finding

The exported18:54–20:49 visit was completed/qualified with correct threshold. Its exact past missing-display cause remains unresolved. User reports1.6.6 completed-session visibility now works. No database repair was justified or performed. This is a NEW1.6.7 physical acceptance test, not proof of that historical cause.
