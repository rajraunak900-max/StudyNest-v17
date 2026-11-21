package com.studynest.app.ui

import androidx.compose.foundation.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.studynest.app.domain.*
import java.time.*
import java.time.format.DateTimeFormatter
import kotlin.math.ceil
import kotlin.math.roundToInt

/** Center today's group where possible, respecting the actual measured range. */
internal fun chartInitialOffset(index: Int, groupWidth: Float, viewport: Float, maximum: Int): Int =
    if (index < 0) 0 else ((index + 0.5f) * groupWidth - viewport / 2).roundToInt().coerceIn(0, maximum)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ComparativeStudyChart(days: Map<LocalDate,HistoryDay>, today: LocalDate, selected: LocalDate,
    selectDate: (LocalDate) -> Unit) {
    var monthMode by rememberSaveable { mutableStateOf(false) }
    var anchorText by rememberSaveable { mutableStateOf(today.toString()) }
    val anchor=LocalDate.parse(anchorText)
    LaunchedEffect(today) { if(anchor >= today.minusDays(1)) anchorText=today.toString() }
    val first=if(monthMode) anchor.withDayOfMonth(1) else anchor.minusDays((anchor.dayOfWeek.value-1).toLong())
    val last=if(monthMode) first.withDayOfMonth(first.lengthOfMonth()) else first.plusDays(6)
    val dates=(0 until if(monthMode)first.lengthOfMonth() else 7).map {first.plusDays(it.toLong())}
    val summary=remember(days,first,last,today){HistoryMath.period(days,first,last,today)}
    val maxMs=dates.filter {it<=today}.maxOfOrNull {maxOf(days[it]?.actual ?: 0,days[it]?.sitting ?: 0)} ?: 0
    val maxHours=ceil(maxMs / 3_600_000.0).coerceAtLeast(1.0)
    val primary=MaterialTheme.colorScheme.primary
    val library=MaterialTheme.colorScheme.tertiary
    val gridColor=MaterialTheme.colorScheme.outlineVariant
    Panel(Modifier.testTag("history-chart")) {
        SettingHeading(Icons.Outlined.BarChart,"Study hours","Your daily Sitting Time and Actual Study across library and Manual sessions.")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(!monthMode,{monthMode=false},label={Text("Week")},modifier=Modifier.weight(1f).heightIn(min=48.dp),colors=FilterChipDefaults.filterChipColors(selectedContainerColor=primary,selectedLabelColor=MaterialTheme.colorScheme.onPrimary))
            FilterChip(monthMode,{monthMode=true},label={Text("Month")},modifier=Modifier.weight(1f).heightIn(min=48.dp),colors=FilterChipDefaults.filterChipColors(selectedContainerColor=primary,selectedLabelColor=MaterialTheme.colorScheme.onPrimary))
        }
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            IconButton(onClick={anchorText=(if(monthMode)anchor.minusMonths(1) else anchor.minusWeeks(1)).toString()}) {Icon(Icons.Outlined.ChevronLeft,"Previous period")}
            Text(if(monthMode)first.format(DateTimeFormatter.ofPattern("MMMM yyyy")) else
                "${first.format(DateTimeFormatter.ofPattern("d MMM"))} – ${last.format(DateTimeFormatter.ofPattern("d MMM"))}",Modifier.weight(1f),style=MaterialTheme.typography.titleMedium)
            IconButton(enabled=last<today,onClick={anchorText=(if(monthMode)anchor.plusMonths(1) else anchor.plusWeeks(1)).toString()}) {Icon(Icons.Outlined.ChevronRight,"Next period")}
        }
        FlowRow(horizontalArrangement=Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(12.dp).background(primary,CircleShape));Text("Actual study time",style=MaterialTheme.typography.labelLarge)
            }
            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(12.dp).background(library,CircleShape));Text("Sitting Time",style=MaterialTheme.typography.labelLarge)
            }
        }
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val groupWidth=maxOf(48.dp * LocalDensity.current.fontScale, if(monthMode)68.dp else (maxWidth-36.dp)/7)
            val scroll=rememberScrollState()
            // Save this flag with the scroll state so rotation/data recomposition
            // preserves the user's position. A newly selected period focuses once.
            var positioned by rememberSaveable(first.toString(),monthMode,today.toString()) { mutableStateOf(false) }
            val density=LocalDensity.current
            val viewport=with(density){(maxWidth-36.dp).toPx()}
            val groupPixels=with(density){groupWidth.toPx()}
            LaunchedEffect(first,monthMode,today,scroll.maxValue,viewport) {
                if(!positioned && scroll.maxValue!=Int.MAX_VALUE) {
                    scroll.scrollTo(chartInitialOffset(dates.indexOf(today),groupPixels,viewport,scroll.maxValue))
                    positioned=true
                }
            }
            Row {
                Column(Modifier.width(36.dp).height(180.dp),verticalArrangement=Arrangement.SpaceBetween) {
                    listOf(maxHours,maxHours*2/3,maxHours/3,0.0).forEach {Text("${"%.1f".format(it).removeSuffix(".0")}h",style=MaterialTheme.typography.labelSmall)}
                }
                Box(Modifier.weight(1f).testTag("history-chart-viewport")) {
                Canvas(Modifier.fillMaxWidth().height(180.dp)) {
                    repeat(4) { index -> val y=size.height * index / 3
                        drawLine(gridColor,Offset(0f,y),Offset(size.width,y),pathEffect=PathEffect.dashPathEffect(floatArrayOf(8f,6f)))
                    }
                }
                Row(Modifier.horizontalScroll(scroll).testTag("history-chart-scroll")) {
                    dates.forEach {day ->
                        val data=days[day] ?: HistoryDay(0,0,0,0)
                        val future=day>today
                        Column(Modifier.width(groupWidth).semantics {
                            contentDescription=if(future) "$day, future date" else "$day, Actual Study ${data.actual?.let(StudyMath::clock) ?: "Unavailable"}, Sitting Time ${StudyMath.clock(data.sitting)}"
                        }.testTag("chart-day-$day").clickable(enabled=!future){selectDate(day)}.padding(horizontal=4.dp,vertical=4.dp),
                            horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(6.dp)) {
                            Row(Modifier.height(180.dp).fillMaxWidth(),verticalAlignment=Alignment.Bottom,horizontalArrangement=Arrangement.spacedBy(4.dp,Alignment.CenterHorizontally)) {
                                fun height(ms:Long)=(180.0 * ms / (maxHours*3_600_000)).coerceIn(1.0,180.0).dp
                                if(!future) {
                                    data.actual?.let {Box(Modifier.width(14.dp).height(height(it)).background(primary, RoundedCornerShape(topStart=6.dp,topEnd=6.dp)))}
                                    Box(Modifier.width(14.dp).height(height(data.sitting)).background(library, RoundedCornerShape(topStart=6.dp,topEnd=6.dp)))
                                }
                            }
                            Text(if(monthMode)day.dayOfMonth.toString() else day.format(DateTimeFormatter.ofPattern("EEE")),style=MaterialTheme.typography.labelSmall)
                            if(!monthMode)Text(day.format(DateTimeFormatter.ofPattern("d MMM")),style=MaterialTheme.typography.labelSmall)
                            if(day==selected)Text("●",color=primary,style=MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                }
            }
        }
        Text("Swipe sideways for all dates if needed. Tap a day for exact values.",style=MaterialTheme.typography.bodySmall)
        if(selected in first..last && selected<=today) {
            val data=days[selected] ?: HistoryDay(0,0,0,0)
            InformationStrip("${selected.format(DateTimeFormatter.ofPattern("EEE, d MMM"))}\nActual Study: ${data.actual?.let(StudyMath::clock) ?: "Unavailable"}\nSitting Time: ${StudyMath.clock(data.sitting)}")
        }
        PeriodSummary(summary,"Actual Study period total")
        Text("Actual Study excludes distractions and pauses. Rest days count as zero; future days are blank. Unavailable measurements stay unavailable.",style=MaterialTheme.typography.bodySmall)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PeriodSummary(summary: HistoryPeriod, totalLabel: String) {
    Surface(shape=RoundedCornerShape(20.dp),color=MaterialTheme.colorScheme.surface) {
        FlowRow(Modifier.fillMaxWidth().padding(16.dp),horizontalArrangement=Arrangement.spacedBy(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            SummaryValue(Icons.Outlined.Schedule,totalLabel,summary.actual?.let(StudyMath::duration) ?: "Unavailable","Sitting Time: ${StudyMath.duration(summary.sitting)}")
            SummaryValue(Icons.Outlined.MenuBook,"Daily average",summary.average?.let(StudyMath::duration) ?: "Unavailable","${summary.elapsedDays} elapsed days")
            SummaryValue(Icons.Outlined.BarChart,"Best day",summary.best?.second?.let(StudyMath::duration) ?: if(summary.actual==null)"Unavailable" else "0h 0m",
                summary.best?.first?.format(DateTimeFormatter.ofPattern("EEE, d MMM")) ?: "")
        }
    }
}

@Composable
private fun SummaryValue(icon: androidx.compose.ui.graphics.vector.ImageVector,label:String,value:String,detail:String) {
    Column(Modifier.widthIn(max=220.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
        Icon(icon,null,tint=MaterialTheme.colorScheme.primary)
        Text(label,style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
        SectionLabel(value)
        Text(detail,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
