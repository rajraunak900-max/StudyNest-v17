package com.studynest.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Shared spacing for the existing calm Material cards and controls. */
object StudyLayout {
    val pagePadding = 20.dp
    val cardPadding = 20.dp
    val sectionGap = 16.dp
    val itemGap = 12.dp
    val controlHeight = 48.dp
}

data class DashboardMetric(val label: String, val value: String, val description: String? = null,
    val efficiency: Double? = null)

/** Only the percentage text changes; the card/icon and accounting stay untouched. */
fun efficiencyValueColor(value: Double?, dark: Boolean, neutral: Color): Color = when {
    value == null || !value.isFinite() -> neutral
    value < 50 -> if (dark) Color(0xFFFFB4AB) else Color(0xFFB3261E)
    value < 75 -> if (dark) Color(0xFFFFD180) else Color(0xFF805600)
    else -> if (dark) Color(0xFF90D59A) else Color(0xFF146C2E)
}

/** Reference-inspired full metric rows, with readable colors in both themes. */
@Composable
fun MetricDashboard(metrics: List<DashboardMetric>) {
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    Column(verticalArrangement = Arrangement.spacedBy(StudyLayout.itemGap)) {
        metrics.forEach { metric ->
            val (icon, ink, pastel) = when(metric.label) {
                "Sitting Time" -> Triple(Icons.Outlined.AccountBalance, Color(0xFF155FA0), Color(0xFFE3EFFC))
                "Actual Study" -> Triple(Icons.Outlined.MenuBook, Color(0xFF167043), Color(0xFFE1F6E6))
                "Distraction" -> Triple(Icons.Outlined.PhonelinkOff, Color(0xFFA12D34), Color(0xFFFFE4E4))
                "Break" -> Triple(Icons.Outlined.FreeBreakfast, Color(0xFF986000), Color(0xFFFFF0CD))
                else -> Triple(Icons.Outlined.TrendingUp, Color(0xFF7130A0), Color(0xFFF0E2FC))
            }
            Surface(Modifier.fillMaxWidth().testTag("metric-${metric.label}"), shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Surface(shape = CircleShape, color = if (dark) ink.copy(alpha = 0.28f) else pastel) {
                        Box(Modifier.size(52.dp), contentAlignment = Alignment.Center) {
                            Icon(icon, null, Modifier.size(28.dp), tint = if (dark) pastel else ink)
                        }
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(metric.label, style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                        BoxWithConstraints(Modifier.fillMaxWidth()) {
                            val size = minOf(30f, maxWidth.value / (metric.value.length * 0.61f * LocalDensity.current.fontScale))
                            Text(metric.value, fontSize = size.sp, fontWeight = FontWeight.Bold, maxLines = 1,
                                color = if (metric.label == "Efficiency") efficiencyValueColor(metric.efficiency, dark,
                                    MaterialTheme.colorScheme.onSurface) else androidx.compose.ui.graphics.Color.Unspecified)
                        }
                        metric.description?.let { Text(it, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                }
            }
        }
    }
}
