package com.studynest.app.automation

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import com.studynest.app.R
import com.studynest.app.data.*
import com.studynest.app.domain.*
import org.json.JSONArray
import java.time.Instant
import java.time.ZoneId

/** Read-only projection. Android renders the clocks; this never records time. */
data class NotificationTiming(val sittingMs: Long, val studyMs: Long?, val sittingRunning: Boolean,
    val studyRunning: Boolean, val state: String, val anchorElapsed: Long) {
    /** Compatibility aliases for internal tests/callers; the displayed clock is Sitting Time. */
    val libraryMs get() = sittingMs
    val libraryRunning get() = sittingRunning
    fun values(atElapsed: Long): Pair<Long, Long?> {
        val delta = (atElapsed - anchorElapsed).coerceAtLeast(0)
        return (sittingMs + if (sittingRunning) delta else 0) to
            studyMs?.let { it + if (studyRunning) delta else 0 }
    }

    fun extras() = Bundle().apply {
        putLong("studynest.sittingMs", sittingMs); putLong("studynest.studyMs", studyMs ?: -1)
        putBoolean("studynest.sittingRunning", sittingRunning); putBoolean("studynest.studyRunning", studyRunning)
        putLong("studynest.anchorElapsed", anchorElapsed); putString("studynest.timerState", state)
    }

    fun views(c: Context, expanded: Boolean): RemoteViews = RemoteViews(c.packageName,
        if (expanded) R.layout.notification_timers_expanded else R.layout.notification_timers).apply {
        fun clock(live: Int, frozen: Int, value: Long?, running: Boolean) {
            setViewVisibility(live, if (running && value != null) View.VISIBLE else View.GONE)
            setViewVisibility(frozen, if (running && value != null) View.GONE else View.VISIBLE)
            // Frozen clocks use TextViews: stopped Chronometer widgets can still
            // update their initial text from a later host inflation time.
            setTextViewText(frozen, value?.let { android.text.format.DateUtils.formatElapsedTime(it / 1000) } ?: "Unavailable")
            setChronometer(live, anchorElapsed - (value ?: 0).coerceAtLeast(0), null, running && value != null)
        }
        clock(R.id.library_clock, R.id.library_frozen, sittingMs, sittingRunning)
        clock(R.id.study_clock, R.id.study_frozen, studyMs, studyRunning)
        if (expanded) setTextViewText(R.id.timer_state, "StudyNest • $state")
    }

    companion object {
        fun snapshot(active: StudySession?, openVisit: LibraryVisit?, visits: List<LibraryVisit>,
            windows: List<FocusWindow>, periods: List<DistractionPeriod>, now: Long, elapsed: Long,
            zone: ZoneId = ZoneId.systemDefault(), sessions: List<StudySession> = listOfNotNull(active),
            segments: List<Segment> = emptyList(), segmentRange: (Segment) -> TimeRange = { TimeRange(it.start,it.end ?: now) }): NotificationTiming {
            val from = Instant.ofEpochMilli(now).atZone(zone).toLocalDate().atStartOfDay(zone).toInstant().toEpochMilli()
            // Match Home's daily Actual projection across every valid manual or
            // automatic focus window. Library remains a separate presence clock.
            val study = FocusMath.summary(visits, windows, periods, now, TimeRange(from, now))
            val sitting = SittingMath.forRange(visits,sessions,segments,now,TimeRange(from,now),segmentRange)
            val current = windows.lastOrNull { it.end == null && it.sessionId == active?.id }
            val sessionRunning = active?.paused == false && current != null && when (current.mode) {
                "OFF" -> true
                "ON" -> {
                    val array = JSONArray(current.packages)
                    val selected = (0 until array.length()).map { array.getString(it) }.toSet()
                    // Reuse the existing switch grace and lock/call classification.
                    val state = UsageTimeline.reduce(current.cursor, now, UsageState.decode(current.tracker),
                        emptyList(), selected, current.calls).second
                    !(current.calls && state.call) && (!state.interactive || (state.known && state.pkg !in selected))
                }
                else -> false
            }
            // Manual study outside a library still contributes to Actual Study.
            // Unknown focus coverage freezes that clock rather than inventing time.
            val running = sessionRunning && study.unknownMs == 0L
            val measured = if (study.unknownMs > 0) null else study.studyMs
            val status = when {
                openVisit?.awaySince != null -> "Temporarily away"
                active?.paused == true -> "Paused"
                active != null && measured == null -> "Study time unavailable"
                active != null && !sessionRunning -> "Study paused for distraction"
                active != null -> "Studying"
                openVisit != null -> "Library visit • No active session"
                else -> "Monitoring"
            }
            val libraryRunning = openVisit != null && openVisit.awaySince == null
            val manualRunning = active?.let { session -> SessionEngine.isManualTrigger(session.trigger) && !session.paused && segments.any { it.sessionId==session.id && it.end==null } } == true
            return NotificationTiming(sitting.coerceAtLeast(0), measured?.coerceAtLeast(0), libraryRunning || manualRunning,
                running && measured != null, status, elapsed)
        }
    }
}
