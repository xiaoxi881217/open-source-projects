package com.kingzcheung.xime.home

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import com.kingzcheung.xime.keyboard.GestureAction
import com.kingzcheung.xime.settings.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal object HomeSwipeSymbolPolicy {
    val common=listOf("@","!","?","#","$","%","&","*","(",")","-","_","+","=","/","\\",":",";","\"","'",",",".","~","…","。","，","？","！")
    fun validate(key:String,text:String) {
        require(key.matches(Regex("[a-z]"))){"请选择一颗字母键"}
        require(text.isNotBlank() && text.codePointCount(0,text.length) in 1..4 && text.none{it.isISOControl()} &&
            text.toByteArray(Charsets.UTF_8).toString(Charsets.UTF_8)==text){"请填1到4个可见字符"}
    }
    fun bind(base:KeyBinding?,text:String?):KeyBinding? {
        if(text==null)return base
        return KeyBinding(base?.tap,base?.doubleTap,base?.longPress,
            KeyAction(GestureAction.COMMIT,text,text,display=DisplayMode.BOTH,bubble=base?.swipeUp?.bubble?:true),
            base?.swipeDown,base?.swipeLeft,base?.swipeRight,base?.width)
    }
    fun decode(text:String):Map<String,String> {
        require(text.length<=8192)
        return Json.decodeFromString<Map<String,String>>(text).also{map->require(map.size<=26);map.forEach{(key,value)->validate(key,value)}}
    }
}

/** 本机输入绑定与装扮分开，绝不写入正在输入的内容。 */
internal object HomeSwipeSymbolStore {
    private val values=mutableStateOf<Map<String,String>>(emptyMap())
    private var initialized=false
    fun initialize(context:Context) {
        if(initialized)return
        values.value=runCatching{context.getSharedPreferences("home-swipe-symbols",Context.MODE_PRIVATE)
            .getString("symbols",null)?.let(HomeSwipeSymbolPolicy::decode)?:emptyMap()}.getOrDefault(emptyMap())
        initialized=true
    }
    fun value(key:String)=values.value[key.lowercase(java.util.Locale.ROOT)]
    fun binding(key:String,base:KeyBinding?)=HomeSwipeSymbolPolicy.bind(base,value(key))
    fun save(context:Context,key:String,text:String?) {
        initialize(context)
        require(key.matches(Regex("[a-z]"))){"请选择一颗字母键"}
        text?.let{HomeSwipeSymbolPolicy.validate(key,it)}
        val next=if(text==null)values.value-key else values.value+(key to text)
        context.getSharedPreferences("home-swipe-symbols",Context.MODE_PRIVATE).edit().putString("symbols",Json.encodeToString(next)).apply()
        values.value=next
        KeysConfigHelper.notifyHomeSymbolsChanged()
    }
}
