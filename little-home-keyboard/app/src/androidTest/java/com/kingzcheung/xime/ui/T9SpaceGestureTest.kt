package com.kingzcheung.xime.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.kingzcheung.xime.ui.keyboard.detectT9SpaceGestures
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** 合成空格键，不启动麦克风，不在本人使用的输入法进程运行。 */
class T9SpaceGestureTest {
    @get:Rule val compose = createComposeRule()
    private var taps = 0
    private var starts = 0
    private var stops = 0
    private var swipes = 0
    private var callbackVersion by mutableIntStateOf(0)

    private fun show(enabled: Boolean = true, sticky: Boolean = false, swipe: Boolean = true) {
        compose.setContent {
            val version = callbackVersion
            val currentSwipe by rememberUpdatedState<(() -> Unit)?> (
                if (swipe) ({ swipes += version + 1 }) else null
            )
            Box(Modifier.size(160.dp).testTag("space")
                .pointerInput(enabled, sticky, swipe) {
                    detectT9SpaceGestures(sticky, enabled, {}, { taps++ }, { starts++ }, { stops++ },
                        if (swipe) ({ currentSwipe?.invoke() }) else null)
                })
        }
    }

    @Test fun tapDoesNotStartVoice() {
        show()
        compose.onNodeWithTag("space").performTouchInput { click() }
        compose.runOnIdle { assertEquals(1, taps); assertEquals(0, starts); assertEquals(0, swipes) }
    }

    @Test fun holdStartsVoiceOnceAndReleaseDoesNotTypeSpace() {
        show()
        compose.onNodeWithTag("space").performTouchInput { down(center) }
        compose.mainClock.advanceTimeBy(700)
        compose.runOnIdle { assertEquals(1, starts) }
        compose.onNodeWithTag("space").performTouchInput { up() }
        compose.runOnIdle { assertEquals(0, taps); assertEquals(0, swipes); assertEquals(0, stops) }
    }

    @Test fun changingSwipeCallbackDuringHoldDoesNotRestartLongPress() {
        show()
        compose.onNodeWithTag("space").performTouchInput { down(center) }
        compose.mainClock.advanceTimeBy(250)
        compose.runOnIdle { callbackVersion++ }
        compose.mainClock.advanceTimeBy(350)
        compose.runOnIdle { assertEquals(1, starts) }
        compose.onNodeWithTag("space").performTouchInput { up() }
        compose.runOnIdle { assertEquals(0, taps) }
    }

    @Test fun swipeUpEmitsOnlyItsConfiguredAction() {
        show()
        compose.onNodeWithTag("space").performTouchInput {
            swipe(Offset(center.x, height - 2f), Offset(center.x, 2f), 180)
        }
        compose.runOnIdle { assertEquals(1, swipes); assertEquals(0, starts); assertEquals(0, taps) }
    }

    @Test fun disabledVoiceHoldAndCancelledTouchDoNotCommit() {
        show(enabled = false)
        compose.onNodeWithTag("space").performTouchInput { down(center) }
        compose.mainClock.advanceTimeBy(700)
        compose.onNodeWithTag("space").performTouchInput { up() }
        compose.onNodeWithTag("space").performTouchInput { down(center); cancel() }
        compose.mainClock.advanceTimeBy(700)
        compose.runOnIdle { assertEquals(0, starts); assertEquals(0, taps) }
    }

    @Test fun stickyVoiceTapStopsWithoutTypingOrStarting() {
        show(sticky = true)
        compose.onNodeWithTag("space").performTouchInput { click() }
        compose.runOnIdle { assertEquals(1, stops); assertEquals(0, starts); assertEquals(0, taps) }
    }

    @Test fun holdWorksWithoutSwipeBinding() {
        show(swipe = false)
        compose.onNodeWithTag("space").performTouchInput { down(center) }
        compose.mainClock.advanceTimeBy(700)
        compose.onNodeWithTag("space").performTouchInput { up() }
        compose.runOnIdle { assertEquals(1, starts); assertEquals(0, taps) }
    }
}
