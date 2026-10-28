package com.kingzcheung.xime.appearance

import org.junit.Assert.*
import org.junit.Test

class HomeKeyIdsTest {
    @Test fun punctuationGetsStableEditableIdsWithoutChangingExistingKeys() {
        assertEquals("qwerty:period", HomeKeyIds.normalize("qwerty:."))
        assertEquals("qwerty:period", HomeKeyIds.normalize("qwerty:。"))
        assertEquals("qwerty:comma", HomeKeyIds.normalize("qwerty:,"))
        assertEquals("qwerty:shift", HomeKeyIds.normalize("qwerty:shift"))
        assertEquals("t9:2", HomeKeyIds.normalize("t9:2"))
        assertEquals("symbol:！", HomeKeyIds.normalize("symbol:！"))
        assertTrue(HomeKeyIds.token("🙂").matches(Regex("[a-z0-9_-]{1,40}")))
        assertNotEquals(HomeKeyIds.token("🙂"), HomeKeyIds.token("🙃"))
    }

    @Test fun periodStyleRoundTripAndSyncKeepsTheOtherKeys() {
        val p = HomeVisualPreset(keys = mapOf("qwerty:period" to HomeKeyStyle(labelFontSize = 21f), "qwerty:a" to HomeKeyStyle(cornerRadius = 8)))
        val changed = p.synchronize("qwerty:period", setOf("qwerty:comma"), HomeStyleProperty.LABEL_FONT_SIZE)
        assertEquals(21f, changed.keys.getValue("qwerty:comma").labelFontSize)
        assertEquals(p.keys["qwerty:a"], changed.keys["qwerty:a"])
        assertEquals(changed, HomePresetCodec.decode(HomePresetCodec.encode(changed).toByteArray()))
    }
}
