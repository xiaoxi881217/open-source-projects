package com.kingzcheung.xime.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.kingzcheung.xime.home.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeConnectionContent(onBack: () -> Unit) {
    val context = LocalContext.current; val scope = rememberCoroutineScope()
    val sync = remember { HomeClipboardSync.get(context) }
    var binding by remember { mutableStateOf(HomeBinding()) }
    var items by remember { mutableStateOf(emptyList<HomeOutboxItem>()) }
    var origin by remember { mutableStateOf("") }; var name by remember { mutableStateOf("我的手机") }
    var code by remember { mutableStateOf("") }; var exclusions by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("连接后仍需单独开启自动同步。") }
    var notice by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }; var confirm by remember { mutableStateOf("") }
    suspend fun refresh() { withContext(Dispatchers.IO) { sync.binding() to sync.items() }.let { (b, list) ->
        binding = b; items = list; notice = withContext(Dispatchers.IO) { sync.notice() }; exclusions = b.excludedEditors.joinToString("\n"); if (b.origin.isNotEmpty()) origin = b.origin
    } }
    fun run(messageOnSuccess: String, action: () -> Unit) {
        if (busy) return
        busy = true
        scope.launch {
            try { withContext(Dispatchers.IO) { action() }; message = messageOnSuccess }
            catch (_: Exception) { message = "这次没完成：请检查地址、网络或设备授权；打字不受影响。" }
            finally { try { refresh() } catch (_: Exception) {} ; code = ""; busy = false }
        }
    }
    LaunchedEffect(Unit) { try { refresh() } catch (_: Exception) { message = "安全存储暂时打不开，自动同步不会启动。" } }
    Scaffold(topBar = { TopAppBar(title = { Text("我们的小家") }, navigationIcon = { TextButton(onClick = onBack) { Text("返回") } }) }) { padding ->
        Column(Modifier.padding(padding).padding(horizontal = 20.dp).fillMaxSize().imePadding().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("把复制过的生活片段，放进小剪贴板。", style = MaterialTheme.typography.titleMedium)
            Text(message, style = MaterialTheme.typography.bodyMedium)
            if (notice.isNotEmpty()) Text(notice, color = MaterialTheme.colorScheme.error)
            if (binding.token.isEmpty()) {
                OutlinedTextField(origin, { origin = it }, label = { Text("小家HTTPS地址") }, placeholder = { Text("https://你的小家地址") }, enabled = !busy, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(name, { name = it }, label = { Text("这部手机叫什么") }, enabled = !busy, singleLine = true, modifier = Modifier.fillMaxWidth())
                TextButton(enabled = !busy, onClick = {
                    try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(HomeSyncPolicy.origin(origin) + HomeSyncPolicy.PREFIX + "/manage"))) }
                    catch (_: Exception) { message = "请先填写正确的HTTPS小家地址。" }
                }) { Text("去小家领取一次性绑定码") }
                OutlinedTextField(code, { code = it }, label = { Text("一次性绑定码") }, enabled = !busy, singleLine = true, modifier = Modifier.fillMaxWidth())
                Button(enabled = !busy && code.isNotBlank(), onClick = { run("已连接，自动同步仍然关闭。") { sync.bind(origin, code.trim(), name.trim()) } }) { Text("连接小家") }
            } else {
                Text("${binding.name} · ${binding.origin}", style = MaterialTheme.typography.bodySmall)
                Row { TextButton(enabled = !busy, onClick = { run("小家连接正常") { sync.check() } }) { Text("检查连接") }
                    TextButton(enabled = !busy, onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(binding.origin + HomeSyncPolicy.PREFIX + "/manage"))) }) { Text("打开生活收件箱") } }
                Row { Text("自动送到小家", Modifier.weight(1f)); Switch(binding.enabled, enabled = !busy, onCheckedChange = {
                    if (it) confirm = "enable" else run("已经暂停，不再发起新的上传；已送达的内容仍在小家。") { sync.configure(false, binding.wifiOnly) }
                }) }
                Row { Text("只在 Wi-Fi 下发送", Modifier.weight(1f)); Switch(binding.wifiOnly, enabled = !busy, onCheckedChange = { next -> run(if(next)"已改为只在不计流量的 Wi-Fi 下发送" else "已允许流量和 Wi-Fi，待发送记录已重新安排") { sync.configure(binding.enabled, next) } }) }
                Text(if(binding.wifiOnly)"当前等待不计流量的 Wi-Fi；关闭上面的开关即可使用手机流量。" else "当前允许手机流量和 Wi-Fi，会使用少量流量。省电或后台限制仍可能延后发送。",style=MaterialTheme.typography.bodySmall)
                Text("只处理系统实际允许读到的文字和链接，不上传按键、密码输入或旧历史。Android无法可靠提供复制来源App；下方仅排除当前正在输入的App，不能保证识别原始复制来源。过滤也可能漏掉隐私内容。", style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(exclusions, { exclusions = it }, label = { Text("在这些App输入时暂不收集（包名，每行一个）") }, enabled = !busy, modifier = Modifier.fillMaxWidth())
                TextButton(enabled = !busy, onClick = { run("排除规则已保存") { sync.configure(binding.enabled, binding.wifiOnly, exclusions.lineSequence().map { it.trim() }.filter { it.isNotBlank() }.toSet()) } }) { Text("保存排除规则") }
                Row { TextButton(enabled = !busy, onClick = { run("已安排重试；暂停时会继续暂存。") { sync.retry() } }) { Text("重试失败记录") }
                    TextButton(enabled = !busy, onClick = { run("发送记录已刷新") {} }) { Text("刷新记录") } }
                Text("最近发送记录 · 最多保留200条、已发送保留7天；待发送不自动清掉。", style = MaterialTheme.typography.bodySmall)
                Text("暂停后可以移除单条发送记录，不影响原剪贴板历史。", style = MaterialTheme.typography.bodySmall)
                for (item in items) {
                    HorizontalDivider()
                    Text(item.text.take(240), maxLines = 4)
                    Text(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(item.copiedAt)) + " · " + when(item.state) { "sent" -> "小家收到了"; "failed" -> "没送达 · ${item.error}"; else -> (if(!binding.enabled)"已暂停 · 片段留在手机" else if(binding.wifiOnly)"待发送 · 仅允许 Wi-Fi · 已尝试${item.attempts}次" else "待发送 · 已允许流量 · 已尝试${item.attempts}次") }, style = MaterialTheme.typography.bodySmall)
                    TextButton(enabled = !busy && !binding.enabled, onClick = { run("这条发送记录已移除") { sync.removeLocal(item.id) } }) { Text("移除这条发送记录") }
                }
                Row { TextButton(enabled = !busy, onClick = { confirm = "local" }) { Text("清空本机发送记录") }
                    TextButton(enabled = !busy, onClick = { confirm = "server" }) { Text("删除本设备的小家副本") } }
                OutlinedButton(enabled = !busy, onClick = { confirm = "unbind" }) { Text("解绑这部手机") }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
    if (confirm.isNotEmpty()) AlertDialog(onDismissRequest = { confirm = "" }, title = { Text(if (confirm == "enable") "让生活片段自动送到小家？" else "确认这次操作") }, text = { Text(when(confirm) {
        "enable" -> "从现在起，系统允许捕获的新复制文字和链接会自动上传，供小家查看。来源App可能未知，密码、验证码等过滤不能保证百分之百准确。敏感资料仍可能被送出；你可以随时暂停。不会上传正常打字或补传旧历史。"
        "local" -> "先暂停上传，再清除本机的发送记录和待发送片段，不删除原剪贴板历史或小家副本。"
        "server" -> "先暂停上传，再删除本设备已经送到小家的全部片段；其他设备的记录保留。"
        else -> "先暂停上传，再向小家撤销本设备授权。成功后清除本机绑定和发送记录。小家已有片段仍保留，可在收件箱管理。连接失败时保留绑定以便重试。"
    }) }, confirmButton = { TextButton(onClick = { val action = confirm; confirm = ""; run(if (action == "enable") "自动同步已开启，只接收此后新复制的片段。" else "处理好了。") { when(action) { "enable" -> sync.configure(true, binding.wifiOnly); "local" -> sync.clearLocal(); "server" -> sync.deleteServerCopies(); "unbind" -> sync.unbind() } } }) { Text(if (confirm == "enable") "我了解风险，开启" else "确认") } }, dismissButton = { TextButton(onClick = { confirm = "" }) { Text("先不改") } })
}
