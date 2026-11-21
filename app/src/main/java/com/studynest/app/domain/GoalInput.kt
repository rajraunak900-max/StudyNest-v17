package com.studynest.app.domain

import kotlin.math.roundToInt

object GoalInput {
    fun minutes(raw: String): Int? = raw.trim().takeIf { it.matches(Regex("[0-9]+")) }
        ?.toIntOrNull()?.takeIf { it in 1..1440 }
    fun hours(raw: String): Int? = raw.trim().takeIf { it.matches(Regex("[0-9]+(?:[.]?[0-9]*)?|[.][0-9]+")) }
        ?.toDoubleOrNull()?.takeIf { it.isFinite() && it >= 1.0 / 60 && it <= 24.0 }
        ?.let { (it * 60).roundToInt().coerceIn(1,1440) }
}
