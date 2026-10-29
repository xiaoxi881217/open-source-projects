package com.kingzcheung.xime.appearance

import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.util.Base64
import java.util.zip.Deflater
import java.util.zip.Inflater

/** 完整装扮的无损文本封装；不依赖服务器或本机素材路径。 */
object HomeDiyCode {
    private const val PREFIX = "XJ1:"
    const val MAX_CHARS = 256 * 1024
    private const val MAX_FILE_CHARS = (HomeStickerBundleCodec.MAX_BYTES + 65536) * 4 / 3 + 8
    fun encode(bundle: ByteArray): String = encodeWithLimit(bundle, MAX_CHARS)
    fun encodeTextFile(bundle: ByteArray): ByteArray = encodeWithLimit(bundle, MAX_FILE_CHARS).toByteArray(Charsets.UTF_8)
    private fun encodeWithLimit(bundle: ByteArray, limit: Int): String {
        HomeStickerBundleCodec.decode(bundle)
        val compact = Json.parseToJsonElement(bundle.decodeToString()).toString().toByteArray(Charsets.UTF_8)
        val deflater = Deflater(Deflater.BEST_COMPRESSION)
        val output = ByteArrayOutputStream()
        try {
            deflater.setInput(compact); deflater.finish()
            val buffer = ByteArray(8192)
            while (!deflater.finished()) {
                val count = deflater.deflate(buffer)
                require(count > 0 && output.size() + count <= limit * 3 / 4) { "图片较多，DIY码太长啦，请导出TXT或完整装扮文件分享" }
                output.write(buffer, 0, count)
            }
        } finally { deflater.end() }
        return (PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(output.toByteArray()))
            .also { require(it.length <= limit) { "DIY码太长啦，请导出TXT或完整装扮文件分享" } }
    }
    fun decode(text: String): HomeStickerPackage = decodeWithLimit(text, MAX_CHARS)
    /** TXT与原有JSON统一入口；读取有上限，不把导入文件当路径或网址执行。 */
    fun readImport(input: java.io.InputStream): HomeStickerPackage {
        val out=ByteArrayOutputStream();val buffer=ByteArray(8192)
        while(true) {
            val count=input.read(buffer);if(count<0)break
            require(count>0 && out.size()+count<=MAX_FILE_CHARS+4096){"装扮文件太大了"}
            out.write(buffer,0,count)
        }
        val bytes=out.toByteArray()
        val text=try { bytes.decodeToString(throwOnInvalidSequence=true).removePrefix("\uFEFF").trimStart() }
            catch(e:java.nio.charset.CharacterCodingException){throw IllegalArgumentException("请使用UTF-8编码的TXT装扮文件",e)}
        return if(text.startsWith("XJ"))decodeWithLimit(text,MAX_FILE_CHARS) else HomeStickerBundleCodec.decode(bytes)
    }
    private fun decodeWithLimit(text: String, limit: Int): HomeStickerPackage {
        require(text.length <= limit + 4096) { "DIY码太长啦，请改用TXT或完整装扮文件" }
        val code = text.filterNot { it == ' ' || it == '\n' || it == '\r' || it == '\t' }
        require(code.startsWith(PREFIX)) { "请粘贴以XJ1:开头的完整DIY码；其他版本请先升级键盘" }
        require(code.length <= limit) { "DIY码太长啦" }
        val value = code.removePrefix(PREFIX)
        require(value.isNotEmpty() && value.all { it in 'A'..'Z' || it in 'a'..'z' || it in '0'..'9' || it == '-' || it == '_' }) { "DIY码里有不认识的字符" }
        val inflater = Inflater()
        val output = ByteArrayOutputStream()
        try {
            inflater.setInput(Base64.getUrlDecoder().decode(value))
            val buffer = ByteArray(8192)
            while (!inflater.finished()) {
                val count = inflater.inflate(buffer)
                require(count > 0 && output.size() + count <= HomeStickerBundleCodec.MAX_BYTES) { "DIY码不完整或解开后太大了" }
                output.write(buffer, 0, count)
            }
            require(inflater.remaining == 0) { "DIY码末尾多了内容" }
        } catch (e: java.util.zip.DataFormatException) {
            throw IllegalArgumentException("DIY码校验失败，请重新复制完整内容", e)
        } finally { inflater.end() }
        return HomeStickerBundleCodec.decode(output.toByteArray())
    }
}
