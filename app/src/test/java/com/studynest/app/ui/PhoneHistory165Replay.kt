// Test-only stable 1.6.5 History.kt; renamed entry and cross-module null assertions only.
// Archived test copy: main-module return types are unresolved by Compose test-source lint.
@file:Suppress("RememberReturnType")

package com.studynest.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.studynest.app.StudyNestApp
import com.studynest.app.data.*
import com.studynest.app.domain.*
import java.time.*
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PhoneHistory165Replay(app: StudyNestApp, sessions:List<StudySession>,segments:List<Segment>,
    totals:Map<LocalDate,Long>,streaks:Streaks,today:LocalDate) {
    val records=focusRecords(app)
    val tabState=rememberSaveableStateHolder()
    val zone=ZoneId.systemDefault()
    var tab by rememberSaveable {mutableStateOf("Tracked study")}
    var selectedText by rememberSaveable {mutableStateOf(today.toString())}
    var monthText by rememberSaveable {mutableStateOf(YearMonth.from(today).toString())}
    var followToday by rememberSaveable {mutableStateOf(true)}
    LaunchedEffect(today) {if(followToday){selectedText=today.toString();monthText=YearMonth.from(today).toString()}}
    val selected=LocalDate.parse(selectedText);val month=YearMonth.parse(monthText)
    fun select(day:LocalDate){followToday=day==today;selectedText=day.toString();monthText=YearMonth.from(day).toString()}
    val index=remember(records.visits,records.windows,records.periods,records.breaks,zone){
        HistoryIndex(records.visits,records.windows,records.periods,records.breaks,zone,records.now)
    }
    val summaries=remember(index,records.now){index.live(records.now)}
    val days=remember(summaries,totals){(summaries.keys+totals.keys).associateWith {HistoryDay.from(summaries[it],totals[it] ?: 0)}}
    val qualifying=remember(records.visits,selected,zone){VisitQualification.forDay(records.visits,selected,zone)}
    val trackingIndex=remember(sessions,segments,zone){HistoryTrackingIndex(sessions,segments,zone)}
    val exclusions=remember(index,selected,records.now){index.visitCoverage(selected,records.now)}
    val periods=remember(trackingIndex,selected,exclusions,totals[selected]){trackingIndex.periods(selected,exclusions,app.engine::range)}
    var visitKey by rememberSaveable {mutableStateOf<Long?>(null)}
    var periodKey by rememberSaveable {mutableStateOf<String?>(null)}
    val visit=qualifying.firstOrNull {it.id==visitKey}
    val periodIndex=periods.indexOfFirst {it.key==periodKey}
    if(visit!=null) {
        BackHandler {visitKey=null}
        LibrarySessionDetails(app,visit,selected,records,zone){visitKey=null};return
    }
    if(periodIndex>=0) {
        BackHandler {periodKey=null}
        StudyPeriodDetails(app,periods[periodIndex],periodIndex+1,selected,zone,sessions,segments){periodKey=null};return
    }
    Page("History","Track your study time and library visits.") {
        BoxWithConstraints(Modifier.fillMaxWidth().testTag("history-navigation")) {
            val stacked=maxWidth < 330.dp * LocalDensity.current.fontScale
            @Composable fun Tab(name:String,icon:androidx.compose.ui.graphics.vector.ImageVector,modifier:Modifier) {
                val active=tab==name
                Surface(onClick={tab=name},color=if(active)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerLow,
                    contentColor=if(active)MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    shape=androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                    modifier=modifier.heightIn(min=48.dp).testTag("history-tab-$name")) {
                    if(stacked) Row(Modifier.padding(12.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                        Icon(icon,null,Modifier.size(24.dp))
                        Text(name,style=MaterialTheme.typography.labelLarge)
                    } else Column(Modifier.padding(10.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(6.dp,Alignment.CenterVertically)) {
                        Icon(icon,null,Modifier.size(24.dp))
                        Text(name,style=MaterialTheme.typography.labelLarge,textAlign=androidx.compose.ui.text.style.TextAlign.Center)
                    }
                }
            }
            val tabs=listOf("Tracked study" to Icons.Outlined.BarChart,"Calendar" to Icons.Outlined.CalendarMonth,"Session info" to Icons.Outlined.ListAlt)
            if(stacked) Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                tabs.forEach {(name,icon)->Tab(name,icon,Modifier.fillMaxWidth())}
            } else Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                tabs.forEach {(name,icon)->Tab(name,icon,Modifier.weight(1f).fillMaxHeight().heightIn(min=64.dp))}
            }
        }
        tabState.SaveableStateProvider(tab) { when(tab) {
            "Tracked study" -> ComparativeStudyChart(days,today,selected,::select)
            "Calendar" -> HistoryCalendar(days,month,selected,today,{monthText=it.toString()},::select)
        }
        }
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            Text(if(tab=="Calendar") "Selected Date Details" else if(selected==today)"Today's Session Report" else "Selected Day Report",Modifier.weight(1f),style=MaterialTheme.typography.titleMedium)
        }
        Text(selected.format(DateTimeFormatter.ofPattern("d MMMM yyyy")),style=MaterialTheme.typography.titleSmall)
        Text("Qualifying library sessions: ${qualifying.size}",Modifier.testTag("qualifying-session-count"),style=MaterialTheme.typography.labelLarge)
        if(periods.isNotEmpty()) Text("Manual / earlier study periods: ${periods.size}",style=MaterialTheme.typography.bodySmall)
        Text("Tracked study: ${StudyMath.duration(totals[selected] ?: 0)} • breaks excluded",style=MaterialTheme.typography.bodySmall)
        if(tab!="Session info") {
            OutlinedButton(onClick={tab="Session info"},modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)) {Text("View all sessions");Icon(Icons.Outlined.ChevronRight,null)}
            FocusHistoryPanel(app,selected,today,suppliedRecords=records,suppliedSummary=summaries[selected] ?: FocusSummary())
        }
        if(tab=="Tracked study") {
            AdditionalHistoryMetrics(summaries,totals,today,::select)
            FocusPeriodOverview(records,today,totals,streaks,summaries)
        }
        if(tab=="Session info") {
            Text("Session Information",Modifier.testTag("session-information"),style=MaterialTheme.typography.titleLarge)
            OutlinedButton(onClick={tab="Calendar"}) {Icon(Icons.Outlined.CalendarMonth,null);Text("Choose date")}
            if(qualifying.isEmpty() && periods.isEmpty())Panel {Text("No qualifying completed sessions on this date. Every recorded minute remains in your daily totals.")}
            qualifying.forEach {v ->
                Panel(Modifier.clickable {visitKey=v.id}.testTag("library-session-${v.id}")) {
                    SettingHeading(Icons.Outlined.AccountBalance,"Library session", "${StudyMath.duration(v.end!!-v.start)} • threshold ${v.minimumMinutes} min")
                    Text("${time(v.start,zone)} – ${time(v.end!!,zone)}")
                    Text("One continuous visit. Tap for its full details.",style=MaterialTheme.typography.bodySmall)
                }
            }
            if(periods.isNotEmpty())SectionLabel("Manual / earlier study periods")
            periods.forEachIndexed {i,p ->
                Panel(Modifier.clickable {periodKey=p.key}) {
                    SectionLabel("Session ${i+1}")
                    Text(StudyMath.duration(p.studyMs),style=MaterialTheme.typography.titleMedium)
                    Text("${time(p.start,zone)} – ${time(p.end,zone)}")
                    Text(p.parts.map {it.trigger}.distinct().joinToString(" • "))
                    Text("Tap for details • breaks excluded",style=MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

private fun time(value:Long,zone:ZoneId)=Instant.ofEpochMilli(value).atZone(zone).format(DateTimeFormatter.ofPattern("d MMM, h:mm a"))

@Composable
private fun LibrarySessionDetails(app:StudyNestApp,visit:LibraryVisit,selected:LocalDate,records:FocusRecords,zone:ZoneId,back:()->Unit) {
    val summary=remember(records.visits,records.windows,records.periods,records.breaks,visit.id){records.summary(visit.start,visit.end!!)}
    Page("Library session","One continuous library visit",back=back,backLabel="Back to history") {
        Panel {
            Text("Start: ${time(visit.start,zone)}")
            Text("End: ${time(visit.end!!,zone)}")
            Text("Full visit: ${StudyMath.clock(visit.end!!-visit.start)}",style=MaterialTheme.typography.titleMedium)
            Text("Minimum at arrival: ${visit.minimumMinutes} minutes")
            Text("A visit crossing midnight is one session. Daily totals show only the time on each date.",style=MaterialTheme.typography.bodySmall)
        }
        FocusHistoryPanel(app,selected,LocalDate.now(),from=visit.start,until=visit.end,suppliedRecords=records,suppliedSummary=summary,rangeTitle="Full visit measurements")
    }
}

@Composable
private fun AdditionalHistoryMetrics(summaries:Map<LocalDate,FocusSummary>,totals:Map<LocalDate,Long>,today:LocalDate,select:(LocalDate)->Unit) {
    var expanded by rememberSaveable {mutableStateOf(false)}
    var metric by rememberSaveable {mutableStateOf("Tracked study")}
    Panel {
        TextButton(onClick={expanded=!expanded}) {Text("Other time metrics");Icon(if(expanded)Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,null)}
        if(expanded) {
            listOf("Tracked study","Actual study (all locations)","Distraction").forEach {name ->
                FilterChip(metric==name,{metric=name},label={Text(name)})
            }
            val values=if(metric=="Tracked study")totals else summaries.mapNotNull {(date,s) ->
                if(metric=="Distraction")date to s.distractionMs else if(s.unknownMs==0L)date to s.studyMs else null
            }.toMap()
            StudyChart(values,today,title=metric,selectDate=select)
        }
    }
}
