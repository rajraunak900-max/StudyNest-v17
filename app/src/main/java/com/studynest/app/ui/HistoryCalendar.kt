package com.studynest.app.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.studynest.app.domain.*
import java.time.*
import java.time.format.DateTimeFormatter

@Composable
fun HistoryCalendar(days:Map<LocalDate,HistoryDay>,month:YearMonth,selected:LocalDate,today:LocalDate,
    onMonth:(YearMonth)->Unit,onDate:(LocalDate)->Unit) {
    val first=month.atDay(1);val last=month.atEndOfMonth()
    val maxAmount=(1..month.lengthOfMonth()).map {month.atDay(it)}.filter {it<=today}.maxOfOrNull {days[it]?.actual ?: 0} ?: 0L
    Panel(Modifier.testTag("history-calendar"), padding = 8.dp) {
        SettingHeading(Icons.Outlined.CalendarMonth,"Calendar","Tap a date to see its study sessions.")
        OutlinedButton(onClick={onMonth(YearMonth.from(today));onDate(today)}) {Text("This month")}
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            IconButton(onClick={onMonth(month.minusMonths(1))}) {Icon(Icons.Outlined.ChevronLeft,"Previous month")}
            Text(month.format(DateTimeFormatter.ofPattern("MMMM yyyy")),Modifier.weight(1f),style=MaterialTheme.typography.titleMedium)
            IconButton(enabled=month<YearMonth.from(today),onClick={onMonth(month.plusMonths(1))}) {Icon(Icons.Outlined.ChevronRight,"Next month")}
        }
        Text("A • Actual Study   S • Sitting Time",style=MaterialTheme.typography.labelMedium)
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val density=LocalDensity.current
            val measurer=rememberTextMeasurer()
            val style=MaterialTheme.typography.labelSmall
            val valueWidth=remember(density,style) {
                with(density){measurer.measure(AnnotatedString(calendarDuration(25*3_600_000L)),style).size.width.toDp()}
            }
            // Seven columns fit normal phones. Large text may scroll only if
            // needed to keep a complete compact value readable, never markers.
            val viewportWidth=maxWidth
            val gridWidth=maxOf(viewportWidth,(valueWidth+6.dp)*7+18.dp)
            val scroll=rememberScrollState()
            LaunchedEffect(month){scroll.scrollTo(0)}
            Column {
            Column(Modifier.horizontalScroll(scroll).width(gridWidth).testTag("calendar-grid"),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                Row {
                    listOf("Mon","Tue","Wed","Thu","Fri","Sat","Sun").forEach {
                        Box(Modifier.weight(1f),contentAlignment=Alignment.Center){Text(it,style=MaterialTheme.typography.labelSmall)}
                    }
                }
                val offset=first.dayOfWeek.value-1
                repeat((offset+month.lengthOfMonth()+6)/7) {row ->
                    Row(horizontalArrangement=Arrangement.spacedBy(3.dp)) {
                        repeat(7) {column ->
                            val number=row*7+column-offset+1
                            if(number !in 1..month.lengthOfMonth())Spacer(Modifier.weight(1f).heightIn(min=76.dp))
                            else {
                                val day=month.atDay(number);val data=days[day] ?: HistoryDay(0,0,0,0)
                                val isSelected=day==selected;val future=day>today
                                val alpha=if(maxAmount>0) ((data.actual ?: 0).toFloat()/maxAmount).coerceIn(0f,1f) * 0.20f + 0.04f else 0.04f
                                val background=if(isSelected)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha=alpha)
                                val foreground=if(isSelected)MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                Column(Modifier.weight(1f).heightIn(min=88.dp).background(background,RoundedCornerShape(12.dp))
                                    .semantics {contentDescription=if(future) "$day, future date" else "$day, Actual Study ${data.actual?.let(StudyMath::clock) ?: "Unavailable"}, Sitting Time ${StudyMath.clock(data.sitting)}"}
                                    .testTag("calendar-day-$day").clickable(enabled=!future){onDate(day)}.padding(horizontal=2.dp,vertical=8.dp),
                                    horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(4.dp)) {
                                    Text(number.toString(),style=MaterialTheme.typography.labelLarge,color=foreground)
                                    val hasData=(data.actual ?: 0)>0 || data.sitting>0 || data.tracked>0 || data.distraction>0 || data.actual==null
                                    if(!future && hasData) {
                                        Text("A ${data.actual?.let { calendarDuration(it) } ?: "—"}",style=style,color=foreground,textAlign=TextAlign.Center)
                                        Text("S ${calendarDuration(data.sitting)}",style=style,color=foreground,textAlign=TextAlign.Center)
                                    }
                                }
                            }
                        }
                    }
                }
                
            }
            if(gridWidth>viewportWidth)Text("Swipe sideways for all seven days.",style=MaterialTheme.typography.bodySmall)
            }
        }
    }
}
