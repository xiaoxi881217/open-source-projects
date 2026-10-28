package com.kingzcheung.xime.appearance

/** 入口装饰编号与编辑光标的home/end动作编号分开。 */
internal fun homeToolbarTargets(items:List<Pair<String,String>>):Map<String,String> = linkedMapOf("more_menu" to "更多").apply {
    items.forEach{(id,label)->put(HomeKeyIds.token(id),label)}
    put("hide_keyboard","收起键盘")
}
