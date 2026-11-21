package com.studynest.app.ui

import android.app.DatePickerDialog
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import com.studynest.app.StudyNestApp
import com.studynest.app.data.*
import com.studynest.app.domain.*
import java.time.*
import java.time.format.DateTimeFormatter

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun TasksScreen(app: StudyNestApp, tasks: List<StudyTask>) {
    var edit by remember { mutableStateOf<StudyTask?>(null) }
    var delete by remember { mutableStateOf<StudyTask?>(null) }
    var completed by rememberSaveable { mutableStateOf(false) }
    edit?.let { task ->
        TaskEditor(task, { edit = null }) { updated ->
            app.runTask { app.db.dao().saveTask(updated) }
            edit = null
        }
        return
    }
    Page("Your tasks", "Clear your mind. Make a little list.") {
        Button(onClick = { edit = StudyTask(title = "") }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Outlined.Add, null)
            Text("Add task")
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                !completed,
                { completed = false },
                label = { Text("To do (${tasks.count { !it.completed }})") })
            FilterChip(
                completed,
                { completed = true },
                label = { Text("Completed (${tasks.count { it.completed }})") })
        }
        val visible = tasks.filter { it.completed == completed }
        if (visible.isEmpty())
            Panel {
                SectionLabel(if (completed) "No completed tasks yet" else "A fresh page")
                Text(
                    if (completed) "Your completed tasks will appear here."
                    else "Add your first task and choose what matters today.")
            }
        visible.forEach { task ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            task.completed,
                            { app.runTask { app.db.dao().saveTask(task.copy(completed = it)) } })
                        Column(Modifier.weight(1f).clickable { edit = task }) {
                            Text(task.title, style = MaterialTheme.typography.titleMedium)
                            PriorityBadge(task.priority)
                            task.dueDate?.let { Text("Due $it", style = MaterialTheme.typography.bodySmall) }
                        }
                    }
                    Row(Modifier.align(Alignment.End)) {
                        TextButton(onClick = { edit = task }) { Text("Edit") }
                        TextButton(onClick = { delete = task }) { Text("Delete") }
                    }
                }
            }
        }
    }
    delete?.let { t ->
        AlertDialog(
            onDismissRequest = { delete = null },
            title = { Text("Delete task?") },
            text = { Text(t.title) },
            confirmButton = {
                TextButton(
                    onClick = {
                        app.runTask { app.db.dao().deleteTask(t) }
                        delete = null
                    }) {
                        Text("Delete")
                    }
            },
            dismissButton = { TextButton(onClick = { delete = null }) { Text("Cancel") } })
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun TaskEditor(task: StudyTask, dismiss: () -> Unit, save: (StudyTask) -> Unit) {
    var title by rememberSaveable(task.id) { mutableStateOf(task.title) }
    var due by rememberSaveable(task.id) { mutableStateOf(task.dueDate) }
    var priority by rememberSaveable(task.id) { mutableIntStateOf(task.priority) }
    val context = LocalContext.current
    Page(
        if (task.id == 0L) "A new task" else "Edit task", "Give your next step a little clarity.") {
            OutlinedTextField(
                title,
                { title = it.take(250) },
                label = { Text("What will you work on?") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2)
            Text("Priority")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Normal", "Medium", "High").forEachIndexed { i, name ->
                    FilterChip(priority == i, { priority = i }, label = { Text(name) })
                }
            }
            OutlinedButton(
                onClick = {
                    val d = due?.let(LocalDate::parse) ?: LocalDate.now()
                    DatePickerDialog(
                            context,
                            { _, y, m, day -> due = LocalDate.of(y, m + 1, day).toString() },
                            d.year,
                            d.monthValue - 1,
                            d.dayOfMonth)
                        .show()
                }) {
                    Icon(Icons.Outlined.CalendarMonth, null)
                    Text(due ?: "Set due date (optional)")
                }
            if (due != null) TextButton(onClick = { due = null }) { Text("Remove due date") }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = dismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
                Button(
                    enabled = title.isNotBlank(),
                    onClick = {
                        save(task.copy(title = title.trim(), dueDate = due, priority = priority))
                    },
                    modifier = Modifier.weight(1f)) {
                        Text("Save")
                    }
            }
        }
}

@Composable
private fun PriorityBadge(priority: Int) {
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val colour = when (priority) {
        2 -> if (dark) androidx.compose.ui.graphics.Color(0xFFFF9A9A) else androidx.compose.ui.graphics.Color(0xFFB3261E)
        1 -> if (dark) androidx.compose.ui.graphics.Color(0xFFFFD54F) else androidx.compose.ui.graphics.Color(0xFF805900)
        else -> if (dark) androidx.compose.ui.graphics.Color(0xFF81C995) else androidx.compose.ui.graphics.Color(0xFF216E39)
    }
    val dot = when (priority) {
        2 -> androidx.compose.ui.graphics.Color(0xFFEF5350)
        1 -> androidx.compose.ui.graphics.Color(0xFFFBC02D)
        else -> androidx.compose.ui.graphics.Color(0xFF43A047)
    }
    Surface(color = dot.copy(alpha = 0.12f), shape = RoundedCornerShape(8.dp)) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Box(Modifier.size(8.dp).background(dot, androidx.compose.foundation.shape.CircleShape))
            Text("${listOf("Normal", "Medium", "High")[priority.coerceIn(0, 2)]} priority", color = colour,
                style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
internal fun StudyPeriodDetails(app: StudyNestApp, period: StudyPeriod, number: Int, day: LocalDate, zone: ZoneId,
    sessions: List<StudySession>, segments: List<Segment>, back: () -> Unit) {
    val records = focusRecords(app)
    val fmt = DateTimeFormatter.ofPattern("h:mm:ss a")
    fun time(ms: Long) = Instant.ofEpochMilli(ms).atZone(zone).format(fmt)
    Page("Session $number", day.format(DateTimeFormatter.ofPattern("d MMMM yyyy"))) {
        OutlinedButton(onClick = back) { Text("Back to history") }
        Panel {
            Text("Total study time: ${StudyMath.duration(period.studyMs)}", style = MaterialTheme.typography.titleLarge)
            Text("Start: ${time(period.start)}")
            Text("End: ${time(period.end)}")
            Text("Gap between tracked periods: ${StudyMath.duration(period.breakMs)}")
            Text("Time between recorded activity is excluded. Session grouping does not change your daily totals.", style = MaterialTheme.typography.bodySmall)
        }
        val sitting = remember(records.visits,sessions,segments,records.now,period.start,period.end) {
            SittingMath.forRange(records.visits, sessions, segments, records.now, TimeRange(period.start, period.end), app.engine::range)
        }
        FocusHistoryPanel(app, day, LocalDate.now(), period.parts.map { it.sessionId }.toSet(), period.start, period.end,
            suppliedRecords=records, sittingMs=sitting)
        SectionLabel("Tracking activity")
        period.parts.forEachIndexed { i, part ->
            if (i > 0) {
                val gap = part.range.start - period.parts[i - 1].range.end
                if (gap > 0) Text("Untracked gap: ${StudyMath.clock(gap)}", style = MaterialTheme.typography.bodySmall)
            }
            Panel {
                SectionLabel(if (part.trigger == SessionEngine.MANUAL_AUTO_TRIGGER) "Manual" else part.trigger)
                Text("${time(part.range.start)} – ${time(part.range.end)}")
                Text(StudyMath.clock(part.range.end - part.range.start))
            }
        }
    }
}

