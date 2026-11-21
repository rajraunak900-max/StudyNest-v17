package com.studynest.app.automation

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.work.*
import com.studynest.app.StudyNestApp
import com.studynest.app.data.ManualBreak
import com.studynest.app.domain.BreakRecorder
import kotlinx.coroutines.*
import java.util.concurrent.TimeUnit

/** One delayed task per continuous break, plus persistent WorkManager fallback.
 * No long wake lock, AlarmManager alarm or new polling loop. Doze may delay it. */
class BreakReminder(private val app: StudyNestApp) {
    private var id: Long? = null
    private var task: Job? = null
    private var initialized = false
    @Synchronized fun schedule(record: ManualBreak?) {
        if (initialized && record?.id == id) return
        initialized = true
        task?.cancel(); task = null
        id = record?.id
        val manager = WorkManager.getInstance(app)
        if (record == null) { manager.cancelUniqueWork(WORK); return }
        val wait = (BreakRecorder.LIMIT_MS - (SystemClock.elapsedRealtime() - record.elapsedStart)).coerceAtLeast(0)
        manager.enqueueUniqueWork(WORK, ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<BreakReminderWorker>()
                .setInputData(workDataOf("break_id" to record.id))
                .setInitialDelay(wait, TimeUnit.MILLISECONDS).build())
        task = app.scope.launch { delay(wait); check(app, record.id) }
    }
    @Synchronized fun close() { task?.cancel(); task = null }
    companion object {
        const val WORK = "manual-break-reminder"
        suspend fun check(app: StudyNestApp, id: Long) {
            // One deadline sample ensures a known Wi-Fi departure can cancel delivery.
            if (app.preferences.value.wifi) app.engine.signal("Wi-Fi",
                app.preferences.value.matchesSsid(Automation.currentSsid(app)))
            app.engine.checkBreak(id)
        }
        fun vibrate(c: Context) {
            val vibrator = if (Build.VERSION.SDK_INT >= 31)
                c.getSystemService(VibratorManager::class.java).defaultVibrator
            else { @Suppress("DEPRECATION") c.getSystemService(Vibrator::class.java) }
            if (!vibrator.hasVibrator()) return
            val attributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
            vibrator.vibrate(VibrationEffect.createOneShot(BreakRecorder.VIBRATION_MS,
                VibrationEffect.DEFAULT_AMPLITUDE), attributes)
        // This callback is already persisted once per continuous break by BreakRecorder.
        Notifications.breakReminder(c)
        }
    }
}

class BreakReminderWorker(c: Context, p: WorkerParameters) : CoroutineWorker(c, p) {
    override suspend fun doWork(): Result = try {
        val id = inputData.getLong("break_id", -1)
        if (id > 0) BreakReminder.check(applicationContext as StudyNestApp, id)
        Result.success()
    } catch (e: CancellationException) { throw e }
    catch (_: Exception) { Result.retry() }
}
