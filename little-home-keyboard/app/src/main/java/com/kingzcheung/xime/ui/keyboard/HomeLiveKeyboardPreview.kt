package com.kingzcheung.xime.ui.keyboard

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.kingzcheung.xime.appearance.*
import com.kingzcheung.xime.settings.KeysConfigHelper
import com.kingzcheung.xime.ui.isTablet

/** 复用系统键盘的行分配、真实键帽、配置和光影；不创建输入服务或发送按键。 */
@Composable
internal fun HomeLiveKeyboardPreview(preset:HomeVisualPreset,dark:Boolean,state:HomePreviewState,chrome:String="toolbar",onCloseMenu:()->Unit={}){
 if(chrome=="menu") {
  val palette=if(dark)preset.dark else preset.light
  HomeBackdropHost(preset,dark,Modifier.fillMaxWidth().height((state.height+48).dp)) {
   MenuBar(MenuBarState(true,dark,backgroundColor=Color(palette.background),keyBgColor=Color(palette.key),keyTextColor=Color(palette.text),
       styleLayout=if(state.layout==KeyboardLayoutState.T9Pinyin)"t9" else "qwerty"),
    MenuBarCallbacks(onCloseMenu,{},{},{},{},{},{},{},{},{}),Modifier.fillMaxSize())
  }
  return
 }
 val version by KeysConfigHelper.configVersion.collectAsState();val palette=if(dark)preset.dark else preset.light
 val landscape=LocalConfiguration.current.screenWidthDp>LocalConfiguration.current.screenHeightDp && !isTablet()
 val ascii=state.layout==KeyboardLayoutState.English;val section=when(state.layout){KeyboardLayoutState.Number->"number";KeyboardLayoutState.CommonSymbol->"common_symbols";KeyboardLayoutState.Symbol->"symbols";KeyboardLayoutState.T9Pinyin->"t9";else->"qwerty"}
 val shadow=resolveKeyboardShadowParams();val radius=preset.cornerRadius.dp
 val env=QwertyRowEnv(ascii,state.shifted,state.shift,Color(palette.key),Color(palette.special),Color(palette.text),Color(palette.text),Color(palette.background),shadow.enabled,shadow.elevation,maxOf(shadow.shapeRadius,radius),{},null,null,null,null,null,false,false,false,state.schemaName,state.enterLabel,remember{mutableStateOf(false)},{_,_->},version,landscape=landscape,section=section)
 HomeBackdropHost(preset,dark,Modifier.fillMaxWidth()){
 HomeStickerHost(Modifier.fillMaxWidth()){drawing->
 CompositionLocalProvider(LocalKeyCornerRadius provides radius,LocalKeyVisualPadding provides PaddingValues(horizontal=preset.spacingX.dp,vertical=preset.spacingY.dp)){
 Column(drawing.padding(horizontal=4.dp,vertical=8.dp)){
  CandidateBar(state=if(chrome=="candidate")CandidateBarState.ChineseCandidates(candidates=listOf("小家","今天","喜欢")) else CandidateBarState.Idle,
    toolbarActions=state.toolbarItems.map{com.kingzcheung.xime.keyboard.ToolbarAction(it,{})},
    visuals=CandidateBarVisuals(Color(palette.background),Color(palette.candidate),Color(palette.accent).copy(alpha=.2f),Color(palette.accent),Color(palette.text),dark),
    callbacks=CandidateBarCallbacks({},onLogoClick={},onHideKeyboard={}))
  Box(Modifier.fillMaxWidth().height(state.height.coerceIn(120,600).dp)){
   when(state.layout){
    KeyboardLayoutState.T9Pinyin->{
     val layout=KeysConfigHelper.getT9Layout()
     HomeT9Grid(layout,landscape,{id->KeysConfigHelper.getT9KeyGesture(id)?.width?.takeIf{it>0}?:when(id){"candidates"->3f;"space"->1.8f;"enter"->2f;else->1f}}){id,m->
      HomeKeyStyleScope("t9:$id"){
       if(id=="candidates")Text("，\n。\n？",color=Color(palette.text),modifier=m.fillMaxWidth().padding(4.dp))
       else if(id.length==1 && id[0].isDigit())HomeT9PreviewDigit(id,Color(palette.key),Color(palette.text),m,landscape){}
       else if(id=="clear")KeyButton("清空",{},backgroundColor=Color(palette.special),textColor=Color(palette.text),modifier=m.fillMaxWidth())
       else FunctionKeyCell(id,env,m.fillMaxHeight())
      }
     }
    }
    KeyboardLayoutState.Number,KeyboardLayoutState.CommonSymbol->{
     val layout=KeysConfigHelper.getPanelLayout(section)
     if(layout!=null){if(layout.columns)ColumnsPanelContent(layout,env,Modifier.fillMaxSize()) else Column(Modifier.fillMaxSize()){layout.rows.forEach{row->PanelRow(row,env,Modifier.weight(1f).fillMaxWidth())}}}
    }
    KeyboardLayoutState.Symbol->SymbolKeyboardLayout({}, {},Color(palette.background),Color(palette.text),Color(palette.accent),Color(palette.key),specialKeyBgColor=Color(palette.special),specialKeyTextColor=Color(palette.text),modifier=Modifier.fillMaxSize())
    KeyboardLayoutState.Stroke->Text("当前是笔画键盘，选26键或九宫格预览装扮。",color=Color(palette.text))
    else->{
     val config=KeysConfigHelper.getRowsLayout(ascii);val rows=normalizeQwertyRows(config.rows);val columns=config.columns?:rows.firstOrNull()?.keys?.size?:10
     HomeKeyboardRows("qwerty",rows.size,Modifier.fillMaxSize()){index,m->val row=rows[index];val fraction=rowWidthFraction(row,columns,config.rowAlign);QwertyRow(row.keys,env,m.fillMaxWidth(fraction).align(Alignment.CenterHorizontally),reserveVerticalInsets=false)}
    }
   }
  }
 }
 }
 }
 }
}
