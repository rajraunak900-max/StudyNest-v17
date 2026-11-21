package com.studynest.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studynest.app.StudyNestApp
import com.studynest.app.automation.Automation
import com.studynest.app.data.*
import com.studynest.app.domain.StudyMath
import com.studynest.app.domain.SessionEngine

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyTimerCard(app: StudyNestApp, cfg: Config, active: StudySession?, activeTime: Long?,
    choice: String, choose: (String) -> Unit, openSettings: () -> Unit) {
    Panel(Modifier.testTag("study-timer-card")) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Outlined.Timer, null, tint = MaterialTheme.colorScheme.primary)
            Text("Study timer", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            IconButton(onClick = openSettings) { Icon(Icons.Outlined.Settings, "Timer settings") }
        }
        val clock = activeTime?.let { StudyMath.clock(it) } ?: "Unavailable"
        BoxWithConstraints(Modifier.fillMaxWidth().padding(vertical = 4.dp), contentAlignment = Alignment.Center) {
            val size = minOf(56f, maxWidth.value / (clock.length * 0.62f * LocalDensity.current.fontScale))
            Text(clock, Modifier.testTag("primary-study-clock"), fontSize = size.sp, fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold, maxLines = 1, color = MaterialTheme.colorScheme.primary)
        }
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val labelSize = minOf(14f, (maxWidth.value / 2 - 24) / (9 * 0.57f * LocalDensity.current.fontScale))
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().testTag("timer-mode-selector")) {
                listOf("Automatic", "Manual").forEachIndexed { index, mode ->
                    SegmentedButton(selected = choice == mode, onClick = { choose(mode) }, enabled = active == null,
                        shape = SegmentedButtonDefaults.itemShape(index, 2), icon = {}) {
                        Text(mode, fontSize = labelSize.sp, maxLines = 1)
                    }
                }
            }
        }
        if (active != null) {
            val manualTimer = SessionEngine.isManualTrigger(active.trigger)
            val triggerLabel = if (active.trigger == SessionEngine.MANUAL_AUTO_TRIGGER) "Manual • automatic library tracking" else active.trigger
            Text("$triggerLabel • ${if (active.paused) "Paused" else "Studying"}",
                style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            @Composable fun PauseButton(modifier: Modifier) {
                FilledTonalButton(onClick = { Automation.start(app, "PAUSE") },
                    modifier = modifier.heightIn(min = StudyLayout.controlHeight)) {
                    Text(if (active.paused) "Resume" else "Pause")
                }
            }
            @Composable fun StopButton(modifier: Modifier) {
                Button(onClick = { Automation.start(app, if (manualTimer) "FINISH" else "AUTO_STOP") },
                    modifier = modifier.heightIn(min = StudyLayout.controlHeight)) {
                    Text(if (manualTimer) "Stop manual" else "Stop automatic")
                }
            }
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                if (maxWidth >= 280.dp * LocalDensity.current.fontScale) {
                    Row(horizontalArrangement = Arrangement.spacedBy(StudyLayout.itemGap)) {
                        PauseButton(Modifier.weight(1f)); StopButton(Modifier.weight(1f))
                    }
                } else Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PauseButton(Modifier.fillMaxWidth()); StopButton(Modifier.fillMaxWidth())
                }
            }
            if (active.pausedByDeparture) Text(if (active.trigger == "Manual")
                "You left the library. Resume when you're ready."
                else "You left the library. Studying resumes when you return.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else if (choice == "Manual") {
            Text("Ready when you are.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = { Automation.start(app, "START") },
                modifier = Modifier.fillMaxWidth().heightIn(min = StudyLayout.controlHeight)) {
                Text("Start manual timer")
            }
        } else {
            Text(if (cfg.tracking && (cfg.wifi || cfg.geo)) "Waiting for your library."
                else "Start studying automatically when you arrive.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (!cfg.wifi && !cfg.geo) {
                OutlinedButton(onClick = openSettings,
                    modifier = Modifier.fillMaxWidth().heightIn(min = StudyLayout.controlHeight)) { Text("Set up library detection") }
            } else {
                Button(onClick = { Automation.start(app, "AUTO_START") },
                    modifier = Modifier.fillMaxWidth().heightIn(min = StudyLayout.controlHeight)) {
                    Text(if (cfg.tracking) "Start / re-arm automatic" else "Start automatic timer")
                }
                if (cfg.tracking) OutlinedButton(onClick = { Automation.start(app, "AUTO_STOP") },
                    modifier = Modifier.fillMaxWidth().heightIn(min = StudyLayout.controlHeight)) { Text("Stop automatic tracking") }
            }
        }
    }
}
