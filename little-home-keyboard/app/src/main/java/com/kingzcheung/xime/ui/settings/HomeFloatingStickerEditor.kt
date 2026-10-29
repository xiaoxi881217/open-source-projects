package com.kingzcheung.xime.ui.settings

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kingzcheung.xime.appearance.*
import java.util.UUID

@Composable
internal fun HomeFloatingStickerEditor(preset:HomeVisualPreset,selected:String?,onSelect:(String?)->Unit,
    protectedDrafts:Collection<HomeVisualPreset>,onChange:(HomeVisualPreset)->Unit){
    Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)){
        preset.floatingStickers.forEachIndexed{i,s->HomeChoiceChip(s.id==selected,{onSelect(s.id)},label={Text("贴纸${i+1}")})}
        HomeActionButton(enabled=preset.floatingStickers.size<12,onClick={val s=HomeFloatingSticker(UUID.randomUUID().toString());onChange(preset.copy(floatingStickers=preset.floatingStickers+s));onSelect(s.id)}){Text("＋自由贴纸")}
    }
    val item=preset.floatingStickers.firstOrNull{it.id==selected}?:return
    Text("在上方预览拖动、双指缩放和旋转。",style=MaterialTheme.typography.bodySmall)
    HomeStickerEditor("floating:${item.id}",preset,protectedDrafts,{s->
        onChange(preset.copy(floatingStickers=if(s==null)preset.floatingStickers.filter{it.id!=item.id} else preset.floatingStickers.map{if(it.id==item.id)it.copy(sticker=s.copy(clipToKey=false)) else it}))
    },{},value=item.sticker,canSync=false,supportsCover=false)
    Row{
        HomeActionButton(onClick={onChange(preset.copy(floatingStickers=preset.floatingStickers.filter{it.id!=item.id}+item))}){Text("移到最上层")}
        HomeActionButton(onClick={onChange(preset.copy(floatingStickers=preset.floatingStickers.filter{it.id!=item.id}))}){Text("移除这张")}
    }
}
