package com.kingzcheung.xime.appearance

/** 入口装饰编号与编辑光标的home/end动作编号分开。 */
internal fun homeToolbarTargets(items:List<Pair<String,String>>):Map<String,String> = linkedMapOf("more_menu" to "更多").apply {
    items.forEach{(id,label)->put(HomeKeyIds.token(id),label)}
    put("hide_keyboard","收起键盘")
}

internal fun homeToolbarOrder(items:List<String>,saved:List<String>,showMore:Boolean=true,showHide:Boolean=true):List<String> {
    val middle=items.filterNot{it in setOf("more_menu","hide_keyboard")}.distinct()
    val available=(if(showMore)listOf("more_menu") else emptyList())+middle+(if(showHide)listOf("hide_keyboard") else emptyList())
    val known=saved.filter{it in available}.distinct()
    return if(known.any{it=="more_menu" || it=="hide_keyboard"})known+(available-known.toSet())
    else (if(showMore)listOf("more_menu") else emptyList())+known+(middle-known.toSet())+(if(showHide)listOf("hide_keyboard") else emptyList())
}
internal fun moveHomeToolbar(order:List<String>,from:String,to:String):List<String> {
    val source=order.indexOf(from);val target=order.indexOf(to)
    if(source<0 || target<0 || source==target)return order
    return order.toMutableList().apply{add(target,removeAt(source))}
}

internal fun homeChromeInset(border:HomeBorder):Float = if(border.kind==HomeBorderKind.NONE)0f else border.widthDp+2f
internal fun fittedHomeCandidateTextSize(requested:Float,fontScale:Float,border:HomeBorder):Float =
    minOf(requested,((44f-homeChromeInset(border)*2-4f).coerceAtLeast(1f)/(1.35f*fontScale.coerceAtLeast(.5f)))).coerceAtLeast(1f)
