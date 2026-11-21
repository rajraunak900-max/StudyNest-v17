package com.studynest.app.domain

import androidx.room.withTransaction
import com.studynest.app.data.*

/** Called under the existing SessionEngine mutex. No inferred historical pauses. */
class BreakRecorder(private val db: StudyDatabase, private val clock: StudyClock,
    private val config: () -> Config, private val schedule: (ManualBreak?) -> Unit,
    private val vibrate: () -> Unit) {
    private val dao = db.breakDao()
    private fun inside(c: Config, s: AutomationState) =
        (c.wifi && s.wifiInside) || (c.geo && s.geoInside)

    fun end(record: ManualBreak): Long = record.end ?: if (record.boot == clock.boot())
        record.start + (clock.elapsed() - record.elapsedStart).coerceAtLeast(0)
    else record.checkpoint

    suspend fun pause(session: StudySession) {
        val state = db.dao().state() ?: AutomationState()
        val visit = db.focusDao().openVisit()
        if (!inside(config(), state) || visit == null || dao.active() != null) return
        dao.insert(ManualBreak(sessionId = session.id, visitId = visit.id,
            start = clock.now(), elapsedStart = clock.elapsed(), boot = clock.boot()))
    }

    suspend fun recover() {
        dao.active()?.let { dao.update(it.copy(end = minOf(it.checkpoint, clock.now()).coerceAtLeast(it.start))) }
    }

    /** Reconcile current state before claiming a deadline. Stale worker IDs cannot
     * deliver reminders for later breaks. Persist the attempt before vibration so
     * retries/process recreation never vibrate twice for a continuous break. */
    suspend fun sync(expectedId: Long? = null) {
        var remind = false
        val pending = db.withTransaction {
            if (dao.tracking() == null) dao.begin(BreakTracking(startedAt = clock.now()))
            var record = dao.active() ?: return@withTransaction null
            if (expectedId != null && record.id != expectedId) return@withTransaction record.takeUnless { it.reminderAttempted }
            val session = db.dao().active()
            val state = db.dao().state() ?: AutomationState()
            val visit = db.focusDao().openVisit()
            val valid = record.boot == clock.boot() && session?.id == record.sessionId &&
                session.paused && !session.pausedByDeparture && visit?.id == record.visitId && inside(config(), state)
            if (!valid) {
                dao.update(record.copy(end = end(record).coerceAtLeast(record.start)))
                return@withTransaction null
            }
            val at = end(record)
            if (!record.reminderAttempted && at - record.start >= LIMIT_MS) {
                record = record.copy(reminderAttempted = true, checkpoint = at)
                dao.update(record)
                remind = true
            } else if (record.checkpoint != at) dao.update(record.copy(checkpoint = at))
            record.takeUnless { it.reminderAttempted }
        }
        if (remind) runCatching { vibrate() } // Android/DND restrictions are respected.
        schedule(pending)
    }

    companion object { const val LIMIT_MS = 45 * 60 * 1000L; const val VIBRATION_MS = 2000L }
}
