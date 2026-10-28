package com.kingzcheung.xime.ui.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.kingzcheung.xime.appearance.*

internal val LocalHomeKeyShape=staticCompositionLocalOf{HomeKeyShape.ROUNDED}
internal val LocalHomeKeyBorder=staticCompositionLocalOf{HomeBorder()}

internal fun homeFacePath(kind:HomeKeyShape,rect:Rect,radius:Float):Path {
    val box=homeFaceRect(kind,rect.width,rect.height)
    val r=Rect(rect.left+box.left,rect.top+box.top,rect.left+box.left+box.width,rect.top+box.top+box.height)
    return Path().apply{when(kind){
        HomeKeyShape.SQUARE->addRect(r)
        HomeKeyShape.CIRCLE->addRoundRect(RoundRect(r,CornerRadius(minOf(r.width,r.height)/2)))
        HomeKeyShape.ROUNDED->addRoundRect(RoundRect(r,CornerRadius(radius.coerceIn(0f,minOf(r.width,r.height)/2))))
    }}
}

@Composable internal fun homeFaceShape():Shape {
    val kind=LocalHomeKeyShape.current;val radius=LocalKeyCornerRadius.current
    return remember(kind,radius){if(kind==HomeKeyShape.ROUNDED)RoundedCornerShape(radius) else GenericShape{size,_->addPath(homeFacePath(kind,Rect(Offset.Zero,size),0f))}}
}

@Composable internal fun Modifier.homeKeyFace(color:Color):Modifier=background(color,homeFaceShape())

@Composable internal fun Modifier.homeKeyBorder():Modifier=homeBorderDrawing(LocalHomeKeyBorder.current,LocalHomeKeyShape.current,LocalKeyCornerRadius.current.value)

@Composable internal fun Modifier.homeBorderDrawing(border:HomeBorder,shape:HomeKeyShape=HomeKeyShape.ROUNDED,radiusDp:Float=12f):Modifier {
    if(border.kind==HomeBorderKind.NONE)return this
    val dark=LocalHomeDarkAppearance.current;val preset=LocalHomePreset.current
    val fallback=if(dark)preset?.dark?.accent else preset?.light?.accent
    val color=Color((if(dark)border.colorDark else border.colorLight)?:fallback?:0xFFAD5978)
    return drawWithCache {
        val width=border.widthDp.dp.toPx().coerceAtMost(minOf(size.width,size.height)/2)
        val path=homeFacePath(shape,Rect(Offset.Zero,size).deflate(width/2),radiusDp.dp.toPx())
        val stroke=Stroke(width=width,pathEffect=if(border.kind==HomeBorderKind.DASHED)PathEffect.dashPathEffect(floatArrayOf(maxOf(3.dp.toPx(),width*3),maxOf(2.dp.toPx(),width*2)))else null)
        onDrawWithContent{drawContent();drawPath(path,color.copy(alpha=color.alpha*border.opacity),style=stroke)}
    }
}

/** 默认底衬与键帽使用同一路径，新预设没有自定义阴影时也保持形状。 */
@Composable internal fun Modifier.homeDefaultKeyShadow(enabled:Boolean,elevation:androidx.compose.ui.unit.Dp,radius:androidx.compose.ui.unit.Dp,color:Color):Modifier {
    if(!enabled)return this
    val kind=LocalHomeKeyShape.current
    val shadowColor=crispShadowColor(color)
    return drawWithCache {
        val offset=elevation.toPx()
        val path=homeFacePath(kind,Rect(0f,offset,size.width,size.height+offset),radius.toPx())
        onDrawBehind{drawPath(path,shadowColor)}
    }
}
