package com.kingzcheung.xime.ui.keyboard

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.unit.dp
import kotlin.math.abs

private enum class T9SpaceAction { TAP, SWIPE_UP, CANCEL }

/** 一次触摸只由一个检测器仲裁，避免上滑先消费微小移动而取消语音长按。 */
internal suspend fun PointerInputScope.detectT9SpaceGestures(
    voiceSticky: Boolean,
    isSttEnabled: Boolean,
    onPress: () -> Unit,
    onTap: () -> Unit,
    onStartVoice: () -> Unit,
    onStopVoice: () -> Unit,
    onSwipeUp: (() -> Unit)?,
) {
    awaitEachGesture {
        val down = awaitFirstDown()
        if (voiceSticky) {
            if (waitForUpOrCancellation() != null) onStopVoice()
            return@awaitEachGesture
        }
        onPress()
        val swipeThreshold = 50.dp.toPx()
        var moved = false
        val action = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
            var result = T9SpaceAction.CANCEL
            while (true) {
                val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                if (change.isConsumed) break
                val dx = change.position.x - down.position.x
                val dy = change.position.y - down.position.y
                moved = moved || abs(dx) > viewConfiguration.touchSlop || abs(dy) > viewConfiguration.touchSlop
                if (!change.pressed) {
                    if (!moved) {
                        change.consume()
                        result = T9SpaceAction.TAP
                    }
                    break
                }
                if (onSwipeUp != null && dy < -swipeThreshold && abs(dy) > abs(dx)) {
                    change.consume()
                    result = T9SpaceAction.SWIPE_UP
                    break
                }
            }
            result
        }
        when (action) {
            T9SpaceAction.TAP -> onTap()
            T9SpaceAction.SWIPE_UP -> onSwipeUp?.invoke()
            null -> if (!moved && isSttEnabled) onStartVoice()
            T9SpaceAction.CANCEL -> Unit
        }
    }
}
