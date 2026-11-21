# StudyNest 1.6.0 phone acceptance checks

These are pending physical-device checks, not results of automated phone testing.
Keep application data: install the supplied APK over 1.5.0 without uninstalling.
Also repeat the existing `DEVICE_CHECKLIST.md` baseline checks, using version 1.6.0.

1. **Update/data:** confirm version 1.6.0, old history, tasks, preferences, library
   networks, geofence, theme and sound settings. Old focus values must say
   unavailable. App Monitoring starts OFF with no automatic selections.
2. **Selector:** Settings > App Monitoring. Check real installed apps and icons,
   search by app name, Show system apps, and independent checkboxes. Select two
   apps quickly; both selections must remain. Restart StudyNest and reboot the
   phone; confirm selections. Rename/update/uninstall/reinstall a selected app
   where practical; the package selection must remain manageable without a crash.
3. **Permission:** turn monitoring ON with selected apps while Usage Access is
   denied. Library/original timer must still work; Actual Study must report
   unavailable. Grant Usage Access using the page's button, return and confirm
   status. Revoke it during a session and repeat. No accessibility/phone/content
   permission should be requested.
4. **A-E, G:** enter a saved library with automatic tracking enabled. Library and
   study begin. Use an allowed app: both continue. Use a selected app: actual study
   pauses, distraction increases, library continues. Return home/StudyNest/an
   allowed app: actual study resumes and the original session is still active.
5. **F:** switch selected app A -> selected app B -> A. Check one continuous
   distraction deduction, no double counting. Repeat very short app/activity
   switches; the documented background grace is one second.
6. **H-I:** add/remove an app during a session. Only subsequent measurement should
   use the changed selection. Turn the master OFF; no subsequent app time should
   be subtracted. ON with no apps/call audio also subtracts nothing.
7. **Manual/pauses:** manually pause inside the library: Library Time continues,
   actual study stops. Resume and finish. With automatic tracking OFF, library
   presence should still be recorded. Start a manual session outside the library:
   selected-app deductions work but do not inflate library efficiency.
8. **J:** enable optional call audio. Make cellular and VoIP calls during study,
   including with screen locked. Check actual-study pause/resume and overlap with
   a selected app. Record phones where audio mode is hidden; detection is best
   effort and is not authoritative call history.
9. **K:** disconnect all enabled library sources. Brief reconnect within 90 seconds
   keeps the visit; confirmed departure ends it at the observed loss timestamp.
   A second enabled source still inside keeps library presence. Verify original
   automatic/manual return and sound-restoration behaviour remains unchanged.
10. **L-M:** check visit arrival/departure, saved durations, breakdown, efficiency,
    daily/week/month charts and session details. Verify 4h library, 15m + 20m
    selected apps -> 35m distraction, 3h25m study, about 85.4% efficiency. Check
    manual breaks, midnight/date changes, zero-length sessions and unknown coverage.
11. **N-Q/background:** lock the screen during selected-app use; lock time itself
    must not be distraction. Check Wi-Fi/geofences, timer, vibration/Normal Sound,
    notification actions, Recents removal, Doze and OEM battery restrictions.
    Restart the process/reboot during a visit; unobserved presence must not be
    invented. Force-stop prevents Android monitoring until reopening.
12. **Regression:** tasks, streaks/goals, quotes, themes, all settings, navigation,
    charts and original launcher icon remain usable. Goals/streaks retain original
    tracked-time semantics; new focus values are shown separately.

Try representative API 26-36 devices, especially the actual phone/library. Record
phone model, Android version, permissions, timestamped sequence, expected versus
observed results and detection/sound status. Usage events and call sampling can
be delayed by Android; live values normally refresh about every 15 seconds.
