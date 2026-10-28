package com.kingzcheung.xime.appearance

import kotlinx.serialization.Serializable

@Serializable
enum class HomeShadowSide(val label: String, val defaultAngle: Float) {
    TOP("上边", 270f), RIGHT("右边", 0f), BOTTOM("下边", 90f), LEFT("左边", 180f)
}

@Serializable
enum class HomeShadowShape(val label: String) { ROUNDED("圆角"), RECTANGLE("方形"), OVAL("椭圆"), CUT("切角") }

@Serializable
data class HomeSideShadow(
    val enabled: Boolean = false,
    val inset: Boolean = false,
    val color: Long = 0xFF55404B,
    val opacity: Float = 0.18f,
    val blurDp: Float = 6f,
    val spreadDp: Float = 0f,
    val distanceDp: Float = 3f,
    val angleDegrees: Float = 90f,
) {
    fun validate() {
        require(color in 0xFF000000..0xFFFFFFFF) { "阴影颜色无效" }
        require(opacity.isFinite() && opacity in 0f..1f && blurDp.isFinite() && blurDp in 0f..12f &&
            spreadDp.isFinite() && spreadDp in -4f..6f && distanceDp.isFinite() && distanceDp in 0f..12f &&
            angleDegrees.isFinite() && angleDegrees in 0f..360f) { "阴影参数超出安全范围" }
    }

    companion object {
        fun forSide(side: HomeShadowSide) = HomeSideShadow(angleDegrees = side.defaultAngle)
    }
}

@Serializable
enum class HomePressMotion(val label: String) { NONE("关闭"), JELLY("果冻"), TYPEWRITER("打字"), BUBBLE("气泡") }

data class HomePressTransform(val scaleX: Float = 1f, val scaleY: Float = 1f, val shiftYDp: Float = 0f,
    val glossAlpha: Float = 0f)

fun pressTransform(motion: HomePressMotion, strength: Float, progress: Float): HomePressTransform {
    val p = if (progress.isFinite()) progress.coerceIn(-0.2f, 1.2f) else 0f
    val amount = if (strength.isFinite()) strength.coerceIn(0f, 4f) * p else 0f
    return when (motion) {
        HomePressMotion.NONE -> HomePressTransform()
        HomePressMotion.JELLY -> HomePressTransform(1f + 0.045f * amount, 1f - 0.1f * amount)
        HomePressMotion.TYPEWRITER -> HomePressTransform(1f - 0.025f * amount, 1f - 0.025f * amount, 2.5f * amount)
        HomePressMotion.BUBBLE -> HomePressTransform(1f - 0.075f * amount, 1f - 0.075f * amount,
            glossAlpha = (amount * 0.18f).coerceIn(0f, 0.22f))
    }
}
