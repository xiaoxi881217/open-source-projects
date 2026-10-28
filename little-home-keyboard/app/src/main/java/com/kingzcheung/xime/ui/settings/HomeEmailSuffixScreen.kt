package com.kingzcheung.xime.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.kingzcheung.xime.home.HomeEmailSuffixPolicy
import com.kingzcheung.xime.home.HomeEmailSuffixStore

@OptIn(ExperimentalMaterial3Api::class)
@Composable internal fun HomeEmailSuffixContent(onBack:()->Unit) {
    val context=LocalContext.current
    var enabled by rememberSaveable{mutableStateOf(HomeEmailSuffixStore.enabled(context))}
    var text by rememberSaveable{mutableStateOf(HomeEmailSuffixStore.domains(context).joinToString("\n"))}
    var message by rememberSaveable{mutableStateOf("")}
    Scaffold(containerColor=MaterialTheme.colorScheme.background,topBar={TopAppBar(title={Text("邮箱后缀")},navigationIcon={TextButton(onClick=onBack){Text("返回")}})}){inset->
        Column(Modifier.fillMaxSize().padding(inset).imePadding().verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Surface(shape=MaterialTheme.shapes.medium,color=MaterialTheme.colorScheme.surfaceContainerLow){
                Row(Modifier.fillMaxWidth().padding(14.dp),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically){
                    Text("输入 @ 时显示后缀",Modifier.weight(1f));Switch(enabled,{enabled=it;message=""})
                }
            }
            Text("点候选只补后面的域名，不会重复输入 @。密码框与选中文字时不显示。",style=MaterialTheme.typography.bodyMedium)
            OutlinedTextField(text,{if(it.length<=4096){text=it;message=""}},label={Text("常用后缀 · 每行一个")},supportingText={Text("可填 qq.com 或 @qq.com；最多12个，保存时按顺序去重。")},minLines=6,modifier=Modifier.fillMaxWidth())
            if(message.isNotEmpty())Text(message,style=MaterialTheme.typography.bodySmall)
            Button(onClick={message=runCatching{HomeEmailSuffixStore.save(context,enabled,text);text=HomeEmailSuffixStore.domains(context).joinToString("\n");"已保存，下次输入 @ 就能选"}.getOrElse{it.message?:"这次没保存，请检查后缀格式"}},modifier=Modifier.fillMaxWidth()){Text("保存")}
            TextButton(onClick={text=HomeEmailSuffixPolicy.defaults.joinToString("\n");message="已填入常用后缀，点保存应用"}){Text("恢复常用后缀")}
            Text("只在本机补全，不保存正在输入的邮箱地址，不加入主题导出，也不发送到小家。",style=MaterialTheme.typography.bodySmall)
        }
    }
}
