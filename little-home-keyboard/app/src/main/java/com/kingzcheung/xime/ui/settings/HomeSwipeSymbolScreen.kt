package com.kingzcheung.xime.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.kingzcheung.xime.home.*
import com.kingzcheung.xime.settings.KeysConfigHelper

@OptIn(ExperimentalMaterial3Api::class,ExperimentalLayoutApi::class)
@Composable internal fun HomeSwipeSymbolContent(onBack:()->Unit) {
    val context=LocalContext.current
    HomeSwipeSymbolStore.initialize(context)
    var selected by remember{mutableStateOf("q")}
    Scaffold(topBar={TopAppBar(title={Text("上滑小符号")},navigationIcon={HomeActionButton(onClick=onBack){Text("返回")}})}){inset->
        Column(Modifier.fillMaxSize().padding(inset).imePadding().verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            HomePaperGroup("先选一颗字母键") {
                FlowRow(horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                    ('a'..'z').forEach{key->HomeChoiceChip(selected==key.toString(),{selected=key.toString()},label={Text(key.uppercase())})}
                }
            }
            HomePaperGroup("${selected.uppercase()} 的上滑") {
                Text("现在显示：${KeysConfigHelper.getSwipeUpLabel(selected)?:"没有小符号"}")
                HomeSwipeSymbolPicker(selected)
                Text("小字和上滑输入一起更改，中英文共用；轻点字母仍正常打字。",style=MaterialTheme.typography.bodyMedium)
            }
            Text("这是本机输入设置，立即生效，不随装扮或DIY码分享。",style=MaterialTheme.typography.bodySmall)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable internal fun HomeSwipeSymbolPicker(key:String) {
    if(!key.matches(Regex("[a-z]")))return
    val context=LocalContext.current
    HomeSwipeSymbolStore.initialize(context)
    val current=HomeSwipeSymbolStore.value(key)
    var open by remember(key){mutableStateOf(false)}
    var text by remember(key){mutableStateOf("")}
    var message by remember(key){mutableStateOf("")}
    HomeActionButton(onClick={text=current?:KeysConfigHelper.getSwipeUpLabel(key).orEmpty();message="";open=true}){Text("选择上滑符号")}
    if(open)AlertDialog(onDismissRequest={open=false},title={Text("${key.uppercase()} 的上滑符号")},text={
        Column(Modifier.heightIn(max=380.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            Text("选一个，或自己输入；小字和上滑输入会保持一致。",style=MaterialTheme.typography.bodySmall)
            FlowRow(horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                (listOf(key.uppercase())+('0'..'9').map{it.toString()}+HomeSwipeSymbolPolicy.common).forEach{symbol->
                    HomeChoiceChip(text==symbol,{text=symbol;message=""},label={Text(symbol)})
                }
            }
            OutlinedTextField(text,{text=it.take(16);message=""},label={Text("自己填 · 1到4个字符")},singleLine=true,modifier=Modifier.fillMaxWidth())
            HomeActionButton(onClick={HomeSwipeSymbolStore.save(context,key,null);open=false}){Text("恢复原来的上滑")}
            if(message.isNotEmpty())Text(message,color=MaterialTheme.colorScheme.error)
        }
    },confirmButton={HomePrimaryAction(onClick={runCatching{HomeSwipeSymbolStore.save(context,key,text)}.onSuccess{open=false}.onFailure{message=it.message?:"请检查符号"}}){Text("用这个")}},
        dismissButton={HomeActionButton(onClick={open=false}){Text("取消")}})
}
