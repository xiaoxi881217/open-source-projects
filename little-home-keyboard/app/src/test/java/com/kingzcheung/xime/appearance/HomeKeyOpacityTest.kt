package com.kingzcheung.xime.appearance

import org.junit.Assert.*
import org.junit.Test

class HomeKeyOpacityTest {
    @Test fun opacityIsOptionalBoundedAndRoundTrips() {
        assertEquals(1f,HomeKeyStyle().keyOpacity)
        val p=HomeVisualPreset(keys=mapOf("qwerty:q" to HomeKeyStyle(keyOpacity=0f)))
        assertEquals(p,HomePresetCodec.decode(HomePresetCodec.encode(p).toByteArray()))
        assertThrows(IllegalArgumentException::class.java){HomeKeyStyle(keyOpacity=Float.NaN).validate()}
        assertThrows(IllegalArgumentException::class.java){HomeKeyStyle(keyOpacity=1.01f).validate()}
    }
    @Test fun opacitySyncPreservesLabelAndSticker() {
        val target=HomeKeyStyle(labelFontSize=26f,labelOffsetX=.2f,sticker=HomeSticker())
        val p=HomeVisualPreset(keys=mapOf("qwerty:q" to HomeKeyStyle(keyOpacity=.35f),"qwerty:w" to target))
        assertEquals(target.copy(keyOpacity=.35f),p.synchronize("qwerty:q",setOf("qwerty:w"),HomeStyleProperty.KEY_OPACITY).keys["qwerty:w"])
        assertEquals(target,p.keys["qwerty:w"])
    }
}
