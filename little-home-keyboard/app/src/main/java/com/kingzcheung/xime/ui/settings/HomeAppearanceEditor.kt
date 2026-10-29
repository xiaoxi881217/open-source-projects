package com.kingzcheung.xime.ui.settings

import androidx.compose.material.icons.filled.Refresh
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import com.kingzcheung.xime.ui.keyboard.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.graphics.toArgb
import com.kingzcheung.xime.settings.KeysConfigHelper
import com.kingzcheung.xime.ui.theme.KeyboardThemes
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kingzcheung.xime.appearance.HomePalette
import com.kingzcheung.xime.appearance.HomePresetCodec
import com.kingzcheung.xime.appearance.HomeStickerAssets
import com.kingzcheung.xime.appearance.HomeStickerBundleCodec
import com.kingzcheung.xime.appearance.HomePresetStore
import com.kingzcheung.xime.appearance.HomeVisualPreset
import com.kingzcheung.xime.appearance.HomeKeyShape
import com.kingzcheung.xime.appearance.HomeKeyIds
import com.kingzcheung.xime.appearance.homeToolbarTargets
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.ui.res.painterResource
import com.kingzcheung.xime.settings.SettingsPreferences
import com.kingzcheung.xime.ui.theme.keyboardBackground
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeAppearanceEditor(onBack: () -> Unit, onApplied: () -> Unit) {
    com.kingzcheung.xime.ui.theme.HomeAtelierTheme(isSystemInDarkTheme(),followKeyboard=false){HomeAppearanceEditorContent(onBack,onApplied)}
}

@OptIn(ExperimentalMaterial3Api::class,ExperimentalLayoutApi::class)
@Composable
private fun HomeAppearanceEditorContent(onBack: () -> Unit, onApplied: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val viewPrefs = remember { context.getSharedPreferences("home-editor-view", android.content.Context.MODE_PRIVATE) }
    var previewPercent by remember { mutableIntStateOf(viewPrefs.getInt("preview_compact_percent",14).coerceIn(10,40)) }
    val live by HomePreviewRuntime.state.collectAsStateWithLifecycle()
    val systemDark=isSystemInDarkTheme()
    val fallbackDark=when(SettingsPreferences.getDarkMode(context)){1->true;2->systemDark;else->false}
    val fallbackLayout=if(isT9Schema(SettingsPreferences.getCurrentSchema(context)))KeyboardLayoutState.T9Pinyin else KeyboardLayoutState.Chinese
    var freeStickers by remember { mutableStateOf(false) }
    var floatingId by remember { mutableStateOf<String?>(null) }
    var floatingGesture by remember { mutableStateOf<com.kingzcheung.xime.appearance.HomeSticker?>(null) }
    var previewChrome by remember { mutableStateOf("toolbar") }
    var selectedPreviewKey by remember { mutableStateOf<String?>(null) }
    var layoutOverride by remember{mutableStateOf<KeyboardLayoutState?>(null)}
    val previewState=(live?:HomePreviewState(layout=fallbackLayout,dark=fallbackDark,toolbarItems=SettingsPreferences.getToolbarButtons(context).mapNotNull{com.kingzcheung.xime.keyboard.ToolbarButton.fromId(it)?.let{b->com.kingzcheung.xime.keyboard.ToolbarButtonItem.Builtin(b)}})).let{it.copy(layout=layoutOverride?:it.layout)}
    fun currentAppearance():HomeVisualPreset {
        HomePresetStore.active?.let{return it}
        fun color(c:Color)=c.toArgb().toLong() and 0xffffffffL
        fun palette(dark:Boolean):HomePalette{val p=resolveKeyboardPalette(HomePresetStore.activeId,dark);return HomePalette(color(p.keyboardBackgroundColor),color(p.keyboardBackgroundColor),color(p.keyBackgroundColor),color(p.specialKeyBackgroundColor),color(p.keyTextColor),color(KeyboardThemes.getCandidateTextColorOverride(HomePresetStore.activeId,dark)?:p.keyTextColor),color(KeyboardThemes.getAccentColor(HomePresetStore.activeId,dark)))}
        val key=KeysConfigHelper.getKeyboardKeyConfig()
        return HomeVisualPreset(name="当前键盘的装扮",light=palette(false),dark=palette(true),cornerRadius=key.cornerRadius.coerceIn(0,24))
    }
    var id by remember { mutableStateOf(HomePresetStore.activeId.takeIf { it in HomePresetStore.presets }) }
    var draft by remember { mutableStateOf(id?.let { HomePresetStore.presets[it] } ?: currentAppearance()) }
    var baseline by remember{mutableStateOf(draft)}
    val undo = remember { mutableStateListOf<HomeVisualPreset>() }
    val redo = remember { mutableStateListOf<HomeVisualPreset>() }
    var dark by remember { mutableStateOf(live?.dark?:fallbackDark) }
    var followDark by remember{mutableStateOf(true)}
    LaunchedEffect(live?.dark,fallbackDark){if(followDark)dark=live?.dark?:fallbackDark}
    var section by remember { mutableStateOf("overall") }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    var colorTarget by remember { mutableStateOf<String?>(null) }
    var deleteConfirm by remember { mutableStateOf(false) }
    var feedbackPage by remember { mutableStateOf(false) }
    var showTrial by remember { mutableStateOf(false) }
    var showDiyImport by remember { mutableStateOf(false) }
    var testText by remember { mutableStateOf("") }
    fun change(next: HomeVisualPreset) {
        if (busy) return
        if (next == draft) return
        if (undo.size >= 100) undo.removeAt(0)
        undo.add(draft)
        redo.clear()
        draft = next
    }
    fun load(nextId: String?, next: HomeVisualPreset) {
        id = nextId
        draft = next
        baseline = next
        undo.clear()
        redo.clear()
    }
    fun save(saving:HomeVisualPreset,symbols:Boolean=false) {
        if(busy)return
        val replacingId=id
        busy=true
        scope.launch {
            runCatching { withContext(Dispatchers.IO) { HomePresetStore.save(saving,replacingId) } }
                .onSuccess { saved ->
                    id=saved;baseline=saving
                    SettingsPreferences.setKeyboardTheme(context,saved);onApplied()
                    status=if(symbols)"数字、常用符号和展开符号键盘都已同步并应用" else "已保存并应用"
                    if(symbols)android.widget.Toast.makeText(context,status,android.widget.Toast.LENGTH_SHORT).show()
                }.onFailure { status="保存没有完成，原来的外观仍然保留" }
            busy=false
        }
    }
    LaunchedEffect(HomePresetStore.activeId,HomePresetStore.active){
        if(!busy && draft==baseline)load(HomePresetStore.activeId.takeIf{it in HomePresetStore.presets},currentAppearance())
        else if(!busy)status="正在使用的外观已变化，未保存草稿先为你留着；可点重新读取切回。"
    }
    val currentDraft by rememberUpdatedState(draft)
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            busy = true
            status = runCatching {
                withContext(Dispatchers.IO) {
                    val bytes = HomeStickerAssets.export(currentDraft)
                    requireNotNull(context.contentResolver.openOutputStream(uri, "wt")).use { it.write(bytes) }
                }
                "完整装扮已经导出，包含贴纸图片，不含连接或剪贴板内容"
            }.getOrElse { "导出没有完成，请检查文件位置" }
            busy = false
        }
    }
    val import = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            busy = true
            runCatching {
                withContext(Dispatchers.IO) {
                    requireNotNull(context.contentResolver.openInputStream(uri)).use {
                        HomeStickerAssets.install(com.kingzcheung.xime.appearance.HomeDiyCode.readImport(it))
                    }
                }
            }.onSuccess { load(null, it); status = "已导入，保存后应用到键盘" }
                .onFailure { status = "这份文件无法导入：请检查版本、大小和外观参数" }
            busy = false
        }
    }
    androidx.activity.compose.BackHandler(feedbackPage){feedbackPage=false}
    if(feedbackPage){KeyEffectSettingsContent(onBack={feedbackPage=false});return}
    HomePaperFrame {
    Scaffold(containerColor=Color.Transparent,topBar={
        HomePaperHeader(busy,onBack,undo.isNotEmpty(),redo.isNotEmpty(),
            {redo.add(draft);draft=undo.removeAt(undo.lastIndex)},
            {undo.add(draft);draft=redo.removeAt(redo.lastIndex)},{showTrial=true})
    }){inset->
        Column(Modifier.fillMaxSize().padding(inset).padding(horizontal=16.dp,vertical=6.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
            HomeAtelierPreview(previewPercent,{ next -> previewPercent=next;viewPrefs.edit().putInt("preview_compact_percent",next).apply() },naturalWidthDp=previewState.width,settingsContent={
                FlowRow(horizontalArrangement=Arrangement.spacedBy(4.dp)){
                    FilterChip(layoutOverride==null,{layoutOverride=null},label={Text("跟随")})
                    FilterChip(layoutOverride==KeyboardLayoutState.Chinese,{layoutOverride=KeyboardLayoutState.Chinese},label={Text("26键")})
                    FilterChip(layoutOverride==KeyboardLayoutState.T9Pinyin,{layoutOverride=KeyboardLayoutState.T9Pinyin},label={Text("九宫格")})
                    FilterChip(layoutOverride==KeyboardLayoutState.Number,{layoutOverride=KeyboardLayoutState.Number},label={Text("符号")})
                }
                FlowRow(horizontalArrangement=Arrangement.spacedBy(4.dp)){
                    FilterChip(!dark,{dark=false;followDark=false},label={Text("浅色")})
                    FilterChip(dark,{dark=true;followDark=false},label={Text("深色")})
                    TextButton(onClick={followDark=true;dark=live?.dark?:fallbackDark}){Text("跟随系统")}
                }
                TextButton(onClick={load(HomePresetStore.activeId.takeIf{it in HomePresetStore.presets},currentAppearance());layoutOverride=null;followDark=true;dark=live?.dark?:fallbackDark}){Text("重新读取当前装扮")}
                TextButton(onClick={feedbackPage=true},enabled=!busy){Text("震动与按键音")}
                FilterChip(previewChrome=="menu",{previewChrome=if(previewChrome=="menu")"toolbar" else "menu"},label={Text("预览更多面板")})
            }) {
                val selectedFloating=draft.floatingStickers.firstOrNull{it.id==floatingId}
                val rendered=floatingGesture?.let{s->draft.copy(floatingStickers=draft.floatingStickers.map{if(it.id==floatingId)it.copy(sticker=s) else it})}?:draft
                if(previewChrome=="menu")HomeLiveKeyboardPreview(rendered,dark,previewState,"menu",{previewChrome="toolbar"})
                else HomePreviewSelectionHost(selectedPreviewKey,{key ->
                    selectedPreviewKey=key
                    if(key.startsWith("toolbar:") || key.startsWith("chrome:")){if(!(section=="sticker"&&key.startsWith("toolbar:")))section="chrome"}
                    else if(section !in listOf("key","shadow","sticker","motion"))section="key"
                },floatingId=if(section=="sticker" && freeStickers)floatingId else null,floating=selectedFloating?.sticker,
                    onFloatingPreview={floatingGesture=it},onFloatingCommit={s->change(draft.copy(floatingStickers=draft.floatingStickers.map{if(it.id==floatingId)it.copy(sticker=s) else it}))}) {
                    HomeLiveKeyboardPreview(rendered,dark,previewState,previewChrome)
                }
            }

            HomePaperTabs(section){section=it}
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)){
                if(section=="overall"){
                    HomePaperGroup("配色与渐变"){
                        val palette=if(dark)draft.dark else draft.light
                        listOf("background" to "键盘底色","key" to "普通按键","special" to "功能按键","text" to "按键文字","candidate" to "候选文字","accent" to "强调色").chunked(2).forEach{pair->
                            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                                pair.forEach{(key,label)->HomePaperSwatch(label,palette.color(key),Modifier.weight(1f)){colorTarget=key}}
                            }
                        }
                        Row(verticalAlignment=Alignment.CenterVertically){Text("柔和渐变",Modifier.weight(1f));Switch(draft.gradient,{change(draft.copy(gradient=it))})}
                        if(draft.gradient){TextButton(onClick={colorTarget="gradientEnd"}){Text("渐变终点颜色")};HomeValueSlider("渐变方向",draft.gradientAngle.toFloat(),0f..360f,90f){change(draft.copy(gradientAngle=it.roundToInt()))}}
                    }
                    HomePaperGroup("键帽与文字"){
                        FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                            HomeKeyShape.entries.forEach{shape->FilterChip(draft.keyShape==shape,{change(draft.withGlobalShape(shape))},label={Text(shape.label)})}
                        }
                        if(draft.keyShape==HomeKeyShape.ROUNDED)HomeValueSlider("整体圆角",draft.cornerRadius.toFloat(),0f..24f,12f){change(draft.withGlobalRadius(it.roundToInt()))}
                        HomeValueSlider("候选字号",draft.candidateTextSize.toFloat(),14f..28f,19f){change(draft.copy(candidateTextSize=it.roundToInt()));previewChrome="candidate"}
                    }
                    HomePaperGroup("键间留白") {
                        Column(verticalArrangement=Arrangement.spacedBy(0.dp)) {
                            HomeValueSlider("横向缝隙",draft.spacingX*2f,0f..8f,6f,compact=true){change(draft.copy(spacingX=it/2f))}
                            HomePaperRule()
                            HomeValueSlider("上下行缝隙",draft.spacingY*2f,0f..16f,8f,compact=true){change(draft.copy(spacingY=it/2f))}
                            HomePaperRule()
                            HomeValueSlider(if(draft.sectionSpacing==null)"分区（跟随）" else "分区留白",draft.sectionSpacing?:2f,0f..12f,2f,compact=true){change(draft.copy(sectionSpacing=it))}
                        }
                        if(draft.sectionSpacing!=null)TextButton(onClick={change(draft.copy(sectionSpacing=null))}){Text("分区恢复跟随布局")}
                    }
                    HomePaperGroup("壁纸与底图"){HomeWallpaperEditor(draft,dark,!busy,::change)}
                    HomePaperGroup("按键边框"){HomeBorderEditor(draft.keyBorder,dark,(if(dark)draft.dark else draft.light).accent,{change(draft.copy(keyBorder=it))})}
                    HomePaperGroup("键盘与上方栏边框"){HomeBorderEditor(draft.frameBorder,dark,(if(dark)draft.dark else draft.light).accent,{change(draft.copy(frameBorder=it))})}
                }
                if(section=="sticker" && freeStickers || section=="chrome" || section in listOf("key","shadow","sticker","motion")){
                    HomePaperGroup(when(section){"chrome"->"上方的小细节";"sticker"->"贴上喜欢的小碎片";"shadow"->"光影与形状";"motion"->"指尖的小动静";else->"这一颗按键"}){
                        val toolbarSelected=selectedPreviewKey?.startsWith("toolbar:")==true
                        if(section=="sticker")FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp)){
                            FilterChip(!freeStickers&&!toolbarSelected,{freeStickers=false;selectedPreviewKey=if(previewState.layout==KeyboardLayoutState.T9Pinyin)"t9:2" else "qwerty:q"},label={Text("按键贴纸")})
                            FilterChip(!freeStickers&&toolbarSelected,{freeStickers=false;previewChrome="toolbar";selectedPreviewKey="toolbar:more_menu"},label={Text("快捷图标")})
                            FilterChip(freeStickers,{freeStickers=true;if(floatingId==null)floatingId=draft.floatingStickers.firstOrNull()?.id},label={Text("自由贴纸")})
                        }
                        if(section=="sticker" && freeStickers)HomeFloatingStickerEditor(draft,floatingId,{floatingId=it},undo.toList()+redo.toList(),::change)
                        if(section=="sticker"&&!freeStickers&&toolbarSelected) {
                            val targets=homeToolbarTargets(previewState.toolbarItems.map{it.id to it.label})
                            HomePerKeyEditor(draft,dark,::change,undo.toList()+redo.toList(),section,"toolbar",selectedKeyId=selectedPreviewKey,
                                onSelectedKey={selectedPreviewKey=it;previewChrome="toolbar"},extraKeys=targets,glyph={key->
                                    val tint=homeKeyText(Color((if(dark)draft.dark else draft.light).text))
                                    when(key){
                                        "more_menu"->Icon(painterResource(com.kingzcheung.xime.R.drawable.ic_home_more),"更多",tint=tint)
                                        "hide_keyboard"->Icon(Icons.Default.KeyboardArrowDown,"收起键盘",tint=tint)
                                        else->previewState.toolbarItems.firstOrNull{HomeKeyIds.token(it.id)==key}?.let{ToolbarButtonGlyph(it,tint)}
                                    }
                                })
                        }
                        if(section=="chrome")HomeChromeEditor(draft,dark,if(previewChrome=="menu")"toolbar" else previewChrome,{previewChrome=it;selectedPreviewKey="chrome:$it"},selectedPreviewKey,undo.toList()+redo.toList(),::change)
                        if(section in listOf("key","shadow","sticker","motion") && !(section=="sticker"&&(freeStickers||toolbarSelected)))HomePerKeyEditor(draft,dark,::change,undo.toList()+redo.toList(),section,if(previewState.layout==KeyboardLayoutState.T9Pinyin)"t9" else "qwerty",{layoutOverride=if(it=="t9")KeyboardLayoutState.T9Pinyin else KeyboardLayoutState.Chinese},selectedPreviewKey,{selectedPreviewKey=it})
                    }
                }
                if(section=="presets")HomePaperGroup("我的预设"){
            FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                OutlinedButton(onClick={load(null,HomeVisualPreset(name="方方小键盘",keyShape=HomeKeyShape.SQUARE,cornerRadius=0))},enabled=!busy){Text("新建正方形预设")}
                OutlinedButton(onClick={load(null,HomeVisualPreset(name="圆圆小键盘",keyShape=HomeKeyShape.CIRCLE))},enabled=!busy){Text("新建圆形预设")}
            }
            OutlinedTextField(draft.name,{if(it.length<=80)change(draft.copy(name=it))},label={Text("预设名字")},singleLine=true,modifier=Modifier.fillMaxWidth(),enabled=!busy)
            Row {
                TextButton(enabled = !busy, onClick = { load(null, HomeVisualPreset()) }) { Text("新建") }
                TextButton(enabled = !busy, onClick = { load(null, draft.copy(name = "${draft.name.take(70)} 副本")) }) { Text("复制这一份") }
                TextButton(enabled = !busy, onClick = { import.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }) { Text("导入") }
                TextButton(enabled = !busy, onClick = { export.launch("小家键盘完整装扮.lhktheme.json") }) { Text("导出") }
            }
            HomeDiyShareActions(draft,busy,{busy=it},{status=it},{showDiyImport=true},{import.launch(arrayOf("text/plain","application/octet-stream","application/json"))})
            HomePresetStore.presets.forEach { (savedId, preset) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { load(savedId, preset) }, enabled = !busy, modifier = Modifier.weight(1f)) {
                        Text((if (savedId == HomePresetStore.activeId) "正在用 · " else "") + preset.name)
                    }
                    TextButton(onClick = {
                        SettingsPreferences.setKeyboardTheme(context, savedId); onApplied(); load(savedId, preset)
                    }, enabled = !busy) { Text("用这份") }
                }
            }
            TextButton(enabled = id != null && !busy, onClick = { deleteConfirm = true }) { Text("删除正在编辑的预设") }
            TextButton(enabled=!busy,onClick={change(HomeVisualPreset(name=draft.name))}){Text("恢复起点")}

                }
                if(status.isNotEmpty())Text(status,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
            }
            HomePaperRule()
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(shape=RoundedCornerShape(15.dp),enabled=!busy && draft.name.isNotBlank(),modifier=Modifier.weight(1f),onClick={
                    val source=draft.panelSyncSource(selectedPreviewKey)
                    val next=draft.withPanelStyle(source,source.substringAfter(':') in KeysConfigHelper.FUNCTION_KEY_IDS)
                    change(next);save(next,true)
                }){Text("一键同步符号")}
                Button(shape=RoundedCornerShape(15.dp),enabled=!busy && draft.name.isNotBlank(),modifier=Modifier.weight(1f),onClick={save(draft)}) {
                    Text(if(busy)"正在保存…" else "保存并用上")
                }
            }
        }
    }
    if(showDiyImport)HomeDiyImportDialog({showDiyImport=false}) { next ->
        load(null,next);status="DIY码已放进预览，保存后用上";showDiyImport=false
    }
    }
    if(showTrial)AlertDialog(onDismissRequest={showTrial=false},title={Text("试打一小句")},text={OutlinedTextField(testText,{testText=it},placeholder={Text("在这里叫出真正的键盘")},modifier=Modifier.fillMaxWidth())},confirmButton={TextButton(onClick={showTrial=false}){Text("好了")}})
    if (deleteConfirm) AlertDialog(onDismissRequest = { deleteConfirm = false }, title = { Text("删除这份预设？") },
        text = { Text("如果它正在使用，键盘会恢复底座外观。") },
        confirmButton = { TextButton(onClick = {
            deleteConfirm = false
            val deleting = id ?: return@TextButton
            scope.launch {
                busy = true
                runCatching { withContext(Dispatchers.IO) { HomePresetStore.delete(context, deleting) } }
                    .onSuccess { load(null, HomeVisualPreset()); onApplied(); status = "预设已删除" }
                    .onFailure { status = "删除没有完成" }
                busy = false
            }
        }) { Text("删除") } }, dismissButton = { TextButton(onClick = { deleteConfirm = false }) { Text("保留") } })
    colorTarget?.let { target ->
        val palette = if (dark) draft.dark else draft.light
        HomeColorDialog(palette.color(target), onDismiss = { colorTarget = null }) { color ->
            val next = palette.withColor(target, color)
            change(if (dark) draft.copy(dark = next) else draft.copy(light = next))
            colorTarget = null
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeValueSlider(label:String,value:Float,range:ClosedFloatingPointRange<Float>,initial:Float,compact:Boolean=false,onChange:(Float)->Unit){
    var precise by remember { mutableStateOf(false) }
    var exact by remember(value){mutableStateOf("%.3f".format(java.util.Locale.ROOT,value).trimEnd('0').trimEnd('.'))}
    val parsed=exact.toFloatOrNull()?.takeIf{it.isFinite()&&it in range}
    Row(Modifier.fillMaxWidth().heightIn(min=if(compact)44.dp else 52.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(4.dp)){
        Text(label,Modifier.weight(.4f),fontSize=12.sp,maxLines=2,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        Slider(value,onChange,valueRange=range,modifier=Modifier.weight(.6f),
            thumb={Spacer(Modifier.size(12.dp).background(MaterialTheme.colorScheme.primary,androidx.compose.foundation.shape.CircleShape))},
            track={state->SliderDefaults.Track(state,modifier=Modifier.height(3.dp),thumbTrackGapSize=0.dp,drawStopIndicator=null)})
        TextButton(onClick={precise=true},contentPadding=PaddingValues(horizontal=2.dp),modifier=Modifier.widthIn(min=40.dp)){
            Text("%.2f".format(java.util.Locale.ROOT,value).trimEnd('0').trimEnd('.'),fontFamily=androidx.compose.ui.text.font.FontFamily.Monospace,fontSize=11.sp)
        }
        IconButton(onClick={onChange(initial)},modifier=Modifier.size(36.dp)){
            Icon(androidx.compose.material.icons.Icons.Default.Refresh,"${label}恢复默认",Modifier.size(16.dp))
        }
    }
    if(precise)AlertDialog(onDismissRequest={precise=false},title={Text(label)},text={
        OutlinedTextField(exact,{exact=it},singleLine=true,label={Text("精确数值")},isError=parsed==null,modifier=Modifier.fillMaxWidth())
    },confirmButton={TextButton(enabled=parsed!=null,onClick={onChange(parsed!!);precise=false}){Text("用这个数值")}},dismissButton={TextButton(onClick={precise=false}){Text("取消")}})
}

@Composable
internal fun HomeColorDialog(initial: Long, onDismiss: () -> Unit, onChoose: (Long) -> Unit) {
    var hex by remember { mutableStateOf("%06X".format(initial and 0xFFFFFF)) }
    val rgb = hex.takeIf { it.matches(Regex("[0-9a-fA-F]{6}")) }?.toLongOrNull(16)
    AlertDialog(onDismissRequest = onDismiss, title = { Text("调一调颜色") }, text = {
        Column {
            Box(Modifier.fillMaxWidth().height(48.dp).background(Color(0xFF000000 or (rgb ?: 0L)), RoundedCornerShape(12.dp)))
            OutlinedTextField(hex, { if (it.length <= 6) hex = it }, label = { Text("六位色号，例如 FFE4ED") }, singleLine = true)
            listOf("红" to 16, "绿" to 8, "蓝" to 0).forEach { (name, shift) ->
                Text(name)
                Slider(((rgb ?: 0L) shr shift and 255).toFloat(), { component ->
                    val mask = 255L shl shift
                    val next = ((rgb ?: 0L) and mask.inv()) or (component.roundToInt().toLong() shl shift)
                    hex = "%06X".format(next)
                }, valueRange = 0f..255f)
            }
        }
    }, confirmButton = { TextButton(enabled = rgb != null, onClick = { onChoose(0xFF000000 or rgb!!) }) { Text("用这个颜色") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } })
}


private fun HomePalette.color(key: String): Long = when (key) {
    "background" -> background; "gradientEnd" -> gradientEnd; "key" -> this.key; "special" -> special
    "text" -> text; "candidate" -> candidate; else -> accent
}

private fun HomePalette.withColor(key: String, color: Long): HomePalette = when (key) {
    "background" -> copy(background = color); "gradientEnd" -> copy(gradientEnd = color)
    "key" -> copy(key = color); "special" -> copy(special = color); "text" -> copy(text = color)
    "candidate" -> copy(candidate = color); else -> copy(accent = color)
}
