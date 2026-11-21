# STUDYNEST — UI REDESIGN + CONFIGURABLE MINIMUM SESSION DURATION UPDATE

I want to make another update to my existing StudyNest Android app.

This update contains:

1. A configurable **Minimum Session Duration** feature.
2. A professional redesign of **Goal & Streak**.
3. The same Goal & Streak improvements in the **installation/onboarding process**.
4. A professional redesign of **Library Wi-Fi**.
5. A professional redesign of **History → Tracked Study / Study Hours graph**.
6. A professional redesign of **History → Calendar**.

I am attaching **FOUR APPROVED UI REFERENCE IMAGES**.

They represent the visual direction I have already reviewed and approved.

---

# CRITICAL — REFERENCE IMAGES

The four attached images are:

### IMAGE 1
**Goal & Streak UI reference**

### IMAGE 2
**Library Wi-Fi UI reference**

### IMAGE 3
**History → Tracked Study / Study Hours graph UI reference**

### IMAGE 4
**History → Calendar UI reference**

These images are **UI DESIGN REFERENCES ONLY**.

Recreate their layout/style using native **Kotlin + Jetpack Compose + Material 3** components.

### DO NOT:

- embed the screenshots in the application;
- use them as backgrounds;
- crop pieces of the screenshots into UI assets;
- replace working Compose UI with static images.

### DO:

- inspect the existing StudyNest theme/components first;
- reproduce the approved hierarchy, spacing, cards, typography, controls and visual treatment natively;
- reuse the existing StudyNest design system/theme where appropriate;
- preserve responsive behaviour;
- preserve light/dark/system theme support;
- maintain accessibility and reasonable touch-target sizes.

The screenshots may contain illustrative values. **Do not hard-code screenshot data.**

Every displayed value must come from StudyNest's actual data/state.

---

# A. PRESERVE THE CURRENT STABLE VERSION FIRST

Before making changes:

1. Identify the current StudyNest version.
2. Save/checkpoint the entire existing project.
3. Do NOT overwrite the previous stable APK/source.
4. Create the next appropriate versionName/versionCode.
5. Preserve package/application ID.
6. Preserve signing/update compatibility.
7. Preserve existing user data.

Use non-destructive migrations if persistent storage changes are required.

This update must install normally over the current StudyNest version without requiring the user to uninstall the app.

---

# B. NEW FEATURE — MINIMUM SESSION DURATION

StudyNest currently allows extremely short library visits to become separate sessions.

For example:

1 minute  
3 minutes  
5 minutes

can appear as separate sessions and inflate the session count.

I do NOT want this.

Add a new user-configurable setting:

# Minimum Session Duration

Default:

**10 minutes**

---

# C. LOCATION OF THIS SETTING

Place this setting inside:

**Settings → Study Tracking → Goal & Streak**

It must visually belong to the redesigned Goal & Streak screen shown in **IMAGE 1**.

It must ALSO appear during the installation/first-run setup inside the existing:

**Goal & Streak**

onboarding/setup step.

Do not create a separate unnecessary onboarding page just for this option if it fits naturally inside Goal & Streak.

---

# D. MINIMUM SESSION DURATION OPTIONS

Provide convenient preset options:

**5 min**

**10 min — DEFAULT**

**15 min**

**20 min**

**30 min**

Also provide:

# Custom / Manual Entry

The user must be able to enter another sensible value manually.

Examples:

**45 minutes**

**46 minutes**

Therefore, do NOT limit users only to the preset buttons.

Validate the manual input properly.

Do not allow:

- zero;
- negative values;
- invalid text;
- obviously impossible/unsafe duration values.

Use a sensible maximum based on the app's existing session architecture.

Clearly display the currently selected value.

Example:

**Minimum session duration: 10 min**

---

# E. WHAT MINIMUM SESSION DURATION MEANS

The selected threshold determines whether a completed library visit qualifies as a separately counted/displayed session.

## Example — threshold = 10 minutes

### 1-minute visit

Library Time:

**+1 minute**

Separate session:

**NO**

Session count:

**NO increase**

---

### 9 minute 59 second visit

Library Time:

**+9m 59s**

Separate session:

**NO**

---

### Exactly 10 minutes

Library Time:

**+10 minutes**

Separate session:

**YES**

Session count:

**+1**

---

### 30-minute visit

Normal qualifying session.

---

# F. VERY IMPORTANT — DO NOT THROW AWAY SHORT VISITS

A visit below the selected threshold must still contribute fully to the appropriate daily aggregate:

# Library Time

Example:

Existing Library Time:

**3h 00m**

Short library visit:

**6 minutes**

Result:

**Library Time = 3h 06m**

but that 6-minute visit must NOT appear as an individual qualifying session.

Do not discard the six minutes.

---

# G. DO NOT COMBINE UNRELATED SHORT VISITS

Example:

Visit 1 = 5 minutes  
Visit 2 = 5 minutes  
Visit 3 = 5 minutes

Threshold = 10 minutes.

Daily Library Time:

**+15 minutes**

Counted sessions:

**0**

Do NOT combine three unrelated short visits and manufacture one artificial 15-minute session.

The threshold applies to each continuous library visit individually.

---

# H. TRACK IMMEDIATELY

Do NOT wait until the minimum duration has elapsed before beginning Library Time.

Existing behaviour should remain:

User enters library → Library Time starts immediately.

Only when the visit finishes should StudyNest determine whether that visit qualifies as a separately counted/displayed session.

Example:

Enter library.

Library Time immediately begins.

Leave after 7 minutes.

Those 7 minutes remain part of Library Time but no qualifying session is created.

---

# I. USE THE THRESHOLD ACTIVE AT SESSION START

When a library visit begins, capture/use the Minimum Session Duration setting active at that time.

Example:

Session starts when threshold = 10 minutes.

During the session, user changes setting to 30 minutes.

That already-running visit should continue using the original **10-minute** qualification threshold.

The new **30-minute** setting should apply to future visits.

This avoids inconsistent session classification.

---

# J. CHANGING THE SETTING MUST NOT REWRITE OLD HISTORY

Changing Minimum Session Duration should apply to:

# FUTURE SESSIONS ONLY

Do NOT retroactively reclassify finalized historical sessions.

Example:

A previous 15-minute session qualified when the threshold was 10 minutes.

Later the user changes the threshold to 20 minutes.

That historical 15-minute session must remain unchanged.

Do not silently rewrite historical statistics.

---

# K. SESSION COUNT

Anywhere StudyNest displays:

**X sessions**

only qualifying sessions based on the threshold active for those sessions should contribute to that count.

Short visits must not inflate:

- Today's session count;
- daily History session count;
- Week/Month statistics where session counts are shown;
- Calendar selected-date session count;
- any other session-count presentation.

---

# L. SESSION INFORMATION

In:

**History → Session Information**

do NOT show a separate normal session card/row for a short visit that did not meet the configured threshold.

Its legitimate time remains incorporated into the appropriate daily Library Time totals.

Do not create an unnecessary "Short Sessions" section.

Keep the UI clean.

---

# M. OTHER TIME METRICS

The minimum-session threshold is primarily a:

**session qualification / session count / session presentation rule**

It is NOT permission to corrupt or discard underlying tracked time.

Preserve legitimate calculations for:

- Library Time
- Actual Study Time
- Break Time
- Distraction Time
- Efficiency

Inspect the current architecture and maintain mathematically correct daily totals.

Avoid double counting.

---

# N. MIDNIGHT CROSSING

Preserve StudyNest's existing midnight/day-splitting behaviour.

A continuous visit must not be treated as two unrelated sessions simply because midnight occurs.

Example:

Enter library:

**11:56 PM**

Leave:

**12:08 AM**

Continuous visit:

**12 minutes**

Threshold:

**10 minutes**

Therefore:

The continuous visit qualifies as a session.

But daily Library Time should still be allocated correctly:

Previous day:

approximately **4 minutes**

New day:

approximately **8 minutes**

The History/day accounting and continuous-session qualification are separate concerns.

Do not break the recently implemented midnight timer/session reconciliation.

---

# O. WI-FI AND GEOFENCE

Minimum Session Duration must work regardless of whether StudyNest detects library presence through:

- configured Library Wi-Fi;
- geofence/location;
- existing combined library-presence logic.

Do not implement separate session thresholds for Wi-Fi and geofence.

Use the existing library-presence source of truth.

---

# P. GOAL & STREAK — PROFESSIONAL REDESIGN

Redesign:

**Settings → Study Tracking → Goal & Streak**

according to **IMAGE 1**.

The existing screen currently contains functionality such as:

- Daily study goal
- preset study goals
- custom study goal
- Minimum daily time for a streak
- Save goals

Preserve these capabilities while improving the presentation.

---

# Q. GOAL & STREAK SCREEN STRUCTURE

The redesigned page should feel like one coherent Study Tracking configuration screen.

Use clear visual sections/cards such as:

## Daily Study Goal

Display the current goal prominently.

Keep useful presets such as:

1h  
2h  
3h  
4h  
5h  
6h  
8h  
10h

Preserve/customize the existing increment/decrement behaviour if still useful.

Provide manual/custom goal entry.

Clearly indicate the selected goal.

---

# R. MINIMUM DAILY TIME FOR A STREAK

Keep:

**Minimum daily time for a streak**

but improve its UI.

Do not force the user to type only into an unattractive raw text field.

Provide a polished control consistent with the rest of the screen.

Most importantly:

# MANUAL ENTRY MUST REMAIN AVAILABLE.

The user must be able to enter an exact custom value.

Examples:

200 minutes  
225 minutes  
250 minutes  
46 minutes

Use integer minutes unless the existing architecture legitimately supports another representation.

Explain concisely:

> A day counts toward your streak after you reach this much actual/tracked study time.

Preserve the existing correct streak semantics.

Do not silently change how streaks are earned unless required by existing architecture.

---

# S. MINIMUM SESSION DURATION UI

Add the new section:

## Minimum Session Duration

Explanation:

> Shorter library visits still count toward Library Time but won't appear as separate sessions.

Provide presets:

5 min  
10 min  
15 min  
20 min  
30 min

Default:

**10 min**

Also provide:

**Custom duration**

where the user can manually enter values such as:

45  
46

minutes.

Clearly show the currently active setting.

---

# T. SAVING GOAL & STREAK SETTINGS

Use one clear primary action such as:

**Save changes**

or the approved equivalent shown in IMAGE 1.

Avoid multiple confusing save buttons unless technically required.

Validate all custom fields before saving.

Show concise inline errors rather than crashing or silently ignoring invalid values.

Persist:

- Daily Study Goal;
- Minimum Daily Time for Streak;
- Minimum Session Duration.

---

# U. ONBOARDING / INSTALLATION — GOAL & STREAK

Apply the same improved Goal & Streak design philosophy during first-run setup.

The onboarding Goal & Streak step must include:

### Daily Study Goal

### Minimum Daily Time for Streak

### Minimum Session Duration

Minimum Session Duration default:

**10 minutes**

Manual/custom entry must also be available during onboarding.

Do not require the user to revisit Settings simply to choose a custom value.

Preserve:

**Skip / Set up later**

where appropriate according to the existing onboarding architecture.

Do not make Goal & Streak configuration a blocker that prevents the user from entering StudyNest.

---

# V. LIBRARY WI-FI — PROFESSIONAL REDESIGN

Redesign:

**Settings → Library Wi-Fi**

according to **IMAGE 2**.

Preserve all existing Wi-Fi functionality.

---

# W. LIBRARY WI-FI HEADER

Use a professional header similar to the approved reference:

# Library Wi-Fi

With concise explanation such as:

> Select the Wi-Fi networks available at your library. When your phone connects to any of these networks, StudyNest can automatically detect that you are in the library.

Do not overfill the screen with technical text.

---

# X. ENABLE WI-FI DETECTION

Provide a clean card containing:

**Enable Wi-Fi detection**

with the existing toggle.

Concise supporting text:

> Detect your library automatically when connected to saved Wi-Fi networks.

Preserve the existing setting/state.

---

# Y. SAVED LIBRARY NETWORKS

Create a professional:

# Saved Library Networks

section.

Show the configured network capacity.

Example:

**2 / 4 networks**

Use the actual configured limit from the code rather than hard-coding 4 if architecture differs.

Each saved network should have its own clean row/card containing:

- Wi-Fi icon;
- SSID/network name;
- useful connection state;
- Rename/Change action if supported;
- Remove action.

---

# Z. CURRENTLY CONNECTED NETWORK — NO DUPLICATION

If the phone is currently connected to one of the saved library networks, show:

**Currently connected**

directly on that network's row.

Example:

**Airtel_Alpha_StudyZone — Currently connected**

### IMPORTANT

Do NOT also create a second large bottom card saying:

**Connected to library — Airtel_Alpha_StudyZone**

That information would be redundant.

The approved IMAGE 2 intentionally removes that duplicate section.

The saved-network row is the single visual source for the connection state.

---

# AA. DO NOT FABRICATE "LAST CONNECTED"

The visual reference may contain text such as:

**Last connected: 2 days ago**

Only show this if StudyNest already stores reliable timestamps or you implement them correctly with a justified, lightweight data model.

Do NOT invent/fabricate last-connected values merely to copy the screenshot.

If that information is not currently available or useful, omit it cleanly.

---

# AB. ADD NETWORK / USE CURRENT WI-FI

Provide two clear actions similar to IMAGE 2:

### Add Wi-Fi Network

Choose/select an available network using the existing legitimate Android flow.

### Use Current Wi-Fi

Add the Wi-Fi network to which the device is currently connected.

Preserve Android-version-specific permission/security restrictions.

StudyNest must NOT connect to Wi-Fi networks on the user's behalf.

---

# AC. PRIVACY INFORMATION

Use a concise professional information card.

Only make statements that are verified against the actual implementation.

Potential statements, IF TRUE after code inspection:

- No Wi-Fi passwords are stored.
- StudyNest never connects to networks on your behalf.
- Any configured saved network can count as your library.
- Up to X library networks can be configured.

Do NOT make privacy claims without checking the code.

---

# AD. SAVE CONFIGURATION

Use one clear primary:

**Save configuration**

button.

Preserve existing configuration persistence.

Unsaved edits must not accidentally be treated as saved configuration unless the existing architecture intentionally autosaves.

---

# AE. HISTORY — TRACKED STUDY GRAPH REDESIGN

Redesign:

**History → Tracked Study / Study Hours**

according to **IMAGE 3**.

The current simple line chart should be replaced/redesigned with the approved professional comparative presentation.

---

# AF. GRAPH PURPOSE

The chart should make the most important StudyNest distinction immediately understandable:

# Actual Study Time

versus

# Total Library Time

This allows the user to visually compare:

**How long I was physically at the library**

against:

**How much of that time was actual study**

Use actual StudyNest data.

---

# AG. CHART TYPE

Use the approved grouped/paired **bar chart** presentation shown in IMAGE 3.

For each day in Week view, display two related values:

- Actual Study Time
- Library Time (total)

Do not use screenshot images for the bars.

Render the chart natively.

Use the existing Compose/charting architecture if suitable, or implement a lightweight native solution consistent with the current project.

Do not introduce a huge dependency merely for this redesign unless genuinely justified.

---

# AH. WEEK / MONTH

Preserve:

**Week**

and

**Month**

switching.

Use a compact professional segmented control.

### Week

Display daily data for the selected week.

### Month

Display an appropriate monthly representation based on the existing History architecture and screen width.

Do not cram 30/31 unreadable bars into the screen.

For Month view, inspect the current behaviour and choose a clear representation consistent with existing semantics—for example daily scrollable data or meaningful weekly grouping—without destroying data precision.

Document the chosen implementation.

---

# AI. PERIOD NAVIGATION

Keep previous/next period navigation.

Example:

**28 Sept – 4 Oct**

with left/right controls.

Do not allow navigation into meaningless unsupported ranges.

Future dates should remain blank rather than falsely appearing as zero-study historical days if that is the existing semantic.

---

# AJ. CHART INTERACTION

When the user taps/selects a day's bar/group, show the exact values.

Example:

**Fri, 2 Oct**

Actual Study: **4h 30m**

Library: **6h 12m**

Where useful, allow access to that day's session information.

The values must come from real data.

---

# AK. CHART SUMMARY

Below the chart, provide compact useful summary metrics similar to IMAGE 3:

### Period total

### Average per day

### Most in a day / Best day

Use real calculations.

Clearly define whether "Period total" represents Actual Study or another metric.

Do not present ambiguous numbers.

If displaying Library total as well, label it explicitly.

---

# AL. REST DAYS AND FUTURE DAYS

Preserve correct semantics:

- genuine past days with no tracked study can display zero;
- future days should remain blank;
- missing data must not be fabricated.

---

# AM. HISTORY — CALENDAR REDESIGN

Redesign:

**History → Calendar**

according to **IMAGE 4 — FINAL APPROVED CALENDAR REFERENCE**.

Important:

The final approved Calendar reference has the earlier extra legend/activity-level strip REMOVED.

Do not reintroduce that removed section.

---

# AN. HISTORY TOP NAVIGATION

Use the professional navigation style shown in the approved History references:

**Tracked study**

**Calendar**

**Session info**

Highlight the selected section clearly.

Preserve existing navigation behaviour.

---

# AO. CALENDAR HEADER

Use:

# Calendar

with concise helper text such as:

> Tap a date to see its study sessions.

Show the current month/year prominently.

Example:

**October 2026**

Provide previous/next month navigation.

Use the device's local date/time-zone rules consistently with the existing app.

---

# AP. CALENDAR CELLS

Each date cell should be clean and readable.

Where enough data exists, display:

- Actual Study Time;
- Library Time.

Use visually distinct but consistent indicators.

Example:

3 October

Actual Study:

**0h 52m**

Library:

**1h 30m**

The selected date should be clearly highlighted.

Do not overload each cell with too much text.

Make the layout responsive for smaller Android devices.

---

# AQ. CALENDAR ACTIVITY INTENSITY

The approved design uses subtle background intensity to make more active days visually distinguishable.

If implementing this:

- derive intensity from actual data;
- use sensible relative buckets;
- maintain accessibility;
- do not rely on color alone for essential information.

### IMPORTANT

The previously generated design contained a separate legend row:

Some activity / Higher activity / Highest activity

The user explicitly requested that this selected area be removed.

Therefore:

# DO NOT DISPLAY THAT EXTRA ACTIVITY-LEVEL LEGEND ROW.

The calendar may retain subtle activity shading, but the removed legend must stay removed.

---

# AR. CALENDAR MONTH SUMMARY

Below the calendar grid, display useful monthly summary metrics similar to the approved reference:

### Total this month

### Daily average

### Best day

If useful, Library total may appear as a secondary value.

Use actual data.

Do not hard-code the sample numbers shown in the reference.

---

# AS. SELECTED DATE DETAILS

Below the calendar, show:

# Selected Date Details

Example:

**3 October 2026**

Actual/Study Time

Library Time

and the appropriate qualifying session count.

Provide access to:

**View all sessions**

or the existing Session Information screen.

Remember:

Session counts must respect the Minimum Session Duration that applied to those finalized sessions.

---

# AT. CALENDAR SHORT VISITS

A short visit below Minimum Session Duration:

- must still affect that day's Library Time;
- must NOT increase the qualifying session count;
- must NOT appear as a normal separate Session Information entry.

Therefore, Calendar Library Time and Calendar session count may legitimately reflect different concepts.

Example:

Library Time = 1h 45m

Qualifying sessions = 1

because some Library Time may have come from short visits.

This is expected.

---

# AU. DO NOT FABRICATE SCREENSHOT VALUES

The screenshots contain sample visual data.

Never hard-code values such as:

56h 25m  
78h 10m  
8h 15m  
26h 42m  
3h 49m

unless the user's actual database produces those values.

All metrics must be calculated from real StudyNest data.

---

# AV. PRESERVE HISTORY DATA

These redesigns must NOT delete or reset existing History.

Preserve:

- existing daily records;
- sessions;
- library visits;
- Actual Study;
- Library Time;
- Break;
- Distraction;
- Efficiency;
- streak history;
- tasks;
- existing settings.

Use non-destructive migrations.

---

# AW. BATTERY REQUIREMENT

This entire update must remain battery-conscious.

The Minimum Session Duration feature requires no new frequent background polling.

Determine qualification from existing:

- arrival/start timestamp;
- departure/end timestamp;
- library-presence events;
- session state.

Do NOT add:

- per-second qualification polling;
- aggressive WorkManager loops;
- unnecessary wake locks;
- repeated Wi-Fi scans;
- repeated location requests;
- repeated UsageStats queries;
- unnecessary database writes.

The four UI redesigns should not meaningfully increase background battery usage.

---

# AX. PERFORMANCE

The History graph and Calendar must remain smooth with a large amount of historical data.

Avoid recalculating the entire database on every Compose recomposition.

Use appropriate ViewModel/state/database aggregation patterns.

Avoid unnecessary recompositions.

---

# AY. ACCESSIBILITY / RESPONSIVENESS

Test the redesigned screens on different screen sizes.

Avoid:

- clipped SSIDs;
- overlapping buttons;
- truncated important labels;
- tiny touch targets;
- unreadable Calendar cells;
- fixed dimensions that break on smaller phones.

Support appropriate font scaling where reasonably possible.

Preserve light/dark/system themes.

---

# AZ. REQUIRED TESTS — MINIMUM SESSION DURATION

Test at least:

### Test 1
Threshold = 10 minutes.

Visit = 1 minute.

Expected:

Library Time +1 minute.

No qualifying session.

---

### Test 2

Visit = 9m59s.

Expected:

Library Time retained.

No qualifying session.

---

### Test 3

Visit = exactly 10m00s.

Expected:

Qualifying session.

---

### Test 4

Visit = 30 minutes.

Expected:

Normal session.

---

### Test 5

Three separate visits:

5m + 5m + 5m.

Expected:

Library Time +15m.

Qualifying sessions = 0.

Do NOT combine them.

---

### Test 6

6-minute visit followed later by 30-minute visit.

Expected:

Library Time = 36 minutes.

Qualifying sessions = 1.

---

### Test 7

Threshold changed from 10 to 20 minutes after old sessions already exist.

Expected:

Historical sessions unchanged.

Future visits use 20 minutes.

---

### Test 8

Threshold changed during active visit.

Expected:

Active visit uses threshold captured when it began.

New threshold applies to future visits.

---

### Test 9

Custom threshold = 45 minutes.

44m59s:

No qualifying session.

45m00s:

Qualifying session.

---

### Test 10

Custom threshold = 46 minutes.

Verify correct boundary behaviour.

---

### Test 11

Continuous 12-minute visit crossing midnight with threshold 10 minutes.

Expected:

One qualifying continuous visit/session.

Daily Library Time split correctly across dates.

---

### Test 12

Phone locked/app backgrounded during visit.

Expected:

Same qualification result after state reconciliation.

---

# BA. GOAL & STREAK TESTS

Verify:

- Daily goal presets;
- custom Daily Goal;
- Minimum Daily Streak Time manual entry;
- Minimum Session Duration presets;
- Minimum Session Duration custom entry;
- persistence after app restart;
- invalid input handling;
- onboarding defaults;
- onboarding custom values;
- Settings values reflect onboarding choices.

---

# BB. LIBRARY WI-FI TESTS

Verify:

- enable/disable detection;
- existing saved networks preserved;
- Add Wi-Fi;
- Use Current Wi-Fi;
- Rename/Change if supported;
- Remove;
- maximum network count;
- Save configuration;
- currently connected indicator;
- no duplicate bottom connection card;
- Wi-Fi detection still works while phone locked according to existing architecture;
- Android permission behaviour preserved.

---

# BC. HISTORY GRAPH TESTS

Verify:

- Week data;
- Month data;
- previous/next period;
- Actual Study bars;
- Library Time bars;
- exact values on selection;
- period totals;
- averages;
- best/most day;
- rest-day zero behaviour;
- future dates blank;
- midnight-split data;
- short visits included in Library Time but not session count.

---

# BD. CALENDAR TESTS

Verify:

- previous/next month;
- selected date;
- Actual Study;
- Library Time;
- session count;
- selected-date details;
- View all sessions;
- month totals;
- daily average;
- best day;
- local timezone/date boundaries;
- midnight crossing;
- short visits;
- future dates;
- empty days;
- no removed activity-level legend.

---

# BE. FULL REGRESSION TEST

After implementation, verify that this update does NOT break:

- automatic library arrival;
- automatic library departure;
- configured Library Wi-Fi;
- geofence/location;
- foreground/background permission handling;
- Allow All The Time/background-location flow;
- App Monitoring;
- Usage Access;
- restricted-settings flow on newer Android;
- automatic timer;
- manual timer;
- Pause/Resume;
- Break tracking;
- 45-minute one-time Break vibration reminder;
- Library Time;
- Actual Study Time;
- Distraction Time;
- Efficiency;
- midnight timer reset;
- midnight daily splitting;
- Home screen;
- Tasks;
- task counts;
- History;
- Calendar;
- Session Information;
- streaks;
- motivational quotes;
- notification/live timer;
- sound/vibration automation;
- restoration of sound state;
- locked-screen operation;
- background operation;
- themes;
- onboarding;
- Permissions & System Access.

---

# BF. ANDROID VERSION TESTING

Verify relevant code paths for supported Android versions, especially:

- Android 13;
- Android 14;
- Android 15;
- Android 16 where supported by the current project.

Do NOT claim physical-device verification unless actually performed on a physical device.

Emulator/unit/instrumented tests are not the same as physical-device testing.

---

# BG. CONTINUOUS SAVE / CHECKPOINT REQUIREMENT

IMPORTANT:

This update may take a long time and the Codex/ChatGPT session may be interrupted, hit capacity, disconnect, or reach a usage limit.

Therefore:

# SAVE PROGRESS AT EVERY REASONABLE SAFE STEP.

After each major stable change:

1. Save modified files.
2. Make sure edits are actually written to disk.
3. Keep the project compilable/recoverable whenever possible.
4. Create/update the existing progress/checkpoint documentation if the project uses it.
5. Do not leave completed work only in temporary unsaved state.

Suggested checkpoints:

### Checkpoint 1
Current stable project backed up.

### Checkpoint 2
Minimum Session Duration data/settings logic complete.

### Checkpoint 3
Goal & Streak redesign complete.

### Checkpoint 4
Onboarding Goal & Streak updated.

### Checkpoint 5
Library Wi-Fi redesign complete.

### Checkpoint 6
History Tracked Study graph redesign complete.

### Checkpoint 7
Calendar redesign complete.

### Checkpoint 8
Data/migration/session qualification tests complete.

### Checkpoint 9
Full regression/build complete.

If interrupted, resume from the latest stable checkpoint rather than restarting completed work.

---

# BH. FINAL BUILD / OUTPUTS

After everything passes:

1. Increment versionName appropriately from the current stable release.
2. Increment versionCode.
3. Preserve package ID.
4. Preserve signing compatibility.
5. Build the final signed APK.
6. Verify it can update over the existing StudyNest installation without uninstalling.
7. Preserve user data.
8. Produce an APK-only ZIP.
9. Produce/update the complete source ZIP/project.
10. Produce an implementation/testing report.

The report should explain:

- Minimum Session Duration implementation;
- default value;
- custom/manual values;
- how short visits are accounted for;
- how session counts are calculated;
- midnight handling;
- Goal & Streak changes;
- onboarding changes;
- Library Wi-Fi redesign;
- graph redesign;
- Calendar redesign;
- database migration if any;
- battery implications;
- tests performed;
- Android-version-specific limitations;
- anything requiring physical-device verification.

---

# FINAL PRIORITY

The purpose of this update is NOT to rewrite StudyNest.

It is to:

1. improve session quality by filtering very short visits from session counts;
2. give the user control over that threshold;
3. preserve every legitimate minute of Library Time;
4. professionally redesign the four approved UI areas;
5. preserve all existing StudyNest functionality and data;
6. avoid additional background battery consumption.

Inspect the existing implementation before changing architecture.

Reuse existing components and sources of truth wherever possible.

If an approved screenshot conflicts with real existing functionality, preserve the functionality and adapt the visual design intelligently rather than deleting working features merely to match the picture.

Do not fabricate data, permissions, privacy claims, or device-testing results.