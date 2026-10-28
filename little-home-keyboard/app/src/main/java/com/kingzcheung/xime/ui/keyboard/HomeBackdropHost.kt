package com.kingzcheung.xime.ui.keyboard

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.LinearGradient
import android.graphics.Shader
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.kingzcheung.xime.appearance.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

private data class BackdropTexture(val plain:ImageBitmap,val blurred:ImageBitmap)
private class BackdropFrame { var rect by mutableStateOf(Rect.Zero);var size by mutableStateOf(IntSize.Zero);var texture by mutableStateOf<BackdropTexture?>(null) }
private val LocalBackdrop = staticCompositionLocalOf<BackdropFrame?> { null }

/** 遮住下层键盘，复用同一张底图；不重复生成纹理或绘制自由贴纸。 */
@Composable
internal fun Modifier.homeBackdropCover(fallback:Color):Modifier {
 val frame=LocalBackdrop.current
 var bounds by remember{mutableStateOf(Rect.Zero)}
 return onGloballyPositioned{bounds=it.boundsInRoot()}.drawWithContent{
  drawRect(fallback)
  val texture=frame?.texture;val root=frame?.rect
  if(texture!=null && root!=null && root.width>0 && root.height>0){
   val x=(bounds.left-root.left)*frame.size.width/root.width
   val y=(bounds.top-root.top)*frame.size.height/root.height
   clipRect{translate(-x,-y){drawImage(texture.plain,dstSize=frame.size)}}
  }
  drawContent()
 }
}

@Composable
internal fun HomeBackdropHost(preset:HomeVisualPreset?,dark:Boolean,modifier:Modifier=Modifier,content:@Composable BoxScope.()->Unit) {
 if(preset==null){Box(modifier,content=content);return}
 val frame=remember{BackdropFrame()};val palette=if(dark)preset.dark else preset.light
 val wallpaper=preset.wallpaper;val gradient=preset.gradient;val angle=preset.gradientAngle;val size=frame.size
 LaunchedEffect(wallpaper,palette,gradient,angle,size){
  if(size.width>0 && size.height>0){kotlinx.coroutines.delay(48);frame.texture=withContext(Dispatchers.IO){
   try{renderBackdrop(palette,gradient,angle,wallpaper,size)}catch(_:Exception){null}catch(_:OutOfMemoryError){null}
  }}
 }
 CompositionLocalProvider(LocalBackdrop provides frame,LocalHomePreset provides preset,LocalHomeDarkAppearance provides dark){
  com.kingzcheung.xime.ui.theme.HomePanelTheme(palette){
  Box(modifier.homeFloatingStickers(preset.floatingStickers).homeBorderDrawing(preset.frameBorder,radiusDp=preset.cornerRadius.toFloat()).background(Color(palette.background)).onSizeChanged{frame.size=it}.onGloballyPositioned{frame.rect=it.boundsInRoot()}.drawWithContent{
   frame.texture?.plain?.let{drawImage(it,dstSize=frame.size)};drawContent()
  },content=content)
  }
 }
}

private fun renderBackdrop(p:HomePalette,gradient:Boolean,angle:Int,wallpaper:HomeWallpaper?,size:IntSize):BackdropTexture {
 val factor=minOf(1f,960f/maxOf(size.width,size.height));val w=maxOf(1,(size.width*factor).roundToInt());val h=maxOf(1,(size.height*factor).roundToInt())
 val bitmap=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);val canvas=Canvas(bitmap);val paint=Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
 canvas.drawColor(p.background.toInt())
 if(gradient){val rad=Math.toRadians(angle.toDouble());val x=kotlin.math.cos(rad).toFloat();val y=kotlin.math.sin(rad).toFloat();val length=(kotlin.math.abs(x)*w+kotlin.math.abs(y)*h)/2
  paint.shader=LinearGradient(w/2-x*length,h/2-y*length,w/2+x*length,h/2+y*length,p.background.toInt(),p.gradientEnd.toInt(),Shader.TileMode.CLAMP);canvas.drawRect(0f,0f,w.toFloat(),h.toFloat(),paint);paint.shader=null}
 wallpaper?.let{wp->HomeStickerAssets.bitmap(wp.asset)?.let{image->val r=wallpaperRect(image.width.toFloat(),image.height.toFloat(),w.toFloat(),h.toFloat(),wp);paint.alpha=(255*wp.opacity).roundToInt();canvas.drawBitmap(image,null,android.graphics.RectF(r.x,r.y,r.x+r.width,r.y+r.height),paint)}}
 val smallScale=minOf(1f,384f/maxOf(w,h));val small=Bitmap.createScaledBitmap(bitmap,maxOf(1,(w*smallScale).roundToInt()),maxOf(1,(h*smallScale).roundToInt()),true)
 val pixels=IntArray(small.width*small.height);small.getPixels(pixels,0,small.width,0,0,small.width,small.height)
 val blurred=Bitmap.createBitmap(blurBackdrop(pixels,small.width,small.height),small.width,small.height,Bitmap.Config.ARGB_8888)
 if(small!==bitmap)small.recycle()
 return BackdropTexture(bitmap.asImageBitmap(),blurred.asImageBitmap())
}

/** 在透明底色下显示共享模糊背景，文字和贴纸稍后绘制。 */
@Composable
internal fun Modifier.homeKeyFrost():Modifier {
 val amount=LocalHomeKeyStyle.current?.frost?:0f;val frame=LocalBackdrop.current
 val kind=LocalHomeKeyShape.current;val radius=LocalKeyCornerRadius.current
 if(amount<=0 || frame==null)return this
 var keyRect by remember{mutableStateOf(Rect.Zero)}
 return onGloballyPositioned{keyRect=it.boundsInRoot()}.drawWithContent{
  val texture=frame.texture;val root=frame.rect
  if(texture!=null && root.width>0 && root.height>0 && keyRect.width>0 && keyRect.height>0){
   val sx=frame.size.width/root.width;val sy=frame.size.height/root.height
   val x=(keyRect.left-root.left)*sx;val y=(keyRect.top-root.top)*sy
   clipPath(homeFacePath(kind,Rect(androidx.compose.ui.geometry.Offset.Zero,size),radius.toPx())){translate(-x,-y){drawImage(texture.blurred,dstOffset=IntOffset.Zero,dstSize=frame.size,alpha=amount)}}
  };drawContent()
 }
}
