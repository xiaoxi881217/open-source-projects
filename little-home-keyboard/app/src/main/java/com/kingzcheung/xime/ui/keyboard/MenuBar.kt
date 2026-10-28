package com.kingzcheung.xime.ui.keyboard


import com.kingzcheung.xime.R
import com.kingzcheung.xime.i18n.tr
import com.kingzcheung.xime.appearance.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.twotone.Assignment
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.twotone.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kingzcheung.xime.viewmodel.SchemaSwitchUiState

data class MenuItem(
    val icon: Painter? = null,
    val label: String,
    val action: () -> Unit,
    val textIcon: String? = null,
)

data class MenuBarState(
    val isVisible: Boolean,
    val isDarkTheme: Boolean,
    val darkMode: Int = 2,
    val backgroundColor: Color,
    val keyBgColor: Color = Color.White,
    val keyTextColor: Color = Color(0xFF202124),
    val isFloatingMode: Boolean = false,
    val schemaSwitches: List<SchemaSwitchUiState> = emptyList(),
    /** 承载区受限（独立 IME 面板）时「键盘调节」「浮动键盘」不可用，入口隐藏。 */
    val canResizeKeyboard: Boolean = true,
    val canFloatKeyboard: Boolean = true,
    val styleLayout: String = "qwerty",
)

data class MenuBarCallbacks(
    val onDismiss: () -> Unit,
    val onClipboard: () -> Unit,
    val onQuickSend: () -> Unit,
    val onKeyboardResize: () -> Unit,
    val onEmoji: () -> Unit,
    val onReloadConfig: () -> Unit,
    val onSettings: () -> Unit,
    val onSchemaList: () -> Unit,
    val onToggleDarkMode: () -> Unit,
    val onFloatingModeToggle: (() -> Unit)? = null,
    val onToolbarCustomize: () -> Unit = {},
    val onToggleSchemaSwitch: ((SchemaSwitchUiState) -> Unit)? = null,
)

@Composable
fun MenuBar(
    state: MenuBarState,
    callbacks: MenuBarCallbacks,
    modifier: Modifier = Modifier
) {
    if (!state.isVisible) return
    
    val textColor = state.keyTextColor
    val itemBgColor = state.keyBgColor
    val configuration = LocalConfiguration.current
    val preset = LocalHomePreset.current
    val tileStyle = preset?.menuKeyStyle(state.styleLayout) ?: HomeKeyStyle()
    val clipboardIcon = rememberVectorPainter(Icons.AutoMirrored.TwoTone.Assignment)
    val quickSendIcon = rememberVectorPainter(Icons.TwoTone.Quickreply)
    val keyboardResizeIcon = rememberVectorPainter(Icons.TwoTone.SettingsOverscan)
    val emojiIcon = rememberVectorPainter(Icons.TwoTone.EmojiEmotions)
    val darkModeIcon = when (state.darkMode) {
        0 -> rememberVectorPainter(Icons.TwoTone.DarkMode)
        1 -> rememberVectorPainter(Icons.TwoTone.LightMode)
        else -> rememberVectorPainter(if (state.isDarkTheme) Icons.TwoTone.LightMode else Icons.TwoTone.DarkMode)
    }
    val deployIcon = rememberVectorPainter(Icons.TwoTone.Rotate90DegreesCcw)
    val customizeIcon = rememberVectorPainter(Icons.TwoTone.Padding)
    val schemaIcon = rememberVectorPainter(Icons.TwoTone.Keyboard)
    val settingsIcon = rememberVectorPainter(Icons.TwoTone.Settings)

    val darkModeLabel = when (state.darkMode) {
        0 -> tr(R.string.s_shensemoshi)
        1 -> tr(R.string.s_qiansemoshi)
        else -> tr(R.string.s_gensuixitong)
    }

    val floatingIcon = rememberVectorPainter(Icons.TwoTone.PictureInPicture)
    val floatingLabel = if (state.isFloatingMode) tr(R.string.s_tuichuxuanfu) else tr(R.string.s_xuanfumoshi)
    val floatingAction = callbacks.onFloatingModeToggle ?: {}

    // 动态方案开关：图标取第一个状态的首字；标题若有 abbrev 则用 abbrev（多个用 🔁 连接），否则用所有状态 🔁 连接
    val switchItems = state.schemaSwitches.map { sw ->
        val textIcon = sw.states.firstOrNull()?.firstOrNull()?.toString() ?: ""
        val label = if (sw.abbrev.isNotEmpty()) sw.abbrev.joinToString("🔁")
            else sw.states.joinToString("🔁")
        MenuItem(icon = null, label = label, action = { callbacks.onToggleSchemaSwitch?.invoke(sw) }, textIcon = textIcon)
    }

    val menuItems = listOf(
            MenuItem(clipboardIcon, tr(R.string.s_jiantieban), callbacks.onClipboard),
            MenuItem(quickSendIcon, tr(R.string.s_kuaijiefasong), callbacks.onQuickSend),
            MenuItem(schemaIcon, tr(R.string.s_shurufangan), callbacks.onSchemaList),
            MenuItem(emojiIcon, tr(R.string.s_biaoqing), callbacks.onEmoji),
        ) + switchItems + listOf(
            MenuItem(customizeIcon, tr(R.string.s_dingzhigongjulan), callbacks.onToolbarCustomize),
            // 悬浮模式下键盘内容为缩放的浮动卡片，高度不可调节，隐藏该入口；
            // 承载区受限（独立 IME 面板）时调节页的操作按钮在面板里显示不全，同样隐藏
            if (!state.isFloatingMode && state.canResizeKeyboard) MenuItem(keyboardResizeIcon, tr(R.string.s_jianpandiaojie), callbacks.onKeyboardResize) else null,
            MenuItem(darkModeIcon, darkModeLabel, callbacks.onToggleDarkMode),
            // 承载区受限时浮动键盘无处安放，隐藏入口
            if (state.canFloatKeyboard) MenuItem(floatingIcon, floatingLabel, floatingAction) else null,
            MenuItem(deployIcon, tr(R.string.s_bushufangan), callbacks.onReloadConfig),
        ).filterNotNull()
    BoxWithConstraints(modifier.fillMaxWidth().homeBackdropCover(state.backgroundColor)) {
        val grid=homeMenuGrid(maxWidth.value,maxHeight.value,configuration.fontScale)
        val pages=menuItems.chunked(grid.pageSize)
        val pager=rememberPagerState(pageCount={pages.size})
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().height(48.dp).padding(horizontal=8.dp),
                verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) {
                TextButton(onClick=callbacks.onDismiss,colors=ButtonDefaults.textButtonColors(contentColor=textColor)) {
                    Icon(Icons.Default.KeyboardArrowDown,contentDescription=null,modifier=Modifier.size(20.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("回到键盘",fontSize=12.sp)
                }
                IconButton(onClick=callbacks.onSettings) {
                    Icon(settingsIcon,contentDescription="设置",tint=textColor,modifier=Modifier.size(21.dp))
                }
            }
            HorizontalPager(pager,Modifier.fillMaxWidth().weight(1f)) { page ->
                val cells=pages[page]+List(grid.pageSize-pages[page].size){null}
                Column(Modifier.fillMaxSize().padding(horizontal=12.dp,vertical=4.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    cells.chunked(grid.columns).forEach { row ->
                        Row(Modifier.fillMaxWidth().weight(1f),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                            row.forEach { item ->
                                if(item==null)Spacer(Modifier.weight(1f))
                                else MenuItemButton(item,itemBgColor,textColor,Modifier.weight(1f).fillMaxHeight(),
                                    style=tileStyle,cornerRadius=preset?.cornerRadius?:12,animations=preset?.animationsEnabled==true)
                            }
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().height(24.dp),horizontalArrangement=Arrangement.spacedBy(6.dp,Alignment.CenterHorizontally),verticalAlignment=Alignment.CenterVertically) {
                if(pages.size>1)repeat(pages.size) { index ->
                    Box(Modifier.size(if(index==pager.currentPage)6.dp else 4.dp).clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha=if(index==pager.currentPage).8f else .25f)))
                }
            }
        }
    }
}

@Composable
fun MenuItemButton(item:MenuItem,bgColor:Color,textColor:Color,modifier:Modifier=Modifier,
    style:HomeKeyStyle=HomeKeyStyle(),cornerRadius:Int=12,animations:Boolean=false) {
    val interactions=remember{MutableInteractionSource()}
    val pressed by interactions.collectIsPressedAsState()
    val action by rememberUpdatedState(item.action)
    CompositionLocalProvider(LocalHomeKeyStyle provides style,LocalHomeAnimationEnabled provides animations,
        LocalHomeKeyShape provides (style.shape?:LocalHomePreset.current?.keyShape?:HomeKeyShape.ROUNDED),
        LocalHomeKeyBorder provides (style.border?:LocalHomePreset.current?.keyBorder?:HomeBorder()),
        LocalKeyCornerRadius provides (if((style.shape?:LocalHomePreset.current?.keyShape)==HomeKeyShape.SQUARE)0 else style.cornerRadius?:cornerRadius).dp) {
        val foreground=homeKeyText(textColor)
        BoxWithConstraints(modifier.clickable(interactionSource=interactions,indication=null,role=Role.Button){action()}) {
            val scale=LocalConfiguration.current.fontScale
            val lines=if(maxHeight.value>=53f+28f*scale)2 else 1
            val edge=(maxHeight.value-14f*scale*lines-8f).coerceIn(20f,48f).dp
            Column(Modifier.fillMaxSize(),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center) {
                Box(Modifier.size(edge).homeKeyEffects(pressed).clip(RoundedCornerShape(LocalKeyCornerRadius.current))
                    .homeKeyFrost().homeKeyFace(homeKeyBackground(bgColor)).homeKeyInnerShadows(),contentAlignment=Alignment.Center) {
                    if(item.textIcon!=null)Text(item.textIcon,color=foreground,fontSize=20.sp,fontWeight=FontWeight.Medium,maxLines=1)
                    else if(item.icon!=null)Icon(item.icon,contentDescription=null,tint=foreground,modifier=Modifier.size(22.dp))
                }
                Spacer(Modifier.height(5.dp))
                Text(item.label,color=foreground,fontSize=11.sp,lineHeight=14.sp,fontWeight=FontWeight.Normal,
                    textAlign=TextAlign.Center,maxLines=lines,overflow=TextOverflow.Ellipsis)
            }
        }
    }
}
