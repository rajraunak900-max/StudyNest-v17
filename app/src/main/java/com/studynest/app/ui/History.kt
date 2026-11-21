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
fun HistoryScreen(app: StudyNestApp, sessions:List<StudySession>,segments:List<Segment>,
    totals:Map<LocalDate,Long>,streaks:Streaks,today:LocalDate,minimumSessionMinutes:Int = 1) {
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
    val sittingDays=remember(records.visits,sessions,segments,totals,summaries,records.now,zone) {
        (records.visits.map { Instant.ofEpochMilli(it.start).atZone(zone).toLocalDate() } + totals.keys + summaries.keys).toSet()
            .associateWith { day -> SittingMath.forDay(records.visits,sessions,segments,day,zone,records.now,app.engine::range) }
    }
    val days=remember(summaries,totals,sittingDays){(summaries.keys+totals.keys+sittingDays.keys).associateWith { day ->
        HistoryDay.from(summaries[day],totals[day] ?: 0).copy(sitting=sittingDays[day] ?: 0L)
    }}
    val qualifying=remember(records.visits,selected,zone){VisitQualification.forDay(records.visits,selected,zone)}
    val openVisits=remember(records.visits){records.visits.filter{it.end==null}}
    val inProgress=remember(openVisits,selected,zone,records.now){activeLibrarySessions(openVisits,selected,zone,records.now)}
    val exclusions=remember(index,selected,records.now){index.visitCoverage(selected,records.now)}
    val periods=remember(sessions,segments,selected,exclusions,totals[selected],minimumSessionMinutes){
        StudyPeriods.forDay(sessions,segments,selected,zone,app.engine::range,exclusions,minimumSessionMinutes*60_000L)
    }
    var visitKey by rememberSaveable {mutableStateOf<Long?>(null)}
    var periodKey by rememberSaveable {mutableStateOf<String?>(null)}
    var showSessions by rememberSaveable {mutableStateOf(false)}
    val visit=(qualifying+inProgress).firstOrNull {it.id==visitKey}
    val periodIndex=periods.indexOfFirst {it.key==periodKey}
    if(visit!=null) {
        BackHandler {visitKey=null}
        LibrarySessionDetails(app,visit,selected,records,zone){visitKey=null};return
    }
    if(periodIndex>=0) {
        BackHandler {periodKey=null}
        StudyPeriodDetails(app,periods[periodIndex],periodIndex+1,selected,zone,sessions,segments){periodKey=null};return
    }
    if(showSessions) {
        BackHandler {showSessions=false}
        Page("Session Information",selected.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy")),
            back={showSessions=false},backLabel="Back to history") {
            Column(Modifier.testTag("session-information"),verticalArrangement=Arrangement.spacedBy(16.dp)) {
                OutlinedButton(onClick={showSessions=false;tab="Calendar"}) {Icon(Icons.Outlined.CalendarMonth,null);Text("Choose date")}
                inProgress.forEach {v ->
                    HistorySessionCard("Current session — In progress", "Visit duration: ${StudyMath.clock(LibraryPresence.duration(v,records.now))}",
                        "Started ${time(v.start,zone)} – ongoing", "Library visit • threshold ${v.minimumMinutes} min",
                        if(v.awaySince!=null) "Temporarily away — awaiting return" else if(sessions.firstOrNull{it.endedAt==null}?.paused==true) "Study timer paused" else "Library visit ongoing",
                        Modifier.clickable {visitKey=v.id}.testTag("active-library-session-${v.id}"))
                }
                if(qualifying.isEmpty() && periods.isEmpty() && inProgress.isEmpty()) {
                    Text("No sessions for this date.")
                    if(openVisits.any {it.start<selected.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() && records.now>selected.atStartOfDay(zone).toInstant().toEpochMilli()})
                        Text("Your library visit is still in progress. It will count when it ends and reaches its minimum duration.",style=MaterialTheme.typography.bodySmall)
                }
                qualifying.forEach {v ->
                    HistorySessionCard("Library visit", "${StudyMath.duration(LibraryPresence.duration(v,v.end!!))} • threshold ${v.minimumMinutes} min",
                        "${time(v.start,zone)} – ${time(v.end,zone)}", "Library visit • one continuous visit", "Tap for full details",
                        Modifier.clickable {visitKey=v.id}.testTag("library-session-${v.id}"))
                }
                if(periods.isNotEmpty())SectionLabel("Study sessions")
                periods.forEachIndexed {i,p ->
                    HistorySessionCard("Study session ${i+1}", "${StudyMath.duration(p.studyMs)} • threshold ${minimumSessionMinutes} min",
                        "${time(p.start,zone)} – ${time(p.end,zone)}", p.parts.map {sessionType(it.trigger)}.distinct().joinToString(" • "),
                        "Tap for details • breaks excluded", Modifier.clickable {periodKey=p.key}.testTag("study-session-${p.key}"))
                }
            }
        }
        return
    }
    Page("History","Track your study time and library visits.") {
        BoxWithConstraints(Modifier.fillMaxWidth().testTag("history-navigation")) {
            val stacked=maxWidth < 250.dp * LocalDensity.current.fontScale
            @Composable fun Tab(name:String,icon:androidx.compose.ui.graphics.vector.ImageVector,modifier:Modifier) {
                val active=tab==name
                Surface(onClick={tab=name},color=if(active)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerLow,
                    contentColor=if(active)MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    shape=androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                    modifier=modifier.heightIn(min=48.dp).testTag("history-tab-$name")) {
                    if(stacked) Row(Modifier.padding(12.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                        Icon(icon,null,Modifier.size(24.dp));Text(name,style=MaterialTheme.typography.labelLarge)
                    } else Column(Modifier.padding(10.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(6.dp,Alignment.CenterVertically)) {
                        Icon(icon,null,Modifier.size(24.dp));Text(name,style=MaterialTheme.typography.labelLarge,textAlign=androidx.compose.ui.text.style.TextAlign.Center)
                    }
                }
            }
            val tabs=listOf("Tracked study" to Icons.Outlined.BarChart,"Calendar" to Icons.Outlined.CalendarMonth)
            if(stacked) Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                tabs.forEach {(name,icon)->Tab(name,icon,Modifier.fillMaxWidth())}
            } else Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                tabs.forEach {(name,icon)->Tab(name,icon,Modifier.weight(1f).fillMaxHeight().heightIn(min=64.dp))}
            }
        }
        tabState.SaveableStateProvider(tab) {
            when(tab) {
                "Tracked study" -> ComparativeStudyChart(days,today,selected,::select)
                "Calendar" -> HistoryCalendar(days,month,selected,today,{monthText=it.toString()},::select)
            }
        }
        FocusHistoryPanel(app,selected,today,suppliedRecords=records,suppliedSummary=summaries[selected] ?: FocusSummary(),showFootnote=false,sittingMs=sittingDays[selected] ?: 0L)
        Panel(Modifier.testTag("daily-session-report")) {
            SectionLabel(if(selected==today) "Today's Session Report" else "${selected.format(DateTimeFormatter.ofPattern("d MMMM"))} Session Report")
            Text(if(selected==today) "Today's sessions: ${qualifying.size+periods.size}" else "Sessions: ${qualifying.size+periods.size}",
                Modifier.testTag("session-count"),style=MaterialTheme.typography.titleMedium)
            if(inProgress.isNotEmpty())Text("${inProgress.size} in progress",Modifier.testTag("session-in-progress"),style=MaterialTheme.typography.bodyMedium)
            OutlinedButton(onClick={showSessions=true},modifier=Modifier.fillMaxWidth().heightIn(min=48.dp).testTag("view-all-sessions")) {
                Text("View all sessions");Icon(Icons.Outlined.ChevronRight,null)
            }
        }
        if(tab=="Tracked study") {
            BreakDistractionTrend(summaries,today,::select)
            FocusPeriodOverview(records,today,totals,streaks,summaries,simplified=false,sittingByDay=sittingDays)
        }
    }
}

private fun time(value:Long,zone:ZoneId)=Instant.ofEpochMilli(value).atZone(zone).format(DateTimeFormatter.ofPattern("d MMM, h:mm a"))

private fun sessionType(trigger:String)=when(trigger) {
    "Manual" -> "Manual timer"
    SessionEngine.MANUAL_AUTO_TRIGGER -> "Manual / automatic timer"
    else -> "Automatic timer"
}

@Composable
private fun HistorySessionCard(title:String,duration:String,timeRange:String,type:String,status:String,modifier:Modifier=Modifier) {
    Panel(modifier,padding=StudyLayout.cardPadding) {
        Column(Modifier.testTag("history-session-card-layout"),verticalArrangement=Arrangement.spacedBy(StudyLayout.itemGap)) {
            SectionLabel(title)
            Text(duration,style=MaterialTheme.typography.titleMedium)
            Text(timeRange)
            Text(type,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            Text(status,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun LibrarySessionDetails(app:StudyNestApp,visit:LibraryVisit,selected:LocalDate,records:FocusRecords,zone:ZoneId,back:()->Unit) {
    val until=visit.end ?: records.now
    val summary=remember(records.visits,records.windows,records.periods,records.breaks,visit.id,until){records.summary(visit.start,until)}
    Page(if(visit.end==null) "Current session — In progress" else "Library session","One continuous library visit",back=back,backLabel="Back to history") {
        Panel {
            Text("Start: ${time(visit.start,zone)}")
            if(visit.end!=null)Text("End: ${time(visit.end,zone)}")
            Text("${if(visit.end==null) "Current visit" else "Full visit"}: ${StudyMath.clock(LibraryPresence.duration(visit,until))}",style=MaterialTheme.typography.titleMedium)
            Text("Minimum at arrival: ${visit.minimumMinutes} minutes")
            Text("A visit crossing midnight is one session. Daily totals show only the time on each date.",style=MaterialTheme.typography.bodySmall)
        }
        FocusHistoryPanel(app,selected,LocalDate.now(),from=visit.start,until=until,suppliedRecords=records,suppliedSummary=summary,
            rangeTitle=if(visit.end==null) "Current visit measurements" else "Full visit measurements",
            sittingMs=LibraryPresence.duration(visit,until))
    }
}

@Composable
private fun BreakDistractionTrend(summaries:Map<LocalDate,FocusSummary>,today:LocalDate,select:(LocalDate)->Unit) {
    val distractions=remember(summaries){summaries.mapValues {it.value.distractionMs}}
    val breaks=remember(summaries){summaries.mapValues {it.value.breakMs}}
    StudyChart(distractions,today,title="Break & Distraction",secondaryTotals=breaks,
        firstLabel="Distraction",secondLabel="Break",collapsible=true,selectDate=select)
}
