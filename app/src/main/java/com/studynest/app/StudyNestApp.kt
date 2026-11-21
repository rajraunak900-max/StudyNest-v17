package com.studynest.app

import android.app.*
import android.media.AudioManager
import android.os.SystemClock
import android.provider.Settings
import com.studynest.app.automation.Notifications
import com.studynest.app.data.*
import com.studynest.app.domain.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow

class StudyNestApp : Application(), androidx.work.Configuration.Provider {
    override val workManagerConfiguration: androidx.work.Configuration
        get() = androidx.work.Configuration.Builder().build()

    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    lateinit var db: StudyDatabase
    lateinit var preferences: Preferences
    lateinit var engine: SessionEngine
    val events by lazy { com.studynest.app.automation.BackgroundEvents(this, refresh = { Notifications.refresh(this) }) }
    val usage by lazy { com.studynest.app.automation.UsageMonitor(db, com.studynest.app.automation.AndroidUsageSource(this)) }
    val breakReminder by lazy { com.studynest.app.automation.BreakReminder(this) }
    val monitoring = MutableStateFlow(false)
    val monitoringSettingsLock = kotlinx.coroutines.sync.Mutex()
    val message = MutableStateFlow("")
    val soundStatus = MutableStateFlow("Checking sound mode…")
    val automationStatus = MutableStateFlow("Manual mode")
    val geofenceStatus = MutableStateFlow("")

    private var soundDeadline: Long? = null
    private var soundDeadlineJob: Job? = null

    @Synchronized fun cancelSoundDeadline() {
        soundDeadlineJob?.cancel()
        soundDeadlineJob = null
        soundDeadline = null
    }

    @Synchronized fun scheduleSoundDeadline(deadline: Long) {
        if (soundDeadline == deadline && soundDeadlineJob?.isActive == true) return
        soundDeadlineJob?.cancel()
        soundDeadline = deadline
        val wake = com.studynest.app.automation.EventWake(this,
            (deadline - System.currentTimeMillis() + 10000).coerceIn(10000, 310000))
        soundDeadlineJob = scope.launch {
            try {
                delay((deadline - System.currentTimeMillis()).coerceAtLeast(0))
                if (preferences.value.wifi) engine.signal("Wi-Fi", preferences.value.matchesSsid(
                    com.studynest.app.automation.Automation.currentSsid(this@StudyNestApp)))
                engine.tick()
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { report("Sound restoration needs attention: ${e.message}") }
            finally { wake.close() }
        }.also { it.invokeOnCompletion { wake.close() } }
    }

    fun report(s: String) {
        message.value = s
    }

    fun runTask(block: suspend () -> Unit) =
        scope.launch {
            try {
                block()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                report(e.message ?: "Operation failed. Please try again.")
            }
        }

    override fun onCreate() {
        super.onCreate()
        db = StudyDatabase.open(this)
        preferences = Preferences(this)
        engine =
            SessionEngine(
                db,
                object : StudyClock {
                    override fun now() = System.currentTimeMillis()

                    override fun elapsed() = SystemClock.elapsedRealtime()

                    override fun boot() =
                        Settings.Global.getInt(contentResolver, Settings.Global.BOOT_COUNT, -1)
                },
                { preferences.value },
                object : SoundControl {
                    val audio
                        get() = getSystemService(AudioManager::class.java)

                    override fun current() = audio.ringerMode

                    override suspend fun set(mode: Int): Boolean =
                        withContext(Dispatchers.Main) {
                            fun failure(reason: String): Boolean {
                                soundStatus.value = reason
                                report(reason)
                                return false
                            }
                            if (!getSystemService(NotificationManager::class.java)
                                .isNotificationPolicyAccessGranted) {
                                return@withContext failure(
                                    "Sound access is missing. Allow Do Not Disturb access for StudyNest.")
                            }
                            if (audio.isVolumeFixed) {
                                return@withContext failure(
                                    "Android reports fixed-volume mode; this device currently blocks ringer changes.")
                            }
                            try {
                                audio.ringerMode = mode
                                // Audio service may apply the request after the setter returns.
                                // Wait for acknowledgement instead of rejecting an immediate stale
                                // read.
                                repeat(6) {
                                    delay(250)
                                    if (audio.ringerMode == mode) return@withContext true
                                }
                                val actual =
                                    when (audio.ringerMode) {
                                        AudioManager.RINGER_MODE_NORMAL -> "Sound"
                                        AudioManager.RINGER_MODE_SILENT -> "Silent"
                                        AudioManager.RINGER_MODE_VIBRATE -> "Vibrate"
                                        else -> "Unknown"
                                    }
                                failure(
                                    "Android did not confirm the requested sound mode after 1.5 seconds. Current mode: $actual. Sound access is granted. Check phone routines or other sound-control apps.")
                            } catch (e: SecurityException) {
                                failure(
                                    "Android blocked the sound request (${e.javaClass.simpleName}). Check Do Not Disturb access.")
                            }
                        }
                },
                ::report,
                { soundStatus.value = it },
                { Notifications.reminder(this) },
                { Notifications.cancelReminder(this) },
                { com.studynest.app.automation.Automation.scheduleOverride(this, it) },
                { com.studynest.app.automation.UsageAccess.allowed(this) },
                { usage.poll() },
                { breakReminder.schedule(it) },
                { com.studynest.app.automation.BreakReminder.vibrate(this) })
        Notifications.create(this)
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                engine.recover(true)
                if (preferences.value.wifi) engine.signal("Wi-Fi", preferences.value.matchesSsid(com.studynest.app.automation.Automation.currentSsid(this@StudyNestApp)))
            } catch (e: Exception) {
                report("Recovery needs attention: ${e.message}")
            }
        }
    }
}

