package com.kingzcheung.xime.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.kingzcheung.xime.ui.keyboard.KeyButton
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

/** 合成滚动容器；仅编译，实际手势由本人或独立测试设备验收。 */
class SymbolScrollGestureTest {
    @get:Rule val compose=createComposeRule()
    @Test fun verticalBrowsingScrollsWithoutCommittingASymbol() {
        val scroll=ScrollState(0);val clicks=AtomicInteger()
        compose.setContent { MaterialTheme {
            Column(Modifier.width(240.dp).height(180.dp).testTag("symbols").verticalScroll(scroll)) {
                repeat(30){ KeyButton("符号$it",{clicks.incrementAndGet()},Color.White,Color.Black,
                    Modifier.fillMaxWidth().height(48.dp),scrollableContainer=true) }
            }
        } }
        compose.onNodeWithTag("symbols").performTouchInput { swipeUp() }
        compose.runOnIdle { assertTrue(scroll.value>0);assertEquals(0,clicks.get()) }
    }
    @Test fun aDeliberateTapCommitsExactlyOnce() {
        val clicks=AtomicInteger()
        compose.setContent { MaterialTheme {
            Box(Modifier.size(100.dp)){ KeyButton("…",{clicks.incrementAndGet()},Color.White,Color.Black,
                Modifier.fillMaxSize(),scrollableContainer=true) }
        } }
        compose.onNodeWithText("…").performTouchInput { click() }
        compose.runOnIdle { assertEquals(1,clicks.get()) }
    }
}
