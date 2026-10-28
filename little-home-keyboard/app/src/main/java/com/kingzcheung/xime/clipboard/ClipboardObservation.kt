package com.kingzcheung.xime.clipboard

internal object ClipboardObservation {
    fun stamp(copiedAt: Long, text: String): String = copiedAt.toString() + ":" +
        java.security.MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
}
