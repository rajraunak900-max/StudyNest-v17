# Explicit manual breaks and one-time reminder

Room migration 5→6 only adds manual_breaks and break_tracking. It preserves every
old table, record, index and schema archive. No old gaps are guessed as breaks.
Break coverage records when measurement started, so older History is marked
Not recorded and partially covered dates explain the update boundary.

An explicit Pause creates a break only for an active session inside a detected
library visit. Resume, session end, departure, disabling detection, reboot or
interrupted-presence recovery terminates the record. Departure cancels immediately
on the known detection transition; existing study/sound departure grace remains.
A return while still paused does not automatically create a new break. A new
explicit Pause after Resume is required. Library presence continues during pauses.
Break intervals never create study/distraction windows and do not subtract study
twice. Daily range intersection splits library/study/distraction/break at local
midnight without an alarm, Finish Day button or open activity.

The existing SessionEngine mutex reconciles the break and claims reminder delivery
inside a Room transaction. The attempted flag is persisted before vibration.
Worker retries, duplicate ticks and process recreation cannot repeat a vibration
for one continuous break. A crash between persistence and vibration can omit the
reminder; at-most-once delivery is chosen over risking duplicate vibrations.

One app-scope delay and one unique WorkManager fallback are scheduled per break.
No 45-minute wake lock, AlarmManager alarm, exact-alarm permission, recurring
notification/sound or new polling loop is introduced. The existing 30-second
service reconciliation can also claim a due reminder. The deadline samples
existing Wi-Fi once before checking valid paused/inside state. Stale IDs cannot
claim a different break. A completed reminder cancels pending work; the next
separate break gets its own ID and eligibility.

The vibration is one supported VibrationEffect one-shot of 2000ms using ordinary
notification-event audio attributes. Device vibration capability, DND and policy
restrictions remain authoritative; failure does not cause repeated attempts.
Doze/OEM scheduling may delay delivery; force-stop or interrupted detection may
prevent it. No guarantee of exact 45:00 execution during deep sleep is claimed.

Tests and final physical acceptance limits will be recorded in the release report.
