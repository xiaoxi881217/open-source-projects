package com.kingzcheung.xime.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import com.kingzcheung.xime.appearance.*
import com.kingzcheung.xime.settings.KeysConfigHelper
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun HomePerKeyEditor(preset: HomeVisualPreset, dark: Boolean, onChange: (HomeVisualPreset) -> Unit, protectedDrafts: Collection<HomeVisualPreset> = emptyList(), section:String="all",previewLayout:String="qwerty",onPreviewLayout:(String)->Unit={},selectedKeyId:String?=null,onSelectedKey:(String)->Unit={},extraKeys:Map<String,String>?=null,glyph:(@Composable (String)->Unit)?=null) {
    val layout = previewLayout
    val keyIds = if(layout=="toolbar")extraKeys.orEmpty().keys.toList() else if (layout == "qwerty") {
        (KeysConfigHelper.getRowsLayout(false).rows + KeysConfigHelper.getRowsLayout(true).rows)
            .flatMap { it.keys }
    } else KeysConfigHelper.getT9Layout().let { it.left + it.rows.flatten() + it.right }
    val ids = keyIds.filter { it != "candidates" }.map { HomeKeyIds.token(it) }.distinct()
    fun title(id:String)=extraKeys?.get(id)?:keyLabel(id)
    val selected = selectedKeyId?.takeIf { it.startsWith("$layout:") }?.substringAfter(':')?.takeIf { it in ids } ?: ids.firstOrNull() ?: "a"
    var chooseKey by remember { mutableStateOf(false) }
    var fineGestures by remember { mutableStateOf(false) }
    var hintGesture by remember { mutableStateOf(false) }
    var stickerGesture by remember { mutableStateOf(false) }
    val sourceId = "$layout:$selected"
    val style = preset.keys[sourceId] ?: HomeKeyStyle()
    var stickerIndex by remember(sourceId){mutableIntStateOf(0)}
    val activeSticker=stickerIndex.coerceIn(0,(style.stickers().size-1).coerceAtLeast(0))
    val weightedWidth=layout=="qwerty" || layout=="t9" && selected in KeysConfigHelper.getT9Layout().rows.flatten().map{HomeKeyIds.token(it)}
    val maxWidth=if(weightedWidth)2f else 1f
    val palette = if (dark) preset.dark else preset.light
    var colorEdit by remember { mutableStateOf<HomeStyleProperty?>(null) }
    var sync by remember { mutableStateOf<HomeStyleProperty?>(null) }
    var syncSide by remember { mutableStateOf<HomeShadowSide?>(null) }
    var preciseLabel by remember(sourceId) { mutableStateOf(false) }
    fun set(next: HomeKeyStyle) { onChange(preset.copy(keys = preset.keys + (sourceId to next))) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
            Text(title(selected),Modifier.weight(1f),style=MaterialTheme.typography.titleSmall)
            HomeActionButton(onClick={chooseKey=true}){Text("换一颗")}
            HomeActionButton(onClick={fineGestures=!fineGestures}){Text(if(fineGestures)"收起微调" else "手势微调")}
        }
        if(fineGestures)HomeSingleKeyPreview(preset, sourceId, title(selected), dark,
            onStickerChange={ set(style.withSticker(activeSticker,it)) },onSizeChange=if(layout=="toolbar")null else {next->set(style.copy(widthScale=next.widthScale.coerceAtMost(maxWidth),heightScale=next.heightScale))},onLabelChange=if(layout=="toolbar")null else {next->set(style.copy(labelOffsetX=next.labelOffsetX,labelOffsetY=next.labelOffsetY,labelFontSize=next.labelFontSize))},
            onHintChange=if(layout=="toolbar")null else {next->set(style.copy(hintOffsetX=next.hintOffsetX,hintOffsetY=next.hintOffsetY,hintFontSize=next.hintFontSize))},initialMode=if(hintGesture)"hint" else if(stickerGesture)"sticker" else "press",glyph=if(glyph!=null)({glyph(selected)})else null,stickerIndex=activeSticker,maxKeyWidth=maxWidth)
        if(section in listOf("all","key")){
        if(layout!="toolbar")HomePaperGroup("这颗键的大小") {
            HomeValueSlider("宽度比例",style.widthScale.coerceAtMost(maxWidth),.5f..maxWidth,1f,onSync={sync=HomeStyleProperty.WIDTH}){set(style.copy(widthScale=it))}
            HomeValueSlider("键帽高度",style.heightScale,.4f..1f,1f,onSync={sync=HomeStyleProperty.HEIGHT}){set(style.copy(heightScale=it))}
            HomeSyncAction("同步宽度与键帽高度",{sync=HomeStyleProperty.SIZE},caption="尺寸")
            Text("主键区宽度会重排行内键位；侧栏宽度与所有键帽高度最多填满槽位，收小时触摸槽位保留。",style=MaterialTheme.typography.bodySmall)
        }
        HomeHintEditor(style,palette,dark,::set,{hintGesture=true;stickerGesture=false;fineGestures=true},{sync=it})
        FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
            HomeKeyShape.entries.forEach{kind->HomeChoiceChip((style.shape?:preset.keyShape)==kind,{set(style.copy(shape=kind))},label={Text(kind.label)})}
            HomeSyncAction("同步形状",{sync=HomeStyleProperty.SHAPE})
        }
        HomePaperGroup("这颗键的边框") {
            HomeBorderEditor(style.border?:preset.keyBorder,dark,palette.accent,{set(style.copy(border=it))},{sync=it})
            HomeActionButton(onClick={set(style.copy(border=null,shape=null))}){Text("边框与形状跟随整体")}
        }
        FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
            HomeOutlinedAction(onClick={set(style.copy(labelFontSize=((style.labelFontSize?:18f)-2f).coerceIn(2f,32f)))}){Text("A−")}
            HomeActionButton(onClick={preciseLabel=true}){Text("文字字号 ${style.labelFontSize?.let { "%.1f".format(it) }?:"跟随原设置"}")}
            HomeOutlinedAction(onClick={set(style.copy(labelFontSize=((style.labelFontSize?:18f)+2f).coerceIn(2f,32f)))}){Text("A＋")}
            HomeSyncAction("同步字号",{sync=HomeStyleProperty.LABEL_FONT_SIZE})
        }
        FlowRow {
            HomeSyncAction("同步文字位置",{sync=HomeStyleProperty.LABEL_POSITION},caption="位置")
            HomeActionButton(onClick={set(style.copy(labelOffsetX=0f,labelOffsetY=0f))}){Text("文字回到原位")}
            HomeActionButton(onClick={preciseLabel=!preciseLabel}){Text(if(preciseLabel)"收起精确数值" else "文字精确数值")}
        }
        if(preciseLabel) {
            HomeValueSlider("主文字字号（与候选词分开）",style.labelFontSize?:18f,2f..32f,18f){set(style.copy(labelFontSize=it))}
            HomeActionButton(onClick={set(style.copy(labelFontSize=null))}){Text("字号跟随原设置")}
            HomeValueSlider("文字横向位置",style.labelOffsetX,-.4f.. .4f,0f){set(style.copy(labelOffsetX=it))}
            HomeSyncAction("只同步横向数值",{sync=HomeStyleProperty.LABEL_X})
            HomeValueSlider("文字纵向位置",style.labelOffsetY,-.4f.. .4f,0f){set(style.copy(labelOffsetY=it))}
            HomeSyncAction("只同步纵向数值",{sync=HomeStyleProperty.LABEL_Y})
        }
        }
        if(section in listOf("all","sticker"))HomeKeyStickersEditor(sourceId,preset,protectedDrafts,style,activeSticker,{stickerIndex=it},::set,{sync=it},{fineGestures=true;hintGesture=false;stickerGesture=true})
        if(section in listOf("all","key")){
        HomeValueSlider("底色不透明度",style.keyOpacity,0f..1f,1f,onSync={sync=HomeStyleProperty.KEY_OPACITY}){set(style.copy(keyOpacity=it))}
        HomeValueSlider("键帽磨砂度",style.frost,0f..1f,0f,onSync={sync=HomeStyleProperty.FROST}){set(style.copy(frost=it))}
        Text("降低底色不透明度，才能看见背后的壁纸和磨砂；字母、贴纸保持清晰。",style=MaterialTheme.typography.bodySmall)
        val backgroundProperty = if (dark) HomeStyleProperty.BACKGROUND_DARK else HomeStyleProperty.BACKGROUND_LIGHT
        val textProperty = if (dark) HomeStyleProperty.TEXT_DARK else HomeStyleProperty.TEXT_LIGHT
        listOf(backgroundProperty to "按键颜色", textProperty to "文字颜色").forEach { (property, title) ->
            Row {
                HomeOutlinedAction(onClick = { colorEdit = property }, modifier = Modifier.weight(1f)) { Text(title) }
                HomeSyncAction("同步$title",{sync = property})
            }
        }
        HomeValueSlider("大字不透明度",style.textOpacity,0f..1f,1f,onSync={sync=HomeStyleProperty.TEXT_OPACITY}){set(style.copy(textOpacity=it))}
        if((style.shape?:preset.keyShape)!=HomeKeyShape.CIRCLE)HomeValueSlider("这颗键的圆角", (style.cornerRadius ?: preset.cornerRadius).toFloat(), 0f..24f, preset.cornerRadius.toFloat()) {
            set(style.copy(cornerRadius = it.roundToInt()))
        }
        Row {
            HomeActionButton(onClick = { set(style.copy(cornerRadius=null)) }) { Text("圆角跟随整体") }
            HomeSyncAction("只同步圆角数值",{sync = HomeStyleProperty.CORNER_RADIUS})
            HomeSyncAction("同步这颗键的整套样式",{sync = HomeStyleProperty.ALL},caption="整套样式")
        }
        HomeActionButton(onClick = { onChange(preset.copy(keys = preset.keys - sourceId)) }) { Text("这颗键恢复跟随主题") }
        }
        if(section in listOf("all","shadow","motion"))HomeKeyEffectsEditor(style, preset.animationsEnabled, ::set,
            { onChange(preset.copy(animationsEnabled = it)) }, { property, side -> syncSide = side; sync = property },section=section,
            pressPopupEnabled=preset.pressPopupEnabled,onPressPopupChanged={onChange(preset.copy(pressPopupEnabled=it))})
        if(layout!="toolbar"&&preset.panelStyle!=null)HomeActionButton(onClick={onChange(preset.copy(panelStyle=null))}){Text("数字 / 符号恢复默认美化")}
        if(section in listOf("key","all"))Text("整套同步包含浅深色按键、文字、圆角、四边阴影、动态和贴纸。未单独设定的颜色同步为“跟随主题”，不会修改动作、长按或布局。",
            style = MaterialTheme.typography.bodySmall)
    }
    if(chooseKey)AlertDialog(onDismissRequest={chooseKey=false},title={Text("选一颗")},text={
        FlowRow(Modifier.verticalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)){
            ids.forEach{id->HomeChoiceChip(selected==id,{onSelectedKey("$layout:$id");chooseKey=false},label={Text(title(id))})}
        }
    },confirmButton={HomeActionButton(onClick={chooseKey=false}){Text("好了")}})
    colorEdit?.let { property ->
        val initial = when (property) {
            HomeStyleProperty.BACKGROUND_LIGHT -> style.backgroundLight ?: preset.light.key
            HomeStyleProperty.BACKGROUND_DARK -> style.backgroundDark ?: preset.dark.key
            HomeStyleProperty.TEXT_LIGHT -> style.textLight ?: preset.light.text
            else -> style.textDark ?: preset.dark.text
        }
        HomeColorDialog(initial, { colorEdit = null }) { color ->
            set(when (property) {
                HomeStyleProperty.BACKGROUND_LIGHT -> style.copy(backgroundLight = color)
                HomeStyleProperty.BACKGROUND_DARK -> style.copy(backgroundDark = color)
                HomeStyleProperty.TEXT_LIGHT -> style.copy(textLight = color)
                else -> style.copy(textDark = color)
            })
            colorEdit = null
        }
    }
    sync?.let { property ->
        var targets by remember(sourceId, property) { mutableStateOf(emptySet<String>()) }
        var gestureMode by remember { mutableStateOf(true) }
        var gridCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
        val bounds = remember { mutableStateMapOf<String, HomeSelectionRect>() }
        val edge = syncSide.takeIf { property.name.startsWith("SHADOW_") && property != HomeStyleProperty.SHADOW_SHAPE }
        AlertDialog(onDismissRequest = { sync = null }, title = { Text("同步 · ${edge?.label.orEmpty()}${property.label}") },
            text = {
                Column(Modifier.heightIn(max = 380.dp).verticalScroll(rememberScrollState())) {
                    Text("从 ${title(selected)} 同步到下列按键。其他属性保持原样，可用页面顶部的撤销恢复。")
                    if(property.name.startsWith("STICKER") && property!=HomeStyleProperty.STICKERS)Text("来源：第${activeSticker+1}张；目标改相同编号，缺少该编号时数值同步会跳过，图片/整张同步会追加。",style=MaterialTheme.typography.bodySmall)
                    if(property==HomeStyleProperty.STICKERS)Text("会替换目标键的全部贴纸，其他样式保留。",style=MaterialTheme.typography.bodySmall)
                    Row {
                        HomeActionButton(onClick = { targets = ids.toSet() }) { Text(if(layout=="toolbar")"全选快捷图标" else "全选本键盘") }
                        HomeActionButton(onClick = { targets = emptySet() }) { Text("取消全选") }
                    }
                    HomeChoiceChip(gestureMode, { gestureMode = !gestureMode }, label = { Text("划过多选") })
                    Text(if (gestureMode) "划过即可选择；从已选按键开始划，会一路取消。关闭此模式可恢复页面滚动。"
                        else "可以点选，也可以打开“划过多选”用手势选择。")
                    FlowRow(modifier = Modifier.onGloballyPositioned { gridCoordinates = it }
                        .pointerInput(gestureMode, ids) {
                            if (!gestureMode) return@pointerInput
                            var previous = Offset.Zero
                            var adding = true
                            var strokeActive = false
                            var beforeStroke = emptySet<String>()
                            val visited = mutableSetOf<String>()
                            fun applyStroke(point: Offset) {
                                val hits = bounds.filter { (id, rect) -> id in ids &&
                                    rect.crosses(previous.x, previous.y, point.x, point.y) }.keys - visited
                                visited.addAll(hits)
                                targets = if (adding) targets + hits else targets - hits
                                previous = point
                            }
                            detectDragGestures(orientationLock = null, onDragStart = { down, trigger, _ ->
                                strokeActive = false
                                val coordinates = gridCoordinates?.takeIf { it.isAttached } ?: return@detectDragGestures
                                beforeStroke = targets
                                strokeActive = true
                                previous = coordinates.localToRoot(down.position)
                                visited.clear()
                                val first = bounds.entries.firstOrNull { (_, rect) ->
                                    rect.crosses(previous.x, previous.y, previous.x, previous.y) }?.key
                                adding = first !in targets
                                applyStroke(coordinates.localToRoot(trigger.position))
                            }, onDragEnd = {
                                strokeActive = false
                            }, onDragCancel = {
                                if (strokeActive) targets = beforeStroke
                                strokeActive = false
                            }, onDrag = { change, _ ->
                                if (!strokeActive) return@detectDragGestures
                                val coordinates = gridCoordinates?.takeIf { it.isAttached } ?: return@detectDragGestures
                                applyStroke(coordinates.localToRoot(change.position))
                                change.consume()
                            })
                        }, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ids.forEach { id ->
                            HomeChoiceChip(id in targets, { targets = if (id in targets) targets - id else targets + id },
                                modifier = Modifier.onGloballyPositioned { coordinates ->
                                    val rect = coordinates.boundsInRoot()
                                    bounds[id] = HomeSelectionRect(rect.left, rect.top, rect.right, rect.bottom)
                                }, label = { Text(title(id)) })
                        }
                    }
                    Text(if(layout=="toolbar")"将改动 ${targets.size} 个快捷图标。" else "将改动 ${targets.size} 颗按键；只作用于当前键盘类型。")
                }
            }, confirmButton = {
                HomeActionButton(enabled = targets.isNotEmpty(), onClick = {
                    onChange(preset.synchronize(sourceId, targets.map { "$layout:$it" }.toSet(), property, edge,activeSticker))
                    sync = null
                }) { Text("确认同步") }
            }, dismissButton = { HomeActionButton(onClick = { sync = null }) { Text("先不改") } })
    }
}

private fun keyLabel(id: String): String = when (id) {
    "period" -> "。"; "question" -> "？"; "exclamation" -> "！"; "semicolon" -> "；"; "colon" -> "："; "space" -> "空格"; "delete" -> "删除"; "enter" -> "回车"; "earth" -> "中/英"; "shift" -> "Shift"
    "mode_change" -> "数字符号"; "comma" -> "逗号"; "symbol" -> "符号"; "number" -> "数字"; "clear" -> "清空"
    else -> id.uppercase(java.util.Locale.ROOT)
}
