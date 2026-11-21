package com.studynest.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.studynest.app.domain.StudyMath
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlin.math.ceil
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StudyChart(totals: Map<LocalDate, Long>, today: LocalDate, title: String = "Study hours",
    secondaryTotals: Map<LocalDate, Long>? = null, firstLabel: String = "Study",
    secondLabel: String = "", collapsible: Boolean = false, selectDate: (LocalDate) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(!collapsible) }
    var monthly by rememberSaveable { mutableStateOf(false) }
    var anchorText by rememberSaveable { mutableStateOf(today.toString()) }
    var selectedText by rememberSaveable { mutableStateOf(today.toString()) }
    val anchor = LocalDate.parse(anchorText)
    val start =
        if (monthly) anchor.withDayOfMonth(1)
        else anchor.minusDays((anchor.dayOfWeek.value - 1).toLong())
    val count = if (monthly) YearMonth.from(anchor).lengthOfMonth() else 7
    val dates = List(count) { start.plusDays(it.toLong()) }
    val hours = dates.map { (totals[it] ?: 0L) / 3600000f }
    val secondaryHours = dates.map { (secondaryTotals?.get(it) ?: 0L) / 3600000f }
    val ceiling = ceil((hours + secondaryHours).maxOrNull()?.toDouble() ?: 0.0).toFloat().coerceAtLeast(1f)
    val selected = LocalDate.parse(selectedText)
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.tertiary
    val grid = MaterialTheme.colorScheme.outlineVariant
    val fmt = DateTimeFormatter.ofPattern("d MMM")
    Panel(Modifier.testTag(if (secondaryTotals != null) "break-distraction-trend" else "study-chart")) {
        if (collapsible) {
            TextButton(onClick={expanded=!expanded}, modifier=Modifier.fillMaxWidth().testTag("trend-toggle")) {
                Text(title, Modifier.weight(1f), style=MaterialTheme.typography.titleMedium)
                Icon(if(expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    if(expanded) "Collapse trend" else "Expand trend")
            }
        } else SectionLabel(title)
        if (!expanded) return@Panel
        if (secondaryTotals != null) {
            FlowRow(horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                Text("● $firstLabel", color=primary)
                Text("● $secondLabel", color=secondary)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(!monthly, { monthly = false }, label = { Text("Week") })
            FilterChip(monthly, { monthly = true }, label = { Text("Month") })
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            IconButton(
                onClick = {
                    anchorText =
                        (if (monthly) anchor.minusMonths(1) else anchor.minusWeeks(1)).toString()
                }) {
                    Icon(Icons.Outlined.ChevronLeft, "Earlier graph period")
                }
            Text(
                if (monthly) anchor.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
                else "${start.format(fmt)} – ${dates.last().format(fmt)}",
                modifier = Modifier.weight(1f))
            IconButton(
                enabled = dates.last() < today,
                onClick = {
                    anchorText =
                        (if (monthly) anchor.plusMonths(1) else anchor.plusWeeks(1)).toString()
                }) {
                    Icon(Icons.Outlined.ChevronRight, "Later graph period")
                }
        }
        Row(Modifier.fillMaxWidth().height(180.dp)) {
            Column(
                Modifier.width(40.dp).fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween) {
                    Text("${ceiling.toInt()}h", style = MaterialTheme.typography.labelSmall)
                    Text("${ceiling / 2}h", style = MaterialTheme.typography.labelSmall)
                    Text("0h", style = MaterialTheme.typography.labelSmall)
                }
            Canvas(
                Modifier.weight(1f)
                    .fillMaxHeight()
                    .semantics {
                        contentDescription =
                            dates
                                .filter { it <= today }
                                .joinToString {
                                    "${it.format(fmt)}: $firstLabel ${StudyMath.duration(totals[it] ?: 0)}" +
                                        (secondaryTotals?.let { other -> ", $secondLabel ${StudyMath.duration(other[it] ?: 0)}" } ?: "")
                                }
                    }
                    .pointerInput(start, monthly, totals, secondaryTotals) {
                        detectTapGestures { point ->
                            val i =
                                ((point.x - 8.dp.toPx()) / (size.width - 16.dp.toPx()) *
                                        (count - 1))
                                    .roundToInt()
                                    .coerceIn(0, count - 1)
                            if (dates[i] <= today) {
                                selectedText = dates[i].toString()
                                selectDate(dates[i])
                            }
                        }
                    }) {
                    val pad = 8.dp.toPx()
                    val h = size.height - 2 * pad
                    val w = size.width - 2 * pad
                    repeat(3) { n ->
                        val y = pad + n * h / 2
                        drawLine(grid, Offset(pad, y), Offset(size.width - pad, y), 1.dp.toPx())
                    }
                    fun series(values: List<Float>, color: androidx.compose.ui.graphics.Color) {
                    val path = Path()
                    dates.forEachIndexed { i, date ->
                        if (date <= today) {
                            val x = pad + w * i / (count - 1)
                            val y = pad + h * (1 - values[i] / ceiling)
                            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                            drawCircle(
                                color,
                                if (date == selected) 5.dp.toPx() else 2.5.dp.toPx(),
                                Offset(x, y))
                        }
                    }
                    drawPath(path, color, style = Stroke(2.dp.toPx()))
                    }
                    series(hours, primary)
                    if (secondaryTotals != null) series(secondaryHours, secondary)
                }
        }
        Row(
            Modifier.fillMaxWidth().padding(start = 40.dp),
            horizontalArrangement = Arrangement.SpaceBetween) {
                if (monthly)
                    listOf(1, 8, 15, 22, count).forEach {
                        Text("$it", style = MaterialTheme.typography.labelSmall)
                    }
                else
                    dates.forEach {
                        Text(
                            it.format(DateTimeFormatter.ofPattern("EEEEE")),
                            style = MaterialTheme.typography.labelSmall)
                    }
            }
        Text("${if(secondaryTotals==null) "Period total" else "$firstLabel total"}: ${StudyMath.duration(dates.filter { it <= today }.sumOf { totals[it] ?: 0 })}")
        secondaryTotals?.let { other ->
            Text("$secondLabel total: ${StudyMath.duration(dates.filter { it <= today }.sumOf { other[it] ?: 0 })}")
        }
        if (selected in dates && selected <= today) {
            Text("${selected.format(fmt)}: ${if(secondaryTotals==null) "" else "$firstLabel "}${StudyMath.duration(totals[selected] ?: 0)}" +
                (secondaryTotals?.let { ", $secondLabel ${StudyMath.duration(it[selected] ?: 0)}" } ?: ""),
                style = MaterialTheme.typography.bodyMedium)
        }
        Text(
            "Tap a point for its hours and sessions below. Rest days count as zero; future days are blank.",
            style = MaterialTheme.typography.bodySmall)
    }
}
