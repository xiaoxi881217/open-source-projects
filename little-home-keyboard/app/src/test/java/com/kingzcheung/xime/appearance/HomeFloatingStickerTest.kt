package com.kingzcheung.xime.appearance

import org.junit.Assert.*
import org.junit.Test

class HomeFloatingStickerTest {
    @Test fun floatingResourcesAndChromeStyleSurviveFullBundle() {
        val bytes="synthetic image fixture".toByteArray();val hash=HomeStickerBundleCodec.hash(bytes)
        val p=HomeVisualPreset(floatingStickers=listOf(HomeFloatingSticker("free-one",HomeSticker(asset="image:$hash",offsetX=.1f,scale=.4f))),
            keys=mapOf("chrome:toolbar" to HomeKeyStyle(keyOpacity=.4f,frost=.6f),"toolbar:clipboard" to HomeKeyStyle(sticker=HomeSticker())))
        assertEquals(setOf(hash),HomeStickerBundleCodec.imageIds(p))
        val decoded=HomeStickerBundleCodec.decode(HomeStickerBundleCodec.encode(p,mapOf(hash to bytes)))
        assertEquals(p,decoded.preset);assertArrayEquals(bytes,decoded.images[hash])
    }
    @Test fun invalidFloatingCountDuplicateAndBoundModeAreRejected() {
        fun rejects(p:HomeVisualPreset){try{p.validate();fail("expected rejection")}catch(_:IllegalArgumentException){}}
        rejects(HomeVisualPreset(floatingStickers=List(13){HomeFloatingSticker("free-$it")}))
        rejects(HomeVisualPreset(floatingStickers=List(2){HomeFloatingSticker("same")}))
        rejects(HomeVisualPreset(floatingStickers=listOf(HomeFloatingSticker("free",HomeSticker(clipToKey=true)))))
        assertTrue(HomeVisualPreset().floatingStickers.isEmpty())
    }
    @Test fun floatingMovementIsIndependentOfKeyStylesAndRetainsLayerOrder() {
        val first=HomeFloatingSticker("first");val second=HomeFloatingSticker("second")
        val p=HomeVisualPreset(floatingStickers=listOf(first,second),keys=mapOf("qwerty:a" to HomeKeyStyle(keyOpacity=.6f)))
        val next=p.copy(floatingStickers=p.floatingStickers.map{if(it.id==first.id)it.copy(sticker=it.sticker.transformed(.1f,-.2f,2f,45f)) else it})
        assertEquals(p.keys,next.keys);assertEquals(second,next.floatingStickers.last());assertEquals(listOf("first","second"),next.floatingStickers.map{it.id})
    }
}
