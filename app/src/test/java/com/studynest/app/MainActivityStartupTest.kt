package com.studynest.app

import androidx.test.core.app.ApplicationProvider
import com.studynest.app.data.Config
import kotlinx.coroutines.*
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [26, 33, 36], application = StudyNestApp::class,
    qualifiers = "w411dp-h891dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MainActivityStartupTest {
    @Test fun actualEntryActivityLaunchesWithSetupAndModernInsets() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<StudyNestApp>()
        app.scope.coroutineContext[Job]!!.children.toList().forEach { it.join() }
        app.preferences.save(Config(setup = false, vibrate = false))
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            shadowOf(android.os.Looper.getMainLooper()).idle()
            assertNotNull(controller.get().window.decorView)
            assertEquals("StudyNest", app.applicationInfo.loadLabel(app.packageManager).toString())
        } finally {
            controller.pause().stop().destroy()
            app.scope.cancel()
        }
    }
}
