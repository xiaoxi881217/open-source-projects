package com.kingzcheung.xime.model

import java.io.File
import java.io.IOException
import java.security.MessageDigest

internal fun verifyModelFile(file: File, expectedSha256: String, expectedBytes: Long = 0) {
    if (!file.isFile || file.length() == 0L || (expectedBytes > 0 && file.length() != expectedBytes)) throw IOException("模型文件大小不正确")
    if (expectedSha256.isNotBlank()) {
        require(expectedSha256.matches(Regex("[0-9a-fA-F]{64}")))
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input -> val buffer = ByteArray(65536); while (true) { val count=input.read(buffer); if(count<0)break; digest.update(buffer,0,count) } }
        val actual = digest.digest().joinToString("") { "%02x".format(it.toInt() and 255) }
        if (!actual.equals(expectedSha256,ignoreCase=true)) throw IOException("模型完整性校验失败，请重试下载")
    }
}
internal fun safeModelEntry(root: File, name: String): File {
    require(name.isNotBlank() && !File(name).isAbsolute && !Regex("^[A-Za-z]:").containsMatchIn(name)) { "模型包路径不安全" }
    val base=root.canonicalFile
    val target=File(base,name).canonicalFile
    require(target.path.startsWith(base.path+File.separator)) { "模型包路径不安全" }
    return target
}

internal fun hasVerifiedModelMarker(dir: File, sha256: String): Boolean {
    val marker=File(dir,".verified-model")
    return try { marker.isFile && marker.length()==64L && marker.readText()==sha256 } catch (_: IOException) { false }
}
