package com.studynest.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.*
import androidx.lifecycle.repeatOnLifecycle
import com.studynest.app.StudyNestApp
import com.studynest.app.automation.AppCatalog
import com.studynest.app.data.*
import com.studynest.app.domain.*
import kotlinx.coroutines.delay
import java.time.*
import java.util.Locale

data class FocusRecords(val visits: List<LibraryVisit>, val windows: List<FocusWindow>,
    val periods: List<DistractionPeriod>, val now: Long, val breaks: List<ManualBreak> = emptyList(), val breakSince: Long? = null) {
    fun summary(from: Long = 0, until: Long = now, sessionId: Long? = null) =
        FocusMath.summary(visits, windows, periods, now, TimeRange(from, until), sessionId, breaks)
}

@Composable
fun focusRecords(app: StudyNestApp): FocusRecords {
    val visits by remember { app.db.focusDao().visits() }.collectAsStateWithLifecycle(emptyList())
    val windows by remember { app.db.focusDao().windows() }.collectAsStateWithLifecycle(emptyList())
    val periods by remember { app.db.focusDao().distractions() }.collectAsStateWithLifecycle(emptyList())
    val breaks by remember { app.db.breakDao().records() }.collectAsStateWithLifecycle(emptyList())
    val coverage by remember { app.db.breakDao().coverage() }.collectAsStateWithLifecycle(null)
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) { now = System.currentTimeMillis(); delay(1000) }
        }
    }
    return FocusRecords(visits, windows, periods, now, breaks, coverage?.startedAt)
}

@Composable
fun FocusNumbers(summary: FocusSummary, breakRecorded: Boolean = true, sittingMs: Long = summary.libraryMs) {
    fun duration(value: Long) = StudyMath.clock(value)
    val efficiency = summary.efficiency(sittingMs)
    MetricDashboard(listOf(
        DashboardMetric("Sitting Time", duration(sittingMs), "Library visits and Manual timer; includes distractions; overlaps counted once"),
        DashboardMetric("Actual Study", if (summary.unknownMs > 0) "Unavailable" else duration(summary.studyMs),
            "Library and valid Manual study"),
        DashboardMetric("Distraction", duration(summary.distractionMs)),
        DashboardMetric("Break", if (breakRecorded) duration(summary.breakMs) else "Not recorded", "Manual Pause inside the library"),
        DashboardMetric("Efficiency", efficiency?.let { String.format(Locale.getDefault(), "%.1f%%", it) } ?: "—",
            "Actual Study ÷ Sitting Time", efficiency = efficiency)))
    if (summary.unknownMs > 0) Text("Unclassified time: ${duration(summary.unknownMs)}. Check Usage Access in Settings.", style = MaterialTheme.typography.bodySmall)
}

@Composable private fun MetricLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
fun LiveFocusPanel(app: StudyNestApp, today: LocalDate, setup: (() -> Unit)? = null,
    sessions: List<StudySession> = emptyList(), segments: List<Segment> = emptyList()) {
    val records = focusRecords(app)
    val zone = ZoneId.systemDefault()
    val sittingMs = remember(records.visits,sessions,segments,records.now,today,zone) {
        SittingMath.forDay(records.visits,sessions,segments,today,zone,records.now,app.engine::range)
    }
    val visit = records.visits.lastOrNull { it.end == null }
    val window = records.windows.lastOrNull { it.end == null }
    val from = today.atStartOfDay(zone).toInstant().toEpochMilli()
    val activeManual = sessions.lastOrNull { it.endedAt == null && SessionEngine.isManualTrigger(it.trigger) }
    val sessionSummary = activeManual?.let { records.summary(it.startedAt, records.now, it.id) }
    val sessionSitting = activeManual?.let { session ->
        SittingMath.forRange(records.visits, listOf(session), segments.filter { it.sessionId == session.id },
            records.now, TimeRange(session.startedAt, records.now), app.engine::range)
    }
    val summary = when {
        sessionSummary != null -> sessionSummary
        visit != null -> records.summary(visit.start)
        else -> records.summary(from)
    }
    val summarySitting = sessionSitting ?: if (visit != null) LibraryPresence.duration(visit,records.now) else sittingMs
    Panel(Modifier.testTag("live-focus-card")) {
        SectionLabel(when {
            activeManual != null -> "Current study session"
            visit != null -> "Current library visit"
            else -> "Today's focus"
        })
        if (summary.recorded) FocusNumbers(summary, breakRecorded = records.breakSince != null, sittingMs = summarySitting)
        else Text("Your next study session will appear here.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (window?.mode == "ON") {
            val state = UsageState.decode(window.tracker)
            val selected = org.json.JSONArray(window.packages).let { a -> (0 until a.length()).map { a.getString(it) } }
            if ((state.interactive && state.pkg in selected) || (window.calls && state.call))
                Text("Study paused for ${if (window.calls && state.call) "call audio" else AppCatalog.label(app, state.pkg!!)}." +
                    if (visit != null) " Sitting Time continues." else "", style = MaterialTheme.typography.bodySmall)
        }
        if (app.preferences.value.networks().isEmpty() && setup != null) {
            Text("Library Wi-Fi not configured", style = MaterialTheme.typography.bodySmall)
            OutlinedButton(onClick = setup, modifier = Modifier.fillMaxWidth().heightIn(min = StudyLayout.controlHeight)) { Text("Set up") }
        }
        if (!app.preferences.value.appMonitoring) Text("App Monitoring is off.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun FocusHistoryPanel(app: StudyNestApp, selected: LocalDate, today: LocalDate,
    sessionIds: Set<Long>? = null, from: Long? = null, until: Long? = null,
    suppliedRecords: FocusRecords? = null, suppliedSummary: FocusSummary? = null, rangeTitle: String? = null,
    showFootnote: Boolean = true, sittingMs: Long? = null) {
    val records = suppliedRecords ?: focusRecords(app)
    val zone = ZoneId.systemDefault()
    val a = from ?: selected.atStartOfDay(zone).toInstant().toEpochMilli()
    val b = until ?: selected.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
    val data = if (sessionIds == null) records else records.copy(windows = records.windows.filter { it.sessionId in sessionIds }, breaks = records.breaks.filter { it.sessionId in sessionIds })
    Panel(Modifier.testTag(if (sessionIds == null) "daily-library-summary" else "session-focus-summary")) {
        SectionLabel(rangeTitle ?: if (sessionIds == null) when(selected) {
            today -> "Today"
            today.minusDays(1) -> "Yesterday"
            else -> selected.format(java.time.format.DateTimeFormatter.ofPattern("d MMMM"))
        } else "Session focus measurements")
        if (sessionIds == null && rangeTitle == null) Text(selected.format(java.time.format.DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy")),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        val summary = suppliedSummary ?: data.summary(a, b)
        if (summary.recorded) {
            FocusNumbers(summary, breakRecorded = records.breakSince?.let { b > it } == true, sittingMs = sittingMs ?: summary.libraryMs)
            if (records.breakSince?.let { a < it && b > it } == true)
                Text("Break recording started with this update at ${Instant.ofEpochMilli(records.breakSince).atZone(zone).format(java.time.format.DateTimeFormatter.ofPattern("d MMM, h:mm a"))}. Earlier breaks were not recorded.", style = MaterialTheme.typography.bodySmall)
        }
        else Text("These measurements are unavailable for older history. Existing study records are preserved.")
        if (summary.libraryDistractionMs > 0 && summary.libraryBreakdown.any { it.value > 0 }) {
            var expanded by rememberSaveable(selected.toString(), sessionIds?.toString()) { mutableStateOf(false) }
            TextButton(onClick = { expanded = !expanded }, modifier = Modifier.fillMaxWidth()) {
                Text("Distraction breakdown", Modifier.weight(1f))
                Icon(if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, null)
            }
            if (expanded) summary.libraryBreakdown.entries.filter {it.value > 0}.sortedByDescending { it.value }.forEach { (pkg, ms) ->
                val label = remember(pkg) { if (pkg == "Call audio") pkg else AppCatalog.label(app, pkg) }
                MetricLine(label, StudyMath.duration(ms))
            }
        }
        if (showFootnote && summary.recorded) Text("Sitting Time includes library visits and Manual timer time. Distractions count; overlapping time is counted once.", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FocusPeriodOverview(records: FocusRecords, today: LocalDate, totals: Map<LocalDate, Long>, streaks: Streaks, dailySummaries: Map<LocalDate,FocusSummary>? = null,
    simplified: Boolean = false, sittingByDay: Map<LocalDate,Long> = emptyMap()) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var monthly by rememberSaveable { mutableStateOf(false) }
    Panel(Modifier.testTag("period-report")) {
        TextButton(onClick = { expanded = !expanded }, modifier = Modifier.fillMaxWidth()) {
            Text("Week & Month Report", Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
            Icon(if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, null)
        }
        if (expanded) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(StudyLayout.itemGap)) {
                FilterChip(!monthly, { monthly = false }, label = { Text("This week") })
                FilterChip(monthly, { monthly = true }, label = { Text("This month") })
            }
            val start = if (monthly) today.withDayOfMonth(1) else today.minusDays((today.dayOfWeek.value - 1).toLong())
            val summary = dailySummaries?.let { combineFocusDays(it.filterKeys {day -> day in start..today}.values.toList()) }
                ?: records.summary(start.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(), records.now)
            val sitting = sittingByDay.filterKeys { it in start..today }.values.sum()
            if (summary.recorded) {
                if(simplified) MetricLine("Efficiency", summary.efficiency(sitting)?.let {String.format(Locale.getDefault(),"%.1f%%",it)} ?: "—")
                else FocusNumbers(summary, breakRecorded = records.breakSince != null, sittingMs = sitting)
                if (!simplified && records.breakSince?.let { start.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() < it } == true)
                    Text("Break totals include only time recorded since this update.", style = MaterialTheme.typography.bodySmall)
            } else Text("No focus measurements for this period yet.")
        }
    }
}
