package com.kingzcheung.xime.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.background
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.kingzcheung.xime.ui.theme.keyboardBackground
import kotlin.math.roundToInt
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.kingzcheung.xime.appearance.*
import com.kingzcheung.xime.ui.keyboard.HomeBackdropHost
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun HomeWallpaperEditor(preset:HomeVisualPreset,dark:Boolean,enabled:Boolean,onChange:(HomeVisualPreset)->Unit){
 val context=LocalContext.current;val scope=rememberCoroutineScope();var busy by remember{mutableStateOf(false)};var status by remember{mutableStateOf("")};var editing by remember{mutableStateOf(false)}
 var gesture by remember{mutableStateOf<HomeWallpaper?>(null)};val latest by rememberUpdatedState(preset);val changed by rememberUpdatedState(onChange)
 val picker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->
  if(uri!=null)scope.launch{busy=true;runCatching{withContext(Dispatchers.IO){requireNotNull(context.contentResolver.openInputStream(uri)).use{HomeStickerAssets.importImage(HomeStickerAssets.readInput(it,wallpaper=true),wallpaper=true)}}}.onSuccess{changed(latest.copy(wallpaper=HomeWallpaper(it)));status="壁纸放好了，可以直接拖动和双指缩放"}.onFailure{status="这张壁纸无法导入，请选8MB以内、尺寸合适的PNG/JPEG/WebP"};busy=false}
 }
 Text("键盘底下的壁纸",style=MaterialTheme.typography.titleMedium)
 Row{TextButton(enabled=enabled && !busy,onClick={picker.launch(arrayOf("image/png","image/jpeg","image/webp"))}){Text(if(busy)"正在整理图片…" else "选一张壁纸")}
  TextButton(enabled=enabled && preset.wallpaper!=null && !busy,onClick={onChange(preset.copy(wallpaper=null))}){Text("拿走壁纸")}}
 preset.wallpaper?.let{wallpaper->
  TextButton(onClick={editing=!editing}){Text(if(editing)"收起壁纸摆放" else "手势摆放壁纸")}
  if(editing){
   val display=preset.copy(wallpaper=gesture?:wallpaper)
   WallpaperPlacement(display,dark,Modifier.fillMaxWidth().height(160.dp).clipToBounds().pointerInput(Unit){
    awaitEachGesture{
     val down=awaitFirstDown(requireUnconsumed=false);val original=latest.wallpaper?:return@awaitEachGesture;down.consume();var draft=original;var completed=false
     try{do{val e=awaitPointerEvent();val pan=e.calculatePan();draft=draft.transformed(pan.x/size.width,pan.y/size.height,e.calculateZoom());gesture=draft;e.changes.forEach{it.consume()}}while(e.changes.any{it.pressed});completed=true}
     finally{if(completed && draft!=original)changed(latest.copy(wallpaper=draft));gesture=null}
    }
   })
   Text("单指移动、双指缩放，松手合并一次修改；空出来的区域露出背景色。",style=MaterialTheme.typography.bodySmall)
  }
  HomeValueSlider("壁纸不透明度",wallpaper.opacity,0f..1f,1f){onChange(preset.copy(wallpaper=wallpaper.copy(opacity=it)))}
  TextButton(onClick={onChange(preset.copy(wallpaper=HomeWallpaper(wallpaper.asset)))}){Text("壁纸回到居中铺满")}
 }
 if(status.isNotEmpty())Text(status,style=MaterialTheme.typography.bodySmall)
 Text("图片只存本机并随完整装扮导出；不会传到小家。",style=MaterialTheme.typography.bodySmall)
}

@Composable
private fun WallpaperPlacement(preset:HomeVisualPreset,dark:Boolean,modifier:Modifier){
 val wallpaper=preset.wallpaper?:return;val image by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null,wallpaper.asset){value=withContext(Dispatchers.IO){HomeStickerAssets.bitmap(wallpaper.asset)?.asImageBitmap()}}
 val palette=if(dark)preset.dark else preset.light
 Box(modifier.keyboardBackground(HomePresetStore.scheme("wallpaper-placement",preset).keyboardBackground,dark,Color(palette.background)).drawWithContent{
  image?.let{val r=wallpaperRect(it.width.toFloat(),it.height.toFloat(),size.width,size.height,wallpaper);drawImage(it,dstOffset=IntOffset(r.x.roundToInt(),r.y.roundToInt()),dstSize=IntSize(r.width.roundToInt(),r.height.roundToInt()),alpha=wallpaper.opacity)};drawContent()
 })
}
