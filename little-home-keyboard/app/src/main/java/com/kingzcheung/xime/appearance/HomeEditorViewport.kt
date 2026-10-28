package com.kingzcheung.xime.appearance

object HomeEditorViewport {
    fun percent(value: Int)=value.coerceIn(10,40)
    fun frameHeight(screenHeight: Float,percentage: Int): Float =
        if(!screenHeight.isFinite() || screenHeight<=0)96f else (screenHeight*percent(percentage)/100f).coerceIn(64f,220f)
    fun scale(width: Float,height: Float,availableWidth: Float,availableHeight: Float): Float {
        if(!listOf(width,height,availableWidth,availableHeight).all { it.isFinite() && it>0 })return 1f
        return minOf(1f,availableWidth/width,availableHeight/height)
    }
}
