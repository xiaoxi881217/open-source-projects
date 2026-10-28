package com.kingzcheung.xime.appearance

import org.junit.Assert.*
import org.junit.Test

class HomePressPopupTest {
    @Test fun hiddenByDefaultAndOnlyOrdinaryPressIsFiltered() {
        val preset=HomeVisualPreset()
        assertFalse(preset.permitsKeyBubble(true,false,false))
        assertFalse(preset.permitsKeyBubble(false,false,false))
        assertTrue(preset.permitsKeyBubble(true,true,false))
        assertTrue(preset.permitsKeyBubble(true,false,true))
        assertTrue(preset.copy(pressPopupEnabled=true).permitsKeyBubble(true,false,false))
        assertFalse(preset.copy(pressPopupEnabled=true).permitsKeyBubble(false,false,false))
    }
    @Test fun presetRoundTripAndLegacyDefaultKeepMotionSeparate() {
        assertFalse(HomePresetCodec.decode("{}".toByteArray()).pressPopupEnabled)
        val preset=HomeVisualPreset(pressPopupEnabled=true,animationsEnabled=false)
        assertEquals(preset,HomePresetCodec.decode(HomePresetCodec.encode(preset).toByteArray()))
        assertFalse(preset.animationsEnabled)
        assertFalse(preset.copy(pressPopupEnabled=false).animationsEnabled)
    }
}
