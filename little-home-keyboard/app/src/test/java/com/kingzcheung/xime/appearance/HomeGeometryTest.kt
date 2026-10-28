package com.kingzcheung.xime.appearance

import org.junit.Assert.*
import org.junit.Test

class HomeGeometryTest {
    @Test fun trueSquareAndCircleAreCenteredWhileLongFunctionKeysStayLong() {
        listOf(HomeKeyShape.SQUARE,HomeKeyShape.CIRCLE).forEach {
            assertEquals(HomeFaceRect(0f,10f,30f,30f),homeFaceRect(it,30f,50f))
            assertEquals(HomeFaceRect(0f,0f,120f,40f),homeFaceRect(it,120f,40f))
        }
        assertEquals(HomeFaceRect(0f,0f,30f,50f),homeFaceRect(HomeKeyShape.ROUNDED,30f,50f))
    }
    @Test fun borderAndShapeRoundTripAndSingleFieldSyncPreservesEverythingElse() {
        val source=HomeKeyStyle(shape=HomeKeyShape.CIRCLE,border=HomeBorder(HomeBorderKind.DASHED,0xFFABCDEF,0xFF123456,3f,.4f))
        val target=HomeKeyStyle(labelFontSize=20f,border=HomeBorder(HomeBorderKind.SOLID,widthDp=1f))
        val p=HomeVisualPreset(keyShape=HomeKeyShape.SQUARE,frameBorder=source.border!!,keys=mapOf("qwerty:q" to source,"qwerty:w" to target))
        assertEquals(p,HomePresetCodec.decode(HomePresetCodec.encode(p).toByteArray()))
        assertEquals(target.copy(border=target.border!!.copy(widthDp=3f)),p.synchronize("qwerty:q",setOf("qwerty:w"),HomeStyleProperty.BORDER_WIDTH).keys["qwerty:w"])
        assertEquals(source.border,p.withPanelStyle("qwerty:q").panelStyle!!.border)
        val next=p.withGlobalShape(HomeKeyShape.ROUNDED)
        assertNull(next.keys["qwerty:q"]!!.shape);assertEquals(source.border,next.keys["qwerty:q"]!!.border)
    }
    @Test fun legacyDefaultsAndImportBoundsRemainSafe() {
        val p=HomePresetCodec.decode("{}".toByteArray())
        assertEquals(HomeKeyShape.ROUNDED,p.keyShape);assertEquals(HomeBorderKind.NONE,p.keyBorder.kind)
        listOf(HomeBorder(widthDp=10f),HomeBorder(opacity=Float.NaN),HomeBorder(colorLight=0)).forEach{assertTrue(runCatching{it.validate()}.isFailure)}
    }
}
