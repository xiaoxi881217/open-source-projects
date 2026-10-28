package com.kingzcheung.xime.rime

import android.content.Context
import android.util.AtomicFile
import android.util.Log
import com.charleskorn.kaml.*
import java.io.File

/** 只增补自有过滤器，不覆盖方案、个人词库或原补丁内容。 */
internal object HomeEmojiPatch {
    private const val filter = "lua_filter@*little_home_emoji"
    private const val begin = "  # little-home-emoji-begin"
    private const val end = "  # little-home-emoji-end"
    private val yaml = Yaml(configuration = YamlConfiguration(strictMode = false))

    fun patch(original:String):String {
        val owned = Regex("(?m)^  # little-home-emoji-begin\\r?\\n[\\s\\S]*?^  # little-home-emoji-end\\r?\\n?")
        val base = original.replace(owned, "")
        val root = if(base.isBlank()) null else yaml.parseToYamlNode(base) as? YamlMap
            ?: if(base.isBlank()) null else error("方案补丁不是映射")
        val node = root?.entries?.entries?.firstOrNull{it.key.content=="patch"}?.value
        require(node==null || node is YamlMap || node is YamlNull){"已有补丁不适合增补"}
        if(base.contains(filter))return base
        val names=(node as? YamlMap)?.entries?.keys?.map{it.content}?.toSet().orEmpty()
        val key=when {
            "engine/filters/+" !in names -> "engine/filters/+"
            "engine/filters/@next" !in names -> "engine/filters/@next"
            else -> generateSequence("0"){"0$it"}.take(16).map{"engine/filters/$it+"}.first{it !in names}
        }
        val entry=if(key.endsWith("@next"))"  \"$key\": $filter\n" else "  \"$key\":\n    - $filter\n"
        val block="$begin\n$entry$end\n"
        val clean=base.trimEnd().removeSuffix("...").trimEnd()
        val patchLine=Regex("(?m)^patch:[ \\t]*(?:#.*)?$").find(clean)
        val result=if(patchLine!=null){
            requireNotNull(patchLine){"请先将行内补丁展开后使用表情候选"}
            val at=patchLine.range.last+1;clean.substring(0,at)+"\n"+block+clean.substring(at).trimStart('\r','\n')+"\n"
        }else { require(node==null){"请先将行内补丁展开后使用表情候选"};clean+(if(clean.isBlank())"" else "\n\n")+"patch:\n"+block }
        val normalized=result.trimEnd()+"\n"
        yaml.parseToYamlNode(normalized)
        return normalized
    }

    @Synchronized fun install(context:Context,rimeDir:File) {
        try {
            val source=context.assets.open("little_home_emoji.lua").bufferedReader().use{it.readText()}
            writeIfChanged(File(rimeDir,"lua/little_home_emoji.lua"),source)
            for(id in listOf("pinyin_simp","t9_pinyin"))if(File(rimeDir,"$id.schema.yaml").isFile){
                val file=File(rimeDir,"$id.custom.yaml")
                runCatching{writeIfChanged(file,patch(if(file.exists())file.readText() else ""))}
                    .onFailure{Log.w("HomeEmojiPatch","Emoji patch kept existing config for $id")}
            }
        }catch(_:Exception){Log.w("HomeEmojiPatch","Emoji setup unavailable; existing input config preserved")}
    }
    private fun writeIfChanged(file:File,text:String){
        if(file.exists() && file.readText()==text)return
        file.parentFile?.mkdirs();val atomic=AtomicFile(file);val out=atomic.startWrite()
        try{out.write(text.toByteArray(Charsets.UTF_8));atomic.finishWrite(out)}catch(e:Exception){atomic.failWrite(out);throw e}
    }
}
