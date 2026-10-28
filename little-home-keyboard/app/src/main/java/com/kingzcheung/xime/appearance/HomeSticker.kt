package com.kingzcheung.xime.appearance

import kotlinx.serialization.Serializable
import kotlin.math.max
import kotlin.math.min

@Serializable
enum class HomeStickerLayer(val label:String) {
    BELOW_SHADOW("阴影下方"), ABOVE_SHADOW("阴影上方"), ABOVE_KEY("文字上方")
}

@Serializable
data class HomeSticker(val asset: String = "builtin:heart", val offsetX: Float = .3f, val offsetY: Float = -.3f,
    val scale: Float = .65f, val rotation: Float = 0f, val opacity: Float = 1f, val clipToKey: Boolean = false,
    val layer:HomeStickerLayer=HomeStickerLayer.ABOVE_KEY) {
    fun effectiveLayer():HomeStickerLayer = if(clipToKey)layer else HomeStickerLayer.ABOVE_KEY
    fun validate() {
        require(asset in BUILTINS || asset.matches(Regex("image:[a-f0-9]{64}"))) { "贴纸来源不合适" }
        require(offsetX.isFinite() && offsetX in -2f..2f && offsetY.isFinite() && offsetY in -2f..2f &&
            scale.isFinite() && scale in .25f..12f && rotation.isFinite() && rotation in -180f..180f &&
            opacity.isFinite() && opacity in 0f..1f) { "贴纸位置或大小超出安全范围" }
    }
    fun transformed(dx: Float, dy: Float, zoom: Float, turn: Float): HomeSticker {
        if (!listOf(dx,dy,zoom,turn).all { it.isFinite() } || zoom <= 0) return this
        return copy(offsetX=(offsetX+dx).coerceIn(-2f,2f),offsetY=(offsetY+dy).coerceIn(-2f,2f),
            scale=(scale*zoom).coerceIn(.25f,12f),rotation=(((rotation+turn+180f)%360f+360f)%360f)-180f)
    }
    companion object {
        val BUILTINS = linkedMapOf("builtin:heart" to "小爱心", "builtin:star" to "小星星", "builtin:bow" to "蝴蝶结", "builtin:cloud" to "软软云")
    }
}

@Serializable
data class HomeFloatingSticker(val id:String,val sticker:HomeSticker=HomeSticker(offsetX=0f,offsetY=0f,scale=.35f)) {
    fun validate(){require(id.matches(Regex("[a-zA-Z0-9_-]{1,64}")) && !sticker.clipToKey){"自由贴纸编号或模式不合适"};sticker.validate()}
}
