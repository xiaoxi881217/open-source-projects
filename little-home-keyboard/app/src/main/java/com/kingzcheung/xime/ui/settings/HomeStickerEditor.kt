package com.kingzcheung.xime.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.kingzcheung.xime.appearance.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun HomeStickerEditor(keyId: String,preset: HomeVisualPreset,protectedDrafts: Collection<HomeVisualPreset>,
    onChange: (HomeSticker?)->Unit,onSync: (HomeStyleProperty)->Unit, value:HomeSticker?=preset.keys[keyId]?.sticker, canSync:Boolean=true, supportsCover:Boolean=true) {
    val sticker=value;val context=LocalContext.current;val scope=rememberCoroutineScope()
    var expanded by remember {mutableStateOf(false)};var busy by remember {mutableStateOf(false)}
    var precise by remember(keyId) {mutableStateOf(false)}
    var message by remember {mutableStateOf("")};var linkDialog by remember{mutableStateOf(false)};var link by remember{mutableStateOf("")};var importTarget by remember {mutableStateOf("")}
    val currentKey by rememberUpdatedState(keyId);val apply by rememberUpdatedState(onChange)
    val currentSticker by rememberUpdatedState(sticker)
    val import=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri ->
        if(uri!=null)scope.launch {
            busy=true
            try {
                val asset=withContext(Dispatchers.IO){requireNotNull(context.contentResolver.openInputStream(uri)).use {HomeStickerAssets.importImage(HomeStickerAssets.readInput(it))}}
                if(currentKey==importTarget){apply((currentSticker?:HomeSticker()).copy(asset=asset));message="贴纸已叠上，原来的颜色和阴影都保留。"}
                else message="图片已收好，请回到刚才那颗键再选一次。"
            }catch(_:Exception){message="这张图片没能导入，请选24MB内的PNG、JPEG或WebP，大图会自动缩小。"}
            finally{busy=false}
        }
    }
    TextButton(onClick={expanded=!expanded}){Text("贴纸 · ${if(sticker==null)"还没贴" else "已叠加"} · ${if(expanded)"收起" else "展开"}")}
    AnimatedVisibility(expanded){Column(verticalArrangement=Arrangement.spacedBy(6.dp)){
        FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp)){
            HomeSticker.BUILTINS.forEach{(asset,title)->FilterChip(sticker?.asset==asset,{onChange((sticker?:HomeSticker()).copy(asset=asset))},label={Text(title)},enabled=!busy)}
        }
        FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){
            OutlinedButton(shape=androidx.compose.foundation.shape.RoundedCornerShape(14.dp),enabled=!busy,onClick={importTarget=keyId;import.launch(arrayOf("image/png","image/jpeg","image/webp"))}){Text("选择图片")}
            OutlinedButton(shape=androidx.compose.foundation.shape.RoundedCornerShape(14.dp),enabled=!busy,onClick={linkDialog=true}){Text("图片链接")}
        }
        Text("较大的图片会自动缩小。自由贴纸可超出按键，覆盖模式会裁在键内。",style=MaterialTheme.typography.bodySmall)
        if(message.isNotEmpty())Text(message,style=MaterialTheme.typography.bodySmall)
        if(sticker!=null){
            if(supportsCover)Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                FilterChip(!sticker.clipToKey,{onChange(sticker.copy(clipToKey=false))},label={Text("自由贴纸")})
                FilterChip(sticker.clipToKey,{onChange(sticker.copy(clipToKey=true,offsetX=0f,offsetY=0f,scale=1f))},label={Text("覆盖键内")})
            }
            if(canSync && supportsCover)TextButton(onClick={onSync(HomeStyleProperty.STICKER_CLIP)}){Text("只同步覆盖模式")}
            if(supportsCover && sticker.clipToKey) {
                FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                    HomeStickerLayer.entries.forEach{layer->FilterChip(sticker.layer==layer,{onChange(sticker.copy(layer=layer))},label={Text(layer.label)})}
                }
                if(canSync)TextButton(onClick={onSync(HomeStyleProperty.STICKER_LAYER)}){Text("只同步贴纸图层")}
            }

            @Composable fun value(title:String,v:Float,range:ClosedFloatingPointRange<Float>,default:Float,property:HomeStyleProperty,change:(Float)->HomeSticker){
                HomeValueSlider(title,v,range,default){onChange(change(it))}
                if(canSync)TextButton(onClick={onSync(property)}){Text("只同步这个数值")}
            }
            Row{
                TextButton(onClick={onSync(HomeStyleProperty.STICKER)}){Text("同步整张贴纸")}
                TextButton(onClick={onSync(HomeStyleProperty.STICKER_ASSET)}){Text("只同步图片")}
            }
            Text("在上面的预览选“摆放贴纸”，直接拖动、捏合和旋转。",style=MaterialTheme.typography.bodySmall)
            TextButton(onClick={precise=!precise}){Text(if(precise)"收起贴纸精确数值" else "贴纸精确数值")}
            if(precise) {
            value("贴纸大小",sticker.scale,.25f..12f,.65f,HomeStyleProperty.STICKER_SCALE){sticker.copy(scale=it)}
            value("横向位置",sticker.offsetX,-2f..2f,.3f,HomeStyleProperty.STICKER_X){sticker.copy(offsetX=it)}
            value("纵向位置",sticker.offsetY,-2f..2f,-.3f,HomeStyleProperty.STICKER_Y){sticker.copy(offsetY=it)}
            value("旋转角度",sticker.rotation,-180f..180f,0f,HomeStyleProperty.STICKER_ROTATION){sticker.copy(rotation=it)}
            value("贴纸透明度",sticker.opacity,0f..1f,1f,HomeStyleProperty.STICKER_OPACITY){sticker.copy(opacity=it)}
            }
            Text("数值只同步到已经有贴纸的目标键；想给空白键也贴上，用整张贴纸或只同步图片。",style=MaterialTheme.typography.bodySmall)
            Row{TextButton(onClick={onChange(HomeSticker(asset=sticker.asset))}){Text("摆回小角落")};TextButton(onClick={onChange(null)}){Text("只移除贴纸")}}
        }
        TextButton(enabled=!busy,onClick={
            val references=HomePresetStore.presets.values.toList()+protectedDrafts+preset
            scope.launch {busy=true;try{val count=withContext(Dispatchers.IO){HomeStickerAssets.cleanUnused(references)};message="整理好了，清掉${count}张未使用图片；刚导入的先保留。"}catch(_:Exception){message="这次没整理好，原装扮保留。"}finally{busy=false}}
        }){Text("整理未使用的贴纸图片")}
    }}
    if(linkDialog)AlertDialog(onDismissRequest={linkDialog=false},title={Text("从链接收一张图片")},text={
        Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
            OutlinedTextField(link,{link=it.take(4096)},label={Text("HTTPS图片直链")},singleLine=true,modifier=Modifier.fillMaxWidth())
            Text("仅导入时下载，之后离线使用。",style=MaterialTheme.typography.bodySmall)
        }
    },confirmButton={TextButton(enabled=link.isNotBlank()&&!busy,onClick={
        val url=link;val target=keyId;linkDialog=false;busy=true;message="正在收图片…"
        scope.launch{try{
            val asset=withContext(Dispatchers.IO){HomeStickerAssets.importImage(HomeStickerLink.fetch(url))}
            if(currentKey==target){apply((currentSticker?:HomeSticker()).copy(asset=asset));message="图片已收好，之后不用联网也能显示。"}
            else message="图片已收好，请回到刚才的位置再选一次。"
        }catch(_:Exception){message="这张图片没有收好，请确认是24MB内的HTTPS图片直链。"}finally{busy=false}}
    }){Text("导入")}},dismissButton={TextButton(onClick={linkDialog=false}){Text("取消")}})

}
