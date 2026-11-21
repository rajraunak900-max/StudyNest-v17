STUDYNEST — NARROW POST-1.6.8 UPDATE
FINAL CONSOLIDATED PROMPT

IMPORTANT:
StudyNest 1.6.8 is currently a stable working baseline.

Almost all major features are now working correctly on the physical device.

This update must therefore be treated as a SMALL, CONTROLLED,
REGRESSION-SENSITIVE UPDATE.

DO NOT redesign the app.
DO NOT rewrite existing architecture unnecessarily.
DO NOT modify unrelated features.
DO NOT change existing tracking semantics except where this prompt
explicitly requests a change.

==================================================
A. BEFORE MAKING ANY CHANGE
==================================================

Before editing:

1. Read STUDYNEST_PROGRESS.md completely.
2. Inspect the current StudyNest 1.6.8 implementation.
3. Treat the current 1.6.8 source as the authoritative baseline.
4. Preserve package ID.
5. Preserve signing/update compatibility.
6. Preserve all existing user data.
7. Preserve existing Room data and migrations.
8. Create a safe checkpoint before modifications.
9. Keep the project compilable/recoverable at each major step.
10. Do not overwrite or destroy the verified 1.6.8 release artifacts.

SAVE PROGRESS AT EVERY REASONABLE SAFE STEP.

After each stable feature/fix:

- save all modified source files;
- make sure changes are actually written to disk;
- keep a recoverable checkpoint;
- update STUDYNEST_PROGRESS.md when appropriate.

This is important because the Codex/Work session may end or renew
before the complete update is finished.

==================================================
B. CURRENT STABLE BASELINE
==================================================

Current version:

StudyNest 1.6.8

Important existing behaviour that MUST remain intact includes:

- automatic library detection
- configured Wi-Fi detection
- geofence/location behaviour
- automatic vibration behaviour
- normal-sound restoration
- locked-screen/background tracking architecture
- approximately 90-second departure confirmation
- 30-minute visit/session-continuity grace
- legitimate-presence accounting
- Library Time accounting
- Actual Study Time accounting
- Distraction accounting
- Break accounting
- Efficiency calculation
- manual Pause/Resume
- existing 45-minute continuous Break reminder
- App Monitoring
- Usage Access handling
- History
- Calendar
- session qualification/minimum duration
- Week/Month graphs
- goals and streaks
- tasks
- foreground notification
- midnight/day-boundary behaviour
- database migrations
- existing user settings

Do not intentionally change any of these except where explicitly
requested below.

==================================================
CHANGE 1 — END OLD MANUAL PAUSE STATE AFTER THE
30-MINUTE LIBRARY GRACE EXPIRES
==================================================

The actual 1.6.8 implementation has already been inspected.

Current scenario:

1. User is inside the library.
2. Automatic library visit is active.
3. User manually presses Pause.
4. Actual Study stops.
5. Break begins.
6. User leaves the library while still manually paused.
7. Legitimate library presence disappears.
8. Existing Break closes.
9. Departure is confirmed using the existing departure logic.
10. Library/Actual/Efficiency stop accumulating according to the
    existing 1.6.8 accounting.
11. The visit remains pending during the existing 30-minute
    continuity grace.
12. User DOES NOT return within 30 minutes.
13. Old library visit finalizes.
14. User later returns to the library.

Current 1.6.8 behaviour:
The new library visit begins, but the Study Timer can retain the old
manual-paused state.

THIS IS NO LONGER DESIRED.

--------------------------------------------------
NEW REQUIRED BEHAVIOUR
--------------------------------------------------

The existing 30-minute grace period must be the boundary between:

TEMPORARY ABSENCE

and

A COMPLETELY NEW LIBRARY VISIT.

If the user DOES NOT return before the existing 30-minute grace expires:

- finalize the old library visit exactly as currently designed;
- permanently close the old Break;
- cancel the old Break reminder;
- clear/reset the MANUAL PAUSE state associated with the previous
  library visit.

If the user later returns AFTER the grace has expired:

- create a NEW library visit;
- treat it as a fresh automatic library arrival;
- Library Time starts normally;
- Actual Study Time starts normally;
- Study Timer must NOT remain paused because of the previous visit;
- Break Time starts at zero;
- Break must NOT automatically begin;
- Distraction remains governed normally by App Monitoring;
- old Break must not carry into the new visit;
- old Break duration must not contribute toward a future
  45-minute Break reminder.

The user should NOT need to remember to press Resume when returning
after the previous visit has completely expired.

--------------------------------------------------
EXAMPLE
--------------------------------------------------

Inside library:

Study Timer = running

User manually pauses:

Break = 20 minutes

User leaves:

Old Break closes.

User remains outside >30 minutes:

Old visit finalizes.
Old manual Pause state expires.

User later returns:

NEW visit starts.

Library Time = RUNNING
Actual Study = RUNNING
Break = 00:00:00
Distraction = normal App Monitoring behaviour

--------------------------------------------------
RETURN WITHIN 30 MINUTES
--------------------------------------------------

DO NOT accidentally change existing temporary-away behaviour.

If the user returns WITHIN the existing 30-minute grace period,
preserve existing same-visit continuity semantics unless a change is
strictly necessary for correctness.

The requested reset applies specifically to:

departure
→ grace expires
→ old visit finalizes
→ later arrival creates NEW visit.

Keep this separate from temporary absence.

--------------------------------------------------
BREAK REQUIREMENTS
--------------------------------------------------

Once the old visit finalizes:

- old Break is finished;
- old Break reminder is cancelled;
- old Break duration remains stored in History;
- it cannot continue into the next visit;
- it cannot combine with a future Break;
- the next Break starts only when the user manually pauses during
  the new visit;
- the next 45-minute Break reminder starts from zero.

==================================================
CHANGE 2 — IMPROVE APP MONITORING SEARCH
==================================================

Improve the App Monitoring search experience.

The user should be able to find an installed app by typing only
approximately 3 or 4 letters of its visible app name.

Examples:

"wha"  → WhatsApp
"inst" → Instagram
"face" → Facebook
"you"  → YouTube

Requirements:

- case-insensitive;
- partial visible-app-name matching;
- no need to enter the full application name;
- dynamically update results while typing;
- prioritize useful matches.

Preferred ranking:

1. visible app name starts with entered text;
2. visible app name contains entered text;
3. other relevant local matches if already supported.

IMPORTANT:

- match visible application labels, not only package names;
- selected apps must remain correctly selected while filtering;
- clearing search restores normal list;
- Save Changes behaviour must remain intact;
- unsaved/saved selection semantics must remain intact;
- master App Monitoring toggle must remain intact;
- Usage Access behaviour must remain intact;
- installed-app loading must remain intact;
- do not introduce online search;
- do not introduce expensive background processing.

Everything remains local.

==================================================
CHANGE 3 — COLOUR-CODE THE EFFICIENCY NUMBER
==================================================

The Efficiency card currently displays a percentage such as:

47.9%

Keep:

- existing card;
- existing icon;
- existing layout;
- existing typography;
- existing spacing;
- existing overall visual design.

Change ONLY the colour of the main percentage NUMBER according to
efficiency.

Use:

0%–49.9%   = RED
50%–74.9%  = YELLOW / AMBER
75%–100%   = GREEN

Boundary behaviour:

49.9% → RED
50.0% → AMBER
74.9% → AMBER
75.0% → GREEN

Use Material-theme-compatible colours that remain readable in both
light and dark themes.

Do NOT strongly colour the whole card.

The percentage number itself should communicate the state.

Example:

47.9%

should display the "47.9%" value in the low/red state.

IMPORTANT:

Do NOT change the Efficiency calculation.

This is a PRESENTATION-ONLY change.

==================================================
CHANGE 4 — CLEAN UP WEEK & MONTH REPORT
==================================================

Use the user-provided screenshot as a visual reference for the current
Week & Month Report.

IMPORTANT:

The screenshot is a UI reference only.
Do not infer tracking/accounting behaviour from the screenshot.

There is currently redundant information in this report.

--------------------------------------------------
REMOVE FROM UPPER PORTION
--------------------------------------------------

Remove:

"Tracked study: ..."

from the upper portion of the Week & Month Report.

--------------------------------------------------
REMOVE FROM BOTTOM PORTION
--------------------------------------------------

Remove:

"All-time tracked study: ..."

"Daily average: ..."

"Best streak: ..."

from the bottom portion of the Week & Month Report.

These values are already displayed elsewhere around/below the main
History bar graph and should not be unnecessarily repeated.

If explanatory text exists only to support those removed duplicate
values, remove it if it is no longer useful.

--------------------------------------------------
PRESERVE
--------------------------------------------------

Do NOT remove or alter the useful report cards.

Preserve:

- Library Time
- Actual Study
- Distraction
- Break
- Efficiency
- Week/Month switching
- report-period calculations
- History calculations
- graph data
- Calendar data
- session information
- goals/streak calculations

This is UI CLEANUP ONLY.

Do not delete underlying data or calculations merely because duplicate
text is removed.

==================================================
CHANGE 5 — FOREGROUND NOTIFICATION SHOULD DISPLAY
TOTAL DAILY LIBRARY TIME + TOTAL DAILY ACTUAL STUDY
==================================================

Current notification appears to show:

CURRENT SESSION / CURRENT LIBRARY VISIT TIME

plus

TOTAL ACTUAL STUDY TIME.

This makes comparison unnecessarily difficult.

Change the main foreground tracking notification so that it displays:

TOTAL LIBRARY TIME FOR CURRENT DAY

and

TOTAL ACTUAL STUDY TIME FOR CURRENT DAY.

Example:

Library: 03:42:18
Actual: 02:51:07

Both should represent TODAY'S accumulated totals.

--------------------------------------------------
TOTAL LIBRARY TIME
--------------------------------------------------

Use the SAME authoritative Library Time accounting used by the rest
of StudyNest.

It must include:

- previous legitimate visits from today;
- currently active legitimate Library Time.

It must exclude:

- confirmed away intervals;
- the 30-minute continuity grace;
- invalid/non-library time.

If today contains:

Visit 1 = 02:00:00

and current Visit 2 = 00:30:00

notification should show approximately:

Library: 02:30:00

NOT merely:

Library: 00:30:00

--------------------------------------------------
TOTAL ACTUAL STUDY
--------------------------------------------------

Use the SAME authoritative Actual Study accounting used by History
and current daily totals.

Do not create a second independent accounting system for notification.

Reuse existing authoritative state/calculations wherever possible.

--------------------------------------------------
MIDNIGHT
--------------------------------------------------

At the local day boundary, notification totals must follow the
existing StudyNest midnight/day-splitting rules.

Do not allow yesterday's daily total to incorrectly remain displayed
as today's total.

--------------------------------------------------
BATTERY-SAFE LIVE DISPLAY
--------------------------------------------------

The notification must remain useful with the screen locked.

DO NOT introduce:

- per-second database writes;
- per-second UsageStats queries;
- aggressive polling;
- repeated alarms;
- long wake locks.

Continue using Android-rendered chronometer/timestamp mechanisms or
the existing battery-efficient notification architecture wherever
possible.

If the live daily total requires:

stored/finalized daily total
+
current active segment,

combine them without changing underlying tracking semantics.

The notification must not interfere with:

- 90-second departure confirmation;
- 30-minute continuity grace;
- locked-screen sound restoration;
- App Monitoring;
- Break tracking;
- foreground-service reliability;
- battery optimizations from 1.6.8.

==================================================
CHANGE 6 — 45-MINUTE BREAK VIBRATION MUST ALSO
SHOW A CLEAR BREAK-REMINDER NOTIFICATION
==================================================

StudyNest already has an existing behaviour where a continuous manual
Break inside the library reaching approximately 45 minutes produces
a short vibration of approximately 2 seconds.

PRESERVE THAT FEATURE.

However, vibration by itself can confuse the user because they may not
know why the phone vibrated.

Therefore, whenever this 45-minute Break reminder vibration is
successfully triggered, StudyNest must ALSO display a clear
notification explaining the reason.

--------------------------------------------------
DESIRED REMINDER
--------------------------------------------------

Suggested notification:

Title:

Break Reminder

Body:

You've been on a break for 45 minutes. Time to get back to studying.

Equivalent concise wording is acceptable if it fits Android
notification design better, but it must clearly explain that:

- this is a StudyNest reminder;
- the user's continuous Break has reached 45 minutes.

--------------------------------------------------
VERY IMPORTANT — THIS IS A ONE-TIME REMINDER
--------------------------------------------------

For each continuous Break:

- trigger the approximately 2-second vibration ONCE;
- show the Break Reminder notification ONCE.

DO NOT repeat it every minute.
DO NOT repeat it every 5 minutes.
DO NOT repeat it every 45 minutes.

Example:

Pause at 10:00
→ Break begins

10:45
→ approximately 2-second vibration
→ Break Reminder notification appears

11:00
→ no second reminder

11:30
→ no second reminder

The reminder has already been delivered for that continuous Break.

--------------------------------------------------
RESET RULE
--------------------------------------------------

When the user presses Resume:

- current Break ends;
- current reminder eligibility ends.

If the user later manually pauses again:

- that is a NEW continuous Break;
- its 45-minute timer starts from zero;
- it becomes eligible for one new vibration + one new notification.

--------------------------------------------------
LEAVING LIBRARY
--------------------------------------------------

If the user leaves the library before the 45-minute Break reminder:

- close/cancel the Break according to existing presence rules;
- cancel its pending reminder;
- do not vibrate later while outside;
- do not display a delayed Break Reminder while outside.

If the old visit completely expires after the 30-minute grace:

- old reminder must remain cancelled;
- old Break minutes must not transfer to a new visit.

When the user later returns:

- no old Break reminder;
- no old Break state;
- no old Break minutes carried forward.

--------------------------------------------------
BACKGROUND / LOCKED SCREEN
--------------------------------------------------

The reminder should work using legitimate Android background
mechanisms when the phone is locked, subject to normal Android/OEM
background restrictions.

It must NOT require:

- opening StudyNest;
- waking the display manually;
- navigating to Home;
- keeping the Activity alive.

Do not use:

- aggressive repeated alarms;
- accessibility-service hacks;
- hidden APIs;
- security bypasses;
- continuous polling;
- long wake locks.

Reuse the existing 45-minute reminder scheduling architecture wherever
possible.

--------------------------------------------------
NOTIFICATION RELATIONSHIP
--------------------------------------------------

IMPORTANT:

Do NOT replace or corrupt the normal persistent StudyNest foreground
tracking notification.

There are two conceptually different notifications:

1. NORMAL TRACKING NOTIFICATION

Shows:

Library: today's total Library Time
Actual: today's total Actual Study

This remains the persistent tracking notification.

2. BREAK REMINDER NOTIFICATION

A one-time event notification when a continuous Break reaches
45 minutes.

Shows something like:

Break Reminder
You've been on a break for 45 minutes. Time to get back to studying.

Keep these responsibilities separate.

--------------------------------------------------
NOTIFICATION CHANNEL / BADGE
--------------------------------------------------

Reuse an appropriate existing StudyNest reminder notification channel
if one already exists.

Do not create unnecessary duplicate channels.

Do not reintroduce the previous persistent app-icon notification-badge
problem.

Review existing channel configuration before creating/changing any
channel.

Do not unnecessarily alter foreground-service notification importance.

--------------------------------------------------
TAPPING THE BREAK REMINDER
--------------------------------------------------

If the existing notification architecture safely supports it, tapping
the Break Reminder should open StudyNest to the appropriate Home /
active-session screen.

Do NOT add complicated navigation solely for this feature.

==================================================
STRICT NON-REGRESSION REQUIREMENT
==================================================

StudyNest 1.6.8 is currently working correctly for almost all major
features.

DO NOT make unrelated changes.

Preserve the verified departure model:

NORMAL LIBRARY PRESENCE

→ departure signal

→ existing departure confirmation

→ legitimate Library/Actual accounting freezes at the appropriate
  original cutoff

→ temporary 30-minute continuity grace

→ reconnect within grace = same visit

OR

→ grace expires = finalize old visit

→ later return = NEW visit.

Do NOT reintroduce the previous bug where Library Time continued
accumulating throughout the 30-minute grace.

Do NOT change:

- Wi-Fi monitoring frequency;
- location request frequency;
- UsageStats polling frequency;
- foreground-service architecture;
- sound scheduler architecture;
- wake-lock duration;
- departure timing;
- grace duration;
- database History values;
- session qualification rules;

unless absolutely necessary for a requested change.

If touching one of these areas is unavoidable, document exactly:

- what was touched;
- why;
- what previous behaviour remains preserved.

==================================================
DATABASE / UPDATE SAFETY
==================================================

This must remain an IN-PLACE UPDATE.

Preserve package:

com.studynest.app

Preserve existing signing certificate.

DO NOT:

- uninstall existing app;
- clear application data;
- reset settings;
- delete History;
- delete visits;
- delete Study Timer records;
- delete Break records;
- reset App Monitoring selections;
- reset Wi-Fi configuration;
- reset Goal/Streak settings.

Avoid a Room schema change unless genuinely necessary.

If a schema change is required:

- use an explicit non-destructive migration;
- preserve all existing rows;
- explain exactly why the migration was necessary.

==================================================
REQUIRED AUTOMATED TESTS
==================================================

Add focused regression coverage for all requested changes.

--------------------------------------------------
MANUAL PAUSE / NEW VISIT
--------------------------------------------------

Test:

1. User inside library.
2. Study running.
3. Manual Pause.
4. Break begins.
5. User leaves.
6. Break closes.
7. Existing departure confirmation occurs.
8. User remains outside >30 minutes.
9. Old visit finalizes.
10. Old manual-pause state expires.
11. User returns.
12. New visit created.
13. Library Time starts.
14. Actual Study starts automatically.
15. Break starts at zero.
16. Old Break does not carry forward.
17. Old reminder cannot fire.
18. New manual Pause creates a completely new Break.
19. New Break receives fresh 45-minute eligibility.

Also test:

20. Return WITHIN grace remains same-visit behaviour.
21. Away time excluded from Library Time.
22. Away time excluded from Actual Study.
23. Away time excluded from Break.
24. Away time excluded from Distraction.
25. Efficiency uses legitimate totals.
26. midnight behaviour remains correct.
27. session qualification remains correct.
28. History/Calendar remain correct.

--------------------------------------------------
APP MONITORING SEARCH
--------------------------------------------------

Test:

29. "wha" matches WhatsApp.
30. "inst" matches Instagram.
31. "face" matches Facebook if installed/test fixture exists.
32. "you" matches YouTube if installed/test fixture exists.
33. matching is case-insensitive.
34. partial visible-label matching works.
35. selected states survive filtering.
36. clearing search restores list.
37. Save Changes semantics remain correct.

--------------------------------------------------
EFFICIENCY COLOUR
--------------------------------------------------

Test:

38. 49.9% → low/red.
39. 50.0% → medium/amber.
40. 74.9% → medium/amber.
41. 75.0% → high/green.
42. efficiency calculation itself remains unchanged.

--------------------------------------------------
WEEK / MONTH REPORT
--------------------------------------------------

Test:

43. upper "Tracked study" duplicate absent.
44. lower "All-time tracked study" duplicate absent.
45. lower "Daily average" duplicate absent.
46. lower "Best streak" duplicate absent.
47. useful Week/Month metrics remain present.
48. report calculations remain unchanged.

--------------------------------------------------
MAIN TRACKING NOTIFICATION
--------------------------------------------------

Test:

49. notification Library value represents TODAY'S TOTAL Library Time.
50. notification Actual represents TODAY'S TOTAL Actual Study.
51. multiple same-day visits aggregate correctly.
52. confirmed away intervals excluded.
53. 30-minute grace does not inflate Library Time.
54. midnight starts correct new-day notification totals.
55. no per-second DB writes introduced.

--------------------------------------------------
45-MINUTE BREAK REMINDER
--------------------------------------------------

Test:

56. Manual Pause starts Break.
57. At <45 minutes, no reminder.
58. At approximately 45 minutes, reminder becomes eligible.
59. Existing approximately 2-second vibration occurs once.
60. Break Reminder notification appears once.
61. notification clearly identifies the 45-minute Break.
62. continuing same Break does not produce another vibration.
63. continuing same Break does not produce another reminder notification.
64. Resume resets eligibility.
65. New later Break can generate one new reminder.
66. Leaving library before threshold cancels pending reminder.
67. Remaining outside cannot trigger delayed reminder.
68. >30-minute departure/finalization permanently cancels old reminder.
69. Returning in a new visit does not inherit old Break minutes.
70. New Break after returning starts reminder timing from zero.
71. reminder delivery does not replace/corrupt persistent tracking notification.
72. reminder channel does not reintroduce persistent app-icon badge issue.

Where practical, test reminder scheduling/background path without an
Activity being present.

==================================================
FULL REGRESSION
==================================================

Run ALL existing 1.6.8 tests in addition to the new tests.

Do not delete, weaken, skip or rewrite existing tests simply to obtain
a passing result.

If an existing test conflicts with an explicitly requested new
behaviour, document:

- old expectation;
- new requested expectation;
- why the test was intentionally updated.

Do not hide failures.

==================================================
BATTERY / PERFORMANCE GUARD
==================================================

Compare the final implementation against 1.6.8.

This update should NOT introduce:

- new continuous polling loops;
- new foreground services;
- repeated alarms;
- per-second database writes;
- unnecessary UsageStats queries;
- additional continuous location requests;
- long wake locks;
- unnecessary Wi-Fi scans.

The new Break Reminder notification should reuse the existing reminder
event whenever possible rather than creating another independent
background timer.

If battery architecture changes at all, document exactly what changed
and why.

==================================================
VERSION / RELEASE
==================================================

If and only if:

- requested changes are complete;
- focused tests pass;
- full regression passes;
- build succeeds;
- lint has no new errors;
- signing/update identity is verified;

prepare the next StudyNest release after 1.6.8.

Do not overwrite the verified 1.6.8 artifacts.

Keep 1.6.8 available as a rollback baseline.

Generate:

- signed APK;
- APK ZIP if existing release workflow uses one;
- source archive;
- verification/test report;
- updated STUDYNEST_PROGRESS.md;
- concise implementation report.

==================================================
PHYSICAL DEVICE ACCEPTANCE ITEMS
==================================================

Clearly identify anything that automated testing cannot fully prove.

At minimum, physical testing should verify:

1. Install over 1.6.8 WITHOUT uninstalling.
2. Existing History/settings remain intact.
3. App Monitoring 3–4 letter search.
4. Efficiency colours in real light/dark themes.
5. Week/Month Report cleanup.
6. Main notification shows TOTAL daily Library + Actual.
7. Notification remains correct after multiple library visits.
8. Manual Pause → leave >30 min → return starts a fresh active study state.
9. Old Break does not return.
10. Fresh Break starts from zero.
11. At 45 minutes of a continuous Break:
    - approximately 2-second vibration occurs;
    - Break Reminder notification appears;
    - notification explains why the phone vibrated.
12. Leave the phone locked around the 45-minute threshold and verify
    background delivery.
13. Verify no repeated Break reminder after the first one.
14. Verify normal battery behaviour.

==================================================
FINAL REPORT
==================================================

Before declaring the release complete, report:

1. Files changed.
2. Exact behaviours changed.
3. Exact behaviours deliberately left unchanged.
4. Manual-Pause/new-visit implementation.
5. App Monitoring search implementation.
6. Efficiency colour implementation and thresholds.
7. Week/Month Report cleanup.
8. Total daily Library/Actual notification implementation.
9. 45-minute Break vibration + notification implementation.
10. Break reminder cancellation/reset behaviour.
11. Database/migration status.
12. Battery/performance impact.
13. Number of retained previous tests passed.
14. Number of new tests added/passed.
15. Full regression result.
16. Lint result.
17. Build result.
18. Package/version/signing verification.
19. Any unresolved limitations.
20. Exact physical-device acceptance steps still required.

MOST IMPORTANT:

DO NOT declare success merely because the app compiles.

The priority is:

PRESERVE THE CURRENTLY WORKING 1.6.8 FEATURES

while making ONLY the explicitly requested changes above.