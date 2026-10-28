package com.kingzcheung.xime.appearance

data class HomeSelectionRect(val left: Float, val top: Float, val right: Float, val bottom: Float)

/** 检查整段划线，快速移动也不会漏掉两次触摸采样之间的按键。 */
fun HomeSelectionRect.crosses(x0: Float, y0: Float, x1: Float, y1: Float): Boolean {
    if (!listOf(left, top, right, bottom, x0, y0, x1, y1).all { it.isFinite() } ||
        right <= left || bottom <= top) return false
    var enter = 0f
    var leave = 1f
    fun clip(start: Float, delta: Float, minimum: Float, maximum: Float): Boolean {
        if (delta == 0f) return start in minimum..maximum
        val first = (minimum - start) / delta
        val second = (maximum - start) / delta
        enter = maxOf(enter, minOf(first, second))
        leave = minOf(leave, maxOf(first, second))
        return enter <= leave
    }
    return clip(x0, x1 - x0, left, right) && clip(y0, y1 - y0, top, bottom)
}
