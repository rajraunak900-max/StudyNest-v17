# STUDYNEST — MASTER UPDATE PROMPT
## UI REDESIGN + APP MONITORING PERMISSION FIX + ANDROID 14/15/16 SUPPORT + MIDNIGHT ROLLOVER + BREAK REMINDER + BATTERY INVESTIGATION + ONBOARDING/PRIVACY IMPROVEMENTS

Continue from the **latest completed and working StudyNest project in this existing workspace/conversation**.

Do NOT rebuild StudyNest from scratch.

I am attaching two different types of reference material with this request:

### VIDEOS = PROOF OF REAL-DEVICE PROBLEMS

The attached **VIDEOS** are evidence/proof of the problems and behaviour I am reporting on my physical phones.

Carefully inspect the videos when investigating:

- first-install/setup navigation problems
- App Monitoring permission problems
- behaviour on newer Android versions
- the Google/Android informational page shown while trying to enable the required App Monitoring access
- any other real-device behaviour visible in the videos

Do NOT dismiss these issues merely because automated tests pass.

Also, do NOT blindly assume my explanation of the technical cause is correct. Use the videos as evidence of the behaviour, then inspect the source code and Android APIs to determine the actual cause.

### SCREENSHOTS = UI DESIGN REFERENCES ONLY

The attached **SCREENSHOTS are NOT proof of the bugs above**.

I am providing screenshots/mockups only to demonstrate the **new UI changes/design direction** I want, particularly for:

- Home
- Study Timer
- History
- Break metric
- Tasks shortcut/summary
- related visual improvements

Use screenshots as visual references rather than evidence of Android permission problems.

---

# 1. PRESERVE THE CURRENT STUDYNEST PROJECT

Start from the latest working StudyNest source.

Do NOT:

- rebuild the project from scratch
- redo the entire compatibility migration
- change package/application ID
- change signing identity
- clear app data
- destructively recreate the database
- unnecessarily rewrite working architecture

The new APK must install directly over my current StudyNest installation.

Preserve existing user data.

---

# 2. PRESERVE EXISTING WORKING FEATURES

Preserve:

- Manual Study Timer
- Automatic Study Timer
- Library Wi-Fi detection
- Library/location automation
- Library Time
- Actual Study Time
- Distraction Time
- Efficiency
- App Monitoring
- selected distracting apps
- Usage Access integration
- Pause/Resume
- automatic vibration/sound behaviour
- sound restoration
- locked-screen/background behaviour
- Notifications
- Tasks
- History
- daily aggregation
- Week & Month reports
- Calendar
- Streaks
- Motivational Quotes
- Settings
- themes
- Android 8–16 compatibility

Do not unnecessarily modify features that already work.

---

# PART A — IMPORTANT ANDROID VERSION CLARIFICATION

# 3. NEWER ANDROID VERSIONS GENERALLY WORK

This distinction is extremely important.

StudyNest's normal functions work correctly on my newer Android phone.

Do NOT treat my report as evidence that StudyNest generally fails on Android 14/15/16.

The specific newer-Android problem is:

# APP MONITORING / USAGE ACCESS AUTHORIZATION

App Monitoring requires special permission/access from the user.

On my Android 13 phone this works correctly.

On my newer Android phone, StudyNest does not properly take me to the Android page required to grant this access.

Focus the compatibility investigation on this specific problem.

Do NOT unnecessarily rewrite other working StudyNest features.

---

# PART B — APP MONITORING PERMISSION NAVIGATION

# 4. CURRENT PROBLEM

When I try to configure App Monitoring on the newer Android phone, the flow can open a **Google/Android informational page** explaining the steps required to enable the necessary access.

That is not the experience I want.

The Google page is effectively telling me where I need to go inside Android Settings.

Currently I have to read those instructions and manually find the required page.

---

# 5. TAKE ME TO THE PAGE GOOGLE IS REFERRING TO

Inspect the attached VIDEO showing this behaviour.

Determine exactly:

- what Google/Android informational page is shown
- what access it describes
- what Android Settings page it tells the user to open
- what permission StudyNest actually needs
- what Settings Intent/API is appropriate

Then make StudyNest navigate me **as directly as Android legitimately permits to that actual authorization page**.

I want:

StudyNest  
→ App Monitoring  
→ Grant Access  
→ Correct Android authorization/settings page  
→ I personally enable access  
→ Return to StudyNest  
→ App Monitoring works

NOT:

StudyNest  
→ Google information page  
→ read instructions  
→ manually search Android Settings  
→ locate Special App Access  
→ locate StudyNest  
→ grant access.

---

# 6. NAVIGATION PRIORITY

Use the following priority.

### Level 1 — StudyNest-specific page

If Android officially supports direct navigation to StudyNest's specific authorization page, use it.

### Level 2 — Correct Special App Access category

If Android does not allow direct per-app navigation, open the exact relevant Special App Access category/list.

Then tell the user:

**Select StudyNest and enable Usage Access.**

### Level 3 — OEM fallback

If Nothing OS or another OEM prevents those routes, open the closest legitimate Android Settings destination and provide short instructions.

### Level 4 — Manual instructions

Only use manual instructions if Android genuinely provides no suitable supported Settings destination.

Do NOT send the user to a Google help page when StudyNest can legitimately open the corresponding Android Settings page directly.

---

# 7. DO NOT BYPASS ANDROID SECURITY

The user must personally approve the permission.

Do NOT:

- automatically grant it
- simulate taps
- abuse Accessibility
- use hidden APIs
- use ADB-style workarounds
- scrape Settings screens

Use supported Android APIs only.

---

# 8. AUTOMATIC PERMISSION RECHECK

When I return from Android Settings:

StudyNest should automatically re-check the permission.

If granted:

**Usage Access — Granted ✓**

App Monitoring should become operational.

If not:

**Usage Access required**

[Open Settings]

Do not falsely report App Monitoring as active.

---

# PART C — OTHER FIRST-INSTALL NAVIGATION

# 9. FIX INSTALLATION NAVIGATION

I am still experiencing navigation problems during first-time setup.

Use my attached **VIDEO as evidence**.

For every legitimate permission/system-access request, navigate to the most specific supported Android destination possible.

This may include:

- Usage Access
- Notifications
- Do Not Disturb / Notification Policy Access
- Location
- Background Location
- Battery Optimization
- Wi-Fi/network-related access
- other access genuinely required by StudyNest

Do not request unnecessary permissions.

---

# 10. OEM DIFFERENCES

Handle Android/OEM differences gracefully.

Where direct navigation is impossible:

1. Open the closest relevant page.
2. Explain exactly what the user needs to select.
3. Recheck the permission automatically when the user returns.

---

# PART D — FIRST-LAUNCH WELCOME & PRIVACY

# 11. ADD A PROFESSIONAL WELCOME PAGE

Before technical configuration, show a simple first-launch page.

Example:

# Welcome to StudyNest

StudyNest helps you understand how your study time is actually spent.

Briefly mention:

- Library Time
- Actual Study Time
- Distraction Time
- Study Efficiency
- Tasks
- History
- Streaks

Keep it concise and attractive.

---

# 12. PRIVACY MESSAGE

Before claiming that StudyNest does not collect data, inspect the entire project for:

- analytics
- telemetry
- advertising SDKs
- crash-reporting uploads
- cloud APIs
- remote databases
- network uploads
- third-party tracking
- cloud synchronization

If StudyNest genuinely keeps the relevant information locally, display something like:

# Your study data stays on your device.

"StudyNest processes your study history, tasks and tracking information locally on your phone."

Do NOT make a false absolute privacy statement if any data is transmitted externally.

---

# 13. EXPLAIN APP MONITORING PRIVACY

Briefly explain:

"StudyNest uses Android Usage Access to identify when selected distracting apps are being used so it can calculate focused study time."

If accurate after code inspection, explain that StudyNest does NOT read:

- messages
- conversations
- typed text
- photos
- screen contents

---

# 14. SKIPPABLE

The first page must contain:

**Continue**

and

**Skip**

---

# PART E — SKIPPABLE SETUP

# 15. SETUP MUST NOT TRAP THE USER

Every appropriate optional setup screen should provide:

**Skip**

or:

**Set up later**

Especially:

- Wi-Fi
- library configuration
- location
- Usage Access
- App Monitoring
- notification/system access
- sound/vibration configuration

---

# 16. SKIPPING LIBRARY WI-FI

A user may install StudyNest while at home.

Skipping Wi-Fi must NOT break StudyNest.

The user must still have:

- Manual Timer
- Tasks
- History
- Quotes
- Settings

Library configuration can be completed later.

---

# PART F — APP MONITORING ON NEWER ANDROID

# 17. INVESTIGATE THE REAL DEVICE VIDEO

Investigate:

- permission navigation
- Usage Access state
- UsageStatsManager behaviour
- foreground-app detection
- permission return detection
- Nothing OS behaviour
- Android-version differences

Do not assume Android 13 and newer Android versions expose identical Settings flows.

---

# 18. USE LEGITIMATE APIs ONLY

Do not use:

- Accessibility abuse
- keylogging
- screen scraping
- hidden APIs
- invasive permissions

Use legitimate Android APIs.

Preserve the existing UsageStats-based approach where appropriate.

---

# 19. REVIEW VERSION-SPECIFIC BEHAVIOUR

Review:

- Android 8–12
- Android 13
- Android 14
- Android 15
- Android 16

Do not break the Android 13 implementation while improving newer versions.

---

# PART G — BATTERY INVESTIGATION

# 20. INVESTIGATE THE BATTERY EVIDENCE

On my newer Nothing Phone, Android showed StudyNest associated with approximately **59% battery usage since last charge**.

The phone had been fully charged roughly five minutes earlier and was around 95% when I checked.

Do NOT automatically interpret the 59% figure as StudyNest consuming 59 percentage points of battery.

Investigate what the Android statistic represents.

More importantly, inspect StudyNest for unnecessary background activity.

---

# 21. CHECK FOR

Review:

- wake locks
- foreground services
- aggressive polling
- UsageStats query frequency
- Wi-Fi scanning
- location requests
- unnecessary database writes
- notification update loops
- WorkManager frequency
- duplicate listeners
- service restarts
- screen-off loops

Optimize where necessary.

---

# 22. DO NOT SACRIFICE FEATURES FOR BATTERY

Preserve:

- locked-screen tracking
- automatic library detection
- App Monitoring
- notification timing
- vibration/sound automation

Eliminate unnecessary work rather than disabling functionality.

---

# PART H — AUTOMATIC MIDNIGHT ROLLOVER

# 23. STUDYNEST DAY ENDS AT MIDNIGHT

The StudyNest calendar day must automatically end at:

# 12:00 AM / 00:00 LOCAL DEVICE TIME

I should NOT have to manually press **Finish Day** to close the previous day.

---

# 24. SPLIT CROSS-MIDNIGHT SESSIONS

Example:

Session:

11:30 PM → 12:30 AM

Correct:

Previous day = 30 minutes

New day = 30 minutes

Do not assign the entire hour to one day.

---

# 25. SPLIT ALL RELEVANT METRICS

Correctly split:

- Library Time
- Actual Study Time
- Distraction Time
- Break Time
- daily study totals
- Efficiency
- other date-based statistics where appropriate

An ongoing session should continue seamlessly after midnight.

---

# 26. DO NOT DEPEND ON UI BEING OPEN

The app does not need to be visibly open at exactly 00:00.

If Android delays background execution, reconcile the timestamps later so History still splits at the correct midnight boundary.

---

# PART I — BREAK FEATURE

# 27. BREAK DEFINITION

Break has a specific meaning:

# Break Time = time when I manually press Pause while I am physically inside the library.

Do NOT treat every non-study period as Break.

---

# 28. BREAK START

If:

- I am detected inside the library
- the relevant study/library session is active
- I manually press Pause

then begin accumulating:

**Break Time**

---

# 29. BREAK END

Break stops when:

- I press Resume
- the session stops
- I leave the library
- another legitimate state transition terminates the paused state

---

# 30. BREAK AND DISTRACTION ARE DIFFERENT

### Break

Intentional manual Pause while inside the library.

### Distraction

Time detected from selected distracting apps through App Monitoring.

Keep them separate.

Prevent double counting.

---

# PART J — NEW 45-MINUTE BREAK VIBRATION REMINDER

# 31. ADD ONE-TIME BREAK LIMIT REMINDER

Add a new feature:

If a **continuous manual Break while inside the library reaches 45 minutes**, StudyNest should remind me through vibration.

The reminder must be:

# ONE vibration lasting approximately 2 seconds.

This is a one-time reminder for that continuous Break.

---

# 32. EXACT TRIGGER

Example:

I am studying inside the library.

I manually press:

**Pause**

Break starts.

If I Resume after 20 minutes:

→ No reminder.

If I Resume after 44 minutes:

→ No reminder.

If the Break reaches:

**45 minutes**

→ Vibrate for approximately **2 seconds once**.

---

# 33. DO NOT REPEAT THE REMINDER

This is extremely important.

After the 45-minute reminder fires:

DO NOT vibrate again at:

- 50 minutes
- 60 minutes
- 75 minutes
- every additional 45 minutes
- any recurring interval

For one continuous Break, there should be **only one 2-second reminder**.

---

# 34. RESET FOR NEXT BREAK

Once I Resume studying, the current Break ends.

The reminder state should reset for the **next separate Break**.

Example:

Break 1:

Pause → 45 min → vibrate once → Resume.

Later:

Break 2:

Pause → reaches 45 min → vibrate once again.

Therefore:

# Maximum one reminder per individual continuous Break.

---

# 35. LEAVING LIBRARY

If I leave the library before the Break reaches 45 minutes:

Do NOT vibrate later.

Terminate/cancel the pending Break reminder.

---

# 36. SESSION ENDS

If the session ends before 45 minutes:

Cancel the pending reminder.

Do not produce a delayed vibration.

---

# 37. MUST WORK WITH PHONE LOCKED

The 45-minute reminder should work when:

- StudyNest is not visibly open
- the phone screen is off
- the phone is locked

as far as legitimate Android background-execution rules permit.

Do NOT require me to reopen StudyNest for the reminder to occur.

Use a reliable and battery-conscious Android mechanism appropriate to the existing StudyNest architecture.

Do NOT create a one-second polling loop to check whether 45 minutes has passed.

---

# 38. VIBRATION DURATION

Target vibration duration:

**approximately 2 seconds**

Use the supported Android vibration APIs appropriate for each Android version.

Do not create an unnecessarily complex vibration pattern.

The purpose is simply:

**"Your break has reached 45 minutes."**

---

# 39. RESPECT DEVICE/ANDROID RESTRICTIONS

Do not attempt to bypass:

- system vibration restrictions
- Do Not Disturb rules
- device policy
- Android security restrictions

Use legitimate Android APIs.

If the device cannot vibrate because of system-level restrictions, fail gracefully.

---

# 40. DO NOT ADD REPEATED NOTIFICATIONS

My requirement is specifically:

# ONE VIBRATION REMINDER

Do not automatically turn this into:

- recurring reminders
- notifications every five minutes
- repeated vibration
- alarms
- sound alerts

unless such behaviour already exists separately for another StudyNest feature.

For this new Break feature, the reminder is **one vibration only**.

---

# PART K — HISTORY UI

# 41. REMOVE "ACTUAL STUDY OUTSIDE LIBRARY"

Remove:

**Actual Study outside library**

from the History UI.

Do not destructively delete historical database data merely because the metric is no longer displayed.

---

# 42. BREAK BECOMES A FULL METRIC CARD

Use my attached **SCREENSHOT/UI reference** for this change.

Break should appear as a full metric similar to:

- Library Time
- Actual Study
- Distraction
- Efficiency

For example:

**Library Time**  
04:09:11

**Actual Study**  
03:25:46

**Distraction**  
00:43:27

**Break**  
00:00:00

**Efficiency**  
82.6%

The screenshot is a **UI reference**, not evidence of a bug.

---

# 43. LIBRARY TIME INCLUDES BREAK

A manual Break does not mean I physically left the library.

Therefore Library Time should continue according to physical library-presence semantics.

Actual Study should follow the correct paused-state calculation.

Avoid double subtraction between:

- Break
- Distraction
- Actual Study

---

# PART L — HOME UI REDESIGN

# 44. SCREENSHOTS ARE DESIGN REFERENCES

Use my attached Home **SCREENSHOT/mockup only as a UI design reference**.

Do not interpret it as evidence of Android permission bugs.

---

# 45. KEEP GREEN CARD APPROXIMATELY SAME SIZE

Keep the:

**Today's Tracked Study Session**

green area approximately its current size.

Do not substantially increase its height.

---

# 46. GREEN CARD CONTENT

Keep it compact and useful.

Show:

- Today's tracked study time
- Daily goal/progress
- Current streak
- Best streak
- meaningful Task summary
- View Tasks

---

# 47. ADD VIEW TASKS BUTTON

Add:

**View Tasks**

inside the green card.

It must navigate directly to the existing Tasks tab/screen.

Do not create a duplicate Tasks screen.

---

# PART M — TASK STATISTICS

# 48. VERIFY TASK VALUES

The current Home screen has displayed values similar to:

**3 tasks • 0 due / overdue**

Verify whether those numbers actually come from the Tasks database/model.

Do not display:

- placeholders
- random values
- hard-coded zeros

---

# 49. IF DUE DATES EXIST

If Tasks support due dates:

### Due Today
Incomplete tasks whose due date is today.

### Overdue
Incomplete tasks whose due date is before today.

### Completed
Tasks actually marked completed.

Completed tasks must not count as overdue.

Use the device's local calendar date correctly.

---

# 50. IF DUE DATES DO NOT EXIST

If the existing Tasks feature has no due-date concept:

Remove:

**Due Today**

and:

**Overdue**

Do not display meaningless zeros.

Instead show something meaningful such as:

**3 tasks • 0 completed**

Do not add an unnecessary due-date system merely to preserve those labels.

---

# PART N — STUDY TIMER UI

# 51. PROMINENT TIMER

Inside the Study Timer card, show the current timer prominently near the top.

Example:

# 00:00:00

This must remain below the green Today's Tracked Study Session card.

---

# 52. INCREASE TIMER SIZE

Increase the timer size slightly.

It should be the main visual focus of the Study Timer card.

---

# 53. REMOVE REDUNDANT WAITING-FOR-LIBRARY TIME

Do not show another competing timer in:

**Waiting for your library**

The main Study Timer should be the primary timer.

If Waiting for your library status text remains functionally useful, keep only concise status information where necessary.

---

# 54. COMPACT MANUAL/AUTOMATIC CONTROL

Replace the oversized Manual and Automatic controls with a compact segmented selector.

Example:

[ Automatic | Manual ]

Clearly indicate which mode is active.

---

# 55. PRESERVE TIMER LOGIC

Do not break:

- Manual
- Automatic
- Start
- Pause
- Resume
- Stop
- Re-arm automatic
- automatic library start/stop

The change is primarily a UI improvement unless an actual logic bug is found.

---

# PART O — PERMISSIONS & SYSTEM ACCESS UI

# 56. PROFESSIONAL PERMISSION SCREEN

The Permissions & System Access section should look like a polished consumer application.

Each permission/access card should contain:

- icon
- friendly title
- short explanation
- current status
- appropriate action

Example:

**Usage Access**

Allows StudyNest to identify when selected distracting apps are used.

✓ Granted

or:

**Action required**

[Grant Access]

---

# 57. GROUP ACCESS LOGICALLY

Possible groups:

### Study Tracking
Usage Access

### Library Automation
Location / Wi-Fi

### Sound Automation
Do Not Disturb access

### Background Reliability
Battery optimization

### Notifications
Notification permission

Only include access StudyNest actually uses.

---

# PART P — NOTIFICATION & BACKGROUND EFFICIENCY

# 58. RESPONSIVE SESSION TIMERS

Library Time and Actual Study Time in the active notification should represent the current running session.

Do NOT perform expensive:

- UsageStats
- database
- Wi-Fi
- GPS

queries every second simply to display a timer.

Use elapsed-time/timestamp-based calculations where possible.

---

# PART Q — TESTING

# 59. ANDROID VERSION TESTING

Review/test relevant paths for:

- Android 13
- Android 14
- Android 15
- Android 16

Preserve older supported Android versions.

---

# 60. APP MONITORING TESTS

Verify:

- Usage Access status
- correct Settings navigation
- return-from-Settings detection
- selected apps
- Save Changes
- permission revoked
- permission granted
- monitoring enabled/disabled
- locked screen
- app reopened
- Android-version-specific paths

---

# 61. BREAK REMINDER TESTS

Add tests where practical for:

### Test A
Pause inside library → Resume at 20 min  
Expected: no vibration.

### Test B
Pause inside library → Resume at 44:59  
Expected: no vibration.

### Test C
Pause inside library → reaches 45:00  
Expected: one approximately 2-second vibration.

### Test D
Remain paused for 90 minutes  
Expected: still only one vibration.

### Test E
45-minute reminder fires → Resume → new Break → reaches 45 minutes  
Expected: one new vibration for the new Break.

### Test F
Pause → leave library at 30 minutes  
Expected: pending reminder cancelled.

### Test G
Pause → session ends at 30 minutes  
Expected: pending reminder cancelled.

### Test H
Phone locked during Break → reaches 45 minutes  
Expected: reminder should still occur as far as supported by Android background rules.

### Test I
App UI closed/backgrounded during Break  
Expected: reminder should not depend on reopening StudyNest.

---

# 62. MIDNIGHT TESTS

Test:

- 11:59 PM → 12:00 AM
- 11:30 PM → 12:30 AM
- library session across midnight
- distraction across midnight
- Break across midnight
- app not open at midnight

---

# 63. BREAK CALCULATION TESTS

Verify:

Inside library → manual Pause → Break increases.

Resume → Break stops.

Distracting app → Distraction, not manual Break.

Leaving library → Break terminates.

Prevent double counting.

---

# 64. TASK TESTS

Verify:

- no tasks
- tasks without due dates
- due today
- overdue
- completed
- mixed states

All Home values must be derived from real stored data.

---

# 65. BATTERY/RESOURCE REVIEW

Review:

- foreground services
- WorkManager
- wake locks
- coroutines/loops
- UsageStats frequency
- Wi-Fi frequency
- location frequency
- database writes
- notification architecture
- Break reminder scheduling

The new 45-minute Break reminder must NOT introduce aggressive polling or unnecessary battery drain.

---

# PART R — DATA SAFETY

# 66. PRESERVE EXISTING DATA

Do NOT clear:

- History
- Library visits
- Tasks
- Streaks
- selected apps
- Settings
- library configuration
- statistics

Use non-destructive migration if necessary.

---

# 67. OLD BREAK DATA

Do not fabricate historical Break values if previous versions did not store them.

Keep old History readable.

---

# PART S — CONTINUOUS SAVE / CHECKPOINT REQUIREMENT

# 68. SAVE PROGRESS FREQUENTLY

This project can take a long time and the Codex session/model limit may interrupt the work.

DO NOT wait until everything is finished before saving.

After every stable major change:

1. Save all modified files.
2. Keep completed work written to disk.
3. Keep the project compilable/recoverable where practical.
4. Create logical checkpoints.

Suggested order:

1. Baseline verified
2. Videos inspected
3. Screenshots/UI references inspected
4. Welcome/privacy
5. Installation navigation
6. App Monitoring authorization navigation
7. Android version handling
8. Battery investigation
9. Midnight rollover
10. Break calculation
11. 45-minute Break reminder
12. History UI
13. Home UI
14. Task verification
15. Study Timer UI
16. Permission UI
17. Notification/background review
18. Regression tests
19. Final build

If the model/session stops, previously completed work must remain saved.

---

# PART T — FINAL REGRESSION TEST

Before finalizing, verify that the update has not broken:

- automatic library arrival
- automatic library departure
- Wi-Fi detection
- location/geofence detection
- Manual Timer
- Automatic Timer
- Pause
- Resume
- Break
- 45-minute Break reminder
- Actual Study
- Library Time
- Distraction
- Efficiency
- App Monitoring
- selected-app persistence
- Tasks
- History
- daily aggregation
- Week & Month reports
- Calendar
- Streaks
- Quotes
- Notifications
- vibration automation
- sound restoration
- locked-screen operation
- background operation
- Settings
- light theme
- dark theme

---

# PART U — VERSIONING & FINAL OUTPUT

Inspect the current StudyNest version first.

Increment:

- versionName
- versionCode

appropriately.

Do NOT reuse an existing versionCode.

Preserve:

- application/package ID
- signing certificate
- update compatibility
- existing user data

The APK must install over my existing StudyNest installation without uninstalling it.

Generate:

- final signed APK
- APK-only ZIP
- updated source ZIP
- implementation/testing report
- Android permission findings
- battery investigation findings

Place final artifacts in the existing:

`outputs`

directory.

---

# FINAL PRIORITY ORDER

## Priority 1 — App Monitoring Permission Navigation

All other major functionality works on my newer Android phone.

Fix the specific App Monitoring authorization/navigation problem demonstrated in my VIDEO.

Take me directly to the Android Settings authorization page referred to by Google's instructions wherever officially possible.

## Priority 2 — Installation / Onboarding

Professional welcome/privacy explanation and genuinely skippable setup.

## Priority 3 — Data Correctness

Automatic midnight rollover and correct Break accounting.

## Priority 4 — 45-MINUTE BREAK REMINDER

If one continuous manual Break inside the library reaches 45 minutes:

# VIBRATE FOR APPROXIMATELY 2 SECONDS ONCE.

Do NOT repeat the vibration during that Break.

Reset eligibility only after the Break ends and a new Break begins.

The reminder should work while the phone is locked/backgrounded as far as legitimate Android APIs allow and must be implemented without aggressive polling.

## Priority 5 — Battery

Investigate the physical-device battery evidence and eliminate unnecessary background work.

## Priority 6 — UI

Implement my screenshot-based Home and History UI changes.

Remember:

# VIDEOS = PROOF/EVIDENCE OF THE REAL-DEVICE PROBLEMS.

# SCREENSHOTS = UI DESIGN REFERENCES FOR MY REQUESTED UI CHANGES.

Do not confuse the two.

## Priority 7 — Regression Safety

Preserve everything that already works.

---

# FINAL INSTRUCTION

Do not claim that StudyNest generally fails on newer Android versions.

My other StudyNest functionality works correctly there.

The main newer-Android issue demonstrated in my video concerns **App Monitoring authorization and navigation to the required Android system-access page**.

Do not bypass Android security.

Take the user as directly as officially possible to the correct page and let the user personally grant access.

Similarly, do not claim a physical-device bug is fixed merely because an emulator/unit test passes.

Clearly state in the final report which fixes are:

- verified through automated tests
- verified through build/static analysis
- still awaiting verification on my physical Nothing Phone

Use my VIDEOS as evidence when investigating the reported device behaviour and my SCREENSHOTS only as references for the requested UI redesign.