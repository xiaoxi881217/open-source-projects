package com.kingzcheung.xime.appearance

import org.junit.Assert.*
import org.junit.Test

class HomeKeySizeTest {
    @Test fun oldThemesRetainTheirOriginalSize() {
        val p=HomePresetCodec.decode("{}".toByteArray())
        assertEquals(1f,HomeKeyStyle().widthScale);assertEquals(1f,HomeKeyStyle().heightScale)
        assertEquals(3f,p.keyWidth("qwerty:space",3f))
    }
    @Test fun wideningOneKeyChangesOnlyItsRelativeWeight() {
        val p=HomeVisualPreset(keys=mapOf("qwerty:q" to HomeKeyStyle(widthScale=2f),"qwerty:space" to HomeKeyStyle(widthScale=.5f)))
        assertEquals(2f,p.keyWidth("qwerty:q",1f));assertEquals(1f,p.keyWidth("qwerty:w",1f))
        assertEquals(1.5f,p.keyWidth("qwerty:space",3f))
        assertEquals(1.8f,p.copy(keys=mapOf("qwerty:period" to HomeKeyStyle(widthScale=1.8f))).keyWidth("qwerty:.",1f))
        val widths=listOf("q","w","e").map{p.keyWidth("qwerty:$it",1f)}.map{it/4f*320f}
        assertEquals(listOf(160f,80f,80f),widths);assertEquals(320f,widths.sum())
    }
    @Test fun pinchIsBoundedAndInvalidInputDoesNotChangeAppearance() {
        val style=HomeKeyStyle(widthScale=1.2f,heightScale=.8f)
        assertEquals(style,style.resizedKey(Float.NaN));assertEquals(style,style.resizedKey(-1f))
        assertEquals(style.copy(widthScale=2f,heightScale=1f),style.resizedKey(10f))
        assertEquals(style.copy(widthScale=.5f,heightScale=.4f),style.resizedKey(.1f))
        assertEquals(.6f,style.resizedKey(.5f).widthScale,.0001f)
    }
    @Test fun dimensionsCanSyncIndividuallyOrTogetherWithoutChangingOtherStyles() {
        val from=HomeKeyStyle(widthScale=1.5f,heightScale=.6f)
        val into=HomeKeyStyle(widthScale=.75f,heightScale=.9f,cornerRadius=3,sticker=HomeSticker())
        val p=HomeVisualPreset(keys=mapOf("qwerty:q" to from,"qwerty:w" to into))
        fun result(property:HomeStyleProperty)=p.synchronize("qwerty:q",setOf("qwerty:w"),property).keys.getValue("qwerty:w")
        assertEquals(into.copy(widthScale=1.5f),result(HomeStyleProperty.WIDTH))
        assertEquals(into.copy(heightScale=.6f),result(HomeStyleProperty.HEIGHT))
        assertEquals(into.copy(widthScale=1.5f,heightScale=.6f),result(HomeStyleProperty.SIZE))
    }
    @Test fun sizeAndCompactRowsSurviveDiyAndTxt() {
        val p=HomeVisualPreset(spacingY=.025f,rowGaps=mapOf("t9" to listOf(.05f,0f)),keys=mapOf("t9:2" to HomeKeyStyle(widthScale=1.35f,heightScale=.55f)))
        val bundle=HomeStickerBundleCodec.encode(p,emptyMap())
        assertEquals(p,HomeDiyCode.decode(HomeDiyCode.encode(bundle)).preset)
        assertEquals(p,HomeDiyCode.readImport(HomeDiyCode.encodeTextFile(bundle).inputStream()).preset)
    }
    @Test fun globalGapActuallyReleasesPreviouslyOverriddenRowsAndAcceptsZero() {
        val p=HomeVisualPreset(rowGaps=mapOf("qwerty" to listOf(20f,15f),"t9" to listOf(12f)))
        val tight=p.withGlobalRowGap(.1f)
        assertTrue(tight.rowGaps.isEmpty());assertEquals(.1f,tight.rowGap("qwerty",0));assertEquals(.1f,tight.rowGap("t9",0))
        val zero=tight.withGlobalRowGap(0f);assertEquals(0f,zero.rowGap("qwerty",1))
        assertEquals(listOf(0f,0f),safeHomeRowGaps(List(2){zero.rowGap("qwerty",it)},150f,3,0f))
        assertEquals(listOf(20f,15f),p.rowGaps["qwerty"])
    }
    @Test fun importedSizesAndNegativeOrNonFiniteGapsAreValidated() {
        listOf(HomeKeyStyle(widthScale=Float.NaN),HomeKeyStyle(widthScale=0f),HomeKeyStyle(heightScale=1.5f),HomeKeyStyle(heightScale=.1f))
            .forEach{assertTrue(runCatching{it.validate()}.isFailure)}
        assertTrue(runCatching{HomeVisualPreset().withGlobalRowGap(-1f)}.isFailure)
        assertTrue(runCatching{HomeVisualPreset().withGlobalRowGap(Float.NaN)}.isFailure)
        assertTrue(runCatching{HomeVisualPreset().withGlobalRowGap(17f)}.isFailure)
    }
}
