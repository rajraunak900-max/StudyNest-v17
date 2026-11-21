# BUG FIX — STUDY TIMER MIDNIGHT RESET LOGIC

I found a small bug in the latest StudyNest version.

Please fix this without changing or breaking any existing StudyNest features.

## CURRENT PROBLEM

The large timer displayed in the **Study Timer** section on the Home screen can continue showing the previous day's accumulated/stale value after the date changes.

For example, it may still show something like:

**07:02:29**

on the next day even though I am no longer inside the library.

This Study Timer needs proper daily rollover behaviour.

---

# REQUIRED BEHAVIOUR AT 12:00 AM

At every local-device midnight (`00:00`), determine whether the user is currently considered to be inside the configured library.

Use StudyNest's existing library-presence logic rather than creating an unrelated second detection system.

Relevant signals include the existing:

- configured library Wi-Fi detection
- geofence/location detection
- existing arrival/departure state
- existing automatic-session rules

## CASE 1 — USER IS NOT IN THE LIBRARY

If, at midnight, the device is:

- NOT connected to a configured library Wi-Fi, AND
- NOT considered inside the configured library/geofence by StudyNest's existing presence logic,

then reset the **Home → Study Timer** display to:

**00:00:00**

The previous day's historical records must remain saved.

This is only a rollover/reset of the timer state/display for the new day. Do NOT delete yesterday's session/history/statistics.

---

## CASE 2 — USER IS STILL IN THE LIBRARY

If, at midnight, StudyNest still considers the device inside the library because:

- it is connected to a configured library Wi-Fi,

OR

- the existing geofence/location logic legitimately considers the device inside the configured library,

then:

**DO NOT reset the Study Timer to 00:00:00 merely because midnight occurred.**

Allow the active timer/session to continue normally.

Example:

At 11:59:59 PM:

**Study Timer = 02:30:00**

User is still connected to the configured library Wi-Fi.

At 12:00 AM:

The Study Timer should continue:

**02:30:01 → 02:30:02 → ...**

It must NOT suddenly become `00:00:00`.

However, continue following StudyNest's existing daily-history rollover/splitting rules so time before midnight belongs to the previous calendar day and time after midnight belongs to the new calendar day.

**The continuous session timer and daily statistical accounting are separate concerns.**

---

# IMPORTANT EDGE CASE

If the user remains inside the library across midnight, keep the timer running.

Later, when the user actually leaves the library and the existing departure logic confirms departure:

- finish/stop the appropriate automatic session normally;
- preserve all historical data;
- ensure the inactive Study Timer does not leave an inappropriate stale value that carries into another day.

Do not reset an actively running legitimate library session simply because the calendar date changed.

---

# MUST WORK WITHOUT OPENING THE APP

The correct rollover state must not depend on the user opening StudyNest exactly at 12:00 AM.

Android may delay background work because of:

- Doze
- battery optimisation
- OEM restrictions
- process termination

Therefore, use timestamps/date reconciliation so that when StudyNest/service next executes, it can determine that midnight passed and apply the correct state.

Do NOT create a battery-heavy per-second polling loop just to detect midnight.

---

# MANUAL MODE SAFETY

Inspect the existing Manual Timer behaviour before changing anything.

Do not accidentally reset or destroy a legitimate manually running session solely because of this automatic-library rule.

If manual and automatic modes require different rollover handling, preserve the intended Manual Timer behaviour and document the decision in the implementation report.

---

# DAILY REPEAT

This is not a one-time fix.

Apply this logic automatically at **every local calendar-day transition**:

Day 1 → Day 2  
Day 2 → Day 3  
Day 3 → Day 4  
etc.

Also handle device timezone/date changes safely using the app's existing time/date architecture.

---

# TEST CASES

Please add/verify tests for at least:

### Test A
11:59 PM — Study Timer has an old/inactive value.  
No library Wi-Fi and outside geofence.  
12:00 AM → **00:00:00**

### Test B
11:59 PM — active automatic session.  
Connected to configured library Wi-Fi.  
12:00 AM → **timer continues without resetting**

### Test C
11:59 PM — active automatic session.  
Not on library Wi-Fi but legitimately inside library according to existing geofence logic.  
12:00 AM → **timer continues**

### Test D
User remains in library from 11:30 PM until 12:30 AM.  
Study Timer remains continuous, while History correctly attributes/splits the relevant daily statistics across the two calendar dates.

### Test E
App is closed/backgrounded and phone locked at midnight.  
When StudyNest next executes, it reconciles the date and produces the same correct result without requiring the UI to have been open.

### Test F
Previous day's History, Library Time, Actual Study, Break, Distraction, Efficiency, streak information and other stored records remain intact after the Home Study Timer resets.

---

# REGRESSION SAFETY

Do not break or redesign:

- Automatic/Manual timer modes
- Wi-Fi library detection
- geofencing/location detection
- arrival/departure handling
- midnight daily History splitting
- Library Time
- Actual Study Time
- Break Time
- Distraction Time
- Efficiency
- streaks
- App Monitoring
- background/locked-screen operation
- notification timer
- sound/vibration automation

Use the existing architecture and library-presence source of truth wherever possible.

Make the smallest reliable fix necessary.

Save/checkpoint the project before this change and again after the fix compiles and tests successfully.