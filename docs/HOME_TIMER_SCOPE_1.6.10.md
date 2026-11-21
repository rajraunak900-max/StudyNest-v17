IMPORTANT CLARIFICATION — HOME STUDY TIMER SCOPE

Please correct one requirement from my previous instructions.

The Home screen "Study Timer" must NOT show only the Actual Study Time
of the currently active session.

I want the Home Study Timer to show:

    TODAY'S TOTAL ACTUAL STUDY TIME

across ALL valid study sessions/visits for the current local calendar day.

This should follow the same authoritative daily Actual Study accounting
used for today's goal and, where applicable, the existing daily Actual
Study total in the foreground notification.


==================================================
EXPECTED BEHAVIOR
==================================================

Example:

Session / Visit 1:
    Actual Study = 1h 20m
    Session ends

Later:

Session / Visit 2:
    Actual Study = 40m

The Home Study Timer must show:

    2h 00m

NOT:

    40m


Ending a session must NOT reset the Home Study Timer to 00:00:00.

Starting a new session later on the same day must continue from today's
already accumulated Actual Study Time.


==================================================
DISTRACTION TIME MUST STILL BE EXCLUDED
==================================================

The Home Study Timer must display ONLY today's Actual Study Time.

For example:

Visit 1:
    Actual Study = 1h 20m
    Distraction = 20m

Visit 2:
    Actual Study = 40m
    Distraction = 10m

Today's totals are:

    Actual Study = 2h 00m
    Distraction = 30m

The Home Study Timer must show:

    2h 00m

NOT:

    2h 30m


==================================================
LIVE BEHAVIOR
==================================================

While Actual Study is legitimately running:

    Home Study Timer increases normally.

If Actual Study stops because of an existing StudyNest rule, such as:

- selected monitored distracting app
- manual Pause
- confirmed departure
- another existing condition that legitimately stops Actual Study

then the Home Study Timer must stop increasing accordingly.

When Actual Study resumes, the Home Study Timer resumes from today's
previous accumulated Actual Study total.

Do not reset it just because:

- a session ends
- a library visit ends
- the user leaves for more than the 30-minute grace
- a new session/visit is created later that same day
- the app is closed/reopened
- the phone is locked/unlocked
- the service/app process restarts

The displayed value must be derived from authoritative stored accounting,
not from an in-memory timer that loses today's accumulated time.


==================================================
MIDNIGHT RESET
==================================================

The Home Study Timer is DAILY.

Therefore, it should reset for the new LOCAL CALENDAR DAY at midnight.

Example:

October 5 total Actual Study:
    4h 15m

After the local date becomes October 6:

    Today's Home Study Timer = 00:00:00

and it should begin accumulating October 6 Actual Study independently.

Historical October 5 data must remain unchanged in History.

If a legitimate study session crosses midnight, preserve the existing
StudyNest midnight/day-splitting rules:

- time before midnight belongs to the previous day
- time after midnight belongs to the new day

Do not destroy or incorrectly split the underlying session merely to reset
the Home display.


==================================================
MANUAL STUDY OUTSIDE LIBRARY
==================================================

Preserve the existing intended scope from the previous accounting fix:

Manual study outside the library should continue to count toward the Home
Study Timer if it currently qualifies as Actual Study under StudyNest's
existing rules.

Do not accidentally restrict the Home Study Timer only to library-detected
Actual Study if the existing daily timer accounting intentionally includes
valid manual study outside the library.

However, do not change the existing History/library-specific metrics just
to make them match this Home display if their scope is intentionally
different.


==================================================
UNKNOWN USAGE
==================================================

Preserve the previous requirement:

Unknown/unavailable App Monitoring usage must NOT be credited as Actual
Study and must NOT be replaced with gross elapsed timer time.

Do not inflate the Home Study Timer when authoritative Actual Study coverage
is unavailable.


==================================================
DAILY GOAL AND HOME TIMER
==================================================

Both should now use TODAY'S ACTUAL STUDY TIME as their source.

Therefore, under normal conditions:

    Home Study Timer = Today's Actual Study
    Daily Goal progress = Today's Actual Study

Example:

Daily Goal = 3h

Today's:
    Actual Study = 2h 10m
    Distraction = 30m
    Break = 20m
    Library Time = 3h

Expected:

    Home Study Timer = 2h 10m
    Goal progress = 2h 10m / 3h

Distraction, Break and additional Library Time must NOT be added to either
value.


==================================================
IMPORTANT — DO NOT CHANGE OTHER FEATURES
==================================================

This is only a clarification of the Home Study Timer's DISPLAY/ACCOUNTING
SCOPE.

Do NOT change the already-working:

- session lifecycle
- session IDs
- session qualification
- 90-second departure confirmation
- 30-minute session continuity grace
- Library Time
- Break Time
- Distraction Time
- Efficiency calculation
- App Monitoring behavior
- Wi-Fi detection
- geofencing
- vibration/sound behavior
- History
- Calendar
- graphs
- notification behavior
- database records
- goals/streak settings
- midnight History splitting
- any other stable v1.6.9 feature

In particular:

DO NOT keep a session artificially open just so the Home timer can show
the daily total.

Sessions should continue to start/end exactly according to existing
StudyNest rules.

The Home Study Timer is simply a DAILY aggregation of Actual Study across
those sessions.


==================================================
ADD/UPDATE TESTS
==================================================

Please add focused tests for this clarification.

TEST A — MULTIPLE SAME-DAY SESSIONS

Session 1 Actual = 60m
Session 1 ends.

Session 2 Actual = 30m

Expected Home Study Timer:
    90m


TEST B — DISTRACTION EXCLUDED

Session 1:
    Actual = 60m
    Distraction = 20m

Session 2:
    Actual = 30m
    Distraction = 10m

Expected:
    Home Study Timer = 90m

NOT:
    120m


TEST C — NEW SESSION DOES NOT RESET DAILY TIMER

Today's previous Actual = 2h

Start a fresh automatic session.

Expected Home timer initially reflects:
    2h

After 10m valid Actual Study:
    2h 10m


TEST D — APP RESTART

Today's stored Actual = 2h 15m.

Close/restart/recreate the app.

Expected Home timer:
    2h 15m

It must not reset to zero.


TEST E — MIDNIGHT

Previous day Actual = 3h.

After entering a new local calendar day with no new Actual Study:

Expected Home timer:
    00:00:00

Previous day's 3h remains intact in History.


TEST F — GOAL CONSISTENCY

Daily Goal = 3h
Today's Actual = 2h
Today's Distraction = 45m

Expected:

    Home Study Timer = 2h
    Goal progress = 2h / 3h
    Goal NOT completed.


==================================================
FINAL REQUIREMENT
==================================================

Please treat this clarification as authoritative and replace any earlier
instruction saying:

    "the timer uses Actual Study for its active session"

with:

    "the Home Study Timer uses TODAY'S TOTAL ACTUAL STUDY TIME across all
     valid sessions for the current local calendar day."

The timer resets at the start of a NEW LOCAL CALENDAR DAY, NOT when an
individual session ends.

Keep the rest of the v1.6.10 bug-fix scope unchanged.