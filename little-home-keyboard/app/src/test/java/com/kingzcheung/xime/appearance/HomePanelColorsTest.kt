package com.kingzcheung.xime.appearance
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.kingzcheung.xime.ui.theme.homePanelColors
import org.junit.Assert.*
import org.junit.Test
class HomePanelColorsTest {
 @Test fun allPanelRolesReuseOverallPalette(){
  val p=HomePalette(background=0xffeeeedd,key=0xffddddff,special=0xffaabbcc,text=0xff223344,candidate=0xff445566,accent=0xff996633)
  val c=homePanelColors(p,lightColorScheme());assertEquals(Color(p.key),c.surface);assertEquals(Color(p.special),c.secondaryContainer);assertEquals(Color(p.text),c.onSurface);assertEquals(Color(p.candidate),c.onSurfaceVariant);assertEquals(Color(p.accent),c.primary);assertEquals(Color(p.background),c.background)
 }
}
