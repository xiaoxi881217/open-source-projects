package com.kingzcheung.xime.ui.keyboard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.kingzcheung.xime.appearance.HomeKeyStyle
import com.kingzcheung.xime.appearance.HomePresetStore
import com.kingzcheung.xime.appearance.HomeVisualPreset

internal val LocalHomePreset=staticCompositionLocalOf<HomeVisualPreset?>{null}
val LocalHomeDarkAppearance = staticCompositionLocalOf { false }
internal val LocalHomeAnimationEnabled = staticCompositionLocalOf { false }
internal val LocalHomeKeyStyle = staticCompositionLocalOf<HomeKeyStyle?> { null }
internal val LocalHomeKeySize = staticCompositionLocalOf<MutableState<IntSize>?> { null }
internal val LocalHomeKeyPressed = staticCompositionLocalOf<MutableState<Boolean>?> { null }

/** 只提供外观值，不建立新布局或触摸监听。 */
@Composable
internal fun HomeKeyStyleScope(id: String, preset: HomeVisualPreset? = LocalHomePreset.current ?: HomePresetStore.active,
    dark: Boolean? = null, content: @Composable () -> Unit) {
    val stableId = remember(id) { com.kingzcheung.xime.appearance.HomeKeyIds.normalize(id) }
    val style = preset?.styleFor(stableId)
    val size = remember(id) { mutableStateOf(IntSize.Zero) }
    val pressed = remember(id) { mutableStateOf(false) }
    val kind=style?.shape?:if(stableId.startsWith("chrome:"))com.kingzcheung.xime.appearance.HomeKeyShape.ROUNDED else preset?.keyShape?:com.kingzcheung.xime.appearance.HomeKeyShape.ROUNDED
    val border=style?.border?:if(stableId.startsWith("chrome:"))preset?.frameBorder else preset?.keyBorder
    CompositionLocalProvider(LocalHomeKeyId provides stableId, LocalHomeKeyStyle provides style,
        LocalHomeKeySize provides size, LocalHomeKeyPressed provides pressed,
        LocalHomeKeyShape provides kind,LocalHomeKeyBorder provides (border?:com.kingzcheung.xime.appearance.HomeBorder()),
        LocalHomeAnimationEnabled provides (preset?.animationsEnabled == true),
        LocalHomeDarkAppearance provides (dark ?: LocalHomeDarkAppearance.current),
        LocalKeyCornerRadius provides (style?.cornerRadius?.dp ?: LocalKeyCornerRadius.current), content = content)
}

@Composable
internal fun homeKeyBackground(fallback: Color): Color {
    val style = LocalHomeKeyStyle.current
    val color=(if (LocalHomeDarkAppearance.current) style?.backgroundDark else style?.backgroundLight)?.let { Color(it) } ?: fallback
    return color.copy(alpha=color.alpha*(style?.keyOpacity?:1f))
}

@Composable
internal fun homeKeyText(fallback: Color): Color {
    val style = LocalHomeKeyStyle.current
    val color=(if (LocalHomeDarkAppearance.current) style?.textDark else style?.textLight)?.let { Color(it) } ?: fallback
    return color.copy(alpha=color.alpha*(style?.textOpacity?:1f))
}

@Composable
internal fun homeKeyFontSize(fallback: TextUnit): TextUnit = LocalHomeKeyStyle.current?.labelFontSize?.sp ?: fallback

@Composable
internal fun homeCandidateTextSize():Int = LocalHomePreset.current?.candidateTextSize
    ?:com.kingzcheung.xime.settings.SettingsPreferences.getCandidateTextSize(LocalContext.current)

@Composable internal fun homeSectionSpacing(fallback:Float):Float=LocalHomePreset.current?.sectionSpacing?:fallback

@Composable
internal fun homeKeyHintFontSize(fallback: TextUnit): TextUnit = LocalHomeKeyStyle.current?.hintFontSize?.sp ?: fallback

@Composable
internal fun homeKeyHintColor(fallback: Color): Color {
    val style=LocalHomeKeyStyle.current ?: return fallback
    val color=(if(LocalHomeDarkAppearance.current)style.hintDark else style.hintLight)?.let{Color(it)}?:fallback
    return color.copy(alpha=if(style.hintVisible)color.alpha*style.hintOpacity else 0f)
}

/** 小字只改变绘制位置，保留原来的滑动动作和按键触摸区域。 */
@Composable
internal fun Modifier.homeKeyHintPosition(): Modifier {
    val style=LocalHomeKeyStyle.current ?: return this
    val keySize=LocalHomeKeySize.current ?: return this
    return drawWithContent {
        val content=this
        withTransform({translate(keySize.value.width*style.hintOffsetX,keySize.value.height*style.hintOffsetY)}){content.drawContent()}
    }
}

/** 只移动主标签的画面，按键动作、触摸区域和提示角标不动。 */
@Composable
internal fun Modifier.homeKeyLabelPosition(): Modifier {
    val style=LocalHomeKeyStyle.current ?: return this
    val keySize=LocalHomeKeySize.current ?: return this
    return drawWithContent {
        val content=this
        withTransform({translate(keySize.value.width*style.labelOffsetX,keySize.value.height*style.labelOffsetY)}){content.drawContent()}
    }
}
