package com.kingzcheung.xime.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.kingzcheung.xime.appearance.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun HomeHintEditor(style:HomeKeyStyle,palette:HomePalette,dark:Boolean,onChange:(HomeKeyStyle)->Unit,
    onGesture:()->Unit,onSync:(HomeStyleProperty)->Unit) {
    var expanded by remember { mutableStateOf(false) }
    var precise by remember { mutableStateOf(false) }
    var color by remember { mutableStateOf(false) }
    val colorProperty=if(dark)HomeStyleProperty.HINT_DARK else HomeStyleProperty.HINT_LIGHT
    Surface(shape=RoundedCornerShape(16.dp),color=MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha=.7f),
        border=BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant)) {
        Column {
            Row(Modifier.fillMaxWidth().clickable{expanded=!expanded}.padding(horizontal=14.dp,vertical=12.dp),
                verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("上方小数字 · 符号",style=MaterialTheme.typography.titleSmall,color=MaterialTheme.colorScheme.primary)
                    Text(if(expanded)"颜色、大小与摆放" else "点开调整 · ${if(style.hintVisible)"显示中" else "已隐藏"}",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(if(expanded)Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,if(expanded)"收起小字设置" else "展开小字设置",tint=MaterialTheme.colorScheme.primary)
            }
            AnimatedVisibility(expanded) {
                Column(Modifier.padding(start=12.dp,end=12.dp,bottom=12.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                    HomePaperRule()
                    Row(verticalAlignment=Alignment.CenterVertically) {
                        Text("显示小字",Modifier.weight(1f),style=MaterialTheme.typography.bodyMedium)
                        Switch(style.hintVisible,{onChange(style.copy(hintVisible=it))})
                        HomeHintSync("同步显示开关"){onSync(HomeStyleProperty.HINT_VISIBLE)}
                    }
                    HomePaperSwatch("小字颜色",(if(dark)style.hintDark else style.hintLight)?:palette.text,Modifier.fillMaxWidth()){color=true}
                    Row {
                        TextButton(onClick={onSync(colorProperty)}){Text("同步颜色")}
                        TextButton(onClick={onChange(if(dark)style.copy(hintDark=null) else style.copy(hintLight=null))}){Text("跟随原色")}
                    }
                    Row(verticalAlignment=Alignment.CenterVertically) {
                        Box(Modifier.weight(1f)){HomeValueSlider("字号",style.hintFontSize?:9f,2f..24f,9f){onChange(style.copy(hintFontSize=it))}}
                        HomeHintSync("同步小字字号"){onSync(HomeStyleProperty.HINT_FONT_SIZE)}
                    }
                    Row(verticalAlignment=Alignment.CenterVertically) {
                        Box(Modifier.weight(1f)){HomeValueSlider("不透明度",style.hintOpacity,0f..1f,1f){onChange(style.copy(hintOpacity=it))}}
                        HomeHintSync("同步小字不透明度"){onSync(HomeStyleProperty.HINT_OPACITY)}
                    }
                    HomePaperRule()
                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick=onGesture,modifier=Modifier.weight(1f),shape=RoundedCornerShape(12.dp)){Text("手势摆放")}
                        OutlinedButton(onClick={precise=!precise},modifier=Modifier.weight(1f),shape=RoundedCornerShape(12.dp)){Text(if(precise)"收起坐标" else "精确位置")}
                    }
                    if(precise) {
                        HomeValueSlider("横向",style.hintOffsetX,-.8f.. .8f,0f){onChange(style.copy(hintOffsetX=it))}
                        TextButton(onClick={onSync(HomeStyleProperty.HINT_X)}){Text("只同步横向")}
                        HomeValueSlider("纵向",style.hintOffsetY,-.8f.. .8f,0f){onChange(style.copy(hintOffsetY=it))}
                        TextButton(onClick={onSync(HomeStyleProperty.HINT_Y)}){Text("只同步纵向")}
                    }
                    FlowRow {
                        TextButton(onClick={onSync(HomeStyleProperty.HINT_POSITION)}){Text("同步位置")}
                        TextButton(onClick={onChange(style.copy(hintOffsetX=0f,hintOffsetY=0f))}){Text("回到原位")}
                        TextButton(onClick={onChange(style.copy(hintFontSize=null))}){Text("字号跟随原设置")}
                    }
                    FilledTonalButton(onClick={onSync(HomeStyleProperty.HINT_ALL)},modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(12.dp)){Text("同步小字整套样式")}
                    TextButton(onClick={onChange(style.copy(hintOffsetX=0f,hintOffsetY=0f,hintFontSize=null,hintLight=null,hintDark=null,hintOpacity=1f,hintVisible=true))}){Text("小字恢复原设置")}
                }
            }
        }
    }
    if(color)HomeColorDialog((if(dark)style.hintDark else style.hintLight)?:palette.text,{color=false}) {
        onChange(if(dark)style.copy(hintDark=it) else style.copy(hintLight=it));color=false
    }
}

@Composable
private fun HomeHintSync(description:String,onClick:()->Unit) {
    IconButton(onClick=onClick){Icon(Icons.Default.Sync,description,Modifier.size(18.dp),tint=MaterialTheme.colorScheme.primary)}
}
