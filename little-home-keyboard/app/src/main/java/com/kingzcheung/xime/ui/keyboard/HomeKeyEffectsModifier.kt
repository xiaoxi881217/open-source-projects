package com.kingzcheung.xime.ui.keyboard

import android.animation.ValueAnimator
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.draw.innerShadow
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.kingzcheung.xime.appearance.*
import kotlin.math.cos
import kotlin.math.sin

@Composable
internal fun homeHasCustomShadows(): Boolean = LocalHomeKeyStyle.current?.shadows?.isNotEmpty() == true

/** 只变绘制坐标；原组件的pointerInput、布局和语义命中区域保持原位。 */
@Composable
internal fun Modifier.homeKeyEffects(isPressed: Boolean): Modifier {
    val previewed = homePreviewKeyTarget()
    val style = LocalHomeKeyStyle.current ?: return previewed
    val pressState=LocalHomeKeyPressed.current
    SideEffect { pressState?.value=isPressed }
    val keySize = LocalHomeKeySize.current
    val sized = previewed.onSizeChanged { keySize?.value=it }
    val radius = LocalKeyCornerRadius.current
    val faceShape=homeFaceShape()
    val shape = remember(style.shadowShape, radius,faceShape) {
        when (style.shadowShape) {
            HomeShadowShape.ROUNDED -> faceShape
            HomeShadowShape.RECTANGLE -> RectangleShape
            HomeShadowShape.OVAL -> GenericShape { size, _ -> addOval(Rect(Offset.Zero, size)) }
            HomeShadowShape.CUT -> CutCornerShape(radius)
        }
    }
    var shadows: Modifier = Modifier
    style.shadows.values.filter { it.enabled && !it.inset }.forEach { layer ->
        val radians = Math.toRadians(layer.angleDegrees.toDouble())
        shadows = shadows.dropShadow(shape, Shadow(radius = layer.blurDp.dp, color = Color(layer.color),
            spread = layer.spreadDp.dp, alpha = layer.opacity,
            offset = DpOffset((cos(radians) * layer.distanceDp).toFloat().dp,
                (sin(radians) * layer.distanceDp).toFloat().dp)))
    }
    val animate = LocalHomeAnimationEnabled.current &&
        style.motion != HomePressMotion.NONE && ValueAnimator.areAnimatorsEnabled()
    if (!animate) return sized.homeStickerTarget(isPressed, rememberUpdatedState(0f), false).then(shadows)
    val animation = remember(style.motion, style.motionDurationMs) {
        tween<Float>(durationMillis = style.motionDurationMs, easing = when (style.motion) {
            HomePressMotion.JELLY -> CubicBezierEasing(0.2f, 1.35f, 0.45f, 1f)
            HomePressMotion.BUBBLE -> CubicBezierEasing(0.18f, 1.15f, 0.5f, 1f)
            else -> FastOutSlowInEasing
        })
    }
    val progress = animateFloatAsState(if (isPressed) 1f else 0f, animation, label = "小家按键按压")
    return sized.homeStickerTarget(isPressed, progress, true).then(Modifier.drawWithContent {
        val transform = pressTransform(style.motion, style.motionStrength, progress.value)
        val contentScope = this
        val shiftPx = transform.shiftYDp.dp.toPx()
        val pivot = center
        withTransform({
            translate(0f, shiftPx)
            scale(transform.scaleX, transform.scaleY, pivot)
        }) {
            contentScope.drawContent()
            if (transform.glossAlpha > 0f) drawOval(color = Color.White.copy(alpha = transform.glossAlpha),
                topLeft = Offset(size.width * 0.15f, size.height * 0.1f), size = Size(size.width * 0.7f, size.height * 0.3f))
        }
    }).then(shadows)
}

/** 放在背景之后，避免内阴影被按键底色覆盖。 */
@Composable
internal fun Modifier.homeKeyInnerShadows(): Modifier {
    val style = LocalHomeKeyStyle.current ?: HomeKeyStyle()
    val radius = LocalKeyCornerRadius.current
    val faceShape=homeFaceShape()
    val shape = remember(style.shadowShape, radius,faceShape) {
        when (style.shadowShape) {
            HomeShadowShape.ROUNDED -> faceShape
            HomeShadowShape.RECTANGLE -> RectangleShape
            HomeShadowShape.OVAL -> GenericShape { size, _ -> addOval(Rect(Offset.Zero, size)) }
            HomeShadowShape.CUT -> CutCornerShape(radius)
        }
    }
    var result: Modifier = this.homeClippedSticker(HomeStickerLayer.BELOW_SHADOW)
    style.shadows.values.filter { it.enabled && it.inset }.forEach { layer ->
        val radians = Math.toRadians(layer.angleDegrees.toDouble())
        result = result.innerShadow(shape, Shadow(radius = layer.blurDp.dp, color = Color(layer.color),
            spread = layer.spreadDp.dp, alpha = layer.opacity,
            offset = DpOffset((cos(radians) * layer.distanceDp).toFloat().dp,
                (sin(radians) * layer.distanceDp).toFloat().dp)))
    }
    return result.homeClippedSticker(HomeStickerLayer.ABOVE_SHADOW).homeKeyBorder()
}
