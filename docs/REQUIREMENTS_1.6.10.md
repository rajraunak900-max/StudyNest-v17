STUDYNEST v1.6.10 — STRICT, NARROW BUG-FIX UPDATE

IMPORTANT BASELINE
==================================================

Use the current stable StudyNest v1.6.9 as the baseline.

StudyNest v1.6.9 is working very well on my real device. All other major
features are currently working correctly.

This update is ONLY for the three issues described below.

DO NOT redesign, refactor, optimize, clean up, rename, restructure, migrate,
or otherwise change unrelated working features.

Before changing anything:
1. Inspect the existing v1.6.9 implementation.
2. Identify the exact source of each issue.
3. Preserve all existing data and behavior.
4. Make the smallest safe production-code changes necessary.
5. Save/checkpoint the project before and after each stable fix.


==================================================
BUG 1 — DAILY GOAL IS USING THE WRONG TIME
==================================================

PROBLEM:

On the Home screen, the Daily Goal progress is currently behaving as though:

    Goal progress = Actual Study Time + Distraction Time

This is incorrect.

For example:

If:
    Actual Study Time = 2 hours
    Distraction Time = 30 minutes

the Daily Goal must receive only:

    2 hours

NOT:

    2 hours 30 minutes


REQUIRED BEHAVIOR:

Daily Goal progress must be based ONLY on authoritative:

    Actual Study Time

Distraction Time must NEVER contribute toward completing the Daily Goal.

The goal percentage, progress bar, completed/not-completed state, displayed
progress value, goal-completion logic, and any related goal/streak logic that
depends on the daily goal must all use the correct Actual Study Time value.

Do not merely change the displayed number while leaving the underlying goal
calculation wrong.

Investigate where the incorrect value is coming from and fix the source.

IMPORTANT:

Do not change the definition of Actual Study Time.

Continue using the existing authoritative StudyNest accounting rules:

- Library Time = legitimate detected presence time.
- Actual Study Time = genuine tracked focus/study time according to the
  existing StudyNest monitoring/accounting rules.
- Distraction Time = time detected in selected monitored distracting apps.
- Break Time = existing manual break accounting.

Distraction Time must not count toward the Daily Goal.


==================================================
BUG 2 — HOME STUDY TIMER DISPLAYS WRONG TIME
==================================================

PROBLEM:

The main "Study Timer" on the Home screen currently appears to display:

    Actual Study Time + Distraction Time

I want this timer to display ONLY:

    Actual Study Time


EXAMPLE:

If today/session currently contains:

    Actual Study Time = 1h 40m
    Distraction Time = 20m

the Home Study Timer must show:

    1h 40m

NOT:

    2h 00m


REQUIRED BEHAVIOR:

The Home Study Timer must use the same authoritative Actual Study Time
accounting already used by StudyNest.

When a monitored distracting app causes Actual Study Time to stop according
to the existing App Monitoring rules, the Home Study Timer must also stop
increasing for that period.

When Actual Study Time resumes, the Home Study Timer should resume normally.

Do not remove or alter Distraction Time itself. Distraction Time must still
be recorded and displayed in the appropriate History/metrics areas.

This is an accounting/display-source correction, not a request to change the
existing App Monitoring behavior.

Also verify that the live timer, notification, History Actual Study value,
and Home Actual Study value remain logically consistent with the existing
authoritative accounting rules.

Do NOT change Library Time accounting.


==================================================
BUG 3 — "HOW APP MONITORING WORKS" DOES NOTHING
==================================================

PROBLEM:

In:

    Settings → App Monitoring

the search field is now working correctly.

Typing a few letters successfully filters/suggests installed apps as intended.

However, directly below the app-search area there is a UI section/card/row
labelled approximately:

    "How App Monitoring Works"

Currently, tapping this section appears to do nothing.

Investigate this before changing it.


STEP 1 — DETERMINE INTENDED BEHAVIOR

Inspect the existing v1.6.9 code and determine whether this element:

A) was intended to be clickable and open/expand some explanation,

OR

B) was intended only as a non-clickable informational heading/section.


STEP 2 — FIX ACCORDING TO THE ORIGINAL INTENT

If it is intended to be clickable:

Make it work properly.

It should show a short, clear explanation of App Monitoring, for example:

- StudyNest can monitor selected apps during an active study session.
- Time spent in selected distracting apps is recorded as Distraction Time.
- That distracting period does not count as Actual Study Time.
- Usage Access may be required for this feature.
- StudyNest uses this information for local study-time tracking.

Use the existing UI style/design language.

This can be an expandable section, dialog, bottom sheet, or existing intended
implementation — prefer whatever the current code/design was already trying
to do.

Do NOT unnecessarily redesign the App Monitoring page.


If it was intentionally non-clickable:

Then investigate why its visual design makes it look interactive.

Make the smallest UI correction necessary so users are not presented with
what appears to be a broken button/card.

Do not invent a large new feature.


==================================================
CRITICAL NON-REGRESSION REQUIREMENT
==================================================

Everything else in v1.6.9 is currently working correctly on my real phone.

DO NOT interfere with unrelated features.

In particular, preserve the existing behavior of:

- Wi-Fi library detection
- Geofencing/location detection
- Automatic arrival/departure
- ~90-second departure confirmation
- 30-minute session-continuity grace
- Library Time accounting
- confirmed-away interval exclusion
- automatic vibration mode
- sound restoration after leaving
- locked-screen/background behavior
- manual and automatic timer modes
- manual Pause/Resume
- Break Time accounting
- 45-minute Break reminder
- break vibration
- separate Break Reminder notification
- App Monitoring app selection
- short-name App Monitoring search
- Usage Access handling
- distraction detection/accounting
- Efficiency calculation and colors
- foreground tracking notification
- today's Library/Actual notification totals
- History
- graphs
- Calendar
- Week & Month Report
- session qualification/minimum duration
- goals configuration
- streak configuration
- tasks/to-do list
- motivational quotes
- onboarding/setup
- saved Wi-Fi networks
- themes
- permissions
- existing notification channels
- database/history
- all existing settings

Do not introduce new polling, aggressive wake locks, unnecessary background
services, frequent database writes, Wi-Fi scans, location requests, or
UsageStats queries.

Do not modify the database schema unless inspection proves it is genuinely
necessary. These bugs appear to be calculation/UI-source problems, so a
schema migration should NOT be added without a strong technical reason.


==================================================
DATA SAFETY
==================================================

This must install directly over StudyNest v1.6.9.

DO NOT:

- clear existing data
- delete History
- reset settings
- remove saved Wi-Fi networks
- reset App Monitoring selections
- reset goals/streaks
- reset tasks
- repair/rewrite historical records unnecessarily

Preserve:

    package ID: com.studynest.app

Preserve the same signing identity/certificate used by v1.6.9.

Use:

    versionName = 1.6.10

Increment versionCode correctly from v1.6.9's versionCode 19.


==================================================
REQUIRED TESTS
==================================================

Add focused regression tests for these fixes.


TEST 1 — GOAL EXCLUDES DISTRACTION

Example:

Daily Goal = 2 hours
Actual Study = 1h 45m
Distraction = 30m

Expected:

Goal progress = 1h 45m
Goal is NOT completed.

It must NOT use 2h 15m.


TEST 2 — GOAL COMPLETION

Daily Goal = 2 hours
Actual Study = exactly 2h
Distraction = any amount

Expected:

Goal completed based on Actual Study alone.


TEST 3 — HOME TIMER

Actual Study = 50m
Distraction = 20m

Expected Home Study Timer:

    50m

NOT:

    1h 10m


TEST 4 — LIVE DISTRACTION

Start with Actual Study running.

Enter a selected monitored distracting app.

Expected:

- Distraction Time increases according to existing behavior.
- Actual Study stops increasing according to existing behavior.
- Home Study Timer therefore stops increasing.
- Daily Goal progress does not increase from the distraction period.

Return to allowed/study state.

Expected:

- Actual Study resumes.
- Home Study Timer resumes.
- Goal progress resumes from Actual Study.


TEST 5 — LIBRARY TIME NON-REGRESSION

During the same distraction period while legitimately inside the configured
library/class/office:

Library Time must continue according to existing v1.6.9 behavior.

Do not accidentally make Library Time follow the Home Study Timer.


TEST 6 — APP MONITORING INFORMATION SECTION

Verify the intended interaction for "How App Monitoring Works".

If clickable:
- tap works
- explanation is visible
- closing/collapsing works
- no crash
- state/search/app selections remain intact

If intentionally non-clickable:
- verify the corrected presentation no longer falsely appears to be a broken
  interactive control.


==================================================
FULL REGRESSION
==================================================

After the focused fixes:

1. Run all existing v1.6.9 tests.
2. Keep all previous regression tests.
3. Do not delete or weaken tests merely to obtain a passing result.
4. Run the new focused tests.
5. Run lint/build/release verification.
6. Verify package/signing/update compatibility.
7. Verify the release APK can update v1.6.9 without data loss.

If an existing test fails because the old test explicitly expected the buggy
behavior, explain this clearly before updating that expectation.

Do not silently change unrelated test expectations.


==================================================
FINAL REPORT
==================================================

When finished, report clearly:

1. Root cause of Bug 1.
2. Exact fix for Bug 1.
3. Root cause of Bug 2.
4. Exact fix for Bug 2.
5. What "How App Monitoring Works" was originally intended to do.
6. What was changed for Bug 3.
7. Exact production files changed.
8. Exact tests added/changed.
9. Full regression result.
10. Lint/build result.
11. APK versionName/versionCode.
12. Confirmation package ID is unchanged.
13. Confirmation signing certificate matches v1.6.9.
14. Confirmation existing database/history/settings are preserved.
15. Any limitations requiring real-device testing.


==================================================
CONTINUOUS CHECKPOINT REQUIREMENT
==================================================

Do not wait until the entire update is finished before saving work.

After each stable fix:

- save all modified files
- checkpoint the completed work
- keep the project compilable/recoverable whenever possible
- do not leave important completed work only in temporary/unsaved state

If the Codex/Work session ends or renews, I should be able to continue from
the latest safe checkpoint.


FINAL PRIORITY
==================================================

StudyNest v1.6.9 is currently stable on my real device.

The purpose of v1.6.10 is ONLY to:

1. Make Daily Goal use Actual Study Time only.
2. Make the Home Study Timer display Actual Study Time only.
3. Fix/clarify the apparently non-working "How App Monitoring Works" section.

Everything else should remain as close to v1.6.9 as possible.

MAKE THE SMALLEST SAFE CHANGE.
DO NOT DISTURB WORKING FEATURES.