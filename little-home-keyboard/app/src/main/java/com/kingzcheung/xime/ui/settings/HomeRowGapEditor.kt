package com.kingzcheung.xime.ui.settings

import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.ui.unit.dp
import com.kingzcheung.xime.appearance.HomeVisualPreset

@Composable internal fun HomeRowGapEditor(preset:HomeVisualPreset,layout:String,rows:Int,onChange:(HomeVisualPreset)->Unit) {
    var expanded by remember{mutableStateOf(false)}
    HomeActionButton(onClick={expanded=!expanded}){Text(if(expanded)"收起逐行调整" else "逐行调整 · 第1—2行等")}
    if(expanded)Column(verticalArrangement=Arrangement.spacedBy(6.dp)) {
        repeat((rows-1).coerceIn(0,4)){index->
            HomeValueSlider("第${index+1}行与第${index+2}行",preset.rowGap(layout,index),0f..20f,preset.spacingY*2f,compact=true){value->
                val gaps=List((rows-1).coerceIn(0,4)){preset.rowGap(layout,it)}.toMutableList();gaps[index]=value
                onChange(preset.copy(rowGaps=preset.rowGaps+(layout to gaps)))
            }
        }
        HomeActionButton(onClick={onChange(preset.copy(rowGaps=preset.rowGaps-layout))}){Text("这些行跟随整体间距")}
        Text("预览和键盘共用真实间隔；矮屏会收紧过大的间隔，为按键留出空间。")
    }
}
