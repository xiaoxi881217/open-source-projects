package com.kingzcheung.xime.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import com.kingzcheung.xime.appearance.*

@Composable
internal fun HomeChromeEditor(preset:HomeVisualPreset,dark:Boolean,chrome:String,onChrome:(String)->Unit,
    selected:String?,protectedDrafts:Collection<HomeVisualPreset>,onChange:(HomeVisualPreset)->Unit,items:List<com.kingzcheung.xime.keyboard.ToolbarButtonItem> = emptyList()){
    val id=selected?.takeIf{it.startsWith("toolbar:") || it=="chrome:$chrome"}?:"chrome:$chrome"
    val style=preset.keys[id]?:HomeKeyStyle();val palette=if(dark)preset.dark else preset.light
    var stickerIndex by remember(id){mutableIntStateOf(0)}
    var color by remember{mutableStateOf(false)}
    fun set(value:HomeKeyStyle){onChange(preset.copy(keys=preset.keys+(id to value)))}
    Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){
        HomeChoiceChip(chrome=="toolbar",{onChrome("toolbar")},label={Text("工具栏")})
        HomeChoiceChip(chrome=="candidate",{onChrome("candidate")},label={Text("候选栏")})
    }
    if(id.startsWith("toolbar:"))HomeActionButton(onClick={onChrome("toolbar")}){Text("改整条工具栏")}
    HomePaperGroup(if(id.startsWith("toolbar:"))"这颗快捷按钮" else if(chrome=="candidate")"候选栏底色与文字" else "工具栏底色与文字") {
        HomeValueSlider("底色不透明度",style.keyOpacity,0f..1f,1f){set(style.copy(keyOpacity=it))}
        HomeValueSlider("文字 / 图标不透明度",style.textOpacity,0f..1f,1f){set(style.copy(textOpacity=it))}
        HomeValueSlider("磨砂",style.frost,0f..1f,0f){set(style.copy(frost=it))}
        HomeActionButton(onClick={color=true}){Text("背景颜色")}
        if(id=="chrome:candidate")HomeValueSlider("候选字号",preset.candidateTextSize.toFloat(),14f..28f,19f){onChange(preset.copy(candidateTextSize=it.toInt()))}
    }
    if(id.startsWith("toolbar:"))HomeToolbarVisualEditor(style,::set)
    else {
        HomePaperGroup("独立壁纸") {
            HomeWallpaperEditor(preset.copy(wallpaper=style.wallpaper),dark,true,{set(style.copy(wallpaper=it.wallpaper))},title=if(chrome=="candidate")"候选栏壁纸" else "工具栏壁纸")
        }
        if(id=="chrome:toolbar")HomeToolbarOrderEditor(preset,items,onChange)
    }
    HomeRegionShadowEditor(style,::set)
    HomePaperGroup("边框") {
        HomeBorderEditor(style.border?:if(id.startsWith("toolbar:"))preset.keyBorder else preset.frameBorder,dark,palette.accent,{set(style.copy(border=it))})
        HomeActionButton(onClick={set(style.copy(border=null))}){Text("边框跟随整体")}
    }
    HomeKeyStickersEditor(id,preset,protectedDrafts,style,stickerIndex,{stickerIndex=it},::set,canSync=false)
    HomeActionButton(onClick={onChange(preset.copy(keys=preset.keys-id))}){Text("这里恢复默认")}
    if(color)HomeColorDialog((if(dark)style.backgroundDark else style.backgroundLight)?:palette.background,{color=false}){
        set(if(dark)style.copy(backgroundDark=it) else style.copy(backgroundLight=it))
    }
}
