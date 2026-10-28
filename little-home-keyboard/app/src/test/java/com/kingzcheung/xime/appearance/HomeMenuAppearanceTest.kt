package com.kingzcheung.xime.appearance

import org.junit.Assert.*
import org.junit.Test

class HomeMenuAppearanceTest {
    @Test fun inheritsCurrentKeyTextureWithoutDecorativeStickersOrThemeMutation() {
        val key=HomeKeyStyle(backgroundLight=0xFFFEDCBA,cornerRadius=21,keyOpacity=.45f,frost=.8f,
            motion=HomePressMotion.JELLY,shadows=mapOf(HomeShadowSide.TOP to HomeSideShadow.forSide(HomeShadowSide.TOP).copy(enabled=true)),sticker=HomeSticker())
        val preset=HomeVisualPreset(keys=mapOf("qwerty:q" to key,"t9:2" to key.copy(keyOpacity=.2f)))
        assertEquals(key.copy(sticker=null),preset.menuKeyStyle("qwerty"))
        assertEquals(.2f,preset.menuKeyStyle("t9").keyOpacity)
        assertNotNull(preset.keys.getValue("qwerty:q").sticker)
    }

    @Test fun commonPanelStyleWinsAndMissingRepresentativeKeyFallsBackSafely() {
        val common=HomeKeyStyle(frost=.7f)
        assertEquals(common,HomeVisualPreset(panelStyle=common).menuKeyStyle("qwerty"))
        assertEquals(HomeKeyStyle(),HomeVisualPreset().menuKeyStyle("qwerty"))
        assertEquals(common,HomeVisualPreset(keys=mapOf("qwerty:w" to common)).menuKeyStyle("qwerty"))
        assertEquals(HomeKeyStyle(),HomeVisualPreset(keys=mapOf("toolbar:settings" to common)).menuKeyStyle("qwerty"))
    }

    @Test fun gridsKeepPagesUsableOnNarrowShortAndLargeTextKeyboards() {
        assertEquals(HomeMenuGrid(4,2),homeMenuGrid(360f,288f,1f))
        val short=homeMenuGrid(720f,160f,1f);assertEquals(1,short.rows);assertEquals(8,short.columns)
        val narrow=homeMenuGrid(280f,220f,1f);assertEquals(3,narrow.columns)
        val large=homeMenuGrid(320f,220f,2f);assertEquals(3,large.columns);assertEquals(1,large.rows)
        assertEquals(homeMenuGrid(360f,280f,1f),homeMenuGrid(Float.NaN,Float.POSITIVE_INFINITY,Float.NaN))
        val entries=(0..26).toList();assertEquals(entries,entries.chunked(short.pageSize).flatten())
    }
}
