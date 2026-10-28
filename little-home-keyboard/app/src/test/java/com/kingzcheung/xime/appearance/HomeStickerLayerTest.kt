package com.kingzcheung.xime.appearance

import org.junit.Assert.*
import org.junit.Test

class HomeStickerLayerTest {
    @Test fun legacyAndUnclippedStickersRemainAboveTheKeyboard() {
        val legacy=HomePresetCodec.decode("""{"keys":{"qwerty:q":{"sticker":{"asset":"builtin:heart","clipToKey":true}}}}""".toByteArray())
        assertEquals(HomeStickerLayer.ABOVE_KEY,legacy.keys.getValue("qwerty:q").sticker!!.effectiveLayer())
        assertEquals(HomeStickerLayer.ABOVE_KEY,HomeSticker(layer=HomeStickerLayer.BELOW_SHADOW).effectiveLayer())
    }
    @Test fun layerOnlySyncPreservesImageGeometryAndCoverMode() {
        val from=HomeSticker(clipToKey=true,layer=HomeStickerLayer.BELOW_SHADOW)
        val into=HomeSticker(asset="builtin:star",scale=3f,offsetX=-.7f,clipToKey=true)
        val preset=HomeVisualPreset(keys=mapOf("qwerty:q" to HomeKeyStyle(sticker=from),"qwerty:w" to HomeKeyStyle(sticker=into,labelFontSize=22f)))
        val result=preset.synchronize("qwerty:q",setOf("qwerty:w"),HomeStyleProperty.STICKER_LAYER).keys.getValue("qwerty:w")
        assertEquals(into.copy(layer=from.layer),result.sticker);assertEquals(22f,result.labelFontSize)
        assertEquals(HomeStickerLayer.BELOW_SHADOW,from.transformed(.1f,.1f,2f,30f).layer)
    }
    @Test fun allLayersRoundTripThroughFullResourceBundlesAndPanelSync() {
        HomeStickerLayer.entries.forEach { layer ->
            val preset=HomeVisualPreset(keys=mapOf("qwerty:q" to HomeKeyStyle(sticker=HomeSticker(clipToKey=true,layer=layer)))).withPanelStyle("qwerty:q")
            assertEquals(preset,HomeStickerBundleCodec.decode(HomeStickerBundleCodec.encode(preset,emptyMap())).preset)
        }
        assertTrue(runCatching{HomePresetCodec.decode("""{"keys":{"qwerty:q":{"sticker":{"layer":"unknown"}}}}""".toByteArray())}.isFailure)
    }
}
