package com.kingzcheung.xime.appearance

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AtomicFile
import android.util.LruCache
import androidx.core.graphics.PathParser
import java.io.ByteArrayOutputStream
import java.io.File

/** 仅自有栅格资源；不保存原名/URI或EXIF，不支持可执行SVG。 */
object HomeStickerAssets {
    private lateinit var directory: File
    private val cache = object : LruCache<String,Bitmap>(16*1024*1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.allocationByteCount
    }
    fun initialize(context: Context) { directory=File(context.filesDir,"home-sticker-assets").apply { mkdirs() } }
    private fun file(id: String): File { require(id.matches(Regex("[a-f0-9]{64}")));return File(directory,"$id.png") }
    fun readInput(input: java.io.InputStream, wallpaper: Boolean=false): ByteArray {
        return HomeImageImportPolicy.read(input)
    }

    @Synchronized fun importImage(bytes: ByteArray, wallpaper: Boolean=false): String {
        require(bytes.isNotEmpty() && bytes.size<=HomeImageImportPolicy.MAX_INPUT_BYTES){"图片太大或没有内容"}
        val edge=if(wallpaper)960 else HomeImageImportPolicy.STICKER_EDGE;val limit=if(wallpaper)HomeStickerBundleCodec.MAX_WALLPAPER_BYTES else HomeStickerBundleCodec.MAX_IMAGE_BYTES;val prefix=if(wallpaper)"wallpaper:" else "image:"
        try {
            val bounds=BitmapFactory.Options().apply { inJustDecodeBounds=true };BitmapFactory.decodeByteArray(bytes,0,bytes.size,bounds)
            require(bounds.outMimeType in setOf("image/png","image/jpeg","image/webp")){"请选择PNG、JPEG或WebP图片"}
            val sample=HomeImageImportPolicy.sampleSize(bounds.outWidth,bounds.outHeight,edge)
            var bitmap=BitmapFactory.decodeByteArray(bytes,0,bytes.size,BitmapFactory.Options().apply{inSampleSize=sample;inPreferredConfig=Bitmap.Config.ARGB_8888})?:error("图片无法打开")
            lateinit var normalized:ByteArray
            try {
                val factor=minOf(1f,edge.toFloat()/maxOf(bitmap.width,bitmap.height))
                if(factor<1f){val smaller=Bitmap.createScaledBitmap(bitmap,maxOf(1,(bitmap.width*factor).toInt()),maxOf(1,(bitmap.height*factor).toInt()),true);if(smaller!==bitmap)bitmap.recycle();bitmap=smaller}
                while(true){
                    val output=ByteArrayOutputStream();require(bitmap.compress(Bitmap.CompressFormat.PNG,100,output)){"图片暂时无法保存"}
                    normalized=output.toByteArray();if(normalized.size<=limit)break
                    require(maxOf(bitmap.width,bitmap.height)>128){"图片仍然太大，请换一张"}
                    val smaller=Bitmap.createScaledBitmap(bitmap,maxOf(1,(bitmap.width*.75f).toInt()),maxOf(1,(bitmap.height*.75f).toInt()),true)
                    if(smaller!==bitmap)bitmap.recycle();bitmap=smaller
                }
            }finally{bitmap.recycle()}
            val id=HomeStickerBundleCodec.hash(normalized);val destination=file(id)
            if(destination.exists() && destination.length()<=limit && HomeStickerBundleCodec.hash(destination.readBytes())==id)return "$prefix$id"
            val images=directory.listFiles().orEmpty().filter { it.extension=="png" }
            require(images.size<200 && images.sumOf { it.length() }+normalized.size<=32*1024*1024){"贴纸库满了，先清理未使用的贴纸吧"}
            val atomic=AtomicFile(destination);val stream=atomic.startWrite()
            try{stream.write(normalized);atomic.finishWrite(stream)}catch(e:Exception){atomic.failWrite(stream);throw e}
            cache.remove("$prefix$id");return "$prefix$id"
        } catch(e:OutOfMemoryError){throw IllegalArgumentException("手机暂时放不下这张图片，试试更小的一张")}
    }
    @Synchronized fun bitmap(asset: String): Bitmap? {
        cache.get(asset)?.let { return it }
        val result=try {
            if(asset in HomeSticker.BUILTINS) builtin(asset) else {
                val wallpaper=asset.startsWith("wallpaper:");val edge=if(wallpaper)960 else HomeImageImportPolicy.STICKER_EDGE
                val p=file(asset.substringAfter(":"));require(p.isFile && p.length()<=(if(wallpaper)HomeStickerBundleCodec.MAX_WALLPAPER_BYTES else HomeStickerBundleCodec.MAX_IMAGE_BYTES))
                val b=BitmapFactory.Options().apply { inJustDecodeBounds=true };BitmapFactory.decodeFile(p.path,b)
                require(b.outMimeType=="image/png" && b.outWidth in 1..edge && b.outHeight in 1..edge)
                BitmapFactory.decodeFile(p.path)
            }
        }catch(_:Exception){null}catch(_:OutOfMemoryError){null}
        if(result!=null)cache.put(asset,result)
        return result
    }
    fun validateReferences(preset: HomeVisualPreset) {
        HomeStickerBundleCodec.imageIds(preset).forEach { id -> require(file(id).isFile && file(id).length()<=HomeStickerBundleCodec.imageLimit(preset,id)){"这份装扮缺少贴纸，请导入带图片的完整装扮文件"} }
    }
    fun export(preset: HomeVisualPreset): ByteArray { validateReferences(preset);return HomeStickerBundleCodec.encode(preset,HomeStickerBundleCodec.imageIds(preset).associateWith { id -> file(id).readBytes() }) }
    fun install(bundle: HomeStickerPackage): HomeVisualPreset {
        val remap=HomeStickerBundleCodec.imageAssets(bundle.preset).map{it.substringAfter(':')}.associateWith { importImage(requireNotNull(bundle.images[it])) }
        val walls=bundle.preset.wallpapers().map{it.asset.substringAfter(':')}.distinct().associateWith{importImage(requireNotNull(bundle.images[it]),wallpaper=true)}
        fun installWallpaper(w:HomeWallpaper?)=w?.copy(asset=requireNotNull(walls[w.asset.substringAfter(':')]))
        val wallpaper=installWallpaper(bundle.preset.wallpaper)
        fun installStyle(style:HomeKeyStyle)=style.copy(sticker=style.sticker?.let{sticker->remap[sticker.asset.removePrefix("image:")]?.let{sticker.copy(asset=it)}?:sticker},
            iconAsset=style.iconAsset?.let{remap[it.removePrefix("image:")]?:it},wallpaper=installWallpaper(style.wallpaper))
        val preset=bundle.preset.copy(floatingStickers=bundle.preset.floatingStickers.map{f->f.copy(sticker=remap[f.sticker.asset.removePrefix("image:")]?.let{f.sticker.copy(asset=it)}?:f.sticker)},wallpaper=wallpaper,panelStyle=bundle.preset.panelStyle?.let(::installStyle),keys=bundle.preset.keys.mapValues{installStyle(it.value)})
        validateReferences(preset);return preset
    }
    @Synchronized fun cleanUnused(presets: Collection<HomeVisualPreset>): Int {
        val used=presets.flatMap { HomeStickerBundleCodec.imageIds(it) }.toSet();var removed=0
        directory.listFiles().orEmpty().filter { it.name.matches(Regex("[a-f0-9]{64}\\.png")) && it.nameWithoutExtension !in used && System.currentTimeMillis()-it.lastModified()>60_000 }.forEach { if(it.delete())removed++ }
        cache.evictAll();return removed
    }
    private fun builtin(asset: String): Bitmap {
        val bitmap=Bitmap.createBitmap(256,256,Bitmap.Config.ARGB_8888);val canvas=Canvas(bitmap);canvas.scale(2.56f,2.56f)
        val paint=Paint(Paint.ANTI_ALIAS_FLAG)
        val layers=when(asset){
            "builtin:heart" -> listOf("M50 88 C8 58 2 35 16 20 C29 7 44 16 50 28 C56 16 71 7 84 20 C98 35 92 58 50 88 Z" to 0xFFF3AEC7)
            "builtin:star" -> listOf("M50 7 L62 34 L92 37 L70 57 L77 88 L50 73 L23 88 L30 57 L8 37 L38 34 Z" to 0xFFF7D77B)
            "builtin:bow" -> listOf("M46 47 C6 9 3 24 8 50 C3 76 6 91 46 53 Z M54 47 C94 9 97 24 92 50 C97 76 94 91 54 53 Z" to 0xFFEAB0CB,"M42 40 L58 40 L58 60 L42 60 Z" to 0xFFD68CAC)
            else -> listOf("M25 76 C4 76 2 48 22 43 C18 17 56 12 65 34 C88 23 104 56 84 68 C80 79 54 76 25 76 Z" to 0xFFCEE7F2)
        }
        for((path,color) in layers){paint.color=color.toInt();canvas.drawPath(requireNotNull(PathParser.createPathFromPathData(path)),paint)}
        return bitmap
    }
}
