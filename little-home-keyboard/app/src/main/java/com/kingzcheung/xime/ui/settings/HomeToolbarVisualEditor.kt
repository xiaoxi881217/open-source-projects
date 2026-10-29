package com.kingzcheung.xime.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.kingzcheung.xime.appearance.*
import com.kingzcheung.xime.keyboard.ToolbarButtonItem
import com.kingzcheung.xime.ui.keyboard.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalLayoutApi::class)
@Composable internal fun HomeToolbarVisualEditor(style:HomeKeyStyle,onChange:(HomeKeyStyle)->Unit) {
    val context=LocalContext.current;val scope=rememberCoroutineScope()
    val latest by rememberUpdatedState(style);val changed by rememberUpdatedState(onChange)
    var status by remember{mutableStateOf("")};var busy by remember{mutableStateOf(false)}
    val picker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->if(uri!=null)scope.launch{
        busy=true
        runCatching{withContext(Dispatchers.IO){requireNotNull(context.contentResolver.openInputStream(uri)).use{HomeStickerAssets.importImage(HomeStickerAssets.readInput(it))}}}
            .onSuccess{changed(latest.copy(iconAsset=it));status="图标换好了，按钮原来的功能保留"}.onFailure{status="这张图片没导入成功，请试试PNG、JPEG或WebP"}
        busy=false
    }}
    HomePaperGroup("快捷图标 · 换图片") {
        FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
            HomeActionButton(enabled=!busy,onClick={picker.launch(arrayOf("image/png","image/jpeg","image/webp"))}){Text("从图片替换")}
            HomeActionButton(onClick={onChange(style.copy(iconAsset=null))}){Text("恢复原图标")}
            HomeSticker.BUILTINS.forEach{(asset,label)->HomeChoiceChip(style.iconAsset==asset,{onChange(style.copy(iconAsset=asset))},label={Text(label)})}
        }
        HomeKeyStyleScope("toolbar:icon-placement",HomeVisualPreset(keys=mapOf("toolbar:icon-placement" to style))) {
            Box(Modifier.fillMaxWidth().height(88.dp).homeControlOutline().pointerInput(Unit){
                detectTransformGestures{_,pan,zoom,_->
                    val edge=64.dp.toPx()
                    changed(latest.copy(labelOffsetX=(latest.labelOffsetX+pan.x/edge).coerceIn(-.4f,.4f),
                        labelOffsetY=(latest.labelOffsetY+pan.y/edge).coerceIn(-.4f,.4f),iconSize=(latest.iconSize*zoom).coerceIn(12f,40f)))
                }
            },contentAlignment=Alignment.Center){
                Box(Modifier.size(64.dp).homeKeyEffects(false),contentAlignment=Alignment.Center){HomeReplacementIcon("图标摆放预览"){Text("图标")}}
            }
        }
        Text("单指拖动位置、双指缩放；点击范围仍属于原来的快捷按钮。",style=MaterialTheme.typography.bodySmall)
        HomeValueSlider("图标大小",style.iconSize,12f..40f,24f){onChange(style.copy(iconSize=it))}
        HomeActionButton(onClick={onChange(style.copy(labelOffsetX=0f,labelOffsetY=0f,iconSize=24f))}){Text("位置与大小回到原位")}
        if(status.isNotEmpty())Text(status,style=MaterialTheme.typography.bodySmall)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable internal fun HomeToolbarOrderEditor(preset:HomeVisualPreset,items:List<ToolbarButtonItem>,onChange:(HomeVisualPreset)->Unit) {
    if(items.isEmpty())return
    val latest by rememberUpdatedState(preset);val changed by rememberUpdatedState(onChange)
    val bounds=remember{mutableStateMapOf<String,Rect>()};var point by remember{mutableStateOf(Offset.Zero)}
    val ordered=items.sortedBy{preset.toolbarOrder.indexOf(it.id).takeIf{n->n>=0}?:Int.MAX_VALUE}
    HomePaperGroup("快捷按钮顺序") {
        Text("长按后拖到另一项的位置。更多与收起入口固定保留。",style=MaterialTheme.typography.bodySmall)
        FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
            ordered.forEach{item->key(item.id){
                Surface(modifier=Modifier.homeControlOutline().onGloballyPositioned{bounds[item.id]=it.boundsInParent()}.pointerInput(item.id){
                    detectDragGesturesAfterLongPress(onDragStart={point=bounds[item.id]?.center?:Offset.Zero},onDrag={change,delta->
                        change.consume();point+=delta
                        val target=bounds.filter{it.value.contains(point)}.keys.firstOrNull()
                        if(target!=null && target!=item.id){
                            val ids=items.sortedBy{latest.toolbarOrder.indexOf(it.id).takeIf{n->n>=0}?:Int.MAX_VALUE}.map{it.id}.toMutableList()
                            val from=ids.indexOf(item.id);val to=ids.indexOf(target)
                            if(from>=0 && to>=0){ids.add(to,ids.removeAt(from));changed(latest.copy(toolbarOrder=ids))}
                        }
                    })
                },color=MaterialTheme.colorScheme.surfaceContainerLow){Text(item.label,Modifier.padding(12.dp))}
            }}
        }
        HomeActionButton(onClick={onChange(preset.copy(toolbarOrder=emptyList()))}){Text("恢复设置中的顺序")}
    }
}
