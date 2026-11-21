package com.studynest.app.domain

import com.studynest.app.data.StudyTask
import java.time.LocalDate

data class TaskSummary(val total: Int, val completed: Int, val dueToday: Int, val overdue: Int)

fun taskSummary(tasks: List<StudyTask>, today: LocalDate): TaskSummary {
    val unfinished = tasks.filterNot { it.completed }
    fun due(task: StudyTask) = task.dueDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
    return TaskSummary(tasks.size, tasks.count { it.completed },
        unfinished.count { due(it) == today }, unfinished.count { due(it)?.isBefore(today) == true })
}
