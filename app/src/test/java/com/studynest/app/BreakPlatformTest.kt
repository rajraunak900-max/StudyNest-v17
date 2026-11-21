package com.studynest.app

import android.os.*
import androidx.test.core.app.ApplicationProvider
import androidx.work.WorkManager
import com.studynest.app.automation.BreakReminder
import com.studynest.app.data.ManualBreak
import com.studynest.app.domain.BreakRecorder
import kotlinx.coroutines.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk=[26,33,34,35,36],application=StudyNestApp::class)
class BreakPlatformTest {
    private lateinit var app:StudyNestApp
    @Before fun setup()=runBlocking {
        app=ApplicationProvider.getApplicationContext()
        app.scope.coroutineContext[Job]!!.children.toList().forEach { it.join() }
    }
    @After fun close() { app.breakReminder.close();app.scope.cancel();app.db.close() }
    @Test fun supportedVibrationApiRequestsOneTwoSecondEffect() {
        val vibrator=if(Build.VERSION.SDK_INT>=31) app.getSystemService(VibratorManager::class.java).defaultVibrator
            else { @Suppress("DEPRECATION") app.getSystemService(Vibrator::class.java) }
        shadowOf(vibrator).setHasVibrator(true)
        BreakReminder.vibrate(app)
        assertEquals(BreakRecorder.VIBRATION_MS,shadowOf(vibrator).milliseconds)
    }
    @Test fun oneDelayedWorkItemPerBreakAndCancellationWithoutAnActivity() {
        val manager=WorkManager.getInstance(app)
        val record=ManualBreak(id=42,sessionId=1,visitId=1,start=System.currentTimeMillis(),
            elapsedStart=SystemClock.elapsedRealtime(),boot=1)
        app.breakReminder.schedule(record)
        val first=manager.getWorkInfosForUniqueWork(BreakReminder.WORK).get(10,TimeUnit.SECONDS)
            .filterNot { it.state.isFinished }
        assertEquals(1,first.size)
        assertTrue(first.single().initialDelayMillis in (BreakRecorder.LIMIT_MS-10000)..BreakRecorder.LIMIT_MS)
        repeat(5) { app.breakReminder.schedule(record) }
        val repeated=manager.getWorkInfosForUniqueWork(BreakReminder.WORK).get(10,TimeUnit.SECONDS)
            .filterNot { it.state.isFinished }
        assertEquals(first.single().id,repeated.single().id)
        app.breakReminder.schedule(null)
        assertTrue(manager.getWorkInfosForUniqueWork(BreakReminder.WORK).get(10,TimeUnit.SECONDS).all { it.state.isFinished })
        val next=record.copy(id=43,elapsedStart=SystemClock.elapsedRealtime())
        app.breakReminder.schedule(next)
        val replacement=manager.getWorkInfosForUniqueWork(BreakReminder.WORK).get(10,TimeUnit.SECONDS)
            .filterNot { it.state.isFinished }
        assertEquals(1,replacement.size);assertNotEquals(first.single().id,replacement.single().id)
        app.breakReminder.schedule(null)
    }
}
