package com.kingzcheung.xime.appearance

import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.Base64
import java.util.zip.DeflaterOutputStream

class HomeDiyCodeTest {
    private fun rejects(block:()->Unit) { try { block();fail("must reject") } catch(_:IllegalArgumentException) {} }
    private fun wrapped(bytes:ByteArray):String {
        val out=ByteArrayOutputStream();DeflaterOutputStream(out).use{it.write(bytes)}
        return "XJ1:"+Base64.getUrlEncoder().withoutPadding().encodeToString(out.toByteArray())
    }
    @Test fun defaultPresetIsLosslessAndShorterThanJson() {
        val preset=HomeVisualPreset();val bundle=HomeStickerBundleCodec.encode(preset,emptyMap())
        val code=HomeDiyCode.encode(bundle)
        assertEquals(preset,HomeDiyCode.decode(code).preset)
        assertTrue(code.length<bundle.size/2)
        println("DIY_DEFAULT_CHARACTERS=${code.length};BUNDLE_BYTES=${bundle.size}")
    }
    @Test fun allAppearanceNumbersAndEmbeddedAssetsSurvive() {
        val image=ByteArray(900){(it%251).toByte()};val id=HomeStickerBundleCodec.hash(image)
        val sticker=HomeSticker(asset="image:$id",offsetX=-.12345f,offsetY=.6789f,scale=5.321f,rotation=123.456f,opacity=.4321f,clipToKey=true,layer=HomeStickerLayer.BELOW_SHADOW)
        val style=HomeKeyStyle(backgroundLight=0xFFABCDEFL,labelFontSize=2.125f,labelOffsetX=.123f,hintFontSize=3.7f,hintOpacity=.4567f,keyOpacity=.6543f,frost=.8765f,
            motion=HomePressMotion.values().last(),motionStrength=3.456f,motionDurationMs=599,sticker=sticker,
            shadows=HomeShadowSide.values().associateWith{HomeSideShadow(enabled=it!=HomeShadowSide.TOP,inset=it==HomeShadowSide.LEFT,opacity=.3456f,angleDegrees=it.defaultAngle,spreadDp=-1.234f)})
        val preset=HomeVisualPreset(name="朋友的装扮",gradient=true,gradientAngle=217,spacingX=1.2345f,spacingY=6.789f,sectionSpacing=.345f,
            candidateTextSize=23,animationsEnabled=true,pressPopupEnabled=true,keyShape=HomeKeyShape.CIRCLE,wallpaper=HomeWallpaper("wallpaper:$id",.123f,-.234f,2.345f,.567f),
            keys=mapOf("qwerty:period" to style,"t9:space" to style.copy(sticker=null),"toolbar:clipboard" to style),panelStyle=style,
            floatingStickers=listOf(HomeFloatingSticker("free",sticker.copy(clipToKey=false))))
        val code=HomeDiyCode.encode(HomeStickerBundleCodec.encode(preset,mapOf(id to image)))
        val restored=HomeDiyCode.decode(code)
        assertEquals(preset,restored.preset);assertArrayEquals(image,restored.images[id]);assertEquals(setOf(id),restored.images.keys)
        println("DIY_FULL_FIXTURE_CHARACTERS=${code.length}")
    }
    @Test fun wrappedLinesAndSpacesCanBePasted() {
        val preset=HomeVisualPreset();val code=HomeDiyCode.encode(HomeStickerBundleCodec.encode(preset,emptyMap()))
        assertEquals(preset,HomeDiyCode.decode(" \n"+code.chunked(32).joinToString("\r\n\t ")+" ").preset)
    }
    @Test fun damagedTruncatedUnknownAndTrailingDataAreRejected() {
        val code=HomeDiyCode.encode(HomeStickerBundleCodec.encode(HomeVisualPreset(),emptyMap()))
        rejects{HomeDiyCode.decode(code.dropLast(7))};rejects{HomeDiyCode.decode(code.replace("XJ1:","XJ2:"))}
        rejects{HomeDiyCode.decode(code+"!")};rejects{HomeDiyCode.decode("XJ1:")}
        val raw=Base64.getUrlDecoder().decode(code.removePrefix("XJ1:"));raw[raw.lastIndex]=(raw.last().toInt() xor 1).toByte()
        rejects{HomeDiyCode.decode("XJ1:"+Base64.getUrlEncoder().withoutPadding().encodeToString(raw))}
        rejects{HomeDiyCode.decode(code+"AAAA")}
    }
    @Test fun oversizedInputAndDecompressionBombAreRejected() {
        rejects{HomeDiyCode.decode("X".repeat(HomeDiyCode.MAX_CHARS+4097))}
        rejects{HomeDiyCode.decode(wrapped(ByteArray(HomeStickerBundleCodec.MAX_BYTES+1)))}
    }
    @Test fun hugeImagesRequireFileInsteadOfSilentOmission() {
        val image=ByteArray(400_000);java.util.Random(10).nextBytes(image);val id=HomeStickerBundleCodec.hash(image)
        val preset=HomeVisualPreset(keys=mapOf("qwerty:q" to HomeKeyStyle(sticker=HomeSticker("image:$id"))))
        rejects{HomeDiyCode.encode(HomeStickerBundleCodec.encode(preset,mapOf(id to image)))}
        val txt=HomeDiyCode.encodeTextFile(HomeStickerBundleCodec.encode(preset,mapOf(id to image)))
        assertTrue(txt.size>HomeDiyCode.MAX_CHARS)
        val imported=HomeDiyCode.readImport(txt.inputStream())
        assertEquals(preset,imported.preset);assertArrayEquals(image,imported.images[id])
        rejects{HomeDiyCode.encode(HomeStickerBundleCodec.encode(HomeVisualPreset(),emptyMap()).decodeToString().replace("\"schemaVersion\": 1","\"schemaVersion\": 999").toByteArray())}
    }
    @Test fun txtAllowsUtf8BomAndOriginalJsonStillImports() {
        val preset=HomeVisualPreset(name="一份TXT装扮",spacingY=7.123f)
        val bundle=HomeStickerBundleCodec.encode(preset,emptyMap())
        val txt=HomeDiyCode.encodeTextFile(bundle)
        assertEquals(preset,HomeDiyCode.readImport(("\uFEFF\n".toByteArray()+txt+"\n".toByteArray()).inputStream()).preset)
        assertEquals(preset,HomeDiyCode.readImport(bundle.inputStream()).preset)
        rejects{HomeDiyCode.readImport("XJ9:anything".byteInputStream())}
    }
    @Test fun txtReaderRejectsOversizedFilesAndInvalidUtf8() {
        rejects{HomeDiyCode.readImport(object:java.io.InputStream(){var left=24*1024*1024
            override fun read():Int=if(left-->0)65 else -1
            override fun read(b:ByteArray,off:Int,len:Int):Int {if(left<=0)return -1;val n=minOf(left,len);b.fill(65,off,off+n);left-=n;return n}
        })}
        rejects{HomeDiyCode.readImport(byteArrayOf(0xc3.toByte(),0x28).inputStream())}
    }
}
