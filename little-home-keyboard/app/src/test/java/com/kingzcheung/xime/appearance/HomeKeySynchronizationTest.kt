package com.kingzcheung.xime.appearance

import org.junit.Assert.*
import org.junit.Test

class HomeKeySynchronizationTest {
    private val a = HomeKeyStyle(backgroundLight = 0xFFAABBCC, backgroundDark = 0xFF123456,
        textLight = 0xFF654321, cornerRadius = 23)
    private val b = HomeKeyStyle(backgroundLight = 0xFF111111, textLight = 0xFFEEEEEE, cornerRadius = 4)
    private val preset = HomeVisualPreset(keys = mapOf("qwerty:a" to a, "qwerty:b" to b, "qwerty:c" to HomeKeyStyle()))

    @Test fun onePropertyLeavesEveryOtherValueAndUnselectedKeyUntouched() {
        val next = preset.synchronize("qwerty:a", setOf("qwerty:b"), HomeStyleProperty.CORNER_RADIUS)
        assertEquals(b.copy(cornerRadius = 23), next.keys["qwerty:b"])
        assertEquals(preset.keys["qwerty:c"], next.keys["qwerty:c"])
        assertEquals(4, preset.keys["qwerty:b"]?.cornerRadius)
    }

    @Test fun wholeStyleIsOneImmutableChangeForMultipleKeys() {
        val next = preset.synchronize("qwerty:a", setOf("qwerty:b", "qwerty:c"), HomeStyleProperty.ALL)
        assertEquals(a, next.keys["qwerty:b"])
        assertEquals(a, next.keys["qwerty:c"])
        assertEquals(b, preset.keys["qwerty:b"])
        assertEquals(next, HomePresetCodec.decode(HomePresetCodec.encode(next).toByteArray()))
    }

    @Test fun oneColorDoesNotOverwriteOtherModeOrTypography() {
        val next = preset.synchronize("qwerty:a", setOf("qwerty:b"), HomeStyleProperty.BACKGROUND_LIGHT)
        assertEquals(b.copy(backgroundLight = a.backgroundLight), next.keys["qwerty:b"])
    }

    @Test fun crossLayoutAndInvalidIdsFailBeforeChangingDraft() {
        assertTrue(runCatching { preset.synchronize("qwerty:a", setOf("t9:2"), HomeStyleProperty.ALL) }.isFailure)
        assertTrue(runCatching { preset.synchronize("qwerty:a", setOf("qwerty:../secret"), HomeStyleProperty.ALL) }.isFailure)
        assertTrue(runCatching { preset.synchronize("qwerty:a", emptySet(), HomeStyleProperty.ALL) }.isFailure)
        assertEquals(b, preset.keys["qwerty:b"])
    }

    @Test fun exportLimitAlsoProtectsSavingLargeKeyMaps() {
        val rich = a.copy(shadows = HomeShadowSide.entries.associateWith { HomeSideShadow.forSide(it).copy(enabled = true) })
        val large = preset.copy(keys = (1..200).associate { "qwerty:key_$it" to rich })
        large.validate()
        assertTrue(runCatching { HomePresetCodec.encode(large) }.isFailure)
        assertEquals(preset, HomePresetCodec.decode(HomePresetCodec.encode(preset).toByteArray()))
    }
}
