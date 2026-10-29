package com.kingzcheung.xime.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
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
        Text("在整行预览里点选，长按拖动换位，双指缩放当前图标。",style=MaterialTheme.typography.bodySmall)
        HomeValueSlider("图标大小",style.iconSize,12f..40f,24f){onChange(style.copy(iconSize=it))}
        HomeActionButton(onClick={onChange(style.copy(labelOffsetX=0f,labelOffsetY=0f,iconSize=24f))}){Text("图标大小恢复默认")}
        if(status.isNotEmpty())Text(status,style=MaterialTheme.typography.bodySmall)
    }
}

@Composable internal fun HomeToolbarOrderEditor(preset:HomeVisualPreset,items:List<ToolbarButtonItem>,onChange:(HomeVisualPreset)->Unit,
    onSelect:(String)->Unit={},selected:String?=null,dark:Boolean=false) {
    val latest by rememberUpdatedState(preset);val changed by rememberUpdatedState(onChange)
    HomePaperGroup("整行快捷键") {
        CompositionLocalProvider(LocalHomePreset provides preset,LocalHomeDarkAppearance provides dark) {
        HomeKeyStyleScope("chrome:toolbar",preset) {
            HomeFullToolbar(items,preset.toolbarOrder,MaterialTheme.colorScheme.onSurfaceVariant,MaterialTheme.colorScheme.primary,
                {onSelect("toolbar:"+HomeKeyIds.token(it))},onMove={from,to->
                    val order=homeToolbarOrder(items.map{it.id},latest.toolbarOrder)
                    changed(latest.copy(toolbarOrder=moveHomeToolbar(order,from,to)))
                },onScale=selected?.takeIf{it.startsWith("toolbar:")}?.let{id->{zoom->
                    val style=latest.keys[id]?:HomeKeyStyle()
                    changed(latest.copy(keys=latest.keys+(id to style.copy(iconSize=(style.iconSize*zoom).coerceIn(12f,40f)))))
                }})
        }
        }
        Text("点选按钮；长按拖到整行其他位置。按钮多时可横向滑动。",style=MaterialTheme.typography.bodySmall)
        HomeActionButton(onClick={onChange(preset.copy(toolbarOrder=emptyList()))}){Text("恢复默认位置")}
    }
}
