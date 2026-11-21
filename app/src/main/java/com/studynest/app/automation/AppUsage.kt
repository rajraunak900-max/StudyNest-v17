package com.studynest.app.automation

import android.app.AppOpsManager
import android.app.KeyguardManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.media.AudioManager
import android.os.Process
import android.os.PowerManager
import android.os.UserManager
import androidx.room.withTransaction
import com.studynest.app.StudyNestApp
import com.studynest.app.data.*
import com.studynest.app.domain.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray

object UsageAccess {
    fun allowed(c: Context): Boolean = runCatching {
        c.getSystemService(AppOpsManager::class.java).checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), c.packageName) == AppOpsManager.MODE_ALLOWED
    }.getOrDefault(false)
    fun unlocked(c: Context) = c.getSystemService(UserManager::class.java).isUserUnlocked
    fun interactive(c: Context) = c.getSystemService(PowerManager::class.java).isInteractive &&
        !c.getSystemService(KeyguardManager::class.java).isKeyguardLocked
}

data class InstalledApp(val pkg: String, val label: String, val system: Boolean)
object AppCatalog {
    fun installed(c: Context): List<InstalledApp> = c.packageManager.queryIntentActivities(
        Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
        .map { it.activityInfo.applicationInfo }.distinctBy { it.packageName }
        .map { InstalledApp(it.packageName, it.loadLabel(c.packageManager).toString(),
            it.flags and ApplicationInfo.FLAG_SYSTEM != 0) }.sortedBy { it.label.lowercase() }
    /** Local label-first ranking; filtering never changes saved or draft selections. */
    fun search(apps: List<InstalledApp>, query: String): List<InstalledApp> {
        val text = query.trim()
        if (text.isEmpty()) return apps
        return apps.filter { it.label.contains(text, true) || it.pkg.contains(text, true) }
            .sortedWith(compareBy<InstalledApp> {
                when {
                    it.label.startsWith(text, true) -> 0
                    it.label.contains(text, true) -> 1
                    else -> 2
                }
            }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.label }.thenBy { it.pkg })
    }
    fun label(c: Context, pkg: String) = runCatching {
        c.packageManager.getApplicationLabel(c.packageManager.getApplicationInfo(pkg, 0)).toString()
    }.getOrDefault(pkg)
}

interface UsageSource {
    fun events(from: Long, until: Long): List<AppEvent>?
    fun interactive(): Boolean
    fun callAudio(): Boolean
}

class AndroidUsageSource(private val context: Context) : UsageSource {
    override fun interactive() = UsageAccess.interactive(context)
    override fun callAudio(): Boolean = runCatching {
        val mode = context.getSystemService(AudioManager::class.java).mode
        mode == AudioManager.MODE_IN_CALL || mode == AudioManager.MODE_IN_COMMUNICATION
    }.getOrDefault(false)
    override fun events(from: Long, until: Long): List<AppEvent>? {
        if (!UsageAccess.allowed(context) || !UsageAccess.unlocked(context)) return null
        return runCatching {
            val stream = context.getSystemService(UsageStatsManager::class.java).queryEvents(from, until + 1) ?: return null
            val event = android.app.usage.UsageEvents.Event()
            val out = mutableListOf<AppEvent>()
            while (stream.hasNextEvent()) {
                stream.getNextEvent(event)
                // 1/2 are MOVE_TO_FOREGROUND/BACKGROUND on 26-28 and RESUMED/PAUSED
                // on newer releases. Screen/keyguard event types 15-18 begin on 28.
                val type = when (event.eventType) {
                    1 -> UsageTimeline.RESUME
                    2 -> UsageTimeline.PAUSE
                    16, 17 -> UsageTimeline.LOCK
                    18 -> UsageTimeline.UNLOCK
                    else -> null
                }
                // The public class name identifies activity changes, not app content.
                // Save only its numeric hash for the selected foreground activity.
                if (type != null) out += AppEvent(event.timeStamp, type, event.packageName,
                    event.className?.hashCode() ?: 0)
            }
            out
        }.getOrNull()
    }
}

/** Queries one recent event stream only during measured sessions, never all apps. */
class UsageMonitor(
    private val db: StudyDatabase, private val source: UsageSource,
    private val now: () -> Long = System::currentTimeMillis
) {
    private val lock = Mutex()
    suspend fun poll(deviceEvent: Int? = null) = withContext(Dispatchers.IO) {
        lock.withLock {
            val time = now()
            val dao = db.focusDao()
            for (w in dao.pending(time).filter { it.mode == "ON" }) {
                val until = minOf(w.end ?: time, time)
                if (until < w.cursor || (until == w.cursor && w.seeded)) continue
                val array = JSONArray(w.packages)
                val selected = (0 until array.length()).map { array.getString(it) }.toSet()
                var state = UsageState.decode(w.tracker)
                val recent = w.cursor >= time - MAX_REPLAY_MS
                val events = if (selected.isEmpty() || (w.end == null && w.seeded && !state.interactive && !source.interactive())) emptyList() else if (!recent) null
                    else source.events(w.cursor, until)
                if (!w.seeded) {
                    state = if (selected.isEmpty()) UsageState(known = true) else {
                        val seed = if (recent) source.events((w.start - MAX_REPLAY_MS).coerceAtLeast(0), w.start) else null
                        if (seed != null) UsageTimeline.reduce((w.start - MAX_REPLAY_MS).coerceAtLeast(0), w.start,
                            UsageState(), seed, selected, w.calls).second else UsageState()
                    }
                }
                val batch = events.orEmpty().toMutableList()
                val callNow = w.calls && source.callAudio()
                if (w.end == null) {
                    if (deviceEvent != null) batch += AppEvent(until, deviceEvent)
                    else if (!source.interactive()) batch += AppEvent(until, UsageTimeline.LOCK)
                    // API 26-27 lack historical keyguard events. Recover the current
                    // unlocked state even if a screen broadcast was missed.
                    else if (!state.interactive) batch += AppEvent(until, UsageTimeline.UNLOCK)
                    if (w.calls) batch += AppEvent(until, if (callNow) UsageTimeline.CALL_ON else UsageTimeline.CALL_OFF)
                }
                val reduced = if ((events == null && state.interactive && selected.isNotEmpty()) ||
                    (w.calls && until - w.cursor > 60000 && (state.call || callNow)))
                    listOf(UsagePiece(w.cursor, until, "UNKNOWN")) to
                        UsageTimeline.reduce(until, until, state.copy(known = false), batch.filter { it.at == until }, selected, w.calls).second
                else UsageTimeline.reduce(w.cursor, until, state, batch, selected, w.calls)
                val periods = reduced.first.map { DistractionPeriod(windowId = w.id,
                    start = it.start, end = it.end, reason = it.reason, packageName = it.pkg) }
                db.withTransaction {
                    dao.insertPeriods(periods)
                    // Never overwrite end/checkpoint: a timer action may have closed it.
                    dao.progress(w.id, until, reduced.second.encode())
                }
            }
        }
    }
    companion object { const val POLL_MS = 15000L; const val MAX_REPLAY_MS = 86400000L }
}

suspend fun StudyNestApp.saveMonitoring(enabled: Boolean? = null, packages: Set<String>? = null,
    calls: Boolean? = null, appSelection: Pair<String, Boolean>? = null) = monitoringSettingsLock.withLock {
    usage.poll() // Finish old selection snapshot before changing the setting.
    val old = preferences.value
    val selected = appSelection?.let { (pkg, on) -> if (on) old.monitoredApps + pkg else old.monitoredApps - pkg }
        ?: packages ?: old.monitoredApps
    preferences.save(old.copy(appMonitoring = enabled ?: old.appMonitoring, monitoredApps = selected,
        monitorCallAudio = calls ?: old.monitorCallAudio))
    engine.syncFocus()
    Notifications.refresh(this)
}
