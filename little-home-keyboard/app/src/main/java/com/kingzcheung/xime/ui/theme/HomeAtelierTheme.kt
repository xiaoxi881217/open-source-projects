package com.kingzcheung.xime.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.kingzcheung.xime.ui.keyboard.HomeBackdropHost
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** 纸页结构保留；启用小家预设后，内页面颜色统一跟随整体。 */
@Composable
internal fun HomeAtelierTheme(dark: Boolean,followKeyboard:Boolean=true,content: @Composable ()->Unit) {
    val colors=if(dark)darkColorScheme(
        primary=Color(0xFFA3B7D1),onPrimary=Color(0xFF161618),primaryContainer=Color(0xFF29323C),onPrimaryContainer=Color(0xFFE9E9E9),
        secondary=Color(0xFFA9B4BF),secondaryContainer=Color(0xFF30343B),onSecondaryContainer=Color(0xFFE9E9E9),
        background=Color(0xFF161618),onBackground=Color(0xFFE9E9E9),surface=Color(0xFF1D1D20),onSurface=Color(0xFFE9E9E9),
        surfaceContainerLowest=Color(0xFF161618),surfaceContainerLow=Color(0xFF202023),surfaceContainer=Color(0xFF28282C),
        surfaceContainerHigh=Color(0xFF303034),surfaceContainerHighest=Color(0xFF38383E),surfaceVariant=Color(0xFF28282C),
        onSurfaceVariant=Color(0xFFA9AAAF),outline=Color(0xFF68696D),outlineVariant=Color(0xFF3D3D42),surfaceTint=Color(0xFFA3B7D1)
    )else lightColorScheme(
        primary=Color(0xFF546680),onPrimary=Color.White,primaryContainer=Color(0xFFE8EDF1),onPrimaryContainer=Color(0xFF34445C),
        secondary=Color(0xFF606C77),secondaryContainer=Color(0xFFE8EDF1),onSecondaryContainer=Color(0xFF34445C),
        background=Color(0xFFF8F6F2),onBackground=Color(0xFF2C2C2C),surface=Color(0xFFF8F6F2),onSurface=Color(0xFF2C2C2C),
        surfaceContainerLowest=Color(0xFFFFFEFB),surfaceContainerLow=Color(0xFFFCFBF8),surfaceContainer=Color(0xFFF2F1ED),
        surfaceContainerHigh=Color(0xFFECECE8),surfaceContainerHighest=Color(0xFFE5E6E3),surfaceVariant=Color(0xFFEEEFEA),
        onSurfaceVariant=Color(0xFF676E75),outline=Color(0xFF8C9195),outlineVariant=Color(0xFFD9D8D4),surfaceTint=Color(0xFF546680)
    )
    val type=MaterialTheme.typography
    val active=com.kingzcheung.xime.appearance.HomePresetStore.active
    val panelColors=active?.takeIf{followKeyboard}?.let{homePanelColors(if(dark)it.dark else it.light,colors)}?:colors
    val paperType=if(followKeyboard)type else type.copy(
        bodyLarge=type.bodyLarge.copy(fontSize=14.sp,lineHeight=22.sp),
        bodyMedium=type.bodyMedium.copy(fontSize=13.sp,lineHeight=20.sp),
        bodySmall=type.bodySmall.copy(fontSize=11.sp,lineHeight=17.sp),
        labelLarge=type.labelLarge.copy(fontSize=12.sp,fontWeight=FontWeight.Normal),
        labelMedium=type.labelMedium.copy(fontSize=11.sp,fontWeight=FontWeight.Normal),
        titleMedium=type.titleMedium.copy(fontFamily=FontFamily.Serif,fontSize=16.sp,fontWeight=FontWeight.Normal),
        titleSmall=type.titleSmall.copy(fontSize=13.sp,fontWeight=FontWeight.Medium))
    MaterialTheme(colorScheme=panelColors,typography=paperType.copy(
        headlineLarge=type.headlineLarge.copy(fontFamily=FontFamily.Serif,fontWeight=FontWeight.Normal,letterSpacing=1.sp),
        headlineMedium=type.headlineMedium.copy(fontFamily=FontFamily.Serif,fontWeight=FontWeight.Normal,letterSpacing=1.sp),
        titleLarge=type.titleLarge.copy(fontFamily=FontFamily.Serif,fontWeight=FontWeight.Normal,letterSpacing=.8.sp)),
        shapes=MaterialTheme.shapes.copy(medium=RoundedCornerShape(14.dp),large=RoundedCornerShape(23.dp),extraLarge=RoundedCornerShape(27.dp))) {
        if(active!=null && followKeyboard)HomeBackdropHost(active.panelBackground(),dark,Modifier.fillMaxSize()) {
            MaterialTheme(colorScheme=panelColors.copy(background=Color.Transparent),content=content)
        } else content()
    }
}
