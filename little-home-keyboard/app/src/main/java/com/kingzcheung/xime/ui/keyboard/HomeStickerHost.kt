package com.kingzcheung.xime.ui.keyboard

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.kingzcheung.xime.appearance.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

private data class StickerTarget(val bounds: State<Rect?>,val stickers: State<List<HomeSticker>>,val bitmaps: State<Map<String,ImageBitmap>>,
    val cornerRadius: State<Float>,val shape:State<HomeKeyShape>,val progress: State<Float>,val pressed: State<Boolean>,val motion: State<HomeKeyStyle>,val animated: State<Boolean>)
private class StickerRegistry { val targets=mutableStateMapOf<Any,StickerTarget>() }
private val LocalStickerRegistry=staticCompositionLocalOf<StickerRegistry?> { null }

/** 仅添加绘制Modifier：不新增布局、可点击节点或装饰命中区域。 */
@Composable
internal fun HomeStickerHost(modifier: Modifier=Modifier,content: @Composable (Modifier)->Unit) {
    val registry=remember { StickerRegistry() };val hostBounds=remember { mutableStateOf<Rect?>(null) }
    val drawing=modifier.onGloballyPositioned { hostBounds.value=it.boundsInRoot() }.drawWithContent {
        drawContent()
        registry.targets.values.forEach { target ->
            val bounds=target.bounds.value ?: return@forEach;val host=hostBounds.value ?: return@forEach
            target.stickers.value.forEach stickerLoop@{ sticker ->
            val image=target.bitmaps.value[sticker.asset] ?: return@stickerLoop
            val sx=if(host.width>0)size.width/host.width else 1f;val sy=if(host.height>0)size.height/host.height else 1f
            val key=Rect((bounds.left-host.left)*sx,(bounds.top-host.top)*sy,(bounds.right-host.left)*sx,(bounds.bottom-host.top)*sy)
            val edge=(if(sticker.clipToKey)maxOf(key.width,key.height) else minOf(key.width,key.height)*.9f)*sticker.scale
            val ratio=image.width.toFloat()/image.height
            val coverWidth=maxOf(key.width,key.height*ratio)*sticker.scale
            val w=if(sticker.clipToKey)coverWidth else if(ratio>=1)edge else edge*ratio
            val h=if(sticker.clipToKey)coverWidth/ratio else if(ratio>=1)edge/ratio else edge
            val center=Offset(key.center.x+sticker.offsetX*key.width,key.center.y+sticker.offsetY*key.height)
            val transform=if(target.animated.value)pressTransform(target.motion.value.motion,target.motion.value.motionStrength,target.progress.value) else HomePressTransform()
            val shift=transform.shiftYDp.dp.toPx()
            val render: DrawScope.()->Unit = {
                withTransform({rotate(sticker.rotation,center)}) {
                    drawImage(image,dstOffset=IntOffset((center.x-w/2).roundToInt(),(center.y-h/2).roundToInt()),
                        dstSize=IntSize(w.roundToInt().coerceAtLeast(1),h.roundToInt().coerceAtLeast(1)),
                        alpha=sticker.opacity*(if(target.pressed.value).82f else 1f))
                }
            }
            withTransform({translate(0f,shift);scale(transform.scaleX,transform.scaleY,key.center)}) {
                if(sticker.clipToKey) {
                    val radius=(target.cornerRadius.value.dp.toPx()).coerceIn(0f,minOf(key.width,key.height)/2)
                    val path=homeFacePath(target.shape.value,key,radius)
                    clipPath(path) { render() }
                } else render()
            }
            }
        }
    }
    CompositionLocalProvider(LocalStickerRegistry provides registry) { content(drawing) }
}

@Composable
internal fun Modifier.homeStickerTarget(pressed: Boolean,progress: State<Float>,animated: Boolean): Modifier {
    val registry=LocalStickerRegistry.current ?: return this
    val style=LocalHomeKeyStyle.current ?: return this
    val stickers=style.stickers().filter{it.effectiveLayer()==HomeStickerLayer.ABOVE_KEY}
    if(stickers.isEmpty())return this
    val token=remember { Any() };val bounds=remember { mutableStateOf<Rect?>(null) }
    val images=homeStickerImages(stickers)
    val descriptor=StickerTarget(bounds,rememberUpdatedState(stickers),images,rememberUpdatedState(LocalKeyCornerRadius.current.value),rememberUpdatedState(LocalHomeKeyShape.current),progress,rememberUpdatedState(pressed),rememberUpdatedState(style),rememberUpdatedState(animated))
    SideEffect { registry.targets[token]=descriptor }
    DisposableEffect(registry,token) { onDispose { registry.targets.remove(token) } }
    return onGloballyPositioned { bounds.value=it.boundsInRoot() }
}

/** 覆盖贴纸留在键帽绘制链，按键变换由外层统一处理。 */
@Composable
internal fun Modifier.homeClippedSticker(layer:HomeStickerLayer):Modifier {
    val stickers=LocalHomeKeyStyle.current?.stickers().orEmpty().filter{it.clipToKey && it.effectiveLayer()==layer}
    if(stickers.isEmpty())return this
    val images=homeStickerImages(stickers)
    val pressed=LocalHomeKeyPressed.current
    val radius=LocalKeyCornerRadius.current
    val shape=LocalHomeKeyShape.current
    return drawWithCache {
        val r=radius.toPx().coerceIn(0f,minOf(size.width,size.height)/2)
        val path=homeFacePath(shape,Rect(Offset.Zero,size),r)
        onDrawWithContent {
            stickers.forEach { sticker ->
            images.value[sticker.asset]?.let{bitmap->
                val ratio=bitmap.width.toFloat()/bitmap.height
                val w=maxOf(size.width,size.height*ratio)*sticker.scale;val h=w/ratio
                val center=Offset(size.width*(.5f+sticker.offsetX),size.height*(.5f+sticker.offsetY))
                clipPath(path){withTransform({rotate(sticker.rotation,center)}){
                    drawImage(bitmap,dstOffset=IntOffset((center.x-w/2).roundToInt(),(center.y-h/2).roundToInt()),
                        dstSize=IntSize(w.roundToInt().coerceAtLeast(1),h.roundToInt().coerceAtLeast(1)),alpha=sticker.opacity*(if(pressed?.value==true).82f else 1f))
                }}
            }
            }
            drawContent()
        }
    }
}

@Composable
private fun homeStickerImages(stickers:List<HomeSticker>):State<Map<String,ImageBitmap>> {
    val assets=stickers.map{it.asset}.distinct()
    return produceState<Map<String,ImageBitmap>>(emptyMap(),assets) {
        // 不让新图片加载期间复用上一组图片，IO完成后一次替换。
        value=emptyMap()
        value=withContext(Dispatchers.IO){assets.mapNotNull{asset->HomeStickerAssets.bitmap(asset)?.asImageBitmap()?.let{asset to it}}.toMap()}
    }
}
