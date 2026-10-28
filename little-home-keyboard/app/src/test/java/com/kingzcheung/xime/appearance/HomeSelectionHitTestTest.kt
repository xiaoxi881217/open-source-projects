package com.kingzcheung.xime.appearance

import org.junit.Assert.*
import org.junit.Test

class HomeSelectionHitTestTest {
    @Test fun fastStrokeSelectsKeysBetweenSamplesButNotOtherRows() {
        val keys = (0..3).map { HomeSelectionRect(it * 40f, 0f, it * 40f + 30f, 40f) }
        assertTrue(keys.all { it.crosses(-5f, 20f, 160f, 20f) })
        assertFalse(HomeSelectionRect(0f, 50f, 30f, 90f).crosses(-5f, 20f, 160f, 20f))
    }

    @Test fun reversedAndVerticalStrokeWorkWithoutTogglingGaps() {
        val key = HomeSelectionRect(10f, 10f, 30f, 30f)
        assertTrue(key.crosses(40f, 20f, 0f, 20f))
        assertTrue(key.crosses(20f, 0f, 20f, 40f))
        assertTrue(key.crosses(20f, 20f, 20f, 20f))
        assertFalse(key.crosses(5f, 0f, 5f, 40f))
        assertFalse(key.crosses(0f, 0f, 9f, 9f))
    }

    @Test fun clippedInvisibleOrInvalidBoundsNeverSelect() {
        assertFalse(HomeSelectionRect(0f, 0f, 0f, 0f).crosses(0f, 0f, 20f, 20f))
        assertFalse(HomeSelectionRect(30f, 0f, 10f, 30f).crosses(0f, 0f, 20f, 20f))
        assertFalse(HomeSelectionRect(0f, 0f, 20f, 20f).crosses(Float.NaN, 0f, 20f, 20f))
    }
}
