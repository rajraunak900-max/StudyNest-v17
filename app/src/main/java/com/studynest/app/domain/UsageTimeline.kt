package com.studynest.app.domain

import org.json.JSONObject

data class AppEvent(val at: Long, val type: Int, val pkg: String? = null, val instance: Int = 0)
data class UsageState(val pkg: String? = null, val known: Boolean = false,
    val interactive: Boolean = true, val pauseAt: Long? = null, val call: Boolean = false,
    val instance: Int = 0) {
    fun encode(): String = JSONObject().put("pkg", pkg).put("known", known)
        .put("interactive", interactive).put("pauseAt", pauseAt).put("call", call).put("instance", instance).toString()
    companion object {
        fun decode(value: String): UsageState = runCatching {
            val o = JSONObject(value)
            UsageState(if (o.isNull("pkg")) null else o.getString("pkg"), o.optBoolean("known"),
                o.optBoolean("interactive", true), if (o.isNull("pauseAt")) null else o.getLong("pauseAt"), o.optBoolean("call"), o.optInt("instance"))
        }.getOrDefault(UsageState())
    }
}
data class UsagePiece(val start: Long, val end: Long, val reason: String, val pkg: String? = null)

/** One timeline: app/call overlap is classified once. Activity switches coalesce for 1s. */
object UsageTimeline {
    const val RESUME = 1
    const val PAUSE = 2
    const val UNLOCK = 3
    const val LOCK = 4
    const val CALL_ON = 5
    const val CALL_OFF = 6
    const val SWITCH_GRACE_MS = 1000L

    fun reduce(from: Long, until: Long, initial: UsageState, events: List<AppEvent>,
        selected: Set<String>, calls: Boolean): Pair<List<UsagePiece>, UsageState> {
        var state = initial
        var at = from
        val pieces = mutableListOf<UsagePiece>()
        fun emit(end: Long) {
            if (end <= at) return
            val reason = when {
                calls && state.call -> "CALL"
                !state.interactive -> "CLEAR"
                !state.known -> "UNKNOWN"
                state.pkg in selected -> "APP"
                else -> "CLEAR"
            }
            if (reason != "CLEAR") {
                val pkg = if (reason == "APP") state.pkg else null
                val old = pieces.lastOrNull()
                if (old?.end == at && old.reason == reason && old.pkg == pkg)
                    pieces[pieces.lastIndex] = old.copy(end = end)
                else pieces += UsagePiece(at, end, reason, pkg)
            }
            at = end
        }
        fun settle(end: Long) {
            val expiry = state.pauseAt?.plus(SWITCH_GRACE_MS)
            if (expiry != null && expiry < end) {
                emit(expiry.coerceAtLeast(at))
                state = state.copy(pkg = null, known = true, pauseAt = null)
            }
        }
        for (event in events.filter { it.at in from..until }.sortedBy { it.at }) {
            settle(event.at)
            emit(event.at)
            state = when (event.type) {
                RESUME -> state.copy(pkg = event.pkg?.takeIf { it in selected }, known = true, pauseAt = null, instance = event.instance)
                PAUSE -> if (event.pkg == state.pkg && (event.instance == 0 || state.instance == 0 || event.instance == state.instance))
                    state.copy(pauseAt = event.at) else state
                LOCK -> state.copy(pkg = null, known = true, interactive = false, pauseAt = null)
                UNLOCK -> state.copy(known = true, interactive = true, pauseAt = null)
                CALL_ON -> state.copy(call = true)
                CALL_OFF -> state.copy(call = false)
                else -> state
            }
        }
        settle(until)
        emit(until)
        return pieces to state
    }
}
