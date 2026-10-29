package com.kingzcheung.xime.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.dp

/** 只给文字操作加边界；预览、图标与变色选项不套这层。 */
@Composable internal fun Modifier.homeControlOutline(shape:Shape=RoundedCornerShape(16.dp),enabled:Boolean=true):Modifier {
    val color=MaterialTheme.colorScheme.outline.copy(alpha=if(enabled).65f else .25f)
    return drawWithContent {
        drawContent()
        val stroke=1.dp.toPx()
        val outline=shape.createOutline(androidx.compose.ui.geometry.Size((size.width-stroke).coerceAtLeast(0f),(size.height-stroke).coerceAtLeast(0f)),layoutDirection,this)
        val path=Path().apply{when(outline){is Outline.Rectangle->addRect(outline.rect);is Outline.Rounded->addRoundRect(outline.roundRect);is Outline.Generic->addPath(outline.path)}}
        withTransform({translate(stroke/2,stroke/2)}){drawPath(path,color,style=Stroke(stroke,pathEffect=PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(),3.dp.toPx()))))}
    }
}

@Composable internal fun HomeActionButton(onClick:()->Unit,modifier:Modifier=Modifier,enabled:Boolean=true,
    shape:Shape=RoundedCornerShape(16.dp),colors:ButtonColors=ButtonDefaults.textButtonColors(),
    contentPadding:PaddingValues=ButtonDefaults.TextButtonContentPadding,content:@Composable RowScope.()->Unit) {
    TextButton(onClick,modifier.homeControlOutline(shape,enabled),enabled,shape=shape,colors=colors,contentPadding=contentPadding,content=content)
}
@Composable internal fun HomeOutlinedAction(onClick:()->Unit,modifier:Modifier=Modifier,enabled:Boolean=true,
    shape:Shape=RoundedCornerShape(16.dp),colors:ButtonColors=ButtonDefaults.outlinedButtonColors(),
    contentPadding:PaddingValues=ButtonDefaults.ContentPadding,content:@Composable RowScope.()->Unit) {
    OutlinedButton(onClick,modifier.homeControlOutline(shape,enabled),enabled,shape=shape,colors=colors,border=null,contentPadding=contentPadding,content=content)
}
@Composable internal fun HomeIconAction(onClick:()->Unit,modifier:Modifier=Modifier,enabled:Boolean=true,content:@Composable ()->Unit) {
    IconButton(onClick,modifier,enabled,content=content)
}
@Composable internal fun HomeChoiceChip(selected:Boolean,onClick:()->Unit,label:@Composable ()->Unit,modifier:Modifier=Modifier,enabled:Boolean=true) {
    FilterChip(selected,onClick,label,modifier,enabled=enabled,shape=RoundedCornerShape(16.dp),border=null)
}
@Composable internal fun HomePrimaryAction(onClick:()->Unit,modifier:Modifier=Modifier,enabled:Boolean=true,
    shape:Shape=RoundedCornerShape(16.dp),colors:ButtonColors=ButtonDefaults.buttonColors(),content:@Composable RowScope.()->Unit) {
    Button(onClick,modifier,enabled,shape=shape,colors=colors,content=content)
}
@Composable internal fun HomeTonalAction(onClick:()->Unit,modifier:Modifier=Modifier,enabled:Boolean=true,
    shape:Shape=RoundedCornerShape(16.dp),colors:ButtonColors=ButtonDefaults.filledTonalButtonColors(),content:@Composable RowScope.()->Unit) {
    FilledTonalButton(onClick,modifier,enabled,shape=shape,colors=colors,content=content)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable internal fun HomeSyncAction(description:String,onClick:()->Unit,caption:String?=null,modifier:Modifier=Modifier) {
    TooltipBox(positionProvider=TooltipDefaults.rememberPlainTooltipPositionProvider(),tooltip={PlainTooltip{Text(description)}},state=rememberTooltipState()) {
        if(caption==null)IconButton(onClick,modifier.size(40.dp)){Icon(Icons.Default.Sync,description,Modifier.size(18.dp),tint=MaterialTheme.colorScheme.primary)}
        else FilledTonalButton(onClick,modifier,contentPadding=PaddingValues(horizontal=8.dp)){
            Icon(Icons.Default.Sync,description,Modifier.size(18.dp));Spacer(Modifier.width(4.dp));Text(caption,style=MaterialTheme.typography.labelMedium)
        }
    }
}
