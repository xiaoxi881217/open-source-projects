package com.kingzcheung.xime.ui.keyboard

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kingzcheung.xime.appearance.safeHomeRowGaps

/** 行间使用真实Spacer，预览与系统键盘共享。 */
@Composable internal fun HomeKeyboardRows(layout:String,count:Int,modifier:Modifier=Modifier,content:@Composable ColumnScope.(Int,Modifier)->Unit) {
    if(count<=0)return
    val preset=LocalHomePreset.current
    BoxWithConstraints(modifier) {
        val edge=minOf(preset?.spacingY?:0f,((maxHeight.value-count*24f)/2).coerceAtLeast(0f))
        val gaps=safeHomeRowGaps(List(count-1){preset?.rowGap(layout,it)?:0f},maxHeight.value,count,edge)
        Column(Modifier.fillMaxSize().padding(vertical=edge.dp)) {
            repeat(count){index->
                if(index>0)Spacer(Modifier.height(gaps[index-1].dp))
                content(index,Modifier.weight(1f))
            }
        }
    }
}
