package com.kingzcheung.xime.appearance

/** 固定键盘高度内先为按键留出24dp，再按比例收紧间隔，不改IME窗口尺寸。 */
fun safeHomeRowGaps(requested:List<Float>,height:Float,rows:Int,edge:Float):List<Float> {
    require(rows>0 && requested.size==rows-1 && requested.all{it.isFinite() && it>=0})
    val available=(height-rows*24f-edge*2).coerceAtLeast(0f)
    val total=requested.sum()
    return if(total<=available || total==0f)requested else requested.map{it*available/total}
}
