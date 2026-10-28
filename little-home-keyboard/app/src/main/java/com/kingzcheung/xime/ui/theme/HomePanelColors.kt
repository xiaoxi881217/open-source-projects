package com.kingzcheung.xime.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.kingzcheung.xime.appearance.HomePalette

/** 内页面直接沿用整体颜色；错误等语义色保留系统职责。 */
internal fun homePanelColors(p:HomePalette,base:ColorScheme):ColorScheme {
 val text=Color(p.text);val candidate=Color(p.candidate);val accent=Color(p.accent);val key=Color(p.key);val special=Color(p.special);val bg=Color(p.background)
 return base.copy(primary=accent,onPrimary=if(accent.luminance()>.45f)Color.Black else Color.White,primaryContainer=special,onPrimaryContainer=text,
  secondary=accent,onSecondary=if(accent.luminance()>.45f)Color.Black else Color.White,secondaryContainer=special,onSecondaryContainer=text,
  tertiary=accent,tertiaryContainer=special,onTertiaryContainer=text,
  background=bg,onBackground=text,surface=key,onSurface=text,surfaceVariant=special,onSurfaceVariant=candidate,
  surfaceContainerLowest=bg,surfaceContainerLow=key,surfaceContainer=key,surfaceContainerHigh=special,surfaceContainerHighest=special,
  surfaceTint=accent,outline=candidate.copy(alpha=.55f),outlineVariant=candidate.copy(alpha=.22f),inverseSurface=text,inverseOnSurface=key,inversePrimary=special)
}
@Composable
internal fun HomePanelTheme(p:HomePalette,content:@Composable ()->Unit){MaterialTheme(colorScheme=homePanelColors(p,MaterialTheme.colorScheme),content=content)}
