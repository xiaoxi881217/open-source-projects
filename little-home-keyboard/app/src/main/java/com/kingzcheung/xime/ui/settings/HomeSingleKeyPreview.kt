package com.kingzcheung.xime.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kingzcheung.xime.appearance.*
import com.kingzcheung.xime.ui.keyboard.*
import com.kingzcheung.xime.settings.KeysConfigHelper
import com.kingzcheung.xime.settings.DisplayMode

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun HomeSingleKeyPreview(preset: HomeVisualPreset,id: String,label: String,dark: Boolean,
    onStickerChange: ((HomeSticker)->Unit)?=null,onLabelChange: ((HomeKeyStyle)->Unit)?=null,
    onHintChange: ((HomeKeyStyle)->Unit)?=null,initialMode:String="press",glyph:(@Composable ()->Unit)?=null) {
    val palette=if(dark)preset.dark else preset.light;val special=!id.startsWith("toolbar:")&&id.substringAfter(':').length>1
    val previewWidth=if(special)100.dp else 72.dp;val previewHeight=if(special)58.dp else 72.dp
    val interactions=remember(id){MutableInteractionSource()};val pressed by interactions.collectIsPressedAsState()
    var mode by remember(id,initialMode){mutableStateOf(initialMode)};var gesture by remember(id){mutableStateOf<HomeKeyStyle?>(null)}
    val current=preset.keys[id]?:HomeKeyStyle();val latestStyle by rememberUpdatedState(current)
    val applySticker by rememberUpdatedState(onStickerChange);val applyLabel by rememberUpdatedState(onLabelChange)
    val applyHint by rememberUpdatedState(onHintChange)
    val hint=if(id.startsWith("qwerty:") && KeysConfigHelper.getSwipeUpDisplay(id.substringAfter(':'),false)!=DisplayMode.BUBBLE)
        KeysConfigHelper.getSwipeUpLabel(id.substringAfter(':'),false) else null
    val display=gesture?.let { preset.copy(keys=preset.keys+(id to it)) }?:preset
    val gestures=if(mode!="press")Modifier.pointerInput(id,mode) {
        awaitEachGesture {
            val down=awaitFirstDown(requireUnconsumed=false);val original=latestStyle
            if(mode=="sticker" && original.sticker==null)return@awaitEachGesture
            down.consume();var draft=original;var completed=false
            try {
                do {
                    val event=awaitPointerEvent();val pan=event.calculatePan()
                    draft=if(mode=="hint")draft.movedHint(pan.x/previewWidth.toPx(),pan.y/previewHeight.toPx()).resizedHint(event.calculateZoom()) else if(mode=="label")draft.movedLabel(pan.x/previewWidth.toPx(),pan.y/previewHeight.toPx()).resizedLabel(event.calculateZoom()) else
                        draft.copy(sticker=draft.sticker!!.transformed(pan.x/previewWidth.toPx(),pan.y/previewHeight.toPx(),event.calculateZoom(),event.calculateRotation()))
                    gesture=draft;event.changes.forEach { it.consume() }
                }while(event.changes.any { it.pressed })
                completed=true
            } finally {
                if(completed && draft!=original){if(mode=="hint")applyHint?.invoke(draft) else if(mode=="label")applyLabel?.invoke(draft) else draft.sticker?.let { applySticker?.invoke(it) }}
                gesture=null
            }
        }
    } else Modifier
    Column {
        FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
            HomeChoiceChip(mode=="press",{mode="press"},label={Text("试按")})
            if(onLabelChange!=null)HomeChoiceChip(mode=="label",{mode="label"},label={Text("移动文字")})
            if(onHintChange!=null)HomeChoiceChip(mode=="hint",{mode="hint"},label={Text("移动小字")})
            if(current.sticker!=null && onStickerChange!=null)HomeChoiceChip(mode=="sticker",{mode="sticker"},label={Text("摆放贴纸")})
        }
        HomeStickerHost(Modifier.fillMaxWidth().height(150.dp).clipToBounds().then(gestures)) { previewModifier ->
            HomeBackdropHost(display,dark,previewModifier) {
            Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center) {
                CompositionLocalProvider(LocalKeyCornerRadius provides preset.cornerRadius.dp) {
                    HomeKeyStyleScope(id,display,dark) {
                        val background=homeKeyBackground(Color(if(special)palette.special else palette.key));val text=homeKeyText(Color(palette.text))
                        Box(Modifier.padding(20.dp).size(previewWidth,previewHeight).homeKeyEffects(pressed)
                            .clip(RoundedCornerShape(LocalKeyCornerRadius.current)).homeKeyFrost().homeKeyFace(background).homeKeyInnerShadows()
                            .then(if(mode=="press")Modifier.clickable(interactionSource=interactions,indication=null){} else Modifier),contentAlignment=Alignment.Center) {
                            if(glyph!=null)glyph() else Text(label,color=text,fontSize=homeKeyFontSize(18.sp),modifier=Modifier.homeKeyLabelPosition())
                            if(!hint.isNullOrEmpty())Text(hint,color=homeKeyHintColor(text.copy(alpha=.6f)),fontSize=homeKeyHintFontSize(9.sp),modifier=Modifier.offset(y=(-14).dp).homeKeyHintPosition())
                        }
                    }
                }
            }
        }
        }
        Text(when(mode){"hint"->if(hint.isNullOrEmpty())"这颗键目前没有上方小字，可换一颗字母键。" else "单指移动小字，双指捏合调字号；松手合并一次修改。";"label"->"单指移动文字，双指捏合调字号；松手合并一次修改。";"sticker"->"单指移动，双指缩放、旋转；松手合并一次修改。";else->"先试按，或切换到移动文字、摆放贴纸。"},style=MaterialTheme.typography.bodySmall)
    }
}
