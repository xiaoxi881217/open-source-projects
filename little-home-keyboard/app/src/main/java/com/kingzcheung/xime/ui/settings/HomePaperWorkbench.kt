package com.kingzcheung.xime.ui.settings

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.kingzcheung.xime.R
import kotlin.math.roundToInt

@Composable
internal fun HomePaperFrame(content:@Composable ()->Unit){
    val image=ImageBitmap.imageResource(R.drawable.atelier_dot_stars)
    val paper=MaterialTheme.colorScheme.background
    Box(Modifier.fillMaxSize().drawBehind{
        drawRect(paper)
        val scale=maxOf(size.width/image.width,size.height/image.height)
        val w=(image.width*scale).roundToInt();val h=(image.height*scale).roundToInt()
        drawImage(image,dstOffset=IntOffset(((size.width-w)/2).roundToInt(),((size.height-h)/2).roundToInt()),
            dstSize=IntSize(w,h),alpha=.1f,blendMode=BlendMode.Multiply)
    }){content()}
}

@Composable
internal fun HomePaperHeader(busy:Boolean,onBack:()->Unit,canUndo:Boolean,canRedo:Boolean,onUndo:()->Unit,onRedo:()->Unit,onTrial:()->Unit){
    Column(Modifier.background(MaterialTheme.colorScheme.background).windowInsetsPadding(WindowInsets.statusBars)){
        Row(Modifier.fillMaxWidth().height(52.dp).padding(horizontal=4.dp),verticalAlignment=Alignment.CenterVertically){
            HomeIconAction(enabled=!busy,onClick=onBack){Icon(Icons.AutoMirrored.Filled.ArrowBack,"返回",Modifier.size(20.dp))}
            Text("装扮小家",Modifier.weight(1f),fontFamily=FontFamily.Serif,color=MaterialTheme.colorScheme.onBackground,fontSize=21.sp,letterSpacing=1.5.sp,maxLines=1,overflow=TextOverflow.Ellipsis)
            HomeIconAction(enabled=!busy&&canUndo,onClick=onUndo){Icon(Icons.Default.Undo,"撤销",Modifier.size(19.dp))}
            HomeIconAction(enabled=!busy&&canRedo,onClick=onRedo){Icon(Icons.Default.Redo,"重做",Modifier.size(19.dp))}
            HomeIconAction(enabled=!busy,onClick=onTrial){Icon(Icons.Default.Keyboard,"试打",Modifier.size(19.dp))}
        }
        HomePaperRule()
    }
}

@Composable
internal fun HomePaperRule(){
    val line=MaterialTheme.colorScheme.outlineVariant
    Canvas(Modifier.fillMaxWidth().height(1.dp)){
        drawLine(line,Offset(0f,0f),Offset(size.width,0f),strokeWidth=1.dp.toPx(),pathEffect=PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(),4.dp.toPx())))
    }
}

@Composable
internal fun HomePaperTabs(selected:String,onSelect:(String)->Unit){
    Column{
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(2.dp)){
            listOf("overall" to "整体","key" to "按键","shadow" to "阴影","sticker" to "贴纸","motion" to "动态","chrome" to "上方","presets" to "预设").forEach{(id,name)->
                val color=if(selected==id)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                HomeActionButton(onClick={onSelect(id)},contentPadding=PaddingValues(horizontal=8.dp),modifier=Modifier.widthIn(min=44.dp).height(44.dp).drawBehind{
                    if(selected==id)drawLine(color,Offset(5.dp.toPx(),size.height-2.dp.toPx()),Offset(size.width-5.dp.toPx(),size.height-2.dp.toPx()),1.dp.toPx(),pathEffect=PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(),3.dp.toPx())))
                }){Text(name,color=color,fontSize=12.sp)}
            }
        }
        HomePaperRule()
    }
}

@Composable
internal fun HomePaperGroup(title:String,content:@Composable ColumnScope.()->Unit){
    Surface(color=MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha=.78f),shape=RoundedCornerShape(20.dp),
        border=BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant.copy(alpha=.7f))){
        Column(Modifier.fillMaxWidth().padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){
                Text(title,style=MaterialTheme.typography.titleSmall,color=MaterialTheme.colorScheme.primary)
                Box(Modifier.weight(1f)){HomePaperRule()}
            }
            content()
        }
    }
}

@Composable
internal fun HomePaperSwatch(label:String,color:Long,modifier:Modifier=Modifier,onClick:()->Unit){
    Surface(onClick=onClick,modifier=modifier.homeControlOutline(),shape=RoundedCornerShape(16.dp),color=Color.Transparent){
        Row(Modifier.heightIn(min=48.dp).padding(horizontal=10.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){
            Box(Modifier.size(24.dp).background(Color(color),CircleShape).border(1.dp,MaterialTheme.colorScheme.outlineVariant,CircleShape))
            Text(label,Modifier.weight(1f),fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
