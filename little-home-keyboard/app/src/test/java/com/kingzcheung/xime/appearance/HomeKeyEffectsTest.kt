package com.kingzcheung.xime.appearance

import org.junit.Assert.*
import org.junit.Test

class HomeKeyEffectsTest {
    @Test fun exaggeratedMotionRemainsFiniteAndSmallFontsKeepOtherProperties() {
        val strong=HomeKeyStyle(motionStrength=4f,motionDurationMs=600,labelFontSize=2f,hintFontSize=2f)
        val p=HomeVisualPreset(keys=mapOf("qwerty:q" to strong))
        assertEquals(p,HomePresetCodec.decode(HomePresetCodec.encode(p).toByteArray()))
        HomePressMotion.entries.forEach { motion ->
            listOf(-100f,0f,.5f,1f,100f,Float.NaN).forEach { progress ->
                val value=pressTransform(motion,4f,progress)
                assertTrue(value.scaleX in .5f..1.25f && value.scaleY in .5f..1.25f)
                assertTrue(value.shiftYDp in -2.01f..12f)
            }
        }
        assertTrue(runCatching{strong.copy(motionStrength=4.1f).validate()}.isFailure)
        assertTrue(runCatching{strong.copy(labelFontSize=1f).validate()}.isFailure)
        assertTrue(runCatching{strong.copy(hintFontSize=1f).validate()}.isFailure)
    }
    @Test fun innerShadowSurvivesExportAndOnlyItsPlacementCanBeSynchronized() {
        val inside = HomeSideShadow(enabled = true, inset = true, opacity = 0.8f)
        val outside = HomeSideShadow(enabled = false, opacity = 0.2f, color = 0xFF112233)
        val preset = HomeVisualPreset(keys = mapOf(
            "qwerty:a" to HomeKeyStyle(shadows = mapOf(HomeShadowSide.LEFT to inside)),
            "qwerty:b" to HomeKeyStyle(shadows = mapOf(HomeShadowSide.LEFT to outside))))
        val next = preset.synchronize("qwerty:a", setOf("qwerty:b"),
            HomeStyleProperty.SHADOW_INSET, HomeShadowSide.LEFT)
        assertEquals(outside.copy(inset = true), next.keys["qwerty:b"]!!.shadows[HomeShadowSide.LEFT])
        assertEquals(next, HomePresetCodec.decode(HomePresetCodec.encode(next).toByteArray()))
        val legacy = HomePresetCodec.encode(preset).replace("\"inset\": true,", "").replace("\"inset\": false,", "")
        assertFalse(HomePresetCodec.decode(legacy.toByteArray()).keys["qwerty:a"]!!.shadows[HomeShadowSide.LEFT]!!.inset)
    }

    @Test fun eachSideStartsInItsOwnDirectionAndRejectsUnsafeValues() {
        assertEquals(listOf(270f, 0f, 90f, 180f), HomeShadowSide.entries.map { HomeSideShadow.forSide(it).angleDegrees })
        HomeShadowSide.entries.forEach { HomeSideShadow.forSide(it).validate() }
        assertTrue(runCatching { HomeSideShadow(blurDp = 100f).validate() }.isFailure)
        assertTrue(runCatching { HomeSideShadow(opacity = Float.NaN).validate() }.isFailure)
        assertTrue(runCatching { HomeKeyStyle(motionDurationMs = 10000).validate() }.isFailure)
    }

    @Test fun motionTransformsAreBoundedAndDisabledIsStill() {
        HomePressMotion.entries.forEach { motion ->
            listOf(-100f, 0f, 0.5f, 1f, 100f, Float.NaN).forEach { progress ->
                val value = pressTransform(motion, 1f, progress)
                assertTrue(value.scaleX in 0.85f..1.1f && value.scaleY in 0.85f..1.1f)
                assertTrue(value.shiftYDp in -1f..3f && value.glossAlpha in 0f..0.22f)
            }
        }
        assertEquals(HomePressTransform(), pressTransform(HomePressMotion.NONE, 1f, 1f))
        assertEquals(HomePressTransform(), pressTransform(HomePressMotion.JELLY, 0f, 1f))
    }

    @Test fun syncingOneRightShadowNumberKeepsOtherSidesAndFields() {
        val source = HomeKeyStyle(shadows = mapOf(HomeShadowSide.RIGHT to HomeSideShadow(opacity = 0.8f, blurDp = 9f)))
        val right = HomeSideShadow(enabled = true, color = 0xFF112233, opacity = 0.2f, blurDp = 3f)
        val target = HomeKeyStyle(cornerRadius = 17, motion = HomePressMotion.JELLY,
            shadows = mapOf(HomeShadowSide.RIGHT to right, HomeShadowSide.TOP to HomeSideShadow.forSide(HomeShadowSide.TOP)))
        val preset = HomeVisualPreset(keys = mapOf("qwerty:a" to source, "qwerty:b" to target))
        val next = preset.synchronize("qwerty:a", setOf("qwerty:b"), HomeStyleProperty.SHADOW_OPACITY, HomeShadowSide.RIGHT)
        assertEquals(target.copy(shadows = target.shadows + (HomeShadowSide.RIGHT to right.copy(opacity = 0.8f))), next.keys["qwerty:b"])
        assertEquals(preset.animationsEnabled, next.animationsEnabled)
        assertEquals(next, HomePresetCodec.decode(HomePresetCodec.encode(next).toByteArray()))
    }
}
