package com.kingzcheung.xime.appearance

import kotlinx.serialization.Serializable
import kotlin.math.max

@Serializable
data class HomeWallpaper(val asset: String, val offsetX: Float=0f, val offsetY: Float=0f, val scale: Float=1f, val opacity: Float=1f) {
 fun validate(){require(asset.matches(Regex("wallpaper:[a-f0-9]{64}"))) { "壁纸资源无效" };require(offsetX.isFinite() && offsetY.isFinite() && offsetX in -1f..1f && offsetY in -1f..1f && scale.isFinite() && scale in 1f..4f && opacity.isFinite() && opacity in 0f..1f){"壁纸位置或大小超出范围"}}
 fun transformed(dx:Float,dy:Float,zoom:Float):HomeWallpaper = if(!listOf(dx,dy,zoom).all{it.isFinite()} || zoom<=0)this else copy(offsetX=(offsetX+dx).coerceIn(-1f,1f),offsetY=(offsetY+dy).coerceIn(-1f,1f),scale=(scale*zoom).coerceIn(1f,4f))
}
data class HomeWallpaperRect(val x:Float,val y:Float,val width:Float,val height:Float)
fun wallpaperRect(iw:Float,ih:Float,w:Float,h:Float,p:HomeWallpaper):HomeWallpaperRect {
 require(listOf(iw,ih,w,h).all{it.isFinite() && it>0});val scale=max(w/iw,h/ih)*p.scale;val width=iw*scale;val height=ih*scale
 return HomeWallpaperRect((w-width)/2+p.offsetX*w,(h-height)/2+p.offsetY*h,width,height)
}

/** 三轮分离盒式模糊；只处理共享小纹理，绝不处理字母或每次按键。 */
fun blurBackdrop(pixels:IntArray,width:Int,height:Int,radius:Int=5):IntArray {
 require(width>0 && height>0 && width.toLong()*height==pixels.size.toLong() && radius in 0..16)
 var src=pixels.copyOf();if(radius==0)return src
 repeat(3){
  for(horizontal in listOf(true,false)){
   val dst=IntArray(src.size);val outer=if(horizontal)height else width;val inner=if(horizontal)width else height;val count=radius*2+1
   fun index(o:Int,i:Int)=if(horizontal)o*width+i else i*width+o
   for(o in 0 until outer){
    var a=0;var r=0;var g=0;var b=0
    fun add(i:Int,sign:Int){val c=src[index(o,i.coerceIn(0,inner-1))];a+=(c ushr 24)*sign;r+=((c ushr 16) and 255)*sign;g+=((c ushr 8) and 255)*sign;b+=(c and 255)*sign}
    for(i in -radius..radius)add(i,1)
    for(i in 0 until inner){dst[index(o,i)]=((a/count) shl 24) or ((r/count) shl 16) or ((g/count) shl 8) or (b/count);add(i-radius,-1);add(i+radius+1,1)}
   };src=dst
  }
 };return src
}
