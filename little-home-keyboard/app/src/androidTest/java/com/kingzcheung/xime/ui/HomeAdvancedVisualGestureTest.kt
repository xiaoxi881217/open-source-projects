package com.kingzcheung.xime.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.kingzcheung.xime.appearance.*
import com.kingzcheung.xime.keyboard.ToolbarButton
import com.kingzcheung.xime.keyboard.ToolbarButtonItem
import com.kingzcheung.xime.ui.keyboard.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

/** 合成布局回归；编译与执行结果分别记录，不操作本人手机。 */
class HomeAdvancedVisualGestureTest {
    @get:Rule val compose=createComposeRule()
    @Test fun rowSpacingIsAnActualGapAndRowTapsStillWork() {
        val clicks=AtomicInteger();var density=1f
        val preset=HomeVisualPreset(spacingY=0f,rowGaps=mapOf("qwerty" to listOf(12f)))
        compose.setContent { MaterialTheme {
            density=LocalDensity.current.density
            CompositionLocalProvider(LocalHomePreset provides preset){
                HomeKeyboardRows("qwerty",2,Modifier.size(240.dp,180.dp)){index,m->Box(m.fillMaxWidth().testTag("row$index").clickable{clicks.incrementAndGet()})}
            }
        } }
        val first=compose.onNodeWithTag("row0").fetchSemanticsNode().boundsInRoot
        val second=compose.onNodeWithTag("row1").fetchSemanticsNode().boundsInRoot
        assertEquals(12f*density,second.top-first.bottom,.1f)
        compose.onNodeWithTag("row0").performTouchInput{click()}
        compose.onNodeWithTag("row1").performTouchInput{click()}
        assertEquals(2,clicks.get())
    }
    @Test fun replacementIconKeepsItsActionAndDoesNotTakeNeighborTaps() {
        val clicks=AtomicInteger();val neighbor=AtomicInteger()
        val preset=HomeVisualPreset(keys=mapOf("toolbar:clipboard" to HomeKeyStyle(iconAsset="builtin:heart",iconSize=40f,labelOffsetX=.4f)))
        compose.setContent { MaterialTheme {
            CompositionLocalProvider(LocalHomePreset provides preset){
                Row {
                    Box(Modifier.size(32.dp).testTag("icon").clickable{clicks.incrementAndGet()}){ToolbarButtonIcon(ToolbarButtonItem.Builtin(ToolbarButton.CLIPBOARD),Color.Black,Modifier.fillMaxSize())}
                    Box(Modifier.size(32.dp).testTag("neighbor").clickable{neighbor.incrementAndGet()})
                }
            }
        } }
        compose.onNodeWithTag("neighbor").performTouchInput{click()}
        assertEquals(0,clicks.get());assertEquals(1,neighbor.get())
        compose.onNodeWithTag("icon").performTouchInput{click()};assertEquals(1,clicks.get())
    }
}
