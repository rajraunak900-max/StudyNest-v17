package com.studynest.app
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.studynest.app.automation.*
import com.studynest.app.ui.efficiencyValueColor
import org.junit.Test
import org.junit.Assert.*

class Post168PresentationTest {
    private val apps=listOf(InstalledApp("pkg.what","Other app",false),InstalledApp("pkg.chat","My WhatsApp helper",false),
        InstalledApp("pkg.main","WhatsApp",false),InstalledApp("pkg.insta","Instagram",false),
        InstalledApp("pkg.face","Facebook",false),InstalledApp("pkg.video","YouTube",false))
    @Test fun shortVisibleLabelsMatchCaseInsensitivelyAndRankBeforePackages(){
        assertEquals(listOf("WhatsApp","My WhatsApp helper","Other app"),AppCatalog.search(apps,"  wHa  ").map{it.label})
        for((q,label) in listOf("inst" to "Instagram","face" to "Facebook","you" to "YouTube"))
            assertEquals(label,AppCatalog.search(apps,q).single().label)
        assertEquals("WhatsApp",AppCatalog.search(apps,"tsap").single {it.label=="WhatsApp"}.label)
        assertSame(apps,AppCatalog.search(apps," "));assertTrue(AppCatalog.search(apps,"nothing").isEmpty())
    }
    @Test fun efficiencyBoundariesAndUnknownUseOnlyTextPalette(){
        val neutral=Color.Magenta
        for(dark in listOf(false,true)){
            assertEquals(efficiencyValueColor(0.0,dark,neutral),efficiencyValueColor(49.9,dark,neutral))
            assertEquals(efficiencyValueColor(50.0,dark,neutral),efficiencyValueColor(74.9,dark,neutral))
            assertEquals(efficiencyValueColor(75.0,dark,neutral),efficiencyValueColor(100.0,dark,neutral))
            assertNotEquals(efficiencyValueColor(49.9,dark,neutral),efficiencyValueColor(50.0,dark,neutral))
            assertNotEquals(efficiencyValueColor(74.9,dark,neutral),efficiencyValueColor(75.0,dark,neutral))
            assertEquals(neutral,efficiencyValueColor(null,dark,neutral));assertEquals(neutral,efficiencyValueColor(Double.NaN,dark,neutral))
            val surface=if(dark)Color(0xFF121B18) else Color(0xFFF7F8F3)
            for(value in listOf(49.9,50.0,75.0)){
                val ink=efficiencyValueColor(value,dark,neutral)
                val ratio=(maxOf(ink.luminance(),surface.luminance())+0.05)/(minOf(ink.luminance(),surface.luminance())+0.05)
                assertTrue("Readable text $value dark=$dark",ratio>=4.5)
            }
        }
    }
}
