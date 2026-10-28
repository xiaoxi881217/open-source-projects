package com.kingzcheung.xime.ui.keyboard

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.kingzcheung.xime.appearance.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/** 画在整个IME内容之上，不创建任何触摸节点。 */
@Composable
internal fun Modifier.homeFloatingStickers(stickers:List<HomeFloatingSticker>):Modifier {
    if(stickers.isEmpty())return this
    val ids=stickers.map{it.sticker.asset}.distinct()
    val images by produceState<Map<String,ImageBitmap>>(emptyMap(),ids){
        value=withContext(Dispatchers.IO){ids.mapNotNull{id->HomeStickerAssets.bitmap(id)?.asImageBitmap()?.let{id to it}}.toMap()}
    }
    return drawWithContent {
        drawContent()
        clipRect {
            stickers.forEach{entry->
                val s=entry.sticker;val image=images[s.asset]?:return@forEach
                val edge=minOf(size.width,size.height)*.9f*s.scale
                val ratio=image.width.toFloat()/image.height
                val w=if(ratio>=1)edge else edge*ratio;val h=if(ratio>=1)edge/ratio else edge
                val center=Offset((.5f+s.offsetX)*size.width,(.5f+s.offsetY)*size.height)
                withTransform({rotate(s.rotation,center)}){
                    drawImage(image,dstOffset=IntOffset((center.x-w/2).roundToInt(),(center.y-h/2).roundToInt()),
                        dstSize=IntSize(w.roundToInt().coerceAtLeast(1),h.roundToInt().coerceAtLeast(1)),alpha=s.opacity)
                }
            }
        }
    }
}
