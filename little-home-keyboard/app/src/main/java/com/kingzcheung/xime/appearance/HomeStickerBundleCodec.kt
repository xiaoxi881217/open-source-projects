package com.kingzcheung.xime.appearance

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.security.MessageDigest
import java.util.Base64

@Serializable
private data class HomeAppearanceBundle(val format: String, val bundleVersion: Int,
    val preset: HomeVisualPreset, val assets: Map<String,String>)
data class HomeStickerPackage(val preset: HomeVisualPreset, val images: Map<String,ByteArray>)
object HomeStickerBundleCodec {
    const val MAX_BYTES = 16 * 1024 * 1024
    const val MAX_ASSETS_BYTES = 12 * 1024 * 1024
    const val MAX_IMAGE_BYTES = 1024 * 1024
    const val MAX_WALLPAPER_BYTES = 4 * 1024 * 1024
    private const val FORMAT = "little-home-keyboard-appearance"
    private val json = Json { encodeDefaults=true;prettyPrint=true }
    fun hash(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
    fun imageAssets(preset:HomeVisualPreset):Set<String> = (preset.stickers().map{it.asset}+preset.iconAssets()).filter{it.startsWith("image:")}.toSet()
    fun imageIds(preset: HomeVisualPreset): Set<String> = imageAssets(preset).map{it.substringAfter(':')}.toSet()+preset.wallpapers().map{it.asset.substringAfter(':')}
    fun imageLimit(preset:HomeVisualPreset,id:String):Int = if(preset.wallpapers().any{it.asset=="wallpaper:$id"} && "image:$id" !in imageAssets(preset))MAX_WALLPAPER_BYTES else MAX_IMAGE_BYTES
    private fun validate(preset: HomeVisualPreset, images: Map<String,ByteArray>) {
        HomePresetCodec.encode(preset)
        require(images.keys==imageIds(preset)) { "贴纸资源不完整，或包含没用到的文件" }
        require(images.size<=200 && images.values.sumOf { it.size.toLong() }<=MAX_ASSETS_BYTES) { "贴纸图片太多了" }
        images.forEach { (id,bytes) -> require(bytes.isNotEmpty() && bytes.size<=imageLimit(preset,id) && hash(bytes)==id) { "贴纸图片校验失败" } }
    }
    fun encode(preset: HomeVisualPreset, images: Map<String,ByteArray>): ByteArray {
        validate(preset,images)
        return json.encodeToString(HomeAppearanceBundle(FORMAT,1,preset,images.mapValues { Base64.getEncoder().encodeToString(it.value) }))
            .toByteArray(Charsets.UTF_8).also { require(it.size<=MAX_BYTES) { "整份装扮太大了，请减少图片贴纸" } }
    }
    fun decode(bytes: ByteArray): HomeStickerPackage {
        require(bytes.size<=MAX_BYTES) { "装扮文件太大了" }
        val text=bytes.decodeToString(throwOnInvalidSequence=true)
        if (!(json.parseToJsonElement(text).jsonObject.containsKey("format"))) return HomeStickerPackage(HomePresetCodec.decode(bytes),emptyMap())
        val bundle=json.decodeFromString<HomeAppearanceBundle>(text)
        require(bundle.format==FORMAT && bundle.bundleVersion==1) { "请升级到支持这份装扮的小家键盘" }
        require(bundle.assets.size<=200 && bundle.assets.all { (id,value) -> value.length<=imageLimit(bundle.preset,id)*4/3+4 }) { "贴纸资源太大了" }
        val images=bundle.assets.mapValues { Base64.getDecoder().decode(it.value) }
        validate(bundle.preset,images)
        return HomeStickerPackage(bundle.preset,images)
    }
    fun read(input: java.io.InputStream): HomeStickerPackage {
        val out=java.io.ByteArrayOutputStream();val buffer=ByteArray(8192)
        while (true) { val count=input.read(buffer);if(count<0)break;require(count>0 && out.size()+count<=MAX_BYTES) { "装扮文件太大了" };out.write(buffer,0,count) }
        return decode(out.toByteArray())
    }
}
