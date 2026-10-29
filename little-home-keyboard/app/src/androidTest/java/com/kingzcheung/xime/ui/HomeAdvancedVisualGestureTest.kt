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
    @Test fun completeToolbarMovesActualHitTargetAndKeepsActions() {
        val more=AtomicInteger();val clipboard=AtomicInteger()
        var order by mutableStateOf(listOf("more_menu","clipboard","hide_keyboard"))
        compose.setContent { MaterialTheme {
            CompositionLocalProvider(LocalHomePreset provides HomeVisualPreset()) {
                Box(Modifier.width(300.dp)) {
                    HomeFullToolbar(listOf(ToolbarButtonItem.Builtin(ToolbarButton.CLIPBOARD)),order,Color.Black,Color.Red,
                        {id->if(id=="more_menu")more.incrementAndGet() else if(id=="clipboard")clipboard.incrementAndGet()})
                }
            }
        } }
        val original=compose.onNodeWithContentDescription("打开更多").fetchSemanticsNode().boundsInRoot
        compose.runOnIdle { order=listOf("clipboard","hide_keyboard","more_menu") }
        val moved=compose.onNodeWithContentDescription("打开更多").fetchSemanticsNode().boundsInRoot
        assertTrue(moved.left>original.left)
        compose.onNodeWithContentDescription("打开更多").performTouchInput{click()}
        assertEquals(1,more.get());assertEquals(0,clipboard.get())
        compose.onNodeWithContentDescription(ToolbarButton.CLIPBOARD.label).performTouchInput{click()}
        assertEquals(1,clipboard.get())
    }
    @Test fun resizedKeysKeepDisjointSlotsAndCenteredHeight() {
        val left=AtomicInteger();val right=AtomicInteger();var density=1f
        val preset=HomeVisualPreset(keys=mapOf("qwerty:q" to HomeKeyStyle(widthScale=2f,heightScale=.5f)))
        compose.setContent { MaterialTheme {
            density=LocalDensity.current.density
            CompositionLocalProvider(LocalHomePreset provides preset,LocalHomeWidthWeighted provides true) {
                Row(Modifier.size(240.dp,100.dp)) {
                    listOf("q","w").forEach { id -> HomeKeyStyleScope("qwerty:$id") {
                        Box(Modifier.weight(preset.keyWidth("qwerty:$id",1f)).fillMaxHeight().testTag("slot$id")
                            .clickable{if(id=="q")left.incrementAndGet() else right.incrementAndGet()}.homeKeyEffects(false)) {
                            Box(Modifier.fillMaxSize().testTag("face$id"))
                        }
                    } }
                }
            }
        } }
        fun bounds(tag:String)=compose.onNodeWithTag(tag,useUnmergedTree=true).fetchSemanticsNode().boundsInRoot
        val q=bounds("slotq");val w=bounds("slotw");val face=bounds("faceq")
        assertEquals(160f*density,q.width,.1f);assertEquals(80f*density,w.width,.1f)
        assertEquals(q.right,w.left,.1f);assertEquals(50f*density,face.height,.1f)
        assertEquals(q.center.y,face.center.y,.1f)
        compose.onNodeWithTag("slotw").performTouchInput{click()}
        assertEquals(0,left.get());assertEquals(1,right.get())
        compose.onNodeWithTag("slotq").performTouchInput{click()};assertEquals(1,left.get())
    }
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
