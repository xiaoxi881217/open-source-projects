package com.kingzcheung.xime.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable internal fun Modifier.homeControlOutline(selected:Boolean=false,color:Color=if(selected)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline):Modifier =
    drawWithContent {
        drawContent()
        val stroke=1.dp.toPx();val radius=minOf(16.dp.toPx(),size.minDimension/2)
        drawRoundRect(color.copy(alpha=if(selected).95f else .7f),Offset(stroke/2,stroke/2),
            androidx.compose.ui.geometry.Size((size.width-stroke).coerceAtLeast(0f),(size.height-stroke).coerceAtLeast(0f)),
            CornerRadius(radius),style=Stroke(stroke,pathEffect=PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(),3.dp.toPx()))))
    }

@Composable internal fun HomeActionButton(onClick:()->Unit,modifier:Modifier=Modifier,enabled:Boolean=true,
    shape:Shape=RoundedCornerShape(16.dp),colors:ButtonColors=ButtonDefaults.textButtonColors(),
    contentPadding:PaddingValues=ButtonDefaults.TextButtonContentPadding,content:@Composable RowScope.()->Unit) {
    TextButton(onClick,modifier.homeControlOutline(),enabled,shape=shape,colors=colors,contentPadding=contentPadding,content=content)
}
@Composable internal fun HomeOutlinedAction(onClick:()->Unit,modifier:Modifier=Modifier,enabled:Boolean=true,
    shape:Shape=RoundedCornerShape(16.dp),colors:ButtonColors=ButtonDefaults.outlinedButtonColors(),
    contentPadding:PaddingValues=ButtonDefaults.ContentPadding,content:@Composable RowScope.()->Unit) {
    OutlinedButton(onClick,modifier.homeControlOutline(),enabled,shape=shape,colors=colors,border=null,contentPadding=contentPadding,content=content)
}
@Composable internal fun HomeIconAction(onClick:()->Unit,modifier:Modifier=Modifier,enabled:Boolean=true,content:@Composable ()->Unit) {
    IconButton(onClick,modifier.homeControlOutline(),enabled,content=content)
}
@Composable internal fun HomeChoiceChip(selected:Boolean,onClick:()->Unit,label:@Composable ()->Unit,modifier:Modifier=Modifier,enabled:Boolean=true) {
    FilterChip(selected,onClick,label,modifier.homeControlOutline(selected),enabled=enabled,shape=RoundedCornerShape(16.dp),border=null)
}
@Composable internal fun HomePrimaryAction(onClick:()->Unit,modifier:Modifier=Modifier,enabled:Boolean=true,
    shape:Shape=RoundedCornerShape(16.dp),colors:ButtonColors=ButtonDefaults.buttonColors(),content:@Composable RowScope.()->Unit) {
    Button(onClick,modifier.homeControlOutline(color=MaterialTheme.colorScheme.onPrimary),enabled,shape=shape,colors=colors,content=content)
}
@Composable internal fun HomeTonalAction(onClick:()->Unit,modifier:Modifier=Modifier,enabled:Boolean=true,
    shape:Shape=RoundedCornerShape(16.dp),colors:ButtonColors=ButtonDefaults.filledTonalButtonColors(),content:@Composable RowScope.()->Unit) {
    FilledTonalButton(onClick,modifier.homeControlOutline(color=MaterialTheme.colorScheme.onSecondaryContainer),enabled,shape=shape,colors=colors,content=content)
}
