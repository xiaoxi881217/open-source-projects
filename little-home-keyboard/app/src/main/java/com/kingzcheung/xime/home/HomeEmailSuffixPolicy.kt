package com.kingzcheung.xime.home

import java.util.Locale

/** 只处理预设域名；不接受或保存邮箱用户名。 */
internal object HomeEmailSuffixPolicy {
    val defaults=listOf("qq.com","163.com","126.com","gmail.com","outlook.com","icloud.com","foxmail.com","yeah.net")
    private val label=Regex("[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?")
    fun parse(text:String):List<String> {
        require(text.length<=4096){"后缀列表太长了"}
        val domains=text.lineSequence().map{it.trim().removePrefix("@").lowercase(Locale.ROOT)}.filter{it.isNotEmpty()}.distinct().toList()
        require(domains.size in 1..12){"请保留1到12个后缀，每行一个"}
        require(domains.all { domain ->
            val parts=domain.split('.')
            domain.length<=253 && parts.size>=2 && parts.all{label.matches(it)} && parts.last().matches(Regex("[a-z]{2,63}"))
        }){"请只填英文邮箱域名，例如 qq.com，不填完整邮箱或网址"}
        return domains
    }
    fun canRead(enabled:Boolean,privateEditor:Boolean,collapsed:Boolean,composing:Boolean,internalEditor:Boolean,visible:Boolean)=
        enabled && !privateEditor && collapsed && !composing && !internalEditor && visible
    fun isAtSign(text:String?)=text=="@" || text=="＠"
    fun symbolKey(text:String)=if(isAtSign(text))"@" else text
    fun suggestions(before:String?,domains:List<String>):List<String> = if(isAtSign(before))domains.map{"@"+it} else emptyList()
    fun completionEdit(label:String,current:List<String>,before:String?):Pair<Int,String>? {
        val domain=completion(label,current)?:return null
        return when(before){"@"->0 to domain;"＠"->1 to "@$domain";else->null}
    }
    fun canShowOn(page:com.kingzcheung.xime.keyboard.KeyboardPage):Boolean = when(page){
        is com.kingzcheung.xime.keyboard.KeyboardPage.Overlay -> page.route is com.kingzcheung.xime.keyboard.OverlayRoute.Symbol
        else->true
    }
    fun completion(label:String,current:List<String>):String? = label.takeIf{it in current && it.startsWith("@")}?.drop(1)
}
