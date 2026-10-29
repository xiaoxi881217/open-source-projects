package com.kingzcheung.xime.appearance

import org.junit.Assert.*
import org.junit.Test

class HomeAdvancedVisualTest {
    private fun rejects(block:()->Unit){try{block();fail("must reject")}catch(_:IllegalArgumentException){}}
    @Test fun oldThemesKeepDefaultsAndSquareRadiusCanBeRounded() {
        val old=HomePresetCodec.decode("{}".toByteArray())
        assertTrue(old.rowGaps.isEmpty());assertTrue(old.toolbarOrder.isEmpty())
        assertEquals(1f,HomeKeyStyle().textOpacity)
        assertEquals(7f,homeFaceCorner(HomeKeyShape.SQUARE,30f,30f,7f))
        assertEquals(15f,homeFaceCorner(HomeKeyShape.SQUARE,30f,30f,24f))
        assertEquals(15f,homeFaceCorner(HomeKeyShape.CIRCLE,30f,30f,7f))
        assertEquals(0f,homeFaceCorner(HomeKeyShape.SQUARE,0f,0f,7f))
    }
    @Test fun rowGapsAreIndependentAndSmallScreensKeepKeysUsable() {
        val p=HomeVisualPreset(spacingY=3f,rowGaps=mapOf("qwerty" to listOf(0f,12f,4f)))
        assertEquals(0f,p.rowGap("qwerty",0));assertEquals(12f,p.rowGap("qwerty",1));assertEquals(6f,p.rowGap("t9",0))
        assertEquals(listOf(0f,12f,4f),safeHomeRowGaps(listOf(0f,12f,4f),220f,4,3f))
        val gaps=safeHomeRowGaps(listOf(20f,20f,20f),120f,4,4f)
        assertEquals(24f,(120f-8f-gaps.sum())/4,.0001f)
        assertTrue(gaps.all{it>=0});assertEquals(listOf(0f),safeHomeRowGaps(listOf(12f),30f,2,0f))
        rejects{p.copy(rowGaps=mapOf("qwerty" to listOf(Float.NaN))).validate()}
        rejects{p.copy(rowGaps=mapOf("bad" to listOf(1f))).validate()}
    }
    @Test fun largeAndSmallTextOpacitySyncIndependently() {
        val from=HomeKeyStyle(textOpacity=.23f,hintOpacity=.91f)
        val to=HomeKeyStyle(textOpacity=.82f,hintOpacity=.34f)
        val p=HomeVisualPreset(keys=mapOf("qwerty:q" to from,"qwerty:w" to to))
        val next=p.synchronize("qwerty:q",setOf("qwerty:w"),HomeStyleProperty.TEXT_OPACITY)
        assertEquals(to.copy(textOpacity=.23f),next.keys["qwerty:w"])
        assertEquals(to.copy(hintOpacity=.91f),p.synchronize("qwerty:q",setOf("qwerty:w"),HomeStyleProperty.HINT_OPACITY).keys["qwerty:w"])
        rejects{from.copy(textOpacity=Float.NaN).validate()};rejects{from.copy(textOpacity=1.1f).validate()}
    }
    @Test fun regionWallpaperAndReplacementIconAreIncludedInDiyAndTxt() {
        val icon=byteArrayOf(1,2,3,4);val wall=byteArrayOf(5,6,7,8);val a=HomeStickerBundleCodec.hash(icon);val b=HomeStickerBundleCodec.hash(wall)
        val p=HomeVisualPreset(name="整套新装扮",keys=mapOf(
            "chrome:candidate" to HomeKeyStyle(wallpaper=HomeWallpaper("wallpaper:$b",scale=2f),shadows=mapOf(HomeShadowSide.TOP to HomeSideShadow(enabled=true,inset=true))),
            "toolbar:clipboard" to HomeKeyStyle(iconAsset="image:$a",iconSize=32f,labelOffsetX=.25f,textOpacity=.4f)),
            toolbarOrder=listOf("quick_send","clipboard"),rowGaps=mapOf("qwerty" to listOf(3f,12f,6f)))
        val images=mapOf(a to icon,b to wall);assertEquals(images.keys,HomeStickerBundleCodec.imageIds(p))
        val txt=HomeDiyCode.encodeTextFile(HomeStickerBundleCodec.encode(p,images))
        val decoded=HomeDiyCode.readImport(txt.inputStream());assertEquals(p,decoded.preset)
        images.forEach{(id,bytes)->assertArrayEquals(bytes,decoded.images[id])}
        rejects{HomeStickerBundleCodec.encode(p,mapOf(a to icon))}
    }
    @Test fun sharedAssetGetsStricterLimitAndUnsafeIconIsRejected() {
        val id="a".repeat(64)
        val wall=HomeVisualPreset(keys=mapOf("chrome:candidate" to HomeKeyStyle(wallpaper=HomeWallpaper("wallpaper:$id"))))
        assertEquals(HomeStickerBundleCodec.MAX_WALLPAPER_BYTES,HomeStickerBundleCodec.imageLimit(wall,id))
        val both=wall.copy(keys=wall.keys+("toolbar:clipboard" to HomeKeyStyle(iconAsset="image:$id")))
        assertEquals(HomeStickerBundleCodec.MAX_IMAGE_BYTES,HomeStickerBundleCodec.imageLimit(both,id))
        rejects{HomeKeyStyle(iconAsset="https://example.com/icon.png").validate()}
        rejects{HomeKeyStyle(iconSize=Float.POSITIVE_INFINITY).validate()}
        rejects{wall.copy(toolbarOrder=listOf("clipboard","clipboard")).validate()}
    }
}
