STUDYNEST — CRITICAL FINAL BUG-FIX UPDATE
READ THIS BEFORE CHANGING ANY CODE
The current StudyNest version is otherwise working correctly on my real phone. I have now found two serious regressions in essential automatic-library behaviour.
Do not redesign, refactor, optimize, clean up, or modify unrelated working functionality.
Treat the currently installed/source version as the stable baseline and make the smallest safe changes necessary to fix the bugs below.
I am also attaching a real-device video showing the problem.
Inspect the video and the current implementation before making assumptions.
The video is evidence of the bug only. Do not import it into the app.
Before editing:
1. Save/checkpoint the complete current source.
2. Preserve the current signed APK/source artifacts unchanged.
3. Review the current Wi-Fi departure state machine, session lifecycle, Library Time calculation, Actual Study calculation, Efficiency calculation, sound/vibration automation, background service/event handling, notification timer and 30-minute grace implementation.
4. Identify the root cause rather than patching only the visible UI.
BUG 1 — LIBRARY TIME CONTINUES FOR 30 MINUTES AFTER LEAVING
Current incorrect behaviour
When I disconnect from the configured Library Wi-Fi:
- Actual Study correctly stops/pauses;
- but Library Time continues increasing;
- because Library Time continues while Actual Study is stopped, Efficiency keeps decreasing;
- Library Time finally stops only when the 30-minute session grace period expires.
This is WRONG.
I introduced the 30-minute grace period only to prevent a temporary departure from splitting one visit into multiple sessions.
It was NOT intended to add another 30 minutes to Library Time.
Required timing model
There are two completely separate concepts:
A. 90-second departure confirmation
After the configured Library Wi-Fi has remained disconnected for approximately 90 seconds and no existing legitimate presence signal says the user is still in the library:
- pause/stop Actual Study accumulation;
- pause/stop Library Time accumulation;
- freeze the relevant live timers;
- Efficiency must stop changing because no additional Library Time or Actual Study is being accumulated;
- restore the phone's previous/normal sound mode according to existing sound automation;
- update the foreground notification/live UI appropriately.
The user should now be considered temporarily away, not actively accumulating library/study time.
B. 30-minute session-continuity grace period
The existing session/visit record must NOT be finalized at 90 seconds.
Instead, keep the same visit/session identity pending for up to 30 minutes from the Wi-Fi departure event according to the established StudyNest grace semantics.
The 30-minute period exists ONLY so a short temporary departure does not create two separate sessions.
During this pending-away period:
- Library Time does NOT increase;
- Actual Study does NOT increase;
- Efficiency does NOT continue decreasing;
- no fake Break time is created;
- no fake Distraction time is created;
- the session remains logically open/pending only for possible reconnection.
A pending session is not the same thing as active library presence.
RECONNECT WITHIN 30 MINUTES
If the user reconnects to a configured Library Wi-Fi within the grace period:
- cancel pending session finalization;
- continue the same session/visit;
- do not create a second session;
- do not duplicate History records;
- resume Library Time accumulation from reconnection;
- resume Actual Study according to the existing automatic-study rules;
- reactivate library vibration behaviour;
- preserve the original session identity/start;
- exclude the away interval from Library Time and Actual Study.
Example:
Library arrival: 10:00 AM
Wi-Fi disconnect: 12:00 PM
90-second departure confirmation: approximately 12:01:30 PM
Reconnect: 12:10 PM
This remains one session, but the confirmed-away interval must not be counted as Library Time or Actual Study.
Do not simply count everything from 10:00–12:10 as continuous Library Time.
REMAIN AWAY FOR 30 MINUTES
If the user does not return within the grace period:
- finalize the existing session correctly;
- do not add the grace interval to Library Time;
- do not add it to Actual Study;
- do not let Efficiency deteriorate during that interval;
- preserve correct minimum-session-duration qualification;
- preserve History/Calendar/session reporting.
Important: determine the correct effective accounting boundary from the existing StudyNest departure semantics and timestamps. Do not blindly use “30 minutes later” as the Library Time/session accounting end.
EFFICIENCY
Efficiency must always be calculated from legitimate accumulated time.
Once the 90-second departure has been confirmed and both relevant timers are frozen:
Efficiency must remain stable during the remainder of the 30-minute session-continuity grace period.
Example:
If Library Time is 2h and Actual Study is 1h40m at confirmed departure, those values should not become 2h10m / 1h40m merely because the session record is waiting for reconnection.
BUG 2 — NORMAL SOUND MODE RESTORES ONLY AFTER I WAKE/OPEN THE SCREEN
Current incorrect behaviour
Automatic vibration → previous/normal sound restoration does not reliably occur while the phone remains locked/screen-off.
It appears to take effect when I wake/open the phone screen.
This defeats one of StudyNest's core automation features.
The attached real-device video demonstrates this behaviour.
Required behaviour
After the configured Library Wi-Fi remains disconnected for the established approximately 90-second departure confirmation:
StudyNest should restore the user's previous/normal sound mode without requiring:
- opening StudyNest;
- unlocking the phone;
- waking the screen;
- navigating to any StudyNest screen.
It should work while:
- screen is off;
- phone is locked;
- StudyNest UI is backgrounded;
- another application is in the foreground;
as far as officially supported Android background execution and sound/DND APIs allow.
IMPORTANT — DO NOT MASK THE PROBLEM
Do not fix this by:
- polling Wi-Fi every second;
- keeping unnecessary wake locks;
- repeatedly waking the device;
- adding aggressive alarms;
- Accessibility Service abuse;
- hidden/private Android APIs;
- simulated user interaction;
- battery-heavy loops.
Investigate why the current departure/sound action is apparently being processed or reflected only when the device wakes.
Inspect:
- connectivity callbacks;
- foreground service lifecycle;
- process/background state;
- Doze/background restrictions;
- existing delayed-departure scheduling;
- sound restoration scheduling;
- DND/Notification Policy access;
- pending callbacks/jobs/alarms if used;
- whether UI lifecycle events are incorrectly triggering reconciliation;
- whether departure state is being persisted but sound restoration waits for UI resume;
- OEM/background restrictions that cannot legitimately be bypassed.
Use the most reliable official Android mechanism appropriate to the existing architecture.
If exact 90-second execution cannot be guaranteed under every Android/OEM power state, document that honestly. However, normal operation must not intentionally depend on opening/waking the screen.
REQUIRED STATE MODEL
Please explicitly verify that the implementation distinguishes these states:
IN_LIBRARY
Library Time accumulating
Actual Study accumulating according to existing rules
Library vibration automation active
↓ Wi-Fi disconnect
DEPARTURE_CONFIRMATION
approximately 90 seconds
protects against tiny Wi-Fi glitches
↓ still disconnected
TEMPORARILY_AWAY / SESSION_GRACE
Library Time frozen
Actual Study frozen
Efficiency frozen
normal/previous sound restored
same session identity retained
waiting only for possible reconnection
↓ reconnect before 30-minute limit
IN_LIBRARY — SAME SESSION
Resume legitimate time accumulation
restore library vibration
no duplicate session
OR
↓ grace expires
SESSION_FINALIZED
finalize using legitimate accumulated durations
do not count away/grace time
Do not confuse session lifetime with Library Time accumulation.
TEST THIS RIGOROUSLY
This is intended to be my final bug-fix update if possible.
Do not rely on one happy-path unit test.
Add/execute focused regression tests including:
Time accounting
1. Disconnect <90 seconds and reconnect → transient Wi-Fi loss handled according to existing intended behaviour.
2. Disconnect >90 seconds → Library Time freezes.
3. Disconnect >90 seconds → Actual Study freezes.
4. Disconnect >90 seconds → Efficiency freezes.
5. Reconnect after 5 minutes → same session resumes, away interval excluded.
6. Reconnect after 10 minutes → same session resumes, away interval excluded.
7. Reconnect just before 30-minute expiry → same session, no duplicate.
8. Remain away beyond grace → one correctly finalized session.
9. Grace interval is not counted as Library Time.
10. Grace interval is not counted as Actual Study.
11. Grace interval does not artificially change Efficiency.
12. Repeated disconnect/reconnect cycles do not double-count or duplicate sessions.
Sound automation
13. Wi-Fi disconnect with StudyNest open → normal sound restores after departure confirmation.
14. Wi-Fi disconnect with StudyNest backgrounded → same.
15. Wi-Fi disconnect with screen off/phone locked → restoration path does not depend on UI resume.
16. Reconnect during grace → library vibration activates again as appropriate.
17. Existing manual sound-override behaviour remains intact.
18. Existing previous-sound-state restoration remains correct.
Interactions
19. minimum session duration;
20. midnight rollover;
21. History;
22. Calendar;
23. session count;
24. Break;
25. Distraction/App Monitoring;
26. foreground notification;
27. manual timer;
28. automatic timer;
29. Wi-Fi + geofence interaction if enabled;
30. process recreation/background reconciliation.
Also run the complete existing regression suite.
PROTECT EVERYTHING ELSE
All other features in the current version are working correctly on my real device.
Do not alter them.
In particular, do not redesign or change:
- Home;
- History UI;
- Calendar UI;
- current Week/Month chart behaviour;
- Week & Month Report;
- App Monitoring search if it is now fixed;
- Goal & Streak;
- minimum session duration;
- Tasks;
- Quotes;
- onboarding;
- permissions UI;
- existing History/session visibility fixes;
- Break reminder;
- midnight rollover;
- current database records;
- existing user settings.
Do not perform unrelated refactoring merely because the code could be cleaner.
BATTERY REGRESSION CHECK
Because this fix involves background/locked-screen behaviour, specifically verify that the solution does not introduce:
- continuous high-frequency Wi-Fi polling;
- per-second database writes;
- unnecessary wake locks;
- repeated UsageStats queries;
- unnecessary foreground-service restarts;
- excessive alarms/jobs;
- unnecessary location requests.
Preserve the battery-conscious architecture.
RELEASE SAFETY
Create the next appropriate version rather than overwriting the currently working release.
Preserve:
- package/application ID;
- signing certificate;
- update compatibility;
- database/user data;
- saved Library Wi-Fi networks;
- App Monitoring selections;
- goals/streaks;
- tasks;
- History/session records;
- all current settings.
Keep the previous stable APK/source artifacts unchanged.
Checkpoint after each major safe step and update STUDYNEST_PROGRESS.md.
FINAL RELEASE GATE
Do not declare the update finished merely because it compiles.
Before producing the final release, explicitly verify and report:
1. Library Time stops/freeze after confirmed departure instead of running for the entire 30-minute grace period.
2. Actual Study stops appropriately.
3. Efficiency remains frozen while temporarily away.
4. The 30-minute period controls session continuity/finalization only.
5. Reconnection within grace continues the same session without counting the away interval.
6. Sound restoration is no longer intentionally dependent on screen wake/UI resume.
7. No duplicate sessions are created.
8. History/Calendar totals remain correct.
9. No destructive database migration occurred.
10. No unrelated working feature was intentionally changed.
11. Full regression suite passes, or every remaining failure is clearly explained before release.
12. Package/signing/update identity matches the previous release.
13. Battery-conscious implementation is preserved.
Then produce:
- signed APK;
- APK ZIP;
- complete source ZIP;
- implementation/testing report;
- updated STUDYNEST_PROGRESS.md;
- release notes;
- explicit list of physical-device tests I still need to perform.
Physical-device verification is mandatory on my side for the locked-screen sound behaviour. Do not claim automated tests prove that specific real-phone behaviour.