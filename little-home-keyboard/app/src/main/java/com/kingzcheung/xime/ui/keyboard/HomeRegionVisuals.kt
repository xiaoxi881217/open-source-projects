package com.kingzcheung.xime.ui.keyboard

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.ContentScale
import com.kingzcheung.xime.appearance.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/** 独立栏壁纸只绘制，不建立点击或布局节点。 */
@Composable internal fun Modifier.homeRegionWallpaper():Modifier {
    val wallpaper=LocalHomeKeyStyle.current?.wallpaper?:return this
    val frost=LocalHomeKeyStyle.current?.frost?:0f
    val bitmap by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null,wallpaper.asset){
        value=withContext(Dispatchers.IO){HomeStickerAssets.bitmap(wallpaper.asset)?.asImageBitmap()}
    }
    val blurred by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null,wallpaper.asset,frost>0f){
        if(frost>0f)value=withContext(Dispatchers.IO){
            runCatching{HomeStickerAssets.bitmap(wallpaper.asset)?.let{source->
                val ratio=minOf(1f,384f/maxOf(source.width,source.height))
                val small=android.graphics.Bitmap.createScaledBitmap(source,(source.width*ratio).roundToInt().coerceAtLeast(1),(source.height*ratio).roundToInt().coerceAtLeast(1),true)
                val pixels=IntArray(small.width*small.height);small.getPixels(pixels,0,small.width,0,0,small.width,small.height)
                val result=android.graphics.Bitmap.createBitmap(blurBackdrop(pixels,small.width,small.height),small.width,small.height,android.graphics.Bitmap.Config.ARGB_8888).asImageBitmap()
                if(small!==source)small.recycle();result
            }}.getOrNull()
        }
    }
    val shape=LocalHomeKeyShape.current;val radius=LocalKeyCornerRadius.current
    return drawWithContent {
        bitmap?.let{image->
            val rect=wallpaperRect(image.width.toFloat(),image.height.toFloat(),size.width,size.height,wallpaper)
            clipPath(homeFacePath(shape,Rect(Offset.Zero,size),radius.toPx())){
                drawContext.canvas.saveLayer(Rect(Offset.Zero,size),androidx.compose.ui.graphics.Paint().apply{alpha=wallpaper.opacity})
                drawImage(image,dstOffset=IntOffset(rect.x.roundToInt(),rect.y.roundToInt()),
                    dstSize=IntSize(rect.width.roundToInt().coerceAtLeast(1),rect.height.roundToInt().coerceAtLeast(1)))
                blurred?.let{drawImage(it,dstOffset=IntOffset(rect.x.roundToInt(),rect.y.roundToInt()),
                    dstSize=IntSize(rect.width.roundToInt().coerceAtLeast(1),rect.height.roundToInt().coerceAtLeast(1)),alpha=frost)}
                drawContext.canvas.restore()
            }
        }
        drawContent()
    }
}

/** 替换图标不替换按钮动作；偏移和缩放只影响图像。 */
@Composable internal fun HomeReplacementIcon(description:String?,fallback:@Composable ()->Unit) {
    val style=LocalHomeKeyStyle.current
    val asset=style?.iconAsset
    val image by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null,asset){
        value=if(asset==null)null else withContext(Dispatchers.IO){HomeStickerAssets.bitmap(asset)?.asImageBitmap()}
    }
    val iconSize=style?.iconSize?:24f
    Box(Modifier.size(24.dp).homeKeyLabelPosition().drawWithContent{
        val factor=iconSize.dp.toPx()/size.minDimension.coerceAtLeast(1f)
        val scope=this
        withTransform({scale(factor,factor,center)}){scope.drawContent()}
    },propagateMinConstraints=true) {
        if(asset!=null && image!=null)Image(image!!,description,Modifier.size((style?.iconSize?:24f).dp),
            contentScale=ContentScale.Fit,alpha=style?.textOpacity?:1f)
        else fallback()
    }
}
