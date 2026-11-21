STUDYNEST — FINAL POLISH UPDATE AFTER STABLE 1.6.6
IMPORTANT BASELINE
StudyNest 1.6.6 is now the stable real-device baseline.
I have tested 1.6.6 on my real phone and almost all important functionality is now working correctly, including the previously problematic completed-session visibility.
Do NOT broadly refactor, redesign, clean up, optimize, or alter working features.
This update must be strictly limited to the issues and corrections described below, plus changes genuinely necessary to implement/test them.
Preserve:
- package/application ID;
- signing identity;
- existing user data;
- Room database and migrations unless a change is absolutely necessary;
- all working 1.6.6 functionality;
- session/minimum-duration logic except for the explicitly requested Wi-Fi grace-period correction;
- midnight handling;
- App Monitoring tracking logic;
- Usage Access handling;
- Break logic and 45-minute reminder;
- automatic/manual timer behaviour not directly affected by this request;
- Wi-Fi/geofence logic not directly affected by the requested grace-period correction;
- History/Calendar/session-report behaviour not explicitly changed below;
- notification behaviour;
- tasks, streaks, goals and quotes;
- onboarding and permissions;
- all battery optimizations and background behaviour already working in 1.6.6.
Do not regress anything that already works in 1.6.6.
Before making changes, create/save a safe checkpoint of the complete working 1.6.6 source and release artifacts.
1. RESTORE THE 30-MINUTE WI-FI DISCONNECTION GRACE PERIOD
Current regression in 1.6.6
StudyNest now ends/finalizes the current automatic library session almost immediately after disconnecting from the configured library Wi-Fi.
This is undesirable.
A user may temporarily leave the library for food, a short errand, washroom, phone call, etc. and return after only 5–10 minutes.
Earlier StudyNest behaviour allowed a 30-minute temporary Wi-Fi disconnection grace period, which is the behaviour I want restored.
Required behaviour
When an automatic library visit is being tracked through a configured Library Wi-Fi and that Wi-Fi disconnects:
Do NOT immediately finalize the visit/session.
Start a 30-minute departure grace period.
Reconnection within 30 minutes
If the user reconnects to a configured Library Wi-Fi within the 30-minute grace period:
- cancel the pending departure/finalization;
- continue the same library visit/session;
- do not create a second visit;
- do not create duplicate session records;
- preserve the original visit/session start;
- preserve correct accumulated timing;
- continue normal tracking.
Example:
User enters library at 10:00 AM → leaves Wi-Fi at 12:00 PM → reconnects at 12:10 PM.
This should remain one continuous library visit/session, not two separate sessions.
No reconnection within 30 minutes
If the user remains disconnected for the entire 30-minute grace period, finalize the departure/session according to the previously established StudyNest behaviour.
Important: inspect the older working implementation/history before changing accounting semantics. Restore the intended previous 30-minute behaviour rather than inventing a new rule for how Library Time/Actual Study should be attributed during the grace interval.
The final stored end time and time accounting must follow the established correct StudyNest semantics and must not artificially add study/library time merely because finalization was delayed.
Presence logic
Respect the existing combined Wi-Fi/geofence/library-presence architecture. Do not make a Wi-Fi disconnection incorrectly terminate a visit when the existing legitimate library-presence logic says the user is still inside.
Background reliability
The grace period must work when:
- StudyNest UI is closed;
- phone is locked;
- screen is off;
- StudyNest is backgrounded.
Do not implement this with aggressive polling or a continuously running one-second loop.
Preserve battery-conscious/event-driven behaviour.
Required tests
Test at minimum:
- disconnect 5 minutes → reconnect → same session;
- disconnect 10 minutes → reconnect → same session;
- disconnect 29:59 → reconnect → same session;
- remain disconnected beyond 30 minutes → session correctly finalized;
- reconnect after finalization → new visit as appropriate;
- repeated short disconnect/reconnect events → no duplicate visits;
- locked-screen/background scenario;
- configured Wi-Fi switching where applicable;
- geofence/presence interaction;
- minimum-session-duration qualification remains correct;
- History/session count remains correct.
2. FIX APP MONITORING SEARCH
Current bug
In:
Settings → App Monitoring
there is a Search apps field.
I can type an app name into it, but typing does not actually filter/search the installed-app list.
Therefore I still have to manually scroll through the complete list to find an app.
The attached screenshot is proof/reference of the current problem, not an image asset to place inside the application.
Required behaviour
Make the existing search field functional.
As the user types, dynamically filter the currently available App Monitoring app list.
Examples:
Typing:
Whats
should show WhatsApp if installed.
Typing:
Pool
should show 8 Ball Pool if installed.
Requirements:
- case-insensitive;
- partial-name matching;
- trim unnecessary whitespace;
- search by user-visible application name;
- optionally package name as a secondary match if useful;
- results update while typing;
- clearing search restores the complete appropriate list;
- preserve selected/unselected state while searching;
- selecting an app from filtered results must behave exactly like selecting it from the full list;
- existing Show system apps behaviour must continue working;
- existing selected-app count must remain correct;
- existing Save Changes semantics must remain unchanged;
- do not accidentally save temporary changes merely because a search was performed.
Add appropriate tests for filtering, clearing, selected state, system-app toggle interaction and Save Changes.
3. HISTORY CHART MUST OPEN FOCUSED ON THE CURRENT DAY/DATE
Current problem
The History chart retains horizontal scrolling, which I want to keep.
However, when I open History, the chart begins at the start of its data range. This means I sometimes have to manually swipe horizontally to find today's bar.
The attached screenshots show the current behaviour.
Required behaviour
Keep horizontal scrolling. Do NOT remove it.
But whenever the relevant History chart is initially opened or the Week/Month period is changed, automatically position the chart so that the current local day/date is visible when the current period contains today.
Week view
When the current week contains today:
- today's day/bar must be visible immediately;
- preferably position it naturally within the visible chart area rather than hiding it at an edge;
- user must still be able to swipe left/right afterward.
Month view
When the current month contains today:
- today's date/bar must be visible immediately;
- automatically scroll to the appropriate horizontal position;
- preserve manual horizontal scrolling.
Example:
If today is October 20, opening the October Month chart should show the area containing 20 October, rather than starting at October 1 and forcing me to manually swipe to October 20.
Historical periods
If the user deliberately navigates to a previous week/month that does not contain today, do not force the chart back to the current period.
Show that selected historical period normally.
Additional requirements
- use device-local date;
- preserve exact stored data;
- no fabricated values;
- changing Week ↔ Month should focus appropriately;
- reopening History on the current period should make today visible;
- screen rotation/recomposition must not cause disruptive repeated auto-scrolling;
- manual user scrolling after initial positioning must not constantly snap back to today.
Test early/middle/end-of-week and early/middle/end-of-month cases.
4. RESTORE THE 1.6.3 WEEK & MONTH REPORT DESIGN
I want the Week & Month Report presentation restored to the way it appeared/worked in the stable StudyNest 1.6.3 APK/source.
Do not approximate it from memory.
Inspect the preserved 1.6.3 source/project or archived implementation and restore that report presentation as faithfully as reasonably possible.
Preserve compatibility with the current 1.6.6 data model and corrected calculations.
I specifically want the useful Week & Month Report presentation from 1.6.3 rather than the current report presentation shown in my screenshot with items such as:
- Actual Study period total — Unavailable
- Daily average — Unavailable
- Best day — Unavailable
where that presentation differs from the older report I preferred.
Placement
Keep Week & Month Report at the bottom of the current Tracked Study History page, as in the current/new layout.
In other words:
Restore the 1.6.3 Week & Month Report design/content, but keep its placement at the bottom of the current 1.6.6 History layout.
Do not restore unrelated 1.6.3 History UI.
Do not reintroduce:
- the removed top-level Session Information tab;
- redundant duplicate graphs;
- redundant daily information;
- old UI elements that were intentionally removed in 1.6.6.
Only restore the requested Week & Month Report component/presentation.
Use real current data and the current database. Do not hardcode/sample/fabricate values.
5. SCREENSHOTS ARE EVIDENCE / UI REFERENCES ONLY
I am providing screenshots from the real StudyNest app.
Treat them as:
- proof of the current App Monitoring search issue;
- proof/reference of current History chart positioning;
- reference for the current History/report presentation.
Do not import these screenshots into the application.
Implement everything natively using the existing Kotlin/Jetpack Compose/Material 3 architecture.
6. STRICT REGRESSION-PROTECTION REQUIREMENT
This is extremely important.
StudyNest 1.6.6 is currently the best-working version I have tested and may be very close to the final version.
Therefore:
Do not change unrelated working code merely for cleanup, style, architecture, modernization or optimization.
Every modification should be traceable to one of the requested changes above or be strictly necessary to support/test it.
Before editing each major subsystem, inspect the existing implementation and reuse it where possible.
Preserve all 1.6.6 functionality that is not explicitly requested to change.
7. RIGOROUS TESTING BEFORE RELEASE
I want this version tested more rigorously than a normal incremental update because I hope this may become my final major StudyNest update.
Run:
- focused tests for every requested change;
- existing unit tests;
- database/migration tests;
- session lifecycle tests;
- Wi-Fi arrival/departure/reconnection tests;
- minimum-session-duration tests;
- midnight/cross-midnight tests;
- History/session retrieval tests;
- Calendar tests;
- App Monitoring tests;
- Usage Access tests where automated testing is possible;
- Break/distraction accounting tests;
- background/locked-screen simulations where supported;
- notification/timer tests;
- responsive UI tests for normal/small screens and larger font scaling;
- full regression suite;
- release compilation;
- lint/static checks;
- signing/package/version verification;
- update-install/data-preservation checks where tooling allows.
Pay special attention to interactions between:
Wi-Fi grace period ↔ session finalization ↔ minimum session duration ↔ History ↔ Calendar ↔ midnight rollover.
Also verify:
App Monitoring search ↔ selected state ↔ Save Changes ↔ Show system apps.
And:
History auto-positioning ↔ manual horizontal scrolling ↔ Week/Month switching ↔ historical period navigation.
Do not treat automated tests as proof of physical-device behaviour. Clearly distinguish:
- automated/simulated verification;
- physical-device verification still required from me.
8. RELEASE SAFETY
Use the current stable 1.6.6 source as the authoritative baseline.
Create the next appropriate version number/versionCode rather than overwriting 1.6.6.
Preserve:
- package ID;
- signing key/certificate;
- user data compatibility;
- existing database records;
- existing settings;
- selected App Monitoring apps;
- saved Library Wi-Fi networks;
- tasks;
- goals/streaks;
- History/session records.
Do not uninstall, clear, reset or recreate user data as part of the update.
Preserve the final stable 1.6.6 APK/source/archive unchanged so I can roll back or investigate if necessary.
9. CHECKPOINTING
Continue the existing StudyNest checkpoint discipline.
After each safe major step:
- save modified files;
- keep the project compilable/recoverable where possible;
- record what was changed and tested;
- update STUDYNEST_PROGRESS.md;
- do not wait until the entire update is finished before saving.
If usage/session limits become low, prioritize a safe checkpoint and clear resume notes over starting another long operation.
10. FINAL DELIVERABLES
After all requested fixes and regression testing pass, produce:
- final signed APK;
- APK ZIP;
- complete updated source ZIP;
- implementation/testing report;
- updated STUDYNEST_PROGRESS.md;
- concise release notes;
- list of anything that still requires physical-device verification.
Before declaring the release ready, explicitly confirm:
1. App Monitoring search actually filters apps.
2. History Week view initially makes the current day visible.
3. History Month view initially makes the current date visible.
4. Horizontal chart scrolling still works normally.
5. The 1.6.3-style Week & Month Report has been restored at the bottom without restoring unrelated old UI.
6. A temporary Wi-Fi disconnect no longer immediately ends the visit/session.
7. Reconnecting within 30 minutes continues the same visit without duplicate sessions.
8. A genuine departure beyond the grace period still finalizes correctly.
9. Existing 1.6.6 data and settings remain compatible.
10. No unrelated 1.6.6 functionality was intentionally changed.
Please inspect the existing 1.6.6 implementation and preserved 1.6.3 implementation first, checkpoint the stable baseline, and then proceed carefully.