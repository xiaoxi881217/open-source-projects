package com.kingzcheung.xime.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.animation.AnimatedVisibility
import com.kingzcheung.xime.appearance.*
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun HomeKeyEffectsEditor(style: HomeKeyStyle, animationsEnabled: Boolean,
    onStyleChange: (HomeKeyStyle) -> Unit, onAnimationsChanged: (Boolean) -> Unit,
    onSync: (HomeStyleProperty, HomeShadowSide?) -> Unit,section:String="all",
    pressPopupEnabled:Boolean=false,onPressPopupChanged:(Boolean)->Unit={}) {
    var side by remember { mutableStateOf(HomeShadowSide.BOTTOM) }
    var colorDialog by remember { mutableStateOf(false) }
    var shadowExpanded by remember(section) { mutableStateOf(section=="shadow") }
    var motionExpanded by remember(section) { mutableStateOf(section=="motion") }
    val shadow = style.shadows[side] ?: HomeSideShadow.forSide(side)
    fun setShadow(next: HomeSideShadow) { onStyleChange(style.copy(shadows = style.shadows + (side to next))) }
    if(section!="motion"){
    TextButton(onClick = { shadowExpanded = !shadowExpanded }) {
        Text("四边阴影 · ${style.shadows.values.count { it.enabled }}边已开 · ${if (shadowExpanded) "收起" else "展开"}")
    }
    AnimatedVisibility(shadowExpanded) { Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
    Row {
        TextButton(onClick={onSync(HomeStyleProperty.SHADOW_ALL,null)}){Text("同步四边整套阴影")}
        TextButton(onClick={onSync(HomeStyleProperty.SHADOW_SIDE,side)}){Text("同步这一边整套")}
    }
    FlowRow {
        HomeShadowSide.entries.forEach { edge -> FilterChip(side == edge, { side = edge }, label = { Text(edge.label) }) }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("开启${side.label}阴影", Modifier.weight(1f))
        Switch(shadow.enabled, { setShadow(shadow.copy(enabled = it)) })
        TextButton(onClick = { onSync(HomeStyleProperty.SHADOW_ENABLED, side) }) { Text("同步") }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("阴影向内凹", Modifier.weight(1f))
        Switch(shadow.inset, { setShadow(shadow.copy(inset = it, enabled = true)) })
        TextButton(onClick = { onSync(HomeStyleProperty.SHADOW_INSET, side) }) { Text("同步内外侧") }
    }
    TextButton(onClick = {
        onStyleChange(style.copy(shadows = HomeShadowSide.entries.associateWith { edge ->
            (style.shadows[edge] ?: HomeSideShadow.forSide(edge)).copy(enabled = true)
        }))
    }) { Text("四边都加上阴影") }
    FlowRow {
        HomeShadowShape.entries.forEach { shape -> FilterChip(style.shadowShape == shape,
            { onStyleChange(style.copy(shadowShape = shape)) }, label = { Text(shape.label) }) }
    }
    TextButton(onClick = { onSync(HomeStyleProperty.SHADOW_SHAPE, null) }) { Text("只同步阴影形状") }
    Row {
        OutlinedButton(onClick = { colorDialog = true }, modifier = Modifier.weight(1f)) { Text("${side.label}阴影颜色") }
        TextButton(onClick = { onSync(HomeStyleProperty.SHADOW_COLOR, side) }) { Text("同步这一项") }
    }
    @Composable
    fun value(label: String, current: Float, range: ClosedFloatingPointRange<Float>, initial: Float,
        property: HomeStyleProperty, change: (Float) -> HomeSideShadow) {
        HomeValueSlider("${side.label} · $label", current, range, initial) { setShadow(change(it).copy(enabled = true)) }
        TextButton(onClick = { onSync(property, side) }) { Text("只同步这个数值") }
    }
    value("深浅", shadow.opacity, 0f..1f, 0.18f, HomeStyleProperty.SHADOW_OPACITY) { shadow.copy(opacity = it) }
    value("模糊", shadow.blurDp, 0f..12f, 6f, HomeStyleProperty.SHADOW_BLUR) { shadow.copy(blurDp = it) }
    value("扩散", shadow.spreadDp, -4f..6f, 0f, HomeStyleProperty.SHADOW_SPREAD) { shadow.copy(spreadDp = it) }
    value("距离", shadow.distanceDp, 0f..12f, 3f, HomeStyleProperty.SHADOW_DISTANCE) { shadow.copy(distanceDp = it) }
    value("角度", shadow.angleDegrees, 0f..360f, side.defaultAngle, HomeStyleProperty.SHADOW_ANGLE) { shadow.copy(angleDegrees = it) }
    Text("四边是独立的光影层，模糊会自然交叠。数值同步不会替目标自动打开阴影。", style = MaterialTheme.typography.bodySmall)
    } }
    }
    if(section!="shadow"){
    HorizontalDivider()
    Row(verticalAlignment=Alignment.CenterVertically) {
        Text("按键放大提示",Modifier.weight(1f))
        Switch(pressPopupEnabled,onPressPopupChanged)
    }
    Text("可选开启；关闭后不遮住键帽动态，长按菜单和滑动提示照常。",style=MaterialTheme.typography.bodySmall)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("按压动态总开关", Modifier.weight(1f))
        Switch(animationsEnabled, onAnimationsChanged)
    }
    TextButton(onClick = { motionExpanded = !motionExpanded }) {
        Text("按压动态 · ${style.motion.label} · ${if (motionExpanded) "收起" else "展开"}")
    }
    AnimatedVisibility(motionExpanded) { Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
    FlowRow {
        HomePressMotion.entries.forEach { motion -> FilterChip(style.motion == motion,
            { onStyleChange(style.copy(motion = motion)) }, label = { Text(motion.label) }) }
    }
    TextButton(onClick = { onSync(HomeStyleProperty.MOTION, null) }) { Text("只同步动态类型") }
    HomeValueSlider("动态强度", style.motionStrength, 0f..4f, 0.65f) { onStyleChange(style.copy(motionStrength = it)) }
    TextButton(onClick = { onSync(HomeStyleProperty.MOTION_STRENGTH, null) }) { Text("只同步强度数值") }
    HomeValueSlider("动态时长（毫秒）", style.motionDurationMs.toFloat(), 40f..600f, 120f) {
        onStyleChange(style.copy(motionDurationMs = it.roundToInt()))
    }
    TextButton(onClick = { onSync(HomeStyleProperty.MOTION_DURATION, null) }) { Text("只同步时长数值") }
    Text("先打开总开关，再为这颗键选动态。系统关闭动画时也会停下，打字动作不会等待动画。", style = MaterialTheme.typography.bodySmall)
    } }
    }
    if (colorDialog) HomeColorDialog(shadow.color, { colorDialog = false }) {
        setShadow(shadow.copy(color = it, enabled = true)); colorDialog = false
    }
}
