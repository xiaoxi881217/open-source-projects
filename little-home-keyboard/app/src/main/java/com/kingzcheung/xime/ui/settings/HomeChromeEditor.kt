package com.kingzcheung.xime.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import com.kingzcheung.xime.appearance.*

@Composable
internal fun HomeChromeEditor(preset:HomeVisualPreset,dark:Boolean,chrome:String,onChrome:(String)->Unit,
    selected:String?,protectedDrafts:Collection<HomeVisualPreset>,onChange:(HomeVisualPreset)->Unit){
    val id=selected?.takeIf{it.startsWith("toolbar:") || it=="chrome:$chrome"}?:"chrome:$chrome"
    val style=preset.keys[id]?:HomeKeyStyle();val palette=if(dark)preset.dark else preset.light
    var color by remember{mutableStateOf(false)}
    fun set(value:HomeKeyStyle){onChange(preset.copy(keys=preset.keys+(id to value)))}
    Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){
        FilterChip(chrome=="toolbar",{onChrome("toolbar")},label={Text("工具栏")})
        FilterChip(chrome=="candidate",{onChrome("candidate")},label={Text("候选栏")})
    }
    if(id.startsWith("toolbar:"))TextButton(onClick={onChrome("toolbar")}){Text("改整条工具栏")}
    HomeValueSlider("透明度",style.keyOpacity,0f..1f,1f){set(style.copy(keyOpacity=it))}
    HomeValueSlider("磨砂",style.frost,0f..1f,0f){set(style.copy(frost=it))}
    TextButton(onClick={color=true}){Text("背景颜色")}
    HomePaperGroup("边框") {
        HomeBorderEditor(style.border?:if(id.startsWith("toolbar:"))preset.keyBorder else preset.frameBorder,dark,palette.accent,{set(style.copy(border=it))})
        TextButton(onClick={set(style.copy(border=null))}){Text("边框跟随整体")}
    }
    HomeStickerEditor(id,preset,protectedDrafts,{set(style.copy(sticker=it))},{},canSync=false)
    TextButton(onClick={onChange(preset.copy(keys=preset.keys-id))}){Text("这里恢复默认")}
    if(color)HomeColorDialog((if(dark)style.backgroundDark else style.backgroundLight)?:palette.background,{color=false}){
        set(if(dark)style.copy(backgroundDark=it) else style.copy(backgroundLight=it))
    }
}
