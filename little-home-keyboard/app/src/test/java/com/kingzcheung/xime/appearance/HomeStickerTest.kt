package com.kingzcheung.xime.appearance

import java.io.File
import org.junit.Assert.*
import org.junit.Test

class HomeStickerTest {
    @Test fun coverModeSurvivesGesturesRoundTripAndIsolatedSync() {
        val covered=HomeSticker(clipToKey=true,scale=1.5f,offsetX=.2f)
        assertTrue(covered.transformed(.1f,0f,1.2f,30f).clipToKey)
        val target=HomeSticker(asset="builtin:star",offsetY=.4f)
        val p=HomeVisualPreset(keys=mapOf("qwerty:q" to HomeKeyStyle(sticker=covered),"qwerty:w" to HomeKeyStyle(sticker=target)))
        val next=p.synchronize("qwerty:q",setOf("qwerty:w"),HomeStyleProperty.STICKER_CLIP)
        assertEquals(target.copy(clipToKey=true),next.keys.getValue("qwerty:w").sticker)
        assertEquals(next,HomePresetCodec.decode(HomePresetCodec.encode(next).toByteArray()))
        assertFalse(HomeSticker().clipToKey)
    }
    private fun rejects(block: () -> Unit) { try { block(); fail("Expected rejection") } catch (_: IllegalArgumentException) {} }
    @Test fun gesturesClampAndRejectInvalidInputs() {
        val initial=HomeSticker(rotation=175f)
        val moved=initial.transformed(10f,-10f,20f,20f)
        assertEquals(2f,moved.offsetX);assertEquals(-2f,moved.offsetY)
        assertEquals(12f,moved.scale);assertEquals(-165f,moved.rotation)
        assertEquals(initial,initial.transformed(Float.NaN,0f,1f,0f))
        assertEquals(initial,initial.transformed(0f,0f,-1f,0f))
        rejects { HomeSticker(asset="file:///private").validate() }
        rejects { HomeSticker(scale=Float.POSITIVE_INFINITY).validate() }
    }
    @Test fun syncingOneNumberPreservesImageAndOtherAppearance() {
        val source=HomeKeyStyle(sticker=HomeSticker(scale=2f))
        val target=HomeKeyStyle(cornerRadius=4,sticker=HomeSticker(asset="builtin:star",rotation=42f))
        val preset=HomeVisualPreset(keys=mapOf("qwerty:q" to source,"qwerty:w" to target))
        val next=preset.synchronize("qwerty:q",setOf("qwerty:w","qwerty:e"),HomeStyleProperty.STICKER_SCALE)
        assertEquals(target.copy(sticker=target.sticker!!.copy(scale=2f)),next.keys["qwerty:w"])
        assertNull(next.keys.getValue("qwerty:e").sticker)
        assertEquals(target,preset.keys["qwerty:w"])
        val asset=preset.synchronize("qwerty:q",setOf("qwerty:w"),HomeStyleProperty.STICKER_ASSET)
        assertEquals(target.sticker!!.copy(asset="builtin:heart"),asset.keys.getValue("qwerty:w").sticker)
        assertEquals(source,preset.synchronize("qwerty:q",setOf("qwerty:w"),HomeStyleProperty.ALL).keys["qwerty:w"])
    }
    @Test fun bundleRoundTripRejectsCorruptionAndExtraData() {
        val bytes=byteArrayOf(1,2,3);val id=HomeStickerBundleCodec.hash(bytes)
        val p=HomeVisualPreset(keys=mapOf("qwerty:q" to HomeKeyStyle(sticker=HomeSticker(asset="image:$id"))))
        val encoded=HomeStickerBundleCodec.encode(p,mapOf(id to bytes))
        val decoded=HomeStickerBundleCodec.decode(encoded)
        assertEquals(p,decoded.preset);assertArrayEquals(bytes,decoded.images[id])
        rejects { HomeStickerBundleCodec.encode(p,emptyMap()) }
        rejects { HomeStickerBundleCodec.encode(p,mapOf(id to byteArrayOf(4))) }
        rejects { HomeStickerBundleCodec.encode(HomeVisualPreset(),mapOf(id to bytes)) }
        rejects { HomeStickerBundleCodec.decode(encoded.decodeToString().replace("\"bundleVersion\": 1","\"bundleVersion\": 2").toByteArray()) }
        rejects { HomeStickerBundleCodec.decode(("{\"serverToken\":\"secret\","+encoded.decodeToString().drop(1)).toByteArray()) }
        rejects { HomeStickerBundleCodec.decode(ByteArray(HomeStickerBundleCodec.MAX_BYTES+1)) }
        assertEquals(HomeVisualPreset(),HomeStickerBundleCodec.decode(HomePresetCodec.encode(HomeVisualPreset()).toByteArray()).preset)
    }
    @Test fun browserBundleIncludesTheImageAndNativeFields() {
        val path="tools/appearance-preview/fixtures/browser-sticker.json"
        val file=listOf(File(path),File("../$path")).first { it.isFile }
        val bundle=HomeStickerBundleCodec.decode(file.readBytes())
        val sticker=bundle.preset.keys.getValue("qwerty:q").sticker!!
        assertEquals(1.75f,sticker.scale);assertEquals(33f,sticker.rotation)
        assertTrue(bundle.images.containsKey(sticker.asset.removePrefix("image:")))
    }
}
