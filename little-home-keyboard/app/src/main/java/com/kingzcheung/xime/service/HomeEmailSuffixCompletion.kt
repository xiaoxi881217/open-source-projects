package com.kingzcheung.xime.service

import android.view.inputmethod.InputConnection
import com.kingzcheung.xime.home.HomeEmailSuffixPolicy

/** 候选与实时光标前缀都匹配才补全，只读取一个字符。 */
internal fun completeHomeEmailSuffix(ic:InputConnection,label:String,candidates:List<String>):Boolean {
    if(HomeEmailSuffixPolicy.completion(label,candidates)==null)return false
    val before=runCatching{ic.getTextBeforeCursor(1,0)?.toString()}.getOrNull()
    val edit=HomeEmailSuffixPolicy.completionEdit(label,candidates,before)?:return false
    var removed=false
    return try {
        ic.beginBatchEdit()
        if(edit.first>0){if(!ic.deleteSurroundingText(edit.first,0))return false;removed=true}
        val committed=ic.commitText(edit.second,1)
        if(!committed && removed)runCatching{ic.commitText(before,1)}
        committed
    }catch(_:Exception){
        if(removed)runCatching{ic.commitText(before,1)}
        false
    }finally{runCatching{ic.endBatchEdit()}}
}
