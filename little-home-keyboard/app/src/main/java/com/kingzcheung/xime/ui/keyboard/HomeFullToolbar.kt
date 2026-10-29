package com.kingzcheung.xime.ui.keyboard

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.kingzcheung.xime.R
import com.kingzcheung.xime.appearance.*
import com.kingzcheung.xime.keyboard.ToolbarButtonItem

/** 编辑与实际快捷栏共用整行按钮；换位移动真实点击位置。 */
@Composable internal fun HomeFullToolbar(items:List<ToolbarButtonItem>,order:List<String>,tint:Color,accent:Color,
    onClick:(String)->Unit,showMore:Boolean=true,showHide:Boolean=true,
    onMove:((String,String)->Unit)?=null,onScale:((Float)->Unit)?=null) {
    val ids=homeToolbarOrder(items.map{it.id},order,showMore,showHide)
    val bounds=remember{mutableStateMapOf<String,Rect>()}
    val move by rememberUpdatedState(onMove);val scale by rememberUpdatedState(onScale)
    var point by remember{mutableStateOf(Offset.Zero)}
    val transform=if(onScale==null)Modifier else Modifier.pointerInput(Unit){
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed=false)
            do {
                val event=awaitPointerEvent()
                if(event.changes.count{it.pressed}>=2){
                    val zoom=event.calculateZoom()
                    if(zoom.isFinite() && zoom>0f && zoom!=1f)scale?.invoke(zoom)
                    event.changes.forEach{it.consume()}
                }
            }while(event.changes.any{it.pressed})
        }
    }
    BoxWithConstraints(Modifier.fillMaxWidth().height(44.dp).then(transform)) {
        val width=maxWidth
        Row(Modifier.horizontalScroll(rememberScrollState()).widthIn(min=width).fillMaxHeight(),
            horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
            ids.forEach { id -> key(id) {
                val item=items.firstOrNull{it.id==id}
                val description=when(id){"more_menu"->"打开更多";"hide_keyboard"->"收起键盘";else->item?.label?:id}
                val interactions=remember{MutableInteractionSource()};val pressed by interactions.collectIsPressedAsState()
                val drag=if(onMove==null)Modifier else Modifier.pointerInput(id) {
                    detectDragGesturesAfterLongPress(onDragStart={point=bounds[id]?.center?:Offset.Zero},onDrag={change,delta->
                        change.consume();point+=delta
                        val target=bounds.entries.firstOrNull{it.key!=id && it.value.contains(point)}?.key
                        if(target!=null)move?.invoke(id,target)
                    })
                }
                Box(Modifier.size(40.dp).onGloballyPositioned{bounds[id]=it.boundsInRoot()}.then(drag)
                    .tolerantClick(showRipple=false,interactionSource=interactions,onClick={onClick(id)}),contentAlignment=Alignment.Center) {
                    HomeKeyStyleScope("toolbar:"+HomeKeyIds.token(id)) {
                        val style=LocalHomeKeyStyle.current
                        val background=(if(LocalHomeDarkAppearance.current)style?.backgroundDark else style?.backgroundLight)?.let{Color(it)}?:Color.Transparent
                        Box(Modifier.size(32.dp).homeKeyEffects(pressed).clip(homeFaceShape()).homeKeyFrost()
                            .homeKeyFace(homeKeyBackground(background)).homeKeyInnerShadows(),contentAlignment=Alignment.Center) {
                            val color=homeKeyText(if(pressed)tint.copy(alpha=.6f) else if(id=="more_menu")accent else tint)
                            when(id) {
                                "more_menu"->HomeReplacementIcon(description){Icon(painterResource(R.drawable.ic_home_more),description,tint=color,modifier=Modifier.size(20.dp))}
                                "hide_keyboard"->HomeReplacementIcon(description){Icon(Icons.Default.KeyboardArrowDown,description,tint=color,modifier=Modifier.size(22.dp))}
                                else->if(item!=null)ToolbarButtonGlyph(item,color,description)
                            }
                        }
                    }
                }
                DisposableEffect(id){onDispose{bounds.remove(id)}}
            } }
        }
    }
}
