package com.studynest.app

import android.content.Context
import android.content.res.Configuration
import android.view.*
import android.widget.*
import androidx.test.core.app.ApplicationProvider
import com.studynest.app.automation.NotificationTiming
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk=[33],qualifiers="w320dp-h640dp-mdpi",application=android.app.Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class NotificationLayoutTest {
    @Test fun compactAndExpandedLayoutsFitNarrowLargeTextInBothThemes() {
        val app=ApplicationProvider.getApplicationContext<Context>()
        for(dark in listOf(false,true)) {
            val configuration=Configuration(app.resources.configuration).apply {
                fontScale=1.5f;uiMode=if(dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
            }
            val c=ContextThemeWrapper(app.createConfigurationContext(configuration),
                if(dark) android.R.style.Theme_DeviceDefault else android.R.style.Theme_DeviceDefault_Light)
            val density=c.resources.displayMetrics.density
            val timing=NotificationTiming(5077000,null,true,false,"Study time unavailable",android.os.SystemClock.elapsedRealtime())
            for(expanded in listOf(false,true)) {
                val view=timing.views(c,expanded).apply(c,FrameLayout(c))
                val width=(260*density).toInt()
                view.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED))
                view.layout(0,0,view.measuredWidth,view.measuredHeight)
                val fitsHeight = view.measuredHeight <= (if(expanded)252 else 48)*density+1
                fun check(node:View) {
                    if(node.visibility!=View.VISIBLE) return
                    if(node is TextView) node.layout?.let { layout ->
                        for(line in 0 until layout.lineCount) assertTrue("Clipped ${node.text}",
                            layout.getLineRight(line)-layout.getLineLeft(line)<=node.width+1)
                    }
                    if(node is ViewGroup) for(i in 0 until node.childCount) check(node.getChildAt(i))
                }
                check(view)
                val bitmap=android.graphics.Bitmap.createBitmap(width,view.height,android.graphics.Bitmap.Config.ARGB_8888)
                val canvas=android.graphics.Canvas(bitmap);canvas.drawColor(if(dark) android.graphics.Color.rgb(32,32,32) else android.graphics.Color.WHITE)
                view.draw(canvas)
                val file=java.io.File("build/reports/screenshots/notification-${if(expanded)"expanded" else "compact"}-${if(dark)"dark" else "light"}.png")
                file.parentFile!!.mkdirs()
                java.io.FileOutputStream(file).use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) }
                assertTrue("Notification height ${view.measuredHeight/density} dp exceeds ${if(expanded)252 else 48} dp; expanded=$expanded",fitsHeight)
            }
        }
    }
}
