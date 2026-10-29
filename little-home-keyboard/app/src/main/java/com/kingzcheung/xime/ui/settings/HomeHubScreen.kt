package com.kingzcheung.xime.ui.settings

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.kingzcheung.xime.MainActivity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeHubContent(onOpen:(String)->Unit){
 val context=LocalContext.current
 Scaffold(topBar={TopAppBar(title={Text("小家键盘")})}){inset->
  Column(Modifier.fillMaxSize().padding(inset).verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
   Text("把喜欢的小细节，放进每天碰到的键盘。",style=MaterialTheme.typography.headlineSmall)
   HomePrimaryAction(onClick={onOpen(SettingsRoutes.HomeAppearance)},modifier=Modifier.fillMaxWidth().heightIn(min=72.dp)){
    Column{Text("装扮键盘",style=MaterialTheme.typography.titleLarge);Text("整体 · 每颗按键 · 四边阴影 · 贴纸 · 动态 · 壁纸与磨砂")}
   }
   listOf(SettingsRoutes.HomeEmailSuffixes to "邮箱后缀 · 输入 @ 时补全",SettingsRoutes.HomeDrafts to "我的草稿 · 写下来、分类、选一小段",SettingsRoutes.ClipboardHistory to "本地剪贴板 · 复制过的小片段",SettingsRoutes.HomeConnection to "我们的小家 · 连接与小剪贴板",SettingsRoutes.KeyEffect to "震动与按键音",SettingsRoutes.Main to "输入设置 · 启用、26键和九宫格").forEach{(route,label)->
    HomeOutlinedAction(onClick={onOpen(route)},modifier=Modifier.fillMaxWidth().heightIn(min=52.dp)){Text(label)}
   }
   HomeActionButton(onClick={context.startActivity(Intent(context,MainActivity::class.java).putExtra("show_setup",true))}){Text("第一次使用 · 打开启用向导")}
   Text("输入完全在本机完成。装扮保存后应用到系统键盘；连接和自动同步可单独关闭。",style=MaterialTheme.typography.bodySmall)
  }
 }
}
