package com.studynest.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.studynest.app.data.Config
import com.studynest.app.domain.GoalInput

@Composable
fun SettingHeading(icon: ImageVector, title: String, explanation: String) {
    val dark=MaterialTheme.colorScheme.surface.luminance()<0.5f
    val accent=when(title) {
        "Minimum daily time for a streak" -> Color(0xFFB24B15) to Color(0xFFFFE0D1)
        "Minimum session duration" -> Color(0xFF08618A) to Color(0xFFD8EDF9)
        else -> MaterialTheme.colorScheme.onPrimaryContainer to MaterialTheme.colorScheme.primaryContainer
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(shape = RoundedCornerShape(16.dp), color = if(dark) accent.first.copy(alpha=0.28f) else accent.second) {
            Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                Icon(icon, null, Modifier.size(28.dp), tint = if(dark) accent.second else accent.first)
            }
        }
        Column(Modifier.weight(1f)) {
            SectionLabel(title)
            Text(explanation, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun InformationStrip(text: String) {
    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(16.dp)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Outlined.Info, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(text, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

/** Shared settings/onboarding controls. Invalid raw input stays visible and cannot be silently saved. */
@Composable
fun GoalFields(c: Config, validity: (Boolean) -> Unit = {}, change: (Config) -> Unit) {
    var hours by rememberSaveable { mutableStateOf((c.goalMinutes / 60.0).toString().removeSuffix(".0")) }
    var streak by rememberSaveable { mutableStateOf(c.streakMinutes.toString()) }
    var minimum by rememberSaveable { mutableStateOf(c.minimumSessionMinutes.toString()) }
    val goalValue = GoalInput.hours(hours)
    val streakValue = GoalInput.minutes(streak)
    val minimumValue = GoalInput.minutes(minimum)
    LaunchedEffect(goalValue, streakValue, minimumValue) {
        validity(goalValue != null && streakValue != null && minimumValue != null)
    }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.testTag("goal-fields")) {
        GoalCard(Icons.Outlined.BarChart, "Daily study goal", "Target tracked study time you want to achieve each day.",
            "Daily study goal: ${c.goalMinutes / 60}h ${c.goalMinutes % 60}m", hours, "Custom goal (hours)", "hours", "goal-hours",
            goalValue == null, "Enter 1 minute to 24 hours; decimals allowed, e.g. 7.5.",
            listOf(60,120,180,240,300,360,480,600), goalValue, { "${it / 60}h" },
            { value -> hours = (value / 60.0).toString().removeSuffix(".0"); change(c.copy(goalMinutes = value)) },
            { text -> hours = text; GoalInput.hours(text)?.let { change(c.copy(goalMinutes = it)) } }, decimal = true) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(enabled = goalValue != null && goalValue > 60, onClick = {
                        val value = (goalValue!! - 60).coerceAtLeast(1)
                        hours = (value / 60.0).toString().removeSuffix(".0"); change(c.copy(goalMinutes = value))
                    }) { Text("−1h") }
                    OutlinedButton(enabled = goalValue != null && goalValue < 1440, onClick = {
                        val value = (goalValue!! + 60).coerceAtMost(1440)
                        hours = (value / 60.0).toString().removeSuffix(".0"); change(c.copy(goalMinutes = value))
                    }) { Text("+1h") }
                }
            }
        GoalCard(Icons.Outlined.LocalFireDepartment, "Minimum daily time for a streak",
            "A day counts toward your streak after this much tracked study time.",
            "Streak minimum: ${c.streakMinutes} min", streak, "Custom streak time (minutes)", "minutes", "goal-streak",
            streakValue == null, "Enter a whole number from 1 to 1440 minutes.", listOf(30,60,90,120,180,200), streakValue,
            { "$it min" }, { value -> streak = value.toString(); change(c.copy(streakMinutes = value)) },
            { text -> streak = text; GoalInput.minutes(text)?.let { change(c.copy(streakMinutes = it)) } }) {
                InformationStrip("Yesterday’s streak stays active while you work towards today’s minimum. Pauses never count.")
            }
        GoalCard(Icons.Outlined.Schedule, "Minimum session duration", "A completed library visit must last this long to count as a separate session.",
            "Minimum session duration: ${c.minimumSessionMinutes} min", minimum, "Custom duration (minutes)", "minutes", "goal-minimum",
            minimumValue == null, "Enter a whole number from 1 to 1440 minutes.", listOf(5,10,15,20,30), minimumValue,
            { "$it min" }, { value -> minimum = value.toString(); change(c.copy(minimumSessionMinutes = value)) },
            { text -> minimum = text; GoalInput.minutes(text)?.let { change(c.copy(minimumSessionMinutes = it)) } }) {
            InformationStrip("This minimum applies to Manual and automatic study sessions at home or in the library. Shorter sessions stay in daily totals but are not listed separately. Library visits keep their arrival-time threshold.")
            }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GoalCard(icon: ImageVector, title: String, explanation: String, selection: String,
    raw: String, label: String, unit: String, tag: String, invalid: Boolean, help: String,
    presets: List<Int>, value: Int?, format: (Int) -> String, choose: (Int) -> Unit, input: (String) -> Unit,
    decimal: Boolean = false, extra: @Composable ColumnScope.() -> Unit) {
    Panel(Modifier.testTag("$tag-card")) {
        SettingHeading(icon, title, explanation)
        Text(if(invalid) "Choose a valid value" else selection, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        BoxWithConstraints(Modifier.fillMaxWidth().testTag("$tag-presets")) {
            val measurer=rememberTextMeasurer()
            val density=LocalDensity.current
            val style=MaterialTheme.typography.labelLarge
            val labelWidth=remember(presets,density,style) {
                with(density) {presets.maxOf {measurer.measure(AnnotatedString(format(it)),style).size.width}.toDp()}
            }
            val gap=8.dp
            val minWidth=maxOf(64.dp,labelWidth+32.dp)
            val preferred=if(tag=="goal-hours")4 else 3
            val columns=((maxWidth+gap)/(minWidth+gap)).toInt().coerceIn(1,preferred)
            val buttonWidth=(maxWidth-gap*(columns-1))/columns
            Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(gap)) {
                presets.chunked(columns).forEach {row ->
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(gap,Alignment.CenterHorizontally)) {
                        row.forEach {amount ->
                            FilterChip(value == amount, { choose(amount) }, label = { Text(format(amount)) },
                                modifier = Modifier.width(buttonWidth).height(56.dp).testTag("$tag-preset-$amount"),
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary))
                        }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(label, Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
            TextButton(onClick = { input("") }) { Text("Clear") }
        }
        OutlinedTextField(raw, { input(it.take(16)) }, modifier = Modifier.fillMaxWidth().testTag(tag),
            label = { Text(label) }, suffix = { Text(unit) }, isError = invalid,
            supportingText = { Text(if (invalid) help else if (decimal) "Up to 24 hours; decimals allowed, e.g. 7.5." else "1–1440 minutes; exact custom values supported.") },
            keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number), singleLine = true)
        extra()
    }
}
