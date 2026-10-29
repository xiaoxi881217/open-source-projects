package com.kingzcheung.xime.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kingzcheung.xime.appearance.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun HomeKeyStickersEditor(id:String,preset:HomeVisualPreset,protectedDrafts:Collection<HomeVisualPreset>,
    style:HomeKeyStyle,selected:Int,onSelected:(Int)->Unit,onChange:(HomeKeyStyle)->Unit,
    onSync:(HomeStyleProperty)->Unit={},onGesture:(()->Unit)?=null,canSync:Boolean=true) {
    val stickers=style.stickers()
    val index=selected.coerceIn(0,(stickers.size-1).coerceAtLeast(0))
    HomePaperGroup("按键贴纸") {
        FlowRow(horizontalArrangement=Arrangement.spacedBy(4.dp),verticalArrangement=Arrangement.spacedBy(2.dp)) {
            stickers.forEachIndexed { i,_ -> HomeChoiceChip(index==i,{onSelected(i)},label={Text("${i+1}")}) }
            HomeIconAction(enabled=stickers.size<HomeKeyStyle.MAX_STICKERS,onClick={
                onChange(style.withSticker(stickers.size,HomeSticker()));onSelected(stickers.size)
            }){Icon(Icons.Default.Add,"再加一张贴纸")}
            if(canSync && stickers.isNotEmpty())HomeSyncAction("同步这颗键的全部贴纸",{onSync(HomeStyleProperty.STICKERS)},caption="全部贴纸")
        }
        if(stickers.isNotEmpty())Row(verticalAlignment=Alignment.CenterVertically) {
            Text("第${index+1}张",Modifier.weight(1f),style=MaterialTheme.typography.labelMedium)
            if(onGesture!=null)HomeIconAction(onClick=onGesture){Icon(Icons.Default.OpenWith,"手势摆放当前贴纸")}
            HomeIconAction(enabled=index>0,onClick={onChange(style.moveSticker(index,-1));onSelected(index-1)}){Icon(Icons.Default.ArrowDownward,"下移一层（同一阴影层内）")}
            HomeIconAction(enabled=index<stickers.lastIndex,onClick={onChange(style.moveSticker(index,1));onSelected(index+1)}){Icon(Icons.Default.ArrowUpward,"上移一层（同一阴影层内）")}
            HomeIconAction(onClick={onChange(style.withSticker(index,null));onSelected((index-1).coerceAtLeast(0))}){Icon(Icons.Default.DeleteOutline,"移除当前贴纸")}
        }
        // 选中项是编辑目标，不进入预设；导出只保存图片、样式和叠放顺序。
        key(id,index) {
            HomeStickerEditor("$id#$index",preset,protectedDrafts,{onChange(style.withSticker(index,it))},onSync,
                value=style.stickerAt(index),canSync=canSync,initialExpanded=true)
        }
    }
}
