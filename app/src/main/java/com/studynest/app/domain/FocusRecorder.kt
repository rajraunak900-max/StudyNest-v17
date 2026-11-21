package com.studynest.app.domain

import androidx.room.withTransaction
import com.studynest.app.data.*
import org.json.JSONArray

/** Runs under SessionEngine's mutex; UsageMonitor updates only cursor/state columns. */
class FocusRecorder(
    private val db: StudyDatabase, private val clock: StudyClock,
    private val config: () -> Config, private val usageAllowed: () -> Boolean
) {
    private val dao = db.focusDao()
    fun end(start: Long, elapsed: Long, boot: Int, checkpoint: Long): Long =
        if (boot == clock.boot()) start + (clock.elapsed() - elapsed).coerceAtLeast(0) else checkpoint

    suspend fun departure(cutoff: Long) {
        dao.openVisit()?.let { dao.updateVisit(VisitQualification.finish(it, cutoff)) }
        dao.openWindow()?.let { dao.updateWindow(it.copy(end = cutoff.coerceAtLeast(it.start))) }
    }

    suspend fun pauseVisit(cutoff: Long) {
        dao.openVisit()?.takeIf { it.awaySince == null }?.let {
            val at = minOf(cutoff, end(it.start, it.elapsedStart, it.boot, it.checkpoint)).coerceAtLeast(it.start)
            dao.updateVisit(it.copy(awaySince = at, checkpoint = at))
        }
    }

    suspend fun closeStudy(cutoff: Long) {
        dao.openWindow()?.let { dao.updateWindow(it.copy(end = minOf(cutoff,
            end(it.start, it.elapsedStart, it.boot, it.checkpoint)).coerceAtLeast(it.start))) }
    }

    suspend fun recoverVisit() {
        // Presence cannot be inferred across a dead process / reboot.
        dao.openVisit()?.let { visit ->
            val exit = db.dao().state()?.exitAt
            dao.updateVisit(VisitQualification.finish(visit, minOf(visit.checkpoint, exit ?: Long.MAX_VALUE)))
        }
    }

    suspend fun sync() = db.withTransaction {
        val c = config()
        val s = db.dao().state() ?: AutomationState()
        val detected = (c.wifi && s.wifiInside) || (c.geo && s.geoInside)
        val present = detected || s.exitAt != null
        val visit = dao.openVisit()
        // Grace preserves an existing visit. It cannot prove a new arrival after
        // recovery has closed that row at its known departure/checkpoint.
        if (detected && visit == null)
            dao.insertVisit(LibraryVisit(start = clock.now(), elapsedStart = clock.elapsed(), boot = clock.boot(),
                minimumMinutes = c.minimumSessionMinutes))
        else if (!present && visit != null)
            dao.updateVisit(VisitQualification.finish(visit, end(visit.start, visit.elapsedStart, visit.boot, visit.checkpoint)))
        else if (visit != null && (detected || visit.awaySince == null)) {
            val resumed = if (detected) LibraryPresence.resume(visit, clock.now()) else visit
            val next = resumed.copy(checkpoint = end(visit.start, visit.elapsedStart, visit.boot, visit.checkpoint))
            if (next != visit) dao.updateVisit(next)
        }
        // A confirmed-away open visit is identity only: no checkpoint writes or
        // advancing library time while its thirty-minute deadline is pending.

        var active = db.dao().active()?.takeIf { !it.paused }
        if (active != null && active.measurementStart == null) {
            active = active.copy(measurementStart = clock.now())
            db.dao().updateSession(active)
        }
        val packages = JSONArray(c.monitoredApps.sorted()).toString()
        val mode = when {
            !c.appMonitoring || (c.monitoredApps.isEmpty() && !c.monitorCallAudio) -> "OFF"
            c.monitoredApps.isNotEmpty() && !usageAllowed() -> "UNKNOWN"
            else -> "ON"
        }
        var window = dao.openWindow()
        if (window != null && (active?.id != window.sessionId || window.mode != mode ||
            window.packages != packages || window.calls != c.monitorCallAudio)) {
            dao.updateWindow(window.copy(end = end(window.start, window.elapsedStart, window.boot, window.checkpoint)))
            window = null
        }
        if (active != null && window == null)
            dao.insertWindow(FocusWindow(sessionId = active.id, start = clock.now(),
                elapsedStart = clock.elapsed(), boot = clock.boot(), mode = mode,
                packages = packages, calls = c.monitorCallAudio))
        else if (window != null)
            dao.updateWindow(window.copy(checkpoint = end(window.start, window.elapsedStart, window.boot, window.checkpoint)))
        Unit
    }
}
