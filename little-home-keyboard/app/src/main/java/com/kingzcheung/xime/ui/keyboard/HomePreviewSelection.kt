package com.kingzcheung.xime.ui.keyboard

import androidx.compose.foundation.gestures.*
import androidx.compose.ui.input.pointer.*
import com.kingzcheung.xime.appearance.HomeSticker
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp

internal class HomePreviewKeys(val selected: State<String?>) {
    val bounds = mutableStateMapOf<Any, Pair<String, Rect>>()
}
internal val LocalHomePreviewKeys = staticCompositionLocalOf<HomePreviewKeys?> { null }
internal val LocalHomeKeyId = staticCompositionLocalOf<String?> { null }

@Composable
internal fun HomePreviewSelectionHost(selected: String?, onSelect: (String) -> Unit, floatingId:String?=null,floating:HomeSticker?=null,onFloatingPreview:(HomeSticker?)->Unit={},onFloatingCommit:(HomeSticker)->Unit={}, content: @Composable () -> Unit) {
    val current = rememberUpdatedState(selected)
    val registry = remember { HomePreviewKeys(current) }
    var position by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val choose by rememberUpdatedState(onSelect)
    val currentFloating by rememberUpdatedState(floating)
    val previewFloating by rememberUpdatedState(onFloatingPreview)
    val commitFloating by rememberUpdatedState(onFloatingCommit)
    CompositionLocalProvider(LocalHomePreviewKeys provides registry) {
        Box(Modifier.fillMaxWidth().onGloballyPositioned { position = it }) {
            content()
            Box(Modifier.matchParentSize().pointerInput(registry,floatingId) {
                if(floatingId!=null) {
                    awaitEachGesture {
                        val down=awaitFirstDown(requireUnconsumed=false);val original=currentFloating?:return@awaitEachGesture
                        down.consume();var next=original;var complete=false
                        try {
                            do {
                                val event=awaitPointerEvent();val pan=event.calculatePan()
                                next=next.transformed(pan.x/size.width.coerceAtLeast(1),pan.y/size.height.coerceAtLeast(1),event.calculateZoom(),event.calculateRotation())
                                previewFloating(next);event.changes.forEach{it.consume()}
                            }while(event.changes.any{it.pressed})
                            complete=true;if(next!=original)commitFloating(next)
                        }finally { previewFloating(null) }
                    }
                } else detectTapGestures { point ->
                    val root = position?.takeIf { it.isAttached }?.localToRoot(point) ?: return@detectTapGestures
                    registry.bounds.values.filter { it.second.contains(root) }.minByOrNull { it.second.width * it.second.height }
                        ?.first?.let(choose)
                }
            })
        }
    }
}

@Composable
internal fun Modifier.homePreviewKeyTarget(): Modifier {
    val registry = LocalHomePreviewKeys.current ?: return this
    val id = LocalHomeKeyId.current ?: return this
    if (id.substringBefore(':') !in setOf("qwerty","t9","toolbar","chrome")) return this
    val token = remember(id) { Any() }
    DisposableEffect(registry, token) { onDispose { registry.bounds.remove(token) } }
    return onGloballyPositioned { registry.bounds[token] = id to it.boundsInRoot() }.drawWithContent {
        drawContent()
        if (registry.selected.value == id && id.substringBefore(':') in setOf("qwerty","t9"))drawRect(Color(0xffa95779).copy(alpha=.08f))
    }
}
