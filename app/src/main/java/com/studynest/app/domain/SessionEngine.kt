package com.studynest.app.domain

import androidx.room.withTransaction
import com.studynest.app.data.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.time.ZoneId

interface StudyClock {
    fun now(): Long

    fun elapsed(): Long

    fun boot(): Int

    fun zone(): ZoneId = ZoneId.systemDefault()
}

interface SoundControl {
    fun current(): Int

    suspend fun set(mode: Int): Boolean
}

/** Serial transactions make manual actions and duplicate presence callbacks safe. */
class SessionEngine(
    private val db: StudyDatabase,
    private val clock: StudyClock,
    private val config: () -> Config,
    private val sound: SoundControl,
    private val report: (String) -> Unit,
    private val soundStatus: (String) -> Unit = {},
    private val remindOnce: () -> Unit = {},
    private val cancelReminder: () -> Unit = {},
    private val scheduleOverride: (Long) -> Unit = {},
    private val usageAllowed: () -> Boolean = { false },
    private val beforeChange: suspend () -> Unit = {},
    private val breakDeadline: (ManualBreak?) -> Unit = {},
    private val vibrateBreak: () -> Unit = {}
) {
    private val lock = Mutex()
    private val dao = db.dao()
    val focus = FocusRecorder(db, clock, config, usageAllowed)

    private val breaks = BreakRecorder(db, clock, config, breakDeadline, vibrateBreak)
    private suspend fun syncTracking() { focus.sync(); breaks.sync() }
    suspend fun checkBreak(id: Long) = lock.withLock { breaks.sync(id) }

    // Keep required checkpoints, but avoid rewriting identical automation state
    // and repeatedly cancelling/replacing WorkManager jobs on every timer tick.
    private var soundWorkPending = true // Clean persisted work once after restart.
    private var scheduledOverride: Long? = null
    private suspend fun saveStateIfChanged(next: AutomationState) {
        if (dao.state() != next) dao.saveState(next)
    }
    private fun cancelSoundReminder() {
        if (!soundWorkPending) return
        soundWorkPending = false; scheduledOverride = null; cancelReminder()
    }
    private fun scheduleSoundReminder(deadline: Long) {
        soundWorkPending = true
        if (scheduledOverride == deadline) return
        scheduledOverride = deadline; scheduleOverride(deadline)
    }

    private suspend fun state() = dao.state() ?: AutomationState()

    private fun present(s: AutomationState) = (config().wifi && s.wifiInside) || (config().geo && s.geoInside)

    /** A departed automatic session used to remain paused indefinitely. Reconcile
     * from persisted timestamps, not an in-memory midnight callback. Running or
     * paused library sessions and all manual sessions retain their identity/time.
     * Same-day departure/re-entry keeps the existing pause/resume behaviour. */
    private suspend fun reconcileTimerDay(): Boolean {
        val active = dao.active() ?: return false
        if (isManualTrigger(active.trigger)) return false
        val s = state()
        if (present(s)) return false
        val zone = clock.zone()
        val today = Instant.ofEpochMilli(clock.now()).atZone(zone).toLocalDate()
        if (Instant.ofEpochMilli(active.startedAt).atZone(zone).toLocalDate() == today) return false
        // A delayed callback must not turn an expired, persisted departure into
        // continuous overnight study. Use the same confirmation as normal ticks.
        if (s.exitAt != null) {
            confirmDeparture()
            if (state().exitAt != null) return false
        }
        // No new study time is inferred outside the library. Closed segments are
        // left byte-for-byte intact; a stray open segment ends at its checkpoint.
        val cutoff = dao.segmentsForSession(active.id)
            .maxOfOrNull { it.end ?: it.checkpoint } ?: active.startedAt
        focus.recoverVisit()
        breaks.recover()
        finishActive(cutoff = cutoff)
        return true
    }

    /** Used by the foreground date transition; service/recovery also reconcile.
     * This has no alarm, wake lock, per-second background loop or schema change. */
    suspend fun reconcileDay(): Boolean = lock.withLock {
        val changed = db.withTransaction { reconcileTimerDay() }
        if (changed) { reconcileSound(); syncTracking() }
        changed
    }

    private suspend fun confirmDeparture() {
        val s = state()
        if (!present(s) && s.exitAt != null) {
            val activeSession=dao.active()
            val away=clock.now()-s.exitAt
            if(away < EXIT_GRACE_MS) return
            focus.pauseVisit(s.exitAt) // Freeze presence even without an active study timer.
            // Preserve the existing 90s departure-confirmation pause/cutoff.
            // Keep the visit pending independently until its reconnection grace.
            val runningAtDeparture = activeSession?.takeIf { active ->
                !active.paused && dao.openSegments().any { it.sessionId == active.id && it.start <= s.exitAt }
            }
            runningAtDeparture?.let { active ->
                closeSegments(s.exitAt)
                focus.closeStudy(s.exitAt)
                dao.updateSession(active.copy(paused = true, pausedByDeparture = true))
            }
            if(away < departureGraceMs(config(),activeSession?.trigger)) return
            focus.departure(s.exitAt)
            // Only a user-paused automatic timer expires with its old Wi-Fi visit.
            // Within-grace returns and explicit Manual timers retain their rules.
            if (config().wifi && activeSession != null && !isExplicitManual(activeSession.trigger) &&
                activeSession.paused && !activeSession.pausedByDeparture)
                finishActive(cutoff = s.exitAt)
            // A manual timer adopted by automatic library tracking follows the
            // same visit deadline. Its counted intervals still remain in Room.
            if (activeSession?.trigger == MANUAL_AUTO_TRIGGER)
                finishActive(cutoff = s.exitAt)
            saveStateIfChanged(s.copy(exitAt = null, suppressed = false,
                normalSoundPending = s.normalSoundPending || (!config().wifi && config().geo),
                soundOverridden = false, overrideAt = null, overrideNotified = false))
        }
    }

    private suspend fun newSession(trigger: String) {
        if (dao.active() != null) return
        val id = dao.insertSession(StudySession(startedAt = clock.now(), trigger = trigger, measurementStart = clock.now()))
        dao.insertSegment(
            Segment(
                sessionId = id,
                start = clock.now(),
                elapsedStart = clock.elapsed(),
                boot = clock.boot()))
    }

    fun range(s: Segment): TimeRange {
        val end =
            s.end
                ?: if (s.boot == clock.boot())
                    s.start + (clock.elapsed() - s.elapsedStart).coerceAtLeast(0)
                else s.checkpoint
        return TimeRange(s.start, end.coerceAtLeast(s.start))
    }

    private suspend fun closeSegments(cutoff: Long? = null) {
        dao.openSegments().forEach {
            dao.updateSegment(
                it.copy(
                    end = minOf(range(it).end, cutoff ?: Long.MAX_VALUE).coerceAtLeast(it.start)))
        }
    }

    private suspend fun finishActive(cutoff: Long? = null, note: String? = null, explicit: Boolean = false) {
        dao.active()?.let {
            closeSegments(cutoff)
            if (cutoff != null) focus.closeStudy(cutoff)
            dao.updateSession(
                it.copy(
                    endedAt = (cutoff ?: clock.now()).coerceAtLeast(it.startedAt),
                    paused = false,
                    pausedByDeparture = false,
                    note = note ?: it.note,
                    endedExplicitly = explicit))
        }
    }

    private suspend fun reconcileSound() {
        var s = state()
        val c = config()
        if (c.wifi && !s.wifiInside && s.wifiExitAt != null && clock.now() - s.wifiExitAt!! >= WIFI_SOUND_GRACE_MS) {
            s = s.copy(wifiExitAt = null, normalSoundPending = true, overrideAt = null,
                overrideNotified = false, soundOverridden = false, soundWasVibrate = false)
            saveStateIfChanged(s)
            cancelSoundReminder()
        }
        val librarySound = if (c.wifi) s.wifiInside || s.wifiExitAt != null
            else c.geo && (s.geoInside || s.exitAt != null)
        // Wi-Fi departure takes priority over geofences and a still-running timer.
        val wanted = c.vibrate && (librarySound || (!c.wifi && !c.geo && dao.active()?.paused == false))
        if (!wanted) {
            if ((s.normalSoundPending || s.soundOwned) && sound.current() != 2) {
                saveStateIfChanged(s.copy(normalSoundPending = true))
                if (!sound.set(2)) return
            }
            saveStateIfChanged(s.copy(soundOwned = false, previousRinger = null, soundOverridden = false,
                normalSoundPending = false, overrideAt = null, overrideNotified = false, soundWasVibrate = false))
            cancelSoundReminder()
            soundStatus(if (!c.vibrate) "Automatic vibration is off." else if (sound.current() == 2) "Outside library • Normal Sound" else "Outside library • no pending sound change")
            return
        }
        val overrideEligible = if (c.wifi) s.wifiInside else c.geo && s.geoInside
        val actual = sound.current()
        if (actual == 1) {
            saveStateIfChanged(s.copy(soundOwned = true, previousRinger = null, soundOverridden = false,
                normalSoundPending = false, overrideAt = null, overrideNotified = false, soundWasVibrate = true))
            cancelSoundReminder()
            soundStatus("Vibrate mode is active. Library departure will enable Normal Sound.")
            return
        }
        // The engine's own setter is serialized under this mutex. Its acknowledgement
        // is recorded before any queued ringer broadcast can enter this method.
        if (s.soundWasVibrate) {
            s = s.copy(soundOverridden = true, soundWasVibrate = false,
                overrideAt = if (actual == 2 && overrideEligible) clock.now() else null,
                overrideNotified = false)
            saveStateIfChanged(s)
        }
        if (s.soundOverridden) {
            if (actual != 2) {
                saveStateIfChanged(s.copy(overrideAt = null, overrideNotified = false))
                cancelSoundReminder()
                soundStatus("Silent mode selected outside StudyNest; respected until your next library visit.")
                return
            }
            // A user can move Vibrate → Silent → Sound. Start the deadline when
            // Sound is actually observed, not when Silent was first selected.
            if (overrideEligible && s.overrideAt == null) {
                s = s.copy(overrideAt = clock.now(), overrideNotified = false)
                saveStateIfChanged(s)
            }
            if (overrideEligible && s.overrideAt != null) {
                if (c.overrideMode == "RESTORE") {
                    if (clock.now() - s.overrideAt!! >= OVERRIDE_DELAY_MS) {
                        if (sound.set(1)) {
                            saveStateIfChanged(s.copy(soundOwned = true, soundOverridden = false,
                                soundWasVibrate = true, overrideAt = null, overrideNotified = false))
                            cancelSoundReminder()
                            soundStatus("Vibration restored after five minutes.")
                        }
                    } else {
                        scheduleSoundReminder(s.overrideAt!! + OVERRIDE_DELAY_MS)
                        soundStatus("Sound override detected • vibration in ${((s.overrideAt!! + OVERRIDE_DELAY_MS - clock.now()) / 1000).coerceAtLeast(0)} seconds if still at the library.")
                    }
                } else {
                    if (!s.overrideNotified) {
                        // Persist before posting so process restart cannot repeat this reminder.
                        saveStateIfChanged(s.copy(overrideNotified = true))
                        soundWorkPending = true
                        remindOnce()
                    }
                    soundStatus("Sound selection respected • one reminder only for this change.")
                }
            } else soundStatus("Sound selection respected while library departure is being checked.")
            return
        }
        saveStateIfChanged(s.copy(soundOwned = true, previousRinger = null))
        if (sound.set(1)) {
            saveStateIfChanged(s.copy(soundOwned = true, soundWasVibrate = true, normalSoundPending = false))
            soundStatus("Vibrate mode is active.")
        } else saveStateIfChanged(s.copy(soundOwned = false))
    }

    /** Explicit retry clears the override without touching session data. */
    suspend fun retrySound() = lock.withLock {
            beforeChange()
        val s = state()
        saveStateIfChanged(s.copy(soundOverridden = false, soundWasVibrate = false, overrideAt = null, overrideNotified = false))
        cancelSoundReminder()
        reconcileSound()
        syncTracking()
    }

    suspend fun startAutomatic() =
        lock.withLock {
            beforeChange()
            db.withTransaction {
                reconcileTimerDay()
                val s = state()
                saveStateIfChanged(s.copy(suppressed = false))
                if (config().tracking && present(s)) {
                    val active = dao.active()
                    if (active == null) newSession(if (s.wifiInside) "Wi-Fi" else "Location")
                    else if (isExplicitManual(active.trigger))
                        dao.updateSession(active.copy(trigger = MANUAL_AUTO_TRIGGER))
                }
            }
            reconcileSound()
            syncTracking()
        }

    suspend fun manualStart() =
        lock.withLock {
            beforeChange()
            db.withTransaction {
                reconcileTimerDay()
                val a = dao.active()
                if (a == null) {
                    val s = state()
                    saveStateIfChanged(s.copy(suppressed = false))
                    newSession(if (config().tracking && present(s)) MANUAL_AUTO_TRIGGER else "Manual")
                }
                else if (a.trigger != "Manual")
                    report("An automatic session is running. Finish it before starting manually.")
            }
            reconcileSound()
            syncTracking()
        }

    suspend fun pauseResume() =
        lock.withLock {
            beforeChange()
            db.withTransaction {
                reconcileTimerDay()
                dao.active()?.let {
                    if (!it.paused) closeSegments()
                    else
                        dao.insertSegment(
                            Segment(
                                sessionId = it.id,
                                start = clock.now(),
                                elapsedStart = clock.elapsed(),
                                boot = clock.boot()))
                    dao.updateSession(it.copy(paused = !it.paused, pausedByDeparture = false))
                    if (!it.paused) breaks.pause(it)
                }
            }
            reconcileSound()
            syncTracking()
        }

    suspend fun finish() =
        lock.withLock {
            beforeChange()
            db.withTransaction {
                finishActive(explicit = true)
                saveStateIfChanged(state().copy(suppressed = present(state()) || state().exitAt != null))
            }
            reconcileSound()
            syncTracking()
        }

    suspend fun signal(method: String, inside: Boolean) =
        lock.withLock {
            beforeChange()
            val c = config()
            if (method == "Wi-Fi" && !c.wifi || method == "Location" && !c.geo) return@withLock
            db.withTransaction {
                // A late deadline callback must not let a new arrival erase an
                // already-expired departure and merge two real library visits.
                // Confirm using the old persisted presence/exit timestamp first.
                confirmDeparture()
                reconcileTimerDay()
                val old = state()
                var next =
                    if (method == "Wi-Fi") old.copy(wifiInside = inside,
                        wifiExitAt = if (inside) null else if (old.wifiInside) clock.now() else old.wifiExitAt,
                        normalSoundPending = if (inside) false else old.normalSoundPending,
                        overrideAt = old.overrideAt)
                    else old.copy(geoInside = inside)
                next =
                    if (present(next)) next.copy(exitAt = null)
                    else if (present(old)) next.copy(exitAt = clock.now()) else next
                if (method == "Wi-Fi" && inside && !old.wifiInside && old.wifiExitAt == null)
                    next = next.copy(soundOverridden = false, soundWasVibrate = false, overrideAt = null, overrideNotified = false)
                saveStateIfChanged(next)
                if (present(next) && c.tracking && !next.suppressed) {
                    var active = dao.active()
                    if (active == null) newSession(method)
                    else {
        // Keep the existing manual session and its accumulated
                        // focus, but hand its library lifecycle to detection.
                        if (isExplicitManual(active.trigger)) {
                            active = active.copy(trigger = MANUAL_AUTO_TRIGGER)
                            dao.updateSession(active)
                        }
                        if (active.pausedByDeparture) {
                            dao.insertSegment(
                                Segment(
                                    sessionId = active.id,
                                    start = clock.now(),
                                    elapsedStart = clock.elapsed(),
                                    boot = clock.boot()))
                            dao.updateSession(active.copy(paused = false, pausedByDeparture = false))
                        }
                    }
                }
            }
            reconcileSound()
            syncTracking()
        }

    suspend fun tick() =
        lock.withLock {
            beforeChange()
            db.withTransaction {
                confirmDeparture()
                reconcileTimerDay()
                dao.openSegments().forEach {
                    dao.updateSegment(it.copy(checkpoint = range(it).end))
                }
            }
            reconcileSound()
            syncTracking()
        }

    /** Only process-start recovery closes unobserved auto time; activity resume does not. */
    suspend fun recover(processRestart: Boolean = false) =
        lock.withLock {
            beforeChange()
            db.withTransaction {
                if (processRestart) { focus.recoverVisit(); breaks.recover() }
                val active = dao.active()
                val stale =
                    dao.openSegments().filter {
                        it.boot != clock.boot() ||
                            (processRestart &&
                                active?.trigger != "Manual" &&
                                active?.trigger != MANUAL_AUTO_TRIGGER &&
                                clock.now() - it.checkpoint > 120000)
                    }
                if (stale.isNotEmpty()) {
                    finishActive(
                        stale.maxOf { it.checkpoint },
                        "Closed at last checkpoint after interrupted monitoring; unobserved time excluded.")
                    saveStateIfChanged(
                        state()
                            .copy(
                                wifiInside = false,
                                geoInside = false,
                                exitAt = null,
                                suppressed = false))
                }
                val c = config()
                val old = state()
                saveStateIfChanged(
                    old.copy(
                        wifiInside = old.wifiInside && c.wifi, geoInside = old.geoInside && c.geo))
                if (!c.tracking) {
                    val activeSession = dao.active()
                    if (activeSession?.trigger == MANUAL_AUTO_TRIGGER)
                        dao.updateSession(activeSession.copy(trigger = "Manual"))
                    else if (activeSession?.trigger != "Manual") finishActive()
                }
                if (!c.wifi && !c.geo)
                    saveStateIfChanged(state().copy(exitAt = null, suppressed = false))
                reconcileTimerDay()
            }
            if (!processRestart || !config().wifi) reconcileSound()
            syncTracking()
        }

    /** Disabling a detection source does not finish or pause an existing timer. */
    suspend fun reconfigure() = lock.withLock {
            beforeChange()
        val c = config()
        val old = state()
        saveStateIfChanged(old.copy(wifiInside = old.wifiInside && c.wifi,
            geoInside = old.geoInside && c.geo, exitAt = null,
            wifiExitAt = if (c.wifi) old.wifiExitAt else null))
        if (!c.tracking) {
            val active = dao.active()
            if (active?.trigger == MANUAL_AUTO_TRIGGER)
                dao.updateSession(active.copy(trigger = "Manual"))
            else if (active?.trigger != "Manual") finishActive()
        }
        syncTracking()
    }

    suspend fun resetPresence() =
        lock.withLock {
            beforeChange()
            db.withTransaction {
                if (dao.active()?.trigger != "Manual") finishActive()
                saveStateIfChanged(
                    state()
                        .copy(
                            wifiInside = false,
                            geoInside = false,
                            exitAt = null,
                            suppressed = false))
            }
            reconcileSound()
            syncTracking()
        }

    suspend fun syncFocus() = lock.withLock { beforeChange(); syncTracking() }

    suspend fun needsService(): Boolean =
        lock.withLock {
            config().wifi || dao.active() != null || present(state()) || state().exitAt != null || state().normalSoundPending
        }

    companion object {
        const val MANUAL_AUTO_TRIGGER = "Manual-Auto"
        fun isManualTrigger(trigger: String?) = trigger == "Manual" || trigger == MANUAL_AUTO_TRIGGER
        private fun isExplicitManual(trigger: String?) = trigger == "Manual"
        const val EXIT_GRACE_MS = 90000L
        const val WIFI_EXIT_GRACE_MS = 30 * 60 * 1000L
        // Wi-Fi-enabled automatic presence uses the requested longer grace;
        // location-only detection and explicit manual timers retain their rules.
        fun departureGraceMs(config: Config, trigger: String?): Long =
            if(config.wifi && trigger!="Manual") WIFI_EXIT_GRACE_MS else EXIT_GRACE_MS
        const val WIFI_SOUND_GRACE_MS = 90000L
        const val OVERRIDE_DELAY_MS = 300000L
    }
}
