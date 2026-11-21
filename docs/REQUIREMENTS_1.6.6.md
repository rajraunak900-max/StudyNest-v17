# STUDYNEST 1.6.6 — HISTORY SIMPLIFICATION + CALENDAR FIX + GOAL & STREAK UI ALIGNMENT

StudyNest **1.6.5 is the current stable baseline**.

I want the next update to be:

# StudyNest 1.6.6

This is primarily a **UI/UX simplification update**.

The main goal is:

# MAKE STUDYNEST AS SIMPLE AND EASY TO UNDERSTAND AS POSSIBLE.

StudyNest already collects useful information, but the current History screen has become too complicated and repeats some of the same information in multiple places.

I do NOT want to remove useful tracking capabilities or historical data.

Instead:

- simplify what the user sees;
- remove duplicated information;
- improve information hierarchy;
- preserve detailed information only where it provides additional value;
- make History understandable at a glance.

---

# IMPORTANT — ATTACHED SCREENSHOTS

I am attaching screenshots from the current StudyNest 1.6.5 UI.

These screenshots show the **current implementation and the areas that need improvement**.

They are NOT replacement image assets.

Do NOT embed these screenshots into the application.

Continue using native:

- Kotlin;
- Jetpack Compose;
- Material 3;
- existing StudyNest components/theme.

Inspect the actual current 1.6.5 implementation before making changes.

---

# A. PRESERVE STUDYNEST 1.6.5

Before making changes:

1. Create/save a stable checkpoint of StudyNest 1.6.5.
2. Do NOT overwrite the stable 1.6.5 APK/source artifacts.
3. Preserve the package/application ID.
4. Preserve the existing signing certificate.
5. Preserve update compatibility.
6. Preserve all existing user data.
7. Preserve all existing database/history records.
8. Preserve existing working tracking logic.

Target:

**versionName = 1.6.6**

Increment versionCode appropriately from the current 1.6.5 versionCode.

1.6.6 must install directly over 1.6.5 without uninstalling the app or deleting user data.

---

# B. CORE UX PRINCIPLE — NO UNNECESSARY REPETITION

This is the most important design rule for this update:

# DO NOT SHOW THE SAME INFORMATION TWICE ON THE SAME VIEW UNLESS THE SECOND PRESENTATION PROVIDES GENUINELY NEW INFORMATION.

StudyNest can continue tracking detailed information internally.

But the normal user should see:

**important information first → additional details only when needed.**

Do NOT make History look like an analytics dashboard intended for developers.

It is a personal study app.

The interface should answer simple questions:

- How much did I study?
- How long was I at the library?
- How much time did I lose to distraction?
- How much time did I spend on break?
- Which apps distracted me?
- How many study sessions did I have?

Avoid presenting the same answer in multiple cards/graphs/sections.

---

# C. HISTORY TOP NAVIGATION — REMOVE SESSION INFO BUTTON

The current History top navigation contains three buttons/tabs:

**Tracked Study**

**Calendar**

**Session info**

Remove:

# Session info

from the TOP navigation.

The top History navigation should contain only:

# Tracked Study | Calendar

Reason:

Session Information is already accessible through the Session Report / **View all sessions** action.

Having Session Information both at the top and again elsewhere is unnecessary duplication.

IMPORTANT:

Do NOT delete the Session Information functionality itself.

Only remove its redundant top-level navigation button.

Users must still be able to access detailed sessions through:

# View all sessions

---

# D. MAIN HISTORY — INFORMATION ORDER

Simplify the main Tracked Study view.

The overall information hierarchy should be approximately:

## 1. Study Hours / Tracked Study graph

The existing top graph comparing:

- Actual Study
- Library Time

should remain.

This is the main trend visualization.

---

## 2. Today's main metrics

Show the existing summary metrics:

- Library Time
- Actual Study
- Distraction
- Break
- Efficiency

Keep this as the primary summary for today.

Do not duplicate these values unnecessarily immediately below.

---

## 3. Today's Session Report

Move:

# Today's Session Report

so that it appears **immediately below the main metrics section** containing:

- Library Time
- Actual Study
- Distraction
- Break
- Efficiency.

Include:

# View all sessions

inside/alongside this section.

This makes the flow:

**How much did I study? → How many sessions produced that result?**

---

# E. SIMPLIFY TODAY'S SESSION REPORT

The current report contains too much technical/repetitive information.

Do not repeat values that are already clearly visible in the metrics immediately above.

For example, avoid unnecessarily repeating:

- Actual Study;
- Library Time;
- Break;
- other values already displayed above.

The Session Report should focus primarily on **sessions**.

Use simple wording understandable to a normal user.

For example:

**Today's sessions: 3**

and:

**View all sessions**

If additional session-specific information genuinely adds value, it can remain, but do not fill this card with duplicate time metrics.

Avoid overly technical user-facing terminology where a simpler phrase communicates the same thing.

---

# F. VIEW ALL SESSIONS

The existing:

# View all sessions

functionality must remain.

When opened from Today's Session Report:

show the sessions belonging to today.

When opened from a historical selected date:

show/filter sessions belonging specifically to that selected date.

Do not accidentally show today's sessions when the user requested a historical date.

Continue respecting the Minimum Session Duration rules introduced in 1.6.5.

Visits that did not qualify as separate sessions must not suddenly appear as qualifying sessions here.

---

# G. OTHER TIME METRICS — REMOVE DUPLICATE STUDY DATA

The current:

# Other Time Metrics

graph includes too much information.

It currently includes metrics such as:

- Tracked Study
- Actual Study
- Distraction
- Break

This repeats Study/Library information that is already represented in the main graph.

Remove from this graph:

# Tracked Study

and

# Actual Study

The graph should contain ONLY:

# Distraction

and

# Break

because these are genuinely secondary time metrics that help explain where non-study time went.

---

# H. RENAME OTHER TIME METRICS

Rename:

**Other Time Metrics**

to:

# Break & Distraction

This is clearer and tells the user exactly what the graph represents.

Do not use unnecessarily abstract terminology when a direct label works better.

---

# I. BREAK & DISTRACTION GRAPH — MAIN HISTORY ONLY

The Break & Distraction graph is useful for showing a **trend across multiple days**.

Therefore keep it in the main History/Tracked Study view.

It can continue supporting the appropriate period controls already available, provided they remain useful and uncluttered.

However:

# DO NOT DISPLAY A BREAK & DISTRACTION GRAPH FOR A SINGLE SELECTED CALENDAR DATE.

For one individual day, the user already sees:

- Distraction;
- Break.

A graph showing those same daily values again would be unnecessary repetition.

---

# J. CALENDAR — CURRENT A/L BUG

The current Calendar attempts to show:

- A = Actual Study
- L = Library Time

inside each date cell.

However, on the current 1.6.5 implementation the date cells are effectively showing dots/markers instead of useful time values.

Fix this.

---

# K. KEEP THE EXISTING CALENDAR LEGEND

Keep the current full labels/legend as usual:

# A • Actual Study

# L • Library Time

Do NOT shorten these labels to:

“A = Study”

or similar.

The current full terminology is clear and should remain.

---

# L. CALENDAR CELL VALUES MUST BE COMPACT

There is not enough space inside a Calendar date cell to show full values such as:

**A 3h 50m**

**L 4h 25m**

Do NOT force long hour/minute strings into narrow date cells.

Instead use a compact display.

---

# M. CALENDAR COMPACT TIME FORMAT

For durations of one hour or more, use compact decimal hours where appropriate.

Examples:

Exact:

**3h 50m**

Calendar:

**3.8h**

Exact:

**4h 25m**

Calendar:

**4.4h**

Exact:

**1h 30m**

Calendar:

**1.5h**

For durations below one hour, show minutes.

Example:

**52m**

**8m**

Therefore a date cell may look conceptually like:

**3**

**A 3.8h**

**L 4.4h**

or:

**3**

**A 52m**

**L 58m**

Keep the display compact enough for supported phone sizes.

---

# N. CALENDAR ROUNDING

Use sensible, consistent rounding for decimal-hour Calendar values.

Prefer one decimal place.

The Calendar value is a compact overview.

Do NOT modify the actual stored duration.

When the user selects the date, show the accurate duration using the normal detailed time format.

For example:

Calendar:

**A 3.8h**

Selected-date details:

**Actual Study — 3h 50m**

Calendar is approximate/compact presentation only.

Underlying data must remain exact.

---

# O. CALENDAR EMPTY DATES

For a date with no tracked data:

# SHOW ONLY THE DATE NUMBER.

Do NOT display:

A •

L •

or meaningless placeholders.

Example:

A completely empty date should simply show:

**7**

This will make the Calendar substantially cleaner.

---

# P. CALENDAR FUTURE DATES

Preserve sensible existing behaviour for future dates.

Do not display fake zero activity or fabricated time.

Future dates should remain visually clean/inactive according to the existing Calendar design.

---

# Q. SELECTING A DATE — SIMPLIFIED DATE DETAILS

When the user selects a particular date in Calendar, the information should follow the same simple summary philosophy.

For the selected date show, in this order:

### 1. Library Time

### 2. Actual Study

### 3. Distraction

### 4. Break

### 5. Efficiency

Then:

### 6. Distraction breakdown by app

Then:

### 7. Session Report + View all sessions

That is enough.

Do NOT add extra graphs or repeat these metrics again below.

---

# R. SELECTED-DATE METRICS

The first selected-date section should clearly show:

- Library Time;
- Actual Study;
- Distraction;
- Break;
- Efficiency.

Use the same visual language/components as the equivalent current-day metrics wherever practical.

This consistency is intentional.

The user should not have to learn two different History layouts.

---

# S. SELECTED-DATE DISTRACTION BREAKDOWN

For a selected historical date, show the apps responsible for Distraction just like StudyNest's older History implementation.

Example:

# Distraction breakdown

WhatsApp — 18 min

Instagram — 12 min

YouTube — 7 min

These are examples only.

Use real Usage/App Monitoring data stored by StudyNest.

Do NOT fabricate apps or durations.

---

# T. ZERO DISTRACTION

If the selected date has:

**0 distraction**

then do NOT show an unnecessary empty Distraction Breakdown section.

Hide the breakdown cleanly.

The Distraction metric above can simply show:

**0m**

Do not display a large empty card saying there is no distraction unless required for clarity.

---

# U. SELECTED-DATE BREAK & DISTRACTION GRAPH — REMOVE

Do NOT show the multi-day:

# Break & Distraction

line graph inside the selected-date detail view.

Reason:

The selected-date metrics already show Break and Distraction.

Repeating them in a graph for one day adds no useful information.

The trend graph belongs only to the main multi-day History view.

---

# V. SELECTED-DATE ACTUAL/LIBRARY GRAPH — DO NOT ADD

Do not add another Actual Study / Library Time chart for a selected date.

Those values are already visible in the selected-date metrics.

Again:

# AVOID REPETITION.

---

# W. SELECTED-DATE SESSION REPORT

After the selected-date metrics and optional Distraction Breakdown, show the Session Report for that date.

The title should reflect the selected date instead of incorrectly saying “Today”.

For example:

# 1 October Session Report

or another concise localized date representation consistent with the app.

Show a simple session count.

Example:

**Sessions: 2**

Then:

# View all sessions

View all sessions must open/filter the detailed session list for the selected date.

---

# X. SELECTED-DATE ORDER — IMPORTANT

The selected-date information order must remain:

# Main metrics
↓
# Distraction breakdown by app — only if distraction exists
↓
# Session Report
↓
# View all sessions

Do not place a duplicate graph between these sections.

Do not repeat the same Library/Actual/Break/Distraction values farther down.

---

# Y. GOAL & STREAK — CURRENT ALIGNMENT PROBLEM

The functionality in:

**Settings → Study Tracking → Goal & Streak**

is currently working.

DO NOT rewrite its logic unnecessarily.

The problem is primarily visual:

# THE PRESET TIME BUTTONS ARE NOT ALIGNED PROPERLY.

The attached screenshot shows inconsistent button widths/positions and awkward wrapping.

Fix this professionally.

---

# Z. DAILY STUDY GOAL PRESET ALIGNMENT

The Daily Study Goal presets currently include values such as:

1h

2h

3h

4h

5h

6h

8h

10h

Display them in a clean responsive grid.

On a normal-width phone, prefer a visually balanced arrangement such as:

**1h | 2h | 3h | 4h**

**5h | 6h | 8h | 10h**

All preset buttons in the same grid should have:

- equal width;
- equal height;
- consistent spacing;
- aligned text;
- consistent selected/unselected states.

Do not let each button size itself differently simply because `10h` contains more characters than `1h`.

---

# AA. MINIMUM DAILY TIME FOR STREAK — ALIGNMENT

Apply the same principle to:

# Minimum Daily Time for a Streak

Existing presets may include values such as:

30

60

90

120

180

200

minutes.

Use a balanced grid.

For example:

**30 | 60 | 90**

**120 | 180 | 200**

The exact number of columns may adapt responsively to available width.

The important requirement is:

# BUTTONS MUST ALIGN CLEANLY.

---

# AB. MINIMUM SESSION DURATION — ALIGNMENT

Apply the same alignment treatment to:

# Minimum Session Duration

Existing presets:

5 min

10 min

15 min

20 min

30 min

The layout should look intentional.

Possible responsive arrangement:

**5 | 10 | 15**

**20 | 30**

The second row should not look accidentally broken.

Center or distribute the remaining buttons professionally, or use an adaptive grid that maintains consistent button dimensions.

Do NOT change the underlying Minimum Session Duration behaviour.

---

# AC. CUSTOM INPUTS

Preserve all existing custom/manual entry functionality.

This includes:

- Custom Daily Study Goal;
- Custom Minimum Daily Time for Streak;
- Custom Minimum Session Duration.

Do not remove custom values simply to make the presets align.

The user must still be able to enter values such as:

45 minutes

46 minutes

etc., where currently supported.

---

# AD. RESPONSIVE GOAL & STREAK LAYOUT

Do not hard-code the grid only for one phone width.

Test with:

- smaller Android screens;
- normal phones;
- larger phones;
- larger font/text scaling.

If four columns do not fit safely, adapt to fewer columns while keeping equal button sizes.

The goal is:

**alignment + readability + simplicity**

not forcing a specific number of columns at all costs.

---

# AE. DO NOT CHANGE WORKING GOAL/STREAK LOGIC

This update is primarily fixing presentation.

Preserve existing 1.6.5 behaviour for:

- Daily Study Goal;
- custom goal;
- streak qualification;
- custom streak minimum;
- Minimum Session Duration;
- preset values;
- custom session duration;
- persistence;
- future-session threshold behaviour;
- historical session qualification.

Do not introduce a new migration or rewrite this logic unless genuinely necessary.

---

# AF. PRESERVE MINIMUM SESSION DURATION RULES FROM 1.6.5

Continue preserving the existing 1.6.5 rules:

- default threshold remains as currently configured;
- short visits still contribute to Library Time;
- under-threshold visits do not become separate qualifying sessions;
- separate short visits are not artificially combined;
- exact threshold qualifies;
- custom thresholds remain supported;
- setting changes apply according to the existing finalized logic;
- historical sessions are not retroactively rewritten;
- cross-midnight continuous visits retain correct qualification;
- daily totals remain accurate.

Do NOT regress this feature while simplifying History.

---

# AG. PRESERVE MIDNIGHT LOGIC

Preserve the 1.6.4/1.6.5 midnight behaviour.

Do not break:

- automatic day rollover;
- daily splitting;
- stale timer reconciliation;
- active session continuation across midnight;
- History date allocation;
- automatic timer reset rules.

---

# AH. PRESERVE APP MONITORING / DISTRACTION DATA

Because the selected-date view needs to show:

# Distraction breakdown by app

do not remove or simplify away the underlying per-app distraction information.

Preserve the existing App Monitoring logic and stored data.

Continue respecting:

- selected monitored apps;
- Usage Access;
- distraction timing;
- Actual Study calculation;
- privacy behaviour.

Only change how this information is presented in History.

---

# AI. DO NOT ADD NEW BATTERY WORK

This update should have essentially no meaningful increase in background battery consumption.

These are mostly presentation changes.

Do NOT add:

- new polling loops;
- new foreground services;
- new wake locks;
- new frequent WorkManager jobs;
- new Wi-Fi scans;
- new location polling;
- new UsageStats polling.

Use existing stored History data and existing ViewModel/state flows.

---

# AJ. PERFORMANCE

History should remain smooth even with a large database.

Do not query/recalculate the entire History database unnecessarily on every Compose recomposition.

Reuse existing aggregation/state architecture where possible.

---

# AK. HISTORY SIMPLICITY CHECK

Before considering the History redesign finished, inspect each visible section and ask:

> “Is this information already clearly visible somewhere else on this same screen?”

If YES:

Remove the duplicate presentation unless it genuinely provides a different useful insight.

Examples:

- Actual Study trend already exists in the main graph → don't repeat it in Break & Distraction.
- Break value already exists for a selected date → don't create another daily Break graph.
- Session Information accessible through View all sessions → don't keep a redundant top Session Info tab.
- Library/Actual values visible in selected-date metrics → don't add another selected-date chart.

This simplicity principle is a core requirement, not an optional suggestion.

---

# AL. MAIN HISTORY TARGET STRUCTURE

The main Tracked Study History view should approximately follow:

### History
**Tracked Study | Calendar**

↓

### Study Hours
**Actual Study vs Library Time trend**

↓

### Today's Metrics
**Library Time | Actual Study | Distraction | Break | Efficiency**

↓

### Today's Session Report
**Session count + View all sessions**

↓

### Break & Distraction
**Multi-day trend graph containing ONLY Break and Distraction**

↓

### Existing useful longer-period reporting
Keep genuinely useful Week/Month information if it provides additional information rather than repeating exactly what is already above.

Do not delete useful functionality blindly.

Simplify it according to the no-repetition rule.

---

# AM. CALENDAR TARGET STRUCTURE

Calendar should approximately follow:

### History
**Tracked Study | Calendar**

↓

### Calendar

Legend remains:

**A • Actual Study**

**L • Library Time**

↓

### Calendar grid

Dates with data:

**3**
**A 3.8h**
**L 4.4h**

or:

**3**
**A 52m**
**L 58m**

Dates without data:

**7**

only.

↓

When a date is selected:

### Selected Date Metrics
Library Time  
Actual Study  
Distraction  
Break  
Efficiency

↓

### Distraction Breakdown
Only if distraction exists.

↓

### Selected Date Session Report
Session count  
View all sessions

And STOP.

Do not append repetitive daily graphs after this.

---

# AN. CALENDAR EXACT DATA

Calendar compact decimal values are presentation-only.

Never convert/store underlying data as decimal rounded hours.

Use exact timestamps/durations internally.

Detailed selected-date metrics should continue using precise user-friendly duration formatting.

---

# AO. ACCESSIBILITY

Preserve:

- appropriate touch targets;
- readable text;
- content descriptions where applicable;
- large-font usability;
- screen-size responsiveness;
- light/dark/system themes.

Pay particular attention to the Goal & Streak preset grids under larger font scaling.

Do not fix alignment for normal font size while making the controls unusable with larger text.

---

# AP. REQUIRED HISTORY TESTS

Test at minimum:

### Test 1
History top navigation shows:

Tracked Study  
Calendar

and no top Session Info button.

### Test 2
View all sessions still opens Session Information correctly.

### Test 3
Today's Session Report appears directly below today's main metrics.

### Test 4
Break & Distraction graph contains only:

Break  
Distraction

No Actual Study.

No Tracked Study.

### Test 5
Main Actual Study vs Library Time graph remains correct.

### Test 6
Calendar date with several hours shows compact values.

Example exact 3h50m → approximately 3.8h.

### Test 7
Calendar duration under one hour shows minutes.

Example 52 minutes → 52m.

### Test 8
Empty Calendar date shows only its date number.

No A/L dots/placeholders.

### Test 9
Selecting a historical date shows exact metrics.

### Test 10
Historical date with distraction shows real per-app distraction breakdown.

### Test 11
Historical date with zero distraction hides the unnecessary distraction-breakdown section.

### Test 12
Historical selected date contains no duplicate Break/Distraction daily graph.

### Test 13
Historical selected date contains no duplicate Actual/Library graph.

### Test 14
View all sessions from a historical date opens sessions for THAT DATE.

### Test 15
Under-threshold short visits remain excluded from qualifying session count while their Library Time remains in totals.

---

# AQ. REQUIRED GOAL & STREAK UI TESTS

Verify:

### Daily Study Goal
All preset buttons align consistently.

### Minimum Daily Time for Streak
All preset buttons align consistently.

### Minimum Session Duration
Preset buttons align intentionally and do not look accidentally wrapped.

### Custom fields
All continue functioning.

### Selected state
Correct preset/custom value is visibly represented.

### Rotation/recomposition/restart
Settings remain correct.

### Smaller screen
No clipping/overlap.

### Larger text
No broken controls or inaccessible buttons.

---

# AR. FULL REGRESSION TEST

Do not limit testing to the redesigned screens.

Confirm that 1.6.6 does NOT break existing StudyNest functionality, especially:

- automatic library detection;
- Library Wi-Fi;
- geofence/location;
- automatic timer;
- manual timer;
- Pause/Resume;
- Break tracking;
- 45-minute Break reminder;
- Library Time;
- Actual Study;
- Distraction;
- Efficiency;
- App Monitoring;
- Usage Access;
- per-app distraction tracking;
- sound/vibration automation;
- background/locked-screen behaviour;
- notifications;
- midnight rollover;
- midnight session splitting;
- stale timer reset;
- Minimum Session Duration;
- session qualification;
- Tasks;
- streaks;
- motivational quotes;
- History;
- Calendar;
- Session Information;
- Goal & Streak;
- onboarding;
- permissions/system access;
- themes.

---

# AS. DO NOT CONFUSE AUTOMATED AND PHYSICAL-DEVICE TESTING

Automated tests/emulators are useful but do not claim that real-device behaviour has been verified unless it was actually tested on a physical phone.

Clearly state in the final report which tests were:

- unit/automated;
- emulator/simulated;
- physical-device tests, if any.

Real-device verification can be performed by the user after installing 1.6.6.

---

# AT. CONTINUOUS SAVE / CHECKPOINT REQUIREMENT

IMPORTANT:

Do not wait until the entire update is complete before saving.

Save/checkpoint work after every reasonable stable stage.

Suggested checkpoints:

### Checkpoint 1
StudyNest 1.6.5 baseline preserved.

### Checkpoint 2
History top navigation + main section ordering simplified.

### Checkpoint 3
Today's Session Report repositioned/simplified.

### Checkpoint 4
Break & Distraction graph simplified.

### Checkpoint 5
Calendar compact-value bug fixed.

### Checkpoint 6
Selected-date History simplified + per-app distraction breakdown verified.

### Checkpoint 7
Goal & Streak alignment fixed.

### Checkpoint 8
Regression tests complete.

### Checkpoint 9
Signed 1.6.6 release built and packaged.

If the Codex session is interrupted or reaches a limit, continue from the latest stable checkpoint rather than starting over.

Keep the project compilable/recoverable whenever practical.

---

# AU. FINAL BUILD

After all changes and tests:

1. Set versionName to **1.6.6**.
2. Increment versionCode appropriately from 1.6.5.
3. Preserve application/package ID.
4. Preserve signing certificate.
5. Build the signed release APK.
6. Verify update compatibility over 1.6.5.
7. Do NOT require uninstall.
8. Preserve existing app data.
9. Preserve the stable 1.6.5 artifacts.
10. Produce the final 1.6.6 APK.
11. Produce APK-only ZIP.
12. Produce updated complete source ZIP/project.
13. Produce implementation/testing report.

---

# AV. FINAL REPORT

The final report should clearly state:

- what was simplified in History;
- removal of redundant top Session Info navigation;
- new placement of Today's Session Report;
- simplification of Break & Distraction graph;
- Calendar A/L compact-value fix;
- empty-date behaviour;
- selected-date layout;
- per-app distraction breakdown behaviour;
- Goal & Streak alignment fix;
- whether any database changes were required;
- whether battery/background behaviour changed;
- tests performed;
- number of tests passed;
- signing/update compatibility;
- anything still requiring physical-device verification.

---

# FINAL PRODUCT PRINCIPLE

StudyNest should be:

# POWERFUL UNDERNEATH, SIMPLE ON THE SURFACE.

Do not remove useful tracking simply because the History UI is being simplified.

Instead, expose information progressively.

A normal user should be able to open History and understand it quickly without seeing the same information repeated in multiple graphs/cards.

The final design should prioritize:

**clarity → simplicity → consistency → detail only when requested.**

Do not add additional cards, graphs, statistics, labels, or features beyond this prompt merely because there is available space.

If information is already clearly displayed once, assume it does not need to be displayed again unless there is a strong functional reason.