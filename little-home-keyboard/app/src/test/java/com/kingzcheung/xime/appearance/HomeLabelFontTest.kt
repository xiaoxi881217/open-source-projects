package com.kingzcheung.xime.appearance

import org.junit.Assert.*
import org.junit.Test

class HomeLabelFontTest {
    @Test fun pinchScalesOnlyTextAndClamps() {
        val original=HomeKeyStyle(sticker=HomeSticker(),labelOffsetX=.2f)
        assertEquals(original,original.resizedLabel(1f));assertEquals(original,original.resizedLabel(Float.NaN))
        assertEquals(original.copy(labelFontSize=27f),original.resizedLabel(1.5f))
        assertEquals(32f,original.resizedLabel(100f).labelFontSize!!)
        assertEquals(2f,original.resizedLabel(.01f).labelFontSize!!)
    }
    @Test fun fontSyncPreservesPositionAndStickerAndCanResetOverride() {
        val target=HomeKeyStyle(labelOffsetY=.2f,sticker=HomeSticker(),labelFontSize=12f)
        val p=HomeVisualPreset(keys=mapOf("qwerty:q" to HomeKeyStyle(labelFontSize=26f),"qwerty:w" to target))
        assertEquals(target.copy(labelFontSize=26f),p.synchronize("qwerty:q",setOf("qwerty:w"),HomeStyleProperty.LABEL_FONT_SIZE).keys["qwerty:w"])
        assertNull(p.copy(keys=p.keys+("qwerty:q" to HomeKeyStyle())).synchronize("qwerty:q",setOf("qwerty:w"),HomeStyleProperty.LABEL_FONT_SIZE).keys["qwerty:w"]!!.labelFontSize)
    }
    @Test fun newFieldRoundTripsAndRejectsInvalidSize() {
        val p=HomeVisualPreset(keys=mapOf("qwerty:q" to HomeKeyStyle(labelFontSize=24f)))
        assertEquals(p,HomePresetCodec.decode(HomePresetCodec.encode(p).toByteArray()))
        assertThrows(IllegalArgumentException::class.java){HomeKeyStyle(labelFontSize=33f).validate()}
        assertThrows(IllegalArgumentException::class.java){HomeKeyStyle(labelFontSize=Float.NaN).validate()}
    }
}
