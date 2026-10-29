package com.kingzcheung.xime.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.kingzcheung.xime.appearance.*

@OptIn(ExperimentalLayoutApi::class)
@Composable internal fun HomeRegionShadowEditor(style:HomeKeyStyle,onChange:(HomeKeyStyle)->Unit) {
    var side by remember{mutableStateOf(HomeShadowSide.BOTTOM)};var color by remember{mutableStateOf(false)}
    var expanded by remember{mutableStateOf(false)}
    val shadow=style.shadows[side]?:HomeSideShadow.forSide(side)
    fun set(value:HomeSideShadow){onChange(style.copy(shadows=style.shadows+(side to value)))}
    HomePaperGroup("四边阴影") {
        HomeActionButton(onClick={expanded=!expanded}){Text(if(expanded)"收起阴影调整" else "编辑阴影 · ${style.shadows.values.count{it.enabled}}边已开")}
        if(expanded) {
        FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp)) {HomeShadowSide.entries.forEach{edge->HomeChoiceChip(side==edge,{side=edge},label={Text(edge.label)})}}
        Row(verticalAlignment=Alignment.CenterVertically){Text("开启${side.label}",Modifier.weight(1f));Switch(shadow.enabled,{set(shadow.copy(enabled=it))})}
        Row(verticalAlignment=Alignment.CenterVertically){Text("向内凹",Modifier.weight(1f));Switch(shadow.inset,{set(shadow.copy(inset=it,enabled=true))})}
        HomeActionButton(onClick={color=true}){Text("${side.label}阴影颜色")}
        HomeValueSlider("深浅",shadow.opacity,0f..1f,.18f){set(shadow.copy(opacity=it,enabled=true))}
        HomeValueSlider("模糊",shadow.blurDp,0f..12f,6f){set(shadow.copy(blurDp=it,enabled=true))}
        HomeValueSlider("扩散",shadow.spreadDp,-4f..6f,0f){set(shadow.copy(spreadDp=it,enabled=true))}
        HomeValueSlider("距离",shadow.distanceDp,0f..12f,3f){set(shadow.copy(distanceDp=it,enabled=true))}
        HomeValueSlider("角度",shadow.angleDegrees,0f..360f,side.defaultAngle){set(shadow.copy(angleDegrees=it,enabled=true))}
        HomeActionButton(onClick={onChange(style.copy(shadows=emptyMap()))}){Text("这里的阴影恢复默认")}
        }
    }
    if(color)HomeColorDialog(shadow.color,{color=false}){set(shadow.copy(color=it,enabled=true));color=false}
}
