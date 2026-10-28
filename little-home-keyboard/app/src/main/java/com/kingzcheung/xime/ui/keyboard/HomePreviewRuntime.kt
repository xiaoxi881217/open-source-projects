package com.kingzcheung.xime.ui.keyboard

import com.kingzcheung.xime.viewmodel.ShiftMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 只记录绘制状态，绝不保存按键、候选词、编辑框或剪贴板内容。 */
internal data class HomePreviewState(val layout:KeyboardLayoutState=KeyboardLayoutState.Chinese,val dark:Boolean=false,val shifted:Boolean=false,val shift:ShiftMode=ShiftMode.OFF,val height:Int=240,val width:Float=0f,val toolbarItems:List<com.kingzcheung.xime.keyboard.ToolbarButtonItem> = emptyList(),
 val schemaName:String="中文",val enterLabel:String="回车")
internal object HomePreviewRuntime {
 private val current=MutableStateFlow<HomePreviewState?>(null)
 val state=current.asStateFlow()
 fun update(value:HomePreviewState){val next=current.value?.let{value.copy(height=it.height,width=it.width)}?:value;if(current.value!=next)current.value=next}
 fun updateSize(width:Float,height:Float){if(width.isFinite() && height.isFinite() && width>0 && height>0){val old=current.value?:HomePreviewState();val next=old.copy(width=width,height=height.toInt().coerceIn(80,600));if(next!=old)current.value=next}}
}
