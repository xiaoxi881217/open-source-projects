package com.kingzcheung.xime.ui.settings

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.kingzcheung.xime.appearance.HomeEditorViewport
import kotlin.math.roundToInt

@Composable
internal fun HomeAtelierPreview(percent: Int,onPercent: (Int)->Unit,naturalWidthDp:Float?=null,settingsContent:@Composable ColumnScope.()->Unit={},content: @Composable ()->Unit) {
    var visible by rememberSaveable { mutableStateOf(true) }
    var options by rememberSaveable { mutableStateOf(false) }
    if (!visible) {
        HomeIconAction(onClick={visible=true},modifier=Modifier.height(40.dp)) { Icon(Icons.Default.Visibility,"展开预览") }
        return
    }
    if(options)AlertDialog(onDismissRequest={options=false},title={Text("预览尺寸")},text={
        Column {
            Row { HomeActionButton(onClick={onPercent(10)}){Text("迷你")};HomeActionButton(onClick={onPercent(14)}){Text("紧凑")};HomeActionButton(onClick={onPercent(28)}){Text("放大")}}
            Slider(percent.toFloat(),{onPercent(it.roundToInt())},valueRange=10f..40f)
            HorizontalDivider()
            settingsContent()
        }
    },confirmButton={HomeActionButton(onClick={options=false}){Text("好了")}})
    val height=HomeEditorViewport.frameHeight(LocalConfiguration.current.screenHeightDp.toFloat(),percent)
    Surface(shape=RoundedCornerShape(12.dp),color=MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier=Modifier.fillMaxWidth().border(1.dp,MaterialTheme.colorScheme.outlineVariant,RoundedCornerShape(12.dp))) {
        Box {
        Layout(content=content,modifier=Modifier.fillMaxWidth().height(height.dp).padding(4.dp)) { children,constraints->
            val width=naturalWidthDp?.takeIf{it.isFinite() && it>0}?.dp?.roundToPx()?:constraints.maxWidth
            val child=children.single().measure(Constraints.fixedWidth(width))
            val scale=HomeEditorViewport.scale(child.width.toFloat(),child.height.toFloat(),constraints.maxWidth.toFloat(),constraints.maxHeight.toFloat())
            layout(constraints.maxWidth,constraints.maxHeight) {
                child.placeRelativeWithLayer(((constraints.maxWidth-child.width*scale)/2).roundToInt(),((constraints.maxHeight-child.height*scale)/2).roundToInt()) {
                    scaleX=scale;scaleY=scale;transformOrigin=TransformOrigin(0f,0f)
                }
            }
        }
        Row(Modifier.align(Alignment.TopEnd)) {
            HomeIconAction(onClick={options=true},modifier=Modifier.size(40.dp)){Icon(Icons.Default.Tune,"调整预览尺寸",modifier=Modifier.size(18.dp))}
            HomeIconAction(onClick={visible=false},modifier=Modifier.size(40.dp)){Icon(Icons.Default.VisibilityOff,"收起预览",modifier=Modifier.size(18.dp))}
        }
        }
    }
}
