package com.studynest.app

import com.studynest.app.data.StudyTask
import com.studynest.app.domain.*
import org.junit.Test
import org.junit.Assert.*
import java.time.*

class TaskSummaryTest {
    private val today=LocalDate.of(2026,10,2)
    @Test fun noTasksAndUndatedTasksHaveRealZeros() {
        assertEquals(TaskSummary(0,0,0,0),taskSummary(emptyList(),today))
        assertEquals(TaskSummary(3,0,0,0),taskSummary(List(3){ StudyTask(title="Task $it") },today))
    }
    @Test fun mixedDueDatesExcludeCompletedTasksAndFutureDates() {
        val tasks=listOf(StudyTask(title="Today",dueDate=today.toString()),
            StudyTask(title="Late",dueDate=today.minusDays(1).toString()),
            StudyTask(title="Done late",dueDate=today.minusDays(1).toString(),completed=true),
            StudyTask(title="Later",dueDate=today.plusDays(1).toString()),StudyTask(title="Undated"))
        assertEquals(TaskSummary(5,1,1,1),taskSummary(tasks,today))
        assertEquals(TaskSummary(5,1,1,2),taskSummary(tasks,today.plusDays(1)))
    }
    @Test fun localDateAndInvalidLegacyDateAreHandledSafely() {
        val instant=Instant.parse("2026-10-01T20:00:00Z")
        val local=instant.atZone(ZoneId.of("Asia/Kolkata")).toLocalDate()
        assertEquals(today,local)
        assertEquals(TaskSummary(2,0,1,0),taskSummary(listOf(StudyTask(title="Local",dueDate=today.toString()),
            StudyTask(title="Old invalid",dueDate="bad-date")),local))
    }
}
