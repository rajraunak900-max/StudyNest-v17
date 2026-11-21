package com.studynest.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.core.view.WindowCompat
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.*
import androidx.lifecycle.repeatOnLifecycle
import com.studynest.app.StudyNestApp
import com.studynest.app.automation.Automation
import com.studynest.app.data.*
import com.studynest.app.domain.*
import java.time.*
import kotlinx.coroutines.delay

private val light =
    lightColorScheme(
        primary = Color(0xFF28594D),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFDCEBDF),
        secondary = Color(0xFF81643E),
        tertiary = Color(0xFF87C4A7),
        onTertiary = Color(0xFF173C30),
        background = Color(0xFFF7F8F3),
        surface = Color(0xFFF7F8F3),
        surfaceVariant = Color(0xFFE8EDE5),
        surfaceContainerLow = Color(0xFFEEF2EC),
        onPrimaryContainer = Color(0xFF173C30))
private val dark =
    darkColorScheme(
        primary = Color(0xFFA9D1BA),
        onPrimary = Color(0xFF10382F),
        tertiary = Color(0xFF83C6A7),
        onTertiary = Color(0xFF10382F),
        primaryContainer = Color(0xFF284D40),
        background = Color(0xFF121B18),
        surface = Color(0xFF121B18),
        surfaceVariant = Color(0xFF29352F),
        surfaceContainerLow = Color(0xFF1C2822),
        onPrimaryContainer = Color(0xFFD6EEDF))

@Composable
fun StudyNest(app: StudyNestApp, refreshTimer: Boolean = true) {
    val cfg by app.preferences.flow.collectAsStateWithLifecycle()
    val sessions by remember { app.db.dao().sessions() }.collectAsStateWithLifecycle(emptyList())
    val segments by remember { app.db.dao().segments() }.collectAsStateWithLifecycle(emptyList())
    val tasks by remember { app.db.dao().tasks() }.collectAsStateWithLifecycle(emptyList())
    val focusWindows by remember { app.db.focusDao().windows() }.collectAsStateWithLifecycle(emptyList())
    val distractions by remember { app.db.focusDao().distractions() }.collectAsStateWithLifecycle(emptyList())
    val message by app.message.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(lifecycle, refreshTimer) {
        if (refreshTimer)
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    now = System.currentTimeMillis()
                    delay(1000)
                }
            }
    }
    val zone=ZoneId.systemDefault()
    val trackedIndex=remember(segments,zone){TrackingTotalsIndex(segments,zone,app.engine::range)}
    val totals=remember(trackedIndex,now){trackedIndex.current(app.engine::range)}
    val today = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).toLocalDate()
    LaunchedEffect(today, ZoneId.systemDefault()) { app.engine.reconcileDay() }
    // Known Actual focus, including manual study outside the library. Gross
    // segments remain available to History; distraction/unknown is never goal credit.
    val actualIndex = remember(focusWindows,distractions,zone) {
        HistoryIndex(emptyList(),focusWindows,distractions,emptyList(),zone,now)
    }
    val actualDays = remember(actualIndex,now) { actualIndex.live(now) }
    val actualTotals = remember(actualDays) { actualDays.mapValues { it.value.studyMs } }
    val streaks = remember(actualTotals,cfg.streakMinutes,today) { StudyMath.streaks(actualTotals, cfg.streakMinutes * 60000L, today) }
    val isDark = resolveDarkTheme(cfg.theme, isSystemInDarkTheme())
    val view = LocalView.current
    SideEffect {
        (view.context as? android.app.Activity)?.window?.let { window ->
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !isDark
                isAppearanceLightNavigationBars = !isDark
            }
        }
    }
    MaterialTheme(
        colorScheme = if (isDark) dark else light,
        shapes = Shapes(medium = RoundedCornerShape(20.dp), large = RoundedCornerShape(28.dp))) {
            Surface(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxSize().safeDrawingPadding()) {
                if (!cfg.setup) SetupScreen(app, cfg)
                else {
                    var page by rememberSaveable { mutableIntStateOf(0) }
                    var settingsPage by rememberSaveable { mutableStateOf("main") }
                    Scaffold(
                        bottomBar = {
                            BoxWithConstraints(Modifier.fillMaxWidth().testTag("main-navigation")) {
                            val labelWidth = maxWidth / 4
                            NavigationBar(tonalElevation = 0.dp) {
                                val icons =
                                    listOf(
                                        Icons.Outlined.Home,
                                        Icons.Outlined.CheckCircle,
                                        Icons.Outlined.BarChart,
                                        Icons.Outlined.Settings)
                                listOf("Home", "Tasks", "History", "Settings").forEachIndexed {
                                    i,
                                    title ->
                                    NavigationBarItem(
                                        selected = page == i,
                                        onClick = { if (i == 3) settingsPage = "main"; page = i },
                                        icon = { Icon(icons[i], title) },
                                        label = { Text(title, Modifier.requiredWidth(labelWidth), textAlign = TextAlign.Center) })
                                }
                            }
                            }
                        }) { padding ->
                            Column(Modifier.padding(padding).fillMaxSize()) {
                                if (message.isNotBlank())
                                    Card(
                                        Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                        colors =
                                            CardDefaults.cardColors(
                                                containerColor =
                                                    MaterialTheme.colorScheme.errorContainer)) {
                                            Row(
                                                Modifier.padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        message,
                                                        Modifier.weight(1f),
                                                        style = MaterialTheme.typography.bodySmall)
                                                    IconButton(
                                                        onClick = { app.message.value = "" }) {
                                                            Icon(
                                                                Icons.Outlined.Close,
                                                                "Dismiss warning")
                                                        }
                                                }
                                        }
                                AnimatedContent(
                                    targetState = page,
                                    label = "navigation",
                                    modifier = Modifier.weight(1f)) { target ->
                                        when (target) {
                                            0 ->
                                                HomeScreen(
                                                    app,
                                                    cfg,
                                                    sessions,
                                                    segments,
                                                    tasks,
                                                    actualDays[today] ?: FocusSummary(),
                                                    sessions.firstOrNull { it.endedAt == null }?.let { active ->
                                                        FocusMath.summary(emptyList(), focusWindows, distractions, now,
                                                            TimeRange(active.startedAt, now), active.id)
                                                    },
                                                    streaks,
                                                    today,
                                                    now,
                                                    { page = 1 },
                                                    { settingsPage = "main"; page = 3 },
                                                    { settingsPage = "wifi"; page = 3 })
                                            1 -> TasksScreen(app, tasks)
                                            2 ->
                                                HistoryScreen(
                                                    app, sessions, segments, totals, streaks, today, cfg.minimumSessionMinutes)
                                            else -> SettingsScreen(app, cfg, settingsPage)
                                        }
                                    }
                            }
                        }
                }
                }
            }
        }
}

@Composable
fun Page(title: String, subtitle: String, back: (() -> Unit)? = null, backLabel: String = "Back to settings", content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(StudyLayout.pagePadding),
        verticalArrangement = Arrangement.spacedBy(StudyLayout.sectionGap)) {
            back?.let { action -> TextButton(onClick = action) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, null); Text(backLabel) } }
            Column {
                Text(
                    title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            content()
            Spacer(Modifier.height(12.dp))
        }
}

@Composable
fun Panel(modifier: Modifier = Modifier, padding: Dp = StudyLayout.cardPadding, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
            Column(
                Modifier.padding(padding),
                verticalArrangement = Arrangement.spacedBy(StudyLayout.itemGap),
                content = content)
        }
}

@Composable
fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun HomeScreen(
    app: StudyNestApp,
    cfg: Config,
    sessions: List<StudySession>,
    segments: List<Segment>,
    tasks: List<StudyTask>,
    dailyActual: FocusSummary,
    activeSessionSummary: FocusSummary?,
    streaks: Streaks,
    today: LocalDate,
    now: Long,
    openTasks: () -> Unit,
    openSettings: () -> Unit,
    openLibrarySetup: () -> Unit
) {
    val libraryVisits by remember { app.db.focusDao().visits() }.collectAsStateWithLifecycle(emptyList())
    val hour = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).hour
    val greeting =
        if (hour < 12) "Good morning" else if (hour < 17) "Good afternoon" else "Good evening"
    val active = sessions.firstOrNull { it.endedAt == null }
    val activeTime = if (active != null) activeSessionSummary?.takeIf { it.unknownMs == 0L }?.studyMs
        else dailyActual.takeIf { it.unknownMs == 0L }?.studyMs
    val todaySitting = remember(libraryVisits,sessions,segments,today,now) {
        SittingMath.forDay(libraryVisits, sessions, segments, today, ZoneId.systemDefault(), now, app.engine::range)
    }
    val todayEfficiency = EfficiencyMath.calculate(dailyActual.studyMs,todaySitting,dailyActual.unknownMs)
    val todayEfficiencyText = todayEfficiency?.let { String.format(java.util.Locale.getDefault(), "%.1f%%", it) } ?: "—"
    val taskCounts = taskSummary(tasks, today)
    var timerChoice by rememberSaveable {
        mutableStateOf(if (cfg.tracking && (cfg.wifi || cfg.geo)) "Automatic" else "Manual")
    }
    LaunchedEffect(active?.trigger) {
        if (active != null) timerChoice = if (SessionEngine.isManualTrigger(active.trigger)) "Manual" else "Automatic"
    }
    Page(greeting, "A little focus, every day.  •  StudyNest") {
        Card(Modifier.fillMaxWidth().testTag("today-study-summary"), colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Column(Modifier.padding(StudyLayout.cardPadding), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SectionLabel("Today's Tracked Study Session")
                Text(StudyMath.duration(dailyActual.studyMs), style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold)
                LinearProgressIndicator(progress = { (dailyActual.studyMs.toFloat() / (cfg.goalMinutes * 60000L)).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(6.dp))
                Text("Daily goal: ${StudyMath.duration(cfg.goalMinutes * 60000L)}", style = MaterialTheme.typography.bodySmall)
                Text("Sitting Time: ${StudyMath.duration(todaySitting)} • Efficiency: $todayEfficiencyText",
                    Modifier.testTag("today-sitting-efficiency"), style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.LocalFireDepartment, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.secondary)
                    Text("${streaks.current}-day streak", Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                    Icon(Icons.Outlined.EmojiEvents, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.secondary)
                    Text("${streaks.longest}-day best", Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                }
                @Composable fun TaskText(modifier: Modifier = Modifier) {
                    Column(modifier) {
                        Text("${taskCounts.total} tasks • ${taskCounts.completed} completed", style = MaterialTheme.typography.labelLarge)
                        Text("${taskCounts.dueToday} due today • ${taskCounts.overdue} overdue", style = MaterialTheme.typography.bodySmall)
                    }
                }
                @Composable fun ViewTasks(modifier: Modifier = Modifier) {
                    FilledTonalButton(onClick = openTasks, modifier = modifier.testTag("view-tasks")) {
                        Icon(Icons.Outlined.FormatListBulleted, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp)); Text("View Tasks")
                    }
                }
                BoxWithConstraints(Modifier.fillMaxWidth().testTag("home-task-summary")) {
                    if (maxWidth >= 320.dp * androidx.compose.ui.platform.LocalDensity.current.fontScale)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TaskText(Modifier.weight(1f)); ViewTasks()
                        }
                    else Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        TaskText(); ViewTasks(Modifier.align(Alignment.End))
                    }
                }
            }
        }
        StudyTimerCard(app, cfg, active, activeTime, timerChoice, { timerChoice = it }, openSettings)
        LiveFocusPanel(app, today, openLibrarySetup, sessions, segments)
        Panel(Modifier.testTag("motivational-quote")) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "A MOMENT OF MOTIVATION",
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.labelMedium)
                IconButton(
                    onClick = {
                        app.preferences.save(cfg.copy(quoteOffset = cfg.quoteOffset + 1))
                    }) {
                        Icon(Icons.Outlined.Refresh, "Another quote")
                    }
            }
            Text(
                "“${quotes[Math.floorMod(today.toEpochDay()+cfg.quoteOffset,quotes.size.toLong()).toInt()]}”",
                style = MaterialTheme.typography.titleLarge)
        }

    }
}

val quotes =
    listOf(
        "A small step today is still a step forward.",
        "You do not need a perfect day to make meaningful progress.",
        "Give this hour your attention. Let the next hour wait.",
        "Consistency turns quiet effort into lasting change.",
        "Start with what you can do. Build from there.",
        "Your future grows in the time you give it today.",
        "A page understood is worth more than a chapter rushed.",
        "Rest is part of the work. Return with intention.",
        "Do the next useful thing.",
        "Progress is built one focused session at a time.",
        "Keep your goal in sight and your next step small.",
        "Learning is a practice, not a performance.",
        "Let curiosity carry you past the difficult part.",
        "Show up for the person you are becoming.",
        "Today's effort is tomorrow's confidence.",
        "A little better is a worthy goal.",
        "Protect a little time for what matters.",
        "You can begin before you feel ready.",
        "A difficult page can become a familiar one.",
        "Give one task your full attention.",
        "Practice makes tomorrow less unfamiliar.",
        "You are allowed to learn slowly.",
        "Finish the small promise you made today.",
        "Focus grows when distractions lose their invitation.",
        "Your schedule should make room for your goal.",
        "Effort becomes useful when you return to it.",
        "Ask the question that helps you understand.",
        "One clear idea is progress.",
        "Learn the reason, not only the answer.",
        "Build a routine you can return to.",
        "An imperfect session can still be worthwhile.",
        "Let your plan be small enough to start.",
        "Put the next ten minutes to good use.",
        "You can change the pace without changing the direction.",
        "A rest day does not erase what you have learned.",
        "Take notes that your future self can use.",
        "Attention is a gift you can give your goals.",
        "Choose a beginning over another delay.",
        "Work at understanding, and speed will follow.",
        "A steady routine needs room for rest.",
        "Progress may be quiet before it becomes visible.",
        "When the task feels large, choose the first part.",
        "Keep showing up with a question to answer.",
        "Today can be a fresh chapter.",
        "Use your mistakes as study material.",
        "Make one confusing thing clearer today.",
        "Give your best energy to your most useful task.",
        "You can restart without starting from nothing.",
        "Small repetitions build lasting familiarity.",
        "A thoughtful review is time well spent.",
        "Keep a promise to your own learning.",
        "Your pace can be different and still take you forward.",
        "Leave your next session an easy place to begin.",
        "A goal becomes practical when it has a place in your day.",
        "Practice what you want to remember.",
        "Curiosity makes difficult work more interesting.",
        "Choose the next useful action.",
        "You do not have to solve everything this hour.",
        "Take a break before tiredness takes your focus.",
        "Measure your effort with patience.",
        "The first attempt is a place to learn.",
        "Make space for deep work, one session at a time.",
        "Understanding is worth the extra question.",
        "A distraction can wait until your break.",
        "Write down the task, then begin it.",
        "Review a little before you forget a lot.",
        "Give difficult ideas a second meeting.",
        "A habit grows each time you return.",
        "Your work today can make tomorrow easier.",
        "Keep your standards high and your steps manageable.",
        "Study with purpose, then rest without guilt.",
        "Choose consistency you can sustain.",
        "Your attention belongs to the task you choose.",
        "Learn something well enough to explain it simply.",
        "A small improvement deserves to be noticed.",
        "Set a clear stopping point and work toward it.",
        "Bring patience to the part you do not understand yet.",
        "A focused half hour can change the shape of your day.",
        "Make learning a daily appointment with yourself.",
        "Let your notes show your thinking.",
        "A useful question can unlock an entire topic.",
        "Begin again with what you know now.",
        "Today is a chance to practise, not prove perfection.",
        "Keep the next step visible.",
        "The work gets lighter when you know where to begin.",
        "Make room for the person you are learning to become.")

fun resolveDarkTheme(theme: String, systemDark: Boolean): Boolean = when (theme) {
    "Dark" -> true
    "Light" -> false
    else -> systemDark
}
