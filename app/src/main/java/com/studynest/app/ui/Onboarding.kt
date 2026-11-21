package com.studynest.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.studynest.app.StudyNestApp
import com.studynest.app.automation.Automation
import com.studynest.app.data.Config

/** Invalid optional detection stays disabled; its saved configuration is retained. */
internal fun readySetup(c: Config): Config = c.copy(setup = true,
    wifi = c.wifi && c.networks().isNotEmpty(),
    geo = c.geo && validLocation(c.copy(wifi = false)), geoVersion = System.currentTimeMillis())

@Composable
fun SetupScreen(app: StudyNestApp, saved: Config) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    var draft by rememberSaveable { mutableStateOf(saved) }
    var goalsValid by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var failure by remember { mutableStateOf<String?>(null) }
    fun finish(useDefaults: Boolean = false) {
        busy = true
        app.runTask {
            try {
                val next = if (useDefaults) draft.copy(goalMinutes = saved.goalMinutes,
                    streakMinutes = saved.streakMinutes, minimumSessionMinutes = saved.minimumSessionMinutes, vibrate = saved.vibrate) else draft
                app.preferences.save(readySetup(next))
                Automation.apply(app)
            } catch (_: Exception) {
                failure = "Setup could not be saved. Please try again."
            } finally { busy = false }
        }
    }
    fun skip() {
        when (step) {
            1 -> draft = draft.copy(wifi = false)
            2 -> draft = draft.copy(geo = false)
            4 -> { finish(true); return }
        }
        step++
    }
    BackHandler(enabled = step > 0 && !busy) { step-- }
    val titles = listOf("Welcome to StudyNest", "Your library Wi-Fi", "Your library location", "Permissions & System Access", "Goal & Streak")
    val descriptions = listOf("Understand how your study time is spent, one session at a time.",
        "Optional. Start automatic study tracking when you join your library network.",
        "Optional. Use location as another way to recognize your library.",
        "Choose the access you want. Every feature can be set up later.",
        "Choose your daily goal, streak target and minimum library session duration.")
    val icons = listOf(Icons.Outlined.AutoAwesome, Icons.Outlined.Wifi, Icons.Outlined.LocationOn,
        Icons.Outlined.Security, Icons.Outlined.Flag)
    Column(Modifier.fillMaxSize().imePadding().testTag("onboarding")) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(StudyLayout.pagePadding),
            verticalArrangement = Arrangement.spacedBy(StudyLayout.sectionGap)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(icons[step], null, Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
                Text("${step + 1} of ${titles.size}", style = MaterialTheme.typography.labelLarge)
            }
            LinearProgressIndicator(progress = { (step + 1).toFloat() / titles.size }, modifier = Modifier.fillMaxWidth())
            Text(titles[step], style = MaterialTheme.typography.headlineMedium)
            Text(descriptions[step], style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            when (step) {
                0 -> Panel {
                    SectionLabel("A clearer picture of your day")
                    Text("See Sitting Time, Actual Study, distractions and efficiency. Plan tasks, review history and build your streak.")
                    Text("Start manually today. Library detection, app monitoring and automatic vibration can be set up later.")
                    SectionLabel("Your study records stay on your device")
                    Text("StudyNest stores your history, tasks and app-timing records locally. No account, ads, analytics or study-data uploads.", style = MaterialTheme.typography.bodySmall)
                    Text("Usage Access measures selected distracting apps. It does not read messages, conversations, typed text, photos or screen contents.", style = MaterialTheme.typography.bodySmall)
                    Text("Optional library location uses Android and Google Play services location features.", style = MaterialTheme.typography.bodySmall)
                }
                1 -> Panel {
                    LocationFields(draft.copy(wifi = true, geo = false), onboarding = true) { draft = draft.withNetworks(it.networks()) }
                    Text("Not at the library? Skip this step and add its network in Settings later.", style = MaterialTheme.typography.bodySmall)
                }
                2 -> Panel {
                    Toggle("Use library location", "Automatic arrival and departure detection.", draft.geo) { draft = draft.copy(geo = it) }
                    if (draft.geo) LocationFields(draft.copy(wifi = false), onboarding = true) {
                        draft = draft.copy(latitude = it.latitude, longitude = it.longitude, radius = it.radius)
                    }
                    else Text("Wi-Fi and manual timing work without this feature.", style = MaterialTheme.typography.bodySmall)
                }
                3 -> PermissionPanel(draft.wifi || draft.geo, draft.vibrate, showIntro = false)
                4 -> Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    GoalFields(draft, { goalsValid = it }) { draft = it }
                    Toggle("Automatic vibration", "Vibrate at the library with Do Not Disturb access. Media volume stays unchanged.", draft.vibrate) {
                        draft = draft.copy(vibrate = it)
                    }
                }
            }
            failure?.let { Text(it) }
        }
        Surface(shadowElevation = 2.dp) {
            Column(Modifier.fillMaxWidth().padding(horizontal = StudyLayout.pagePadding, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Button(enabled = !busy && (step != 4 || goalsValid) && (step != 2 || !draft.geo || validLocation(draft.copy(wifi = false))),
                    modifier = Modifier.fillMaxWidth().heightIn(min = StudyLayout.controlHeight).testTag("setup-continue"),
                    onClick = {
                        if (step == 4) finish()
                        else { if (step == 1) draft = draft.copy(wifi = draft.networks().isNotEmpty()); step++ }
                    }) { Text(if (busy) "Saving…" else if (step == 4) "Enter StudyNest" else "Continue") }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    if (step > 0) TextButton(enabled = !busy, onClick = { step-- }) { Text("Back") }
                    Spacer(Modifier.weight(1f))
                    TextButton(enabled = !busy, onClick = { skip() }, modifier = Modifier.testTag("setup-skip")) { Text("Skip") }
                }
            }
        }
    }
}
