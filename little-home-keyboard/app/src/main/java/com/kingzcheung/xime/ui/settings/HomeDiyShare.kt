package com.kingzcheung.xime.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.PersistableBundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.kingzcheung.xime.appearance.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable internal fun HomeDiyShareActions(preset:HomeVisualPreset,busy:Boolean,setBusy:(Boolean)->Unit,status:(String)->Unit,openImport:()->Unit,openFileImport:()->Unit) {
    val context=LocalContext.current
    val scope=rememberCoroutineScope()
    val currentPreset by rememberUpdatedState(preset)
    val exportTxt=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")){uri->
        if(uri!=null) {
            val snapshot=currentPreset
            setBusy(true)
            scope.launch {
                runCatching{withContext(Dispatchers.IO){
                    val bytes=HomeDiyCode.encodeTextFile(HomeStickerAssets.export(snapshot))
                    requireNotNull(context.contentResolver.openOutputStream(uri,"wt")).use{it.write(bytes)}
                }}.onSuccess{status("TXT已保存，包含完整数值和用到的图片，可以作为附件发给朋友")}
                    .onFailure{status("TXT这次没保存好，请检查文件位置和素材；原装扮保留")}
                setBusy(false)
            }
        }
    }
    Column(verticalArrangement=Arrangement.spacedBy(4.dp)) {
    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        OutlinedButton(enabled=!busy,onClick={
            setBusy(true)
            scope.launch {
                runCatching {
                    val code=withContext(Dispatchers.Default){HomeDiyCode.encode(HomeStickerAssets.export(preset))}
                    val clip=ClipData.newPlainText("小家键盘DIY码",code)
                    clip.description.extras=PersistableBundle().apply{putBoolean("android.content.extra.IS_SENSITIVE",true)}
                    (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(clip)
                    status("DIY码已复制，共${code.length}字，包含全部装扮数值和用到的图片"+if(code.length>8000)"；聊天可能限长，也可以用完整装扮文件" else "")
                }.onFailure{status(it.message?:"这次没复制好，原装扮保留")}
                setBusy(false)
            }
        }){Text("复制DIY码")}
        OutlinedButton(enabled=!busy,onClick=openImport){Text("输入DIY码")}
    }
    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        TextButton(enabled=!busy,onClick={exportTxt.launch("小家键盘装扮-DIY.txt")}){Text("导出TXT")}
        TextButton(enabled=!busy,onClick=openFileImport){Text("导入TXT")}
    }
    }
}

@Composable internal fun HomeDiyImportDialog(dismiss:()->Unit,loaded:(HomeVisualPreset)->Unit) {
    val scope=rememberCoroutineScope()
    var text by remember{mutableStateOf("")}
    var busy by remember{mutableStateOf(false)}
    var error by remember{mutableStateOf("")}
    AlertDialog(onDismissRequest={if(!busy)dismiss()},title={Text("用朋友的装扮")},text={
        Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Text("粘贴或输入以 XJ1: 开头的完整DIY码。先放进预览，不会直接替换正在用的装扮。")
            OutlinedTextField(text,{if(it.length<=HomeDiyCode.MAX_CHARS+4096){text=it;error=""}else error="码太长了，请用完整装扮文件"},
                label={Text("DIY码")},minLines=3,maxLines=6,isError=error.isNotEmpty(),enabled=!busy,modifier=Modifier.fillMaxWidth())
            if(error.isNotEmpty())Text(error,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.error)
            Text("图片只安装到本机；不会连接小家或下载外部链接。",style=MaterialTheme.typography.bodySmall)
        }
    },confirmButton={TextButton(enabled=!busy&&text.isNotBlank(),onClick={
        busy=true
        scope.launch {
            runCatching{withContext(Dispatchers.IO){HomeStickerAssets.install(HomeDiyCode.decode(text))}}
                .onSuccess(loaded).onFailure{error="这份DIY码没能导入，请确认复制完整、双方键盘已更新，或改用装扮文件"}
            busy=false
        }
    }){Text(if(busy)"正在解开…" else "放进预览")}},dismissButton={TextButton(enabled=!busy,onClick=dismiss){Text("取消")}})
}
