package com.studynest.app

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.studynest.app.domain.FocusSummary
import com.studynest.app.ui.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk=[33],qualifiers="w411dp-h891dp-xhdpi",application=android.app.Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class Post168EfficiencyUiTest {
    @get:Rule val rule=createAndroidComposeRule<ComponentActivity>()
    @After fun close(){rule.runOnIdle{rule.activity.setContent{}};rule.waitForIdle()}
    @Test fun percentageOnlyChangesColourInLightAndDarkWithUnchangedMathAndCards(){
        var dark by mutableStateOf(false);var percent by mutableStateOf(47.9)
        rule.setContent {
            MaterialTheme(colorScheme=if(dark)darkColorScheme(surface=Color(0xFF121B18)) else lightColorScheme(surface=Color(0xFFF7F8F3))){
                Column(Modifier.fillMaxSize()){
                    FocusNumbers(FocusSummary(libraryMs=100000,libraryStudyMs=(percent*1000).toLong()))
                }
            }
        }
        for(night in listOf(false,true))for(value in listOf(47.9,49.9,50.0,74.9,75.0)){
            rule.runOnIdle{dark=night;percent=value};rule.waitForIdle()
            val label=String.format(java.util.Locale.getDefault(),"%.1f%%",value)
            val layouts=mutableListOf<TextLayoutResult>()
            rule.onNodeWithText(label,useUnmergedTree=true).performSemanticsAction(SemanticsActions.GetTextLayoutResult){it(layouts)}
            assertEquals(efficiencyValueColor(value,night,Color.Unspecified),layouts.single().layoutInput.style.color)
            for(metric in listOf("Sitting Time","Actual Study","Distraction","Break","Efficiency"))rule.onNodeWithTag("metric-$metric").assertExists()
            if(value==47.9 || value==75.0)rule.runOnIdle{
                val view=rule.activity.window.decorView
                val bitmap=android.graphics.Bitmap.createBitmap(view.width,view.height,android.graphics.Bitmap.Config.ARGB_8888)
                view.draw(android.graphics.Canvas(bitmap))
                val file=java.io.File("build/reports/screenshots/169-efficiency-${if(night)"dark" else "light"}-$value.png");file.parentFile!!.mkdirs()
                java.io.FileOutputStream(file).use{bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)}
            }
        }
    }
}
