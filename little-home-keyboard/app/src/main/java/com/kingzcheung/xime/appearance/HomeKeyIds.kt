package com.kingzcheung.xime.appearance

/** 保存用编号与输入动作分开，标点也要有可导入的稳定编号。 */
internal object HomeKeyIds {
    fun token(raw: String): String {
        val id = raw.lowercase(java.util.Locale.ROOT)
        if (id.matches(Regex("[a-z0-9_-]{1,40}"))) return id
        return when (id) {
            ".", "。" -> "period"; ",", "，" -> "comma"; "?", "？" -> "question"
            "!", "！" -> "exclamation"; ";", "；" -> "semicolon"; ":", "：" -> "colon"
            else -> "u_" + java.security.MessageDigest.getInstance("SHA-256").digest(id.toByteArray(Charsets.UTF_8))
                .take(12).joinToString("") { "%02x".format(it) }
        }
    }
    fun normalize(full: String): String {
        val group = full.substringBefore(':')
        return if (group == "qwerty" || group == "t9") "$group:${token(full.substringAfter(':'))}" else full
    }
}
