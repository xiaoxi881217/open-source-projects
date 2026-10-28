package com.kingzcheung.xime.home

import android.content.Context

/** 与主题和剪贴板分离，只保存启用状态及域名。 */
internal object HomeEmailSuffixStore {
    private fun prefs(context:Context)=context.getSharedPreferences("home-email-suffixes",Context.MODE_PRIVATE)
    fun enabled(context:Context)=runCatching{prefs(context).getBoolean("enabled",true)}.getOrDefault(true)
    fun domains(context:Context):List<String> = runCatching {
        prefs(context).getString("domains",null)?.let{HomeEmailSuffixPolicy.parse(it)}?:HomeEmailSuffixPolicy.defaults
    }.getOrDefault(HomeEmailSuffixPolicy.defaults)
    fun save(context:Context,enabled:Boolean,text:String){
        val normalized=HomeEmailSuffixPolicy.parse(text)
        prefs(context).edit().putBoolean("enabled",enabled).putString("domains",normalized.joinToString("\n")).apply()
    }
}
