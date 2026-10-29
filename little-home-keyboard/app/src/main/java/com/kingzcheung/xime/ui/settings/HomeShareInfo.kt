package com.kingzcheung.xime.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp

internal const val HOME_ANDROID_SOURCE="https://github.com/xiaoxi881217/open-source-projects/releases/tag/keyboard-20261068"
internal const val HOME_SERVER_SOURCE="https://github.com/xiaoxi881217/open-source-projects/tree/master/little-home-keyboard-server"

@Composable internal fun HomeQuickHelp(onDismiss:()->Unit) {
    AlertDialog(onDismissRequest=onDismiss,title={Text("小家键盘，简单用")},text={
        Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Text("开始打字：在系统输入法设置中启用小家键盘，再切换为当前输入法。")
            Text("装扮：打开装扮页，点击预览选键，调整后点保存并应用。")
            Text("草稿：键盘剪贴板里打开我的草稿，写好后可只选一小段放进输入框。")
            Text("连接小家：填写自己的 HTTPS 地址，领取一次性绑定码。绑定后，了解隐私风险再开启自动同步。")
            Text("日常用流量：关闭“只在 Wi-Fi 下发送”。已有待发送记录会重新安排；低电量或后台限制可能延后。")
            Text("分享：开源与致谢里有许可证、此版本源码和服务器部署说明。连接自己的服务器，使用自己的绑定码。")
        }
    },confirmButton={HomeActionButton(onClick=onDismiss){Text("知道啦")}})
}

@Composable internal fun HomeLicenseSummary() {
    val context=LocalContext.current;val uri=LocalUriHandler.current
    var document by remember{mutableStateOf<String?>(null)}
    Column(Modifier.fillMaxWidth().padding(vertical=8.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Text("小家键盘 · 开源与致谢",style=MaterialTheme.typography.titleMedium)
        Text("基于 Xime / Rime 修改，独立发布，沿用 GPL-3.0。保留原项目版权与依赖声明；不是 Xime 官方版本。",style=MaterialTheme.typography.bodyMedium)
        HomeActionButton(onClick={document=runCatching{context.assets.open("open-source/GPL-3.0.txt").bufferedReader().use{it.readText()}}.getOrElse{"许可证暂时打不开，请查看项目源码中的 LICENSE。"}}){Text("离线查看 GPL-3.0 全文")}
        HomeActionButton(onClick={document=runCatching{context.assets.open("NOTICE.txt").bufferedReader().use{it.readText()}}.getOrElse{"声明暂时打不开。"}}){Text("查看原项目第三方声明")}
        HomeActionButton(onClick={uri.openUri(HOME_SERVER_SOURCE)}){Text("服务器源码与部署说明")}
        HomeActionButton(onClick={uri.openUri(HOME_ANDROID_SOURCE)}){Text("此版本安卓源码与构建说明")}
        Text("下方为主要依赖来源；更多版权和资源来源请查看对应源码中的声明。",style=MaterialTheme.typography.bodySmall)
    }
    document?.let{body->AlertDialog(onDismissRequest={document=null},title={Text("许可与声明")},text={Text(body,Modifier.heightIn(max=420.dp).verticalScroll(rememberScrollState()),style=MaterialTheme.typography.bodySmall)},confirmButton={HomeActionButton(onClick={document=null}){Text("关闭")}})}
}
