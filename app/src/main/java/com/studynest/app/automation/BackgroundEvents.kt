package com.studynest.app.automation

import android.os.PowerManager
import com.studynest.app.StudyNestApp
import com.studynest.app.domain.SessionEngine
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Event-only wake protection. Never holds a lock for an entire study session. */
internal class EventWake(context: android.content.Context, timeout: Long) : AutoCloseable {
    private val lock = context.getSystemService(PowerManager::class.java)
        .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "StudyNest:presence-event")
        .apply { setReferenceCounted(false); acquire(timeout) }
    @Synchronized override fun close() { if (lock.isHeld) runCatching { lock.release() } }
}

class BackgroundEvents(
    private val app: StudyNestApp,
    private val scope: CoroutineScope = app.scope,
    private val now: () -> Long = System::currentTimeMillis,
    private val sampleWifi: () -> Boolean = { app.preferences.value.matchesSsid(Automation.currentSsid(app)) },
    private val refresh: suspend () -> Unit = {},
    private val wait: suspend (Long) -> Unit = { delay(it) }
) {
    private val mutex = Mutex()
    private var deadlines: List<Long> = emptyList()
    private var settling: Job? = null

    /** Acquire before dispatch: network callbacks can arrive with the screen off. */
    fun dispatch(source: String, inside: Boolean): Job {
        val wake = EventWake(app, 10000)
        return scope.launch {
            try { signal(source, inside) }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { app.report("Presence event failed: ${e.message}") }
            finally { wake.close() }
        }.also { it.invokeOnCompletion { wake.close() } }
    }

    suspend fun signal(source: String, inside: Boolean) {
        val wake = EventWake(app, 10000)
        try {
            mutex.withLock {
                val cfg = app.preferences.value
                if (source == "Wi-Fi" && !cfg.wifi || source == "Location" && !cfg.geo) return@withLock
                app.engine.signal(source, inside)
                app.engine.tick()
                armDeadlines()
                refresh()
            }
        } finally { wake.close() }
    }

    /** Rebuild pending deadlines from Room after service/process recreation. */
    suspend fun recover() {
        val wake = EventWake(app, 10000)
        try { mutex.withLock { app.engine.tick(); armDeadlines(); refresh() } }
        finally { wake.close() }
    }

    private suspend fun armDeadlines() {
        val state = app.db.dao().state()
        val active=if(state?.exitAt!=null) app.db.dao().active() else null
        val visit=if(state?.exitAt!=null) app.db.focusDao().openVisit() else null
        val departure=state?.exitAt?.plus(SessionEngine.departureGraceMs(app.preferences.value,active?.trigger))
        val next = listOfNotNull(
            state?.wifiExitAt?.plus(SessionEngine.WIFI_SOUND_GRACE_MS),
            state?.exitAt?.takeIf {active?.paused==false || (visit!=null && visit.awaySince==null)}?.plus(SessionEngine.EXIT_GRACE_MS),
            departure
        ).distinct().sorted()
        if (next == deadlines && settling?.isActive == true) return
        settling?.cancel()
        deadlines = next
        if (next.isEmpty()) { settling = null; return }
        Automation.scheduleExit(app,next.last()-now()) // Persisted fallback for the actual deadline.
        // Protect the FIRST short confirmation/sound deadline, independently of
        // the long visit deadline. A coroutine delay alone cannot keep the CPU
        // running with the screen off. Release before waiting out session grace.
        val wake = if(next.first()-now()<=100000) EventWake(app, (next.first() - now() + 10000).coerceIn(10000, 100000)) else null
        settling = scope.launch {
            try {
                for (deadline in next) {
                    wait((deadline - now()).coerceAtLeast(0))
                    val eventWake=EventWake(app,10000)
                    try { mutex.withLock {
                        if (app.preferences.value.wifi) app.engine.signal("Wi-Fi", sampleWifi())
                        app.engine.tick()
                        refresh()
                    } } finally {eventWake.close(); if (deadline == next.first()) wake?.close()}
                }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { app.report("Departure check failed: ${e.message}") }
            finally { wake?.close() }
        }.also { it.invokeOnCompletion { wake?.close() } }
    }
}
