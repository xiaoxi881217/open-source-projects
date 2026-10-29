package com.kingzcheung.xime.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kingzcheung.xime.appearance.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun HomeBorderEditor(value:HomeBorder,dark:Boolean,fallback:Long,onChange:(HomeBorder)->Unit,onSync:((HomeStyleProperty)->Unit)?=null) {
    var color by remember{mutableStateOf(false)}
    FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
        HomeBorderKind.entries.forEach{kind->HomeChoiceChip(value.kind==kind,{onChange(value.copy(kind=kind))},label={Text(kind.label)})}
    }
    if(value.kind!=HomeBorderKind.NONE) {
        Row(verticalAlignment=Alignment.CenterVertically) {
            HomePaperSwatch("边框颜色",(if(dark)value.colorDark else value.colorLight)?:fallback,Modifier.weight(1f)){color=true}
            onSync?.let{sync->HomeBorderSync("同步颜色"){sync(if(dark)HomeStyleProperty.BORDER_DARK else HomeStyleProperty.BORDER_LIGHT)}}
        }
        Row(verticalAlignment=Alignment.CenterVertically) {
            Box(Modifier.weight(1f)){HomeValueSlider("粗细",value.widthDp,.5f..6f,1f){onChange(value.copy(widthDp=it))}}
            onSync?.let{sync->HomeBorderSync("同步粗细"){sync(HomeStyleProperty.BORDER_WIDTH)}}
        }
        Row(verticalAlignment=Alignment.CenterVertically) {
            Box(Modifier.weight(1f)){HomeValueSlider("不透明度",value.opacity,0f..1f,1f){onChange(value.copy(opacity=it))}}
            onSync?.let{sync->HomeBorderSync("同步透明度"){sync(HomeStyleProperty.BORDER_OPACITY)}}
        }
    }
    onSync?.let{sync->Row{
        HomeSyncAction("同步线型",{sync(HomeStyleProperty.BORDER_KIND)},caption="线型")
        HomeSyncAction("同步整套边框",{sync(HomeStyleProperty.BORDER)},caption="整套")
    }}
    if(color)HomeColorDialog((if(dark)value.colorDark else value.colorLight)?:fallback,{color=false}){
        onChange(if(dark)value.copy(colorDark=it) else value.copy(colorLight=it));color=false
    }
}

@Composable private fun HomeBorderSync(label:String,onClick:()->Unit){HomeSyncAction(label,onClick)}
