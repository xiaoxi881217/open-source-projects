package com.kingzcheung.xime.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kingzcheung.xime.clipboard.*
import com.kingzcheung.xime.home.HomeClipboardSync
import com.kingzcheung.xime.home.HomeOutboxItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class,ExperimentalLayoutApi::class)
@Composable
internal fun ClipboardHistoryContent(onBack: ()->Unit,onNavigateToHome: ()->Unit) {
    var drafts by remember { mutableStateOf(false) }
    if(drafts){HomeDraftPanel(onBack={drafts=false});return}
    val context=LocalContext.current;val scope=rememberCoroutineScope()
    val manager=remember { ClipboardManager.getInstance(context) }
    val history by manager.clipboardItems.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") };var filter by remember { mutableStateOf(HistoryFilter.ALL) }
    var selected by remember { mutableStateOf(emptySet<Long>()) };var choosing by remember { mutableStateOf(false) }
    var detail by remember { mutableStateOf<ClipboardItem?>(null) }
    var deleteIds by remember { mutableStateOf<List<Long>?>(null) }
    var busy by remember { mutableStateOf(false) };var refreshing by remember { mutableStateOf(false) }
    var outbox by remember { mutableStateOf(emptyMap<String,HomeOutboxItem>()) }
    var statusError by remember { mutableStateOf(false) }
    val snackbar=remember { SnackbarHostState() }
    val lifecycle=LocalLifecycleOwner.current.lifecycle
    val dates=remember { DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT) }
    val visible by produceState(emptyList<ClipboardItem>(),history,query,filter) {
        value=withContext(Dispatchers.Default) { ClipboardHistoryModel.visible(history,query,filter) }
    }
    val textIds=remember(history) { history.filter { !it.isImage && !it.isQuickSend }.map { it.id }.toSet() }
    LaunchedEffect(textIds) { selected=selected.intersect(textIds);if(detail?.id !in textIds)detail=null }
    fun refreshStatus() {
        if(refreshing)return
        refreshing=true
        scope.launch { try {
            outbox=withContext(Dispatchers.IO) { ClipboardHistoryModel.latestByText(HomeClipboardSync.get(context).items(200)) }
            statusError=false
        }catch(e:CancellationException){throw e}catch(_:Exception){outbox=emptyMap();statusError=true}finally{refreshing=false} }
    }
    DisposableEffect(lifecycle) {
        val observer=LifecycleEventObserver { _,event->if(event==Lifecycle.Event.ON_RESUME)refreshStatus() }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    fun action(success: String, task: suspend ()->Unit) {
        if(busy)return
        busy=true
        scope.launch {
            var result=success
            try { withContext(Dispatchers.IO) { task() } }
            catch(e:CancellationException){throw e}
            catch(_:Exception){result="这次没有完成，请稍后再试。"}
            finally { busy=false }
            snackbar.showSnackbar(result)
        }
    }
    fun copy(item: ClipboardItem) {
        if(busy)return
        try { manager.copyToSystemClipboard(item.text);scope.launch { snackbar.showSnackbar("已复制；开启自动同步时，重新复制也可能成为新片段。") } }
        catch(_:Exception){scope.launch { snackbar.showSnackbar("系统暂时没有接住这次复制。") }}
    }
    Scaffold(topBar={TopAppBar(title={Text("复制过的小片段")},navigationIcon={TextButton(onClick=onBack){Text("返回")}})},
        snackbarHost={SnackbarHost(snackbar)}) { padding->
        Column(Modifier.padding(padding).padding(horizontal=16.dp).fillMaxSize().imePadding(),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            TextButton(onClick={drafts=true}){Text("我的草稿 · 写一篇 / 分类")}
            OutlinedTextField(query,{query=it.take(200)},label={Text("搜一搜文字和链接")},singleLine=true,modifier=Modifier.fillMaxWidth())
            FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) { HistoryFilter.entries.forEach { option->
                FilterChip(filter==option,{filter=option},label={Text(option.label)})
            } }
            Row(verticalAlignment=Alignment.CenterVertically) {
                Text("${visible.size}条",Modifier.weight(1f),style=MaterialTheme.typography.bodySmall)
                TextButton(onClick={choosing=!choosing;selected=emptySet()},enabled=!busy){Text(if(choosing)"结束多选" else "多选")}
                TextButton(onClick={deleteIds=textIds.toList()},enabled=!busy&&textIds.isNotEmpty()){Text("清空文字")}
            }
            if(choosing)FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                TextButton(onClick={selected=selected+visible.map { it.id }},enabled=!busy){Text("选中当前结果")}
                TextButton(onClick={selected=emptySet()},enabled=!busy){Text("取消全选")}
                TextButton(onClick={deleteIds=selected.toList()},enabled=!busy&&selected.isNotEmpty()){Text("删除所选（${selected.size}）")}
            }
            Row(verticalAlignment=Alignment.CenterVertically) {
                TextButton(onClick=onNavigateToHome,modifier=Modifier.weight(1f)){Text("小家连接与发送记录")}
                TextButton(onClick=::refreshStatus,enabled=!refreshing){Text(if(refreshing)"读取中" else "刷新状态")}
            }
            Text(if(statusError)"发送状态暂时读不到，本地历史仍可使用。" else "仅比对最近200条发送记录；没有记录不代表从未发送。删除本地历史不会撤回待发送或小家副本。",
                style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp),contentPadding=PaddingValues(bottom=24.dp)) {
                if(visible.isEmpty())item { Text(if(history.none { !it.isImage && !it.isQuickSend })"这里还没有文字片段。只会显示系统允许捕获到的内容。" else "没有找到这次筛选的小片段。",Modifier.padding(vertical=24.dp)) }
                items(visible,key={it.id}) { item->
                    Column(Modifier.fillMaxWidth().padding(vertical=8.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment=Alignment.CenterVertically) {
                            if(choosing)Checkbox(item.id in selected,{checked->selected=if(checked)selected+item.id else selected-item.id},enabled=!busy)
                            Text((if(item.isPinned)"已收藏 · " else "")+dates.format(Date(item.timestamp)),style=MaterialTheme.typography.labelMedium,modifier=Modifier.weight(1f))
                        }
                        Text(item.text,maxLines=4,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                        Text(if(statusError)"发送状态暂不可用" else ClipboardHistoryModel.status(outbox[item.text]),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                        FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                            TextButton(onClick={detail=item},enabled=!busy){Text("看完整")}
                            TextButton(onClick={copy(item)},enabled=!busy){Text("复制")}
                            TextButton(onClick={action(if(item.isPinned)"已取消收藏" else "已收藏") {manager.setTextHistoryPinned(item.id,!item.isPinned)}},enabled=!busy){Text(if(item.isPinned)"取消收藏" else "收藏")}
                            TextButton(onClick={deleteIds=listOf(item.id)},enabled=!busy){Text("删除")}
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
    }
    detail?.let { item->AlertDialog(onDismissRequest={detail=null},title={Text("这一段小片段")},text={
        SelectionContainer { Column(Modifier.heightIn(max=360.dp).verticalScroll(rememberScrollState())) { Text(item.text) } }
    },confirmButton={TextButton(onClick={copy(item)}){Text("复制")}},dismissButton={TextButton(onClick={detail=null}){Text("收好")}}) }
    deleteIds?.let { snapshot->AlertDialog(onDismissRequest={deleteIds=null},title={Text("删除${snapshot.size}条文字记录？")},
        text={Text("会删除所选本地文字，包括其中的收藏。图片、常用语、小家待发送记录和服务器副本保持原样。这次无法撤销。")},
        confirmButton={TextButton(enabled=!busy,onClick={deleteIds=null;action("所选文字记录已删除") {manager.removeTextHistoryAndWait(snapshot)}}){Text("确认删除")}},
        dismissButton={TextButton(onClick={deleteIds=null}){Text("先留着")}}) }
}
