package com.kingzcheung.xime.appearance

import org.junit.Assert.*
import org.junit.Test

class HomeHintStyleTest {
    private val source=HomeKeyStyle(hintOffsetX=.3f,hintOffsetY=-.2f,hintFontSize=16f,
        hintLight=0xFFFF6699,hintDark=0xFFAABBCC,hintOpacity=.7f,hintVisible=false)

    @Test fun oldPresetsKeepOriginalHintsAndNewFieldsRoundTrip() {
        val old="""{"schemaVersion":1,"keys":{"qwerty:q":{"labelFontSize":20}}}"""
        val original=HomePresetCodec.decode(old.toByteArray()).keys.getValue("qwerty:q")
        assertNull(original.hintFontSize);assertNull(original.hintLight);assertTrue(original.hintVisible)
        assertEquals(0f,original.hintOffsetY);assertEquals(1f,original.hintOpacity)
        val preset=HomeVisualPreset(keys=mapOf("qwerty:q" to source))
        assertEquals(preset,HomePresetCodec.decode(HomePresetCodec.encode(preset).toByteArray()))
    }

    @Test fun individualAndWholeHintSyncPreserveMainLabelAndOtherVisuals() {
        val target=HomeKeyStyle(labelFontSize=25f,labelOffsetX=.1f,cornerRadius=4,hintOffsetY=.4f,hintDark=0xFF123456)
        val preset=HomeVisualPreset(keys=mapOf("qwerty:q" to source,"qwerty:w" to target))
        val x=preset.synchronize("qwerty:q",setOf("qwerty:w"),HomeStyleProperty.HINT_X).keys.getValue("qwerty:w")
        assertEquals(target.copy(hintOffsetX=.3f),x)
        val all=preset.synchronize("qwerty:q",setOf("qwerty:w"),HomeStyleProperty.HINT_ALL).keys.getValue("qwerty:w")
        assertEquals(25f,all.labelFontSize);assertEquals(.1f,all.labelOffsetX);assertEquals(4,all.cornerRadius)
        assertEquals(source.hintDark,all.hintDark);assertFalse(all.hintVisible);assertEquals(16f,all.hintFontSize)
        val color=preset.synchronize("qwerty:q",setOf("qwerty:w"),HomeStyleProperty.HINT_LIGHT).keys.getValue("qwerty:w")
        assertEquals(target.copy(hintLight=source.hintLight),color)
        assertEquals(source,preset.withPanelStyle("qwerty:q").panelStyle?.let{it.copy(backgroundLight=null,backgroundDark=null,textLight=null,textDark=null,cornerRadius=null)})
    }

    @Test fun gesturesClampWithoutChangingMainLabelOrInvalidInput() {
        assertEquals(.8f,source.movedHint(100f,0f).hintOffsetX)
        assertEquals(-.8f,source.movedHint(0f,-100f).hintOffsetY)
        assertEquals(24f,source.resizedHint(100f).hintFontSize)
        assertEquals(2f,source.resizedHint(.01f).hintFontSize)
        assertEquals(source,source.movedHint(Float.NaN,0f));assertEquals(source,source.resizedHint(Float.POSITIVE_INFINITY))
        assertNull(source.movedHint(.1f,.1f).resizedHint(2f).labelFontSize)
    }

    @Test fun unsafeImportedHintParametersAreRejected() {
        listOf(source.copy(hintOffsetX=.81f),source.copy(hintOffsetY=Float.NaN),source.copy(hintFontSize=25f),
            source.copy(hintOpacity=-.1f),source.copy(hintLight=0x00112233)).forEach {
            assertTrue(runCatching{it.validate()}.isFailure)
        }
    }
}
