package com.kingzcheung.xime.appearance

/** 更多页继承通用面板或当前键型的代表键质感，不复制字母贴纸。 */
internal fun HomeVisualPreset.menuKeyStyle(layout:String):HomeKeyStyle {
    val prefix=if(layout=="t9")"t9" else "qwerty"
    val source=panelStyle?:keys["$prefix:${if(prefix=="t9")"2" else "q"}"]
        ?:keys.entries.firstOrNull{it.key.startsWith("$prefix:") && it.key.substringAfter(':').let{s->s.length==1 && s[0].isLetterOrDigit()}}?.value
        ?:HomeKeyStyle()
    return source.copy(sticker=null)
}

internal data class HomeMenuGrid(val columns:Int,val rows:Int) { val pageSize:Int get()=columns*rows }

internal fun homeMenuGrid(widthDp:Float,heightDp:Float,fontScale:Float):HomeMenuGrid {
    val width=widthDp.takeIf{it.isFinite() && it>0}?:360f
    val height=heightDp.takeIf{it.isFinite() && it>0}?:280f
    val scale=fontScale.takeIf{it.isFinite()}?.coerceIn(1f,1.4f)?:1f
    val columns=((width-16f)/(60f*scale+8f)).toInt().coerceIn(2,if(width>=600f)8 else 4)
    val rows=if(height-76f>=2*(72f+16f*(scale-1f))+8f)2 else 1
    return HomeMenuGrid(columns,rows)
}
