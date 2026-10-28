package com.kingzcheung.xime.appearance

import org.junit.Assert.*
import org.junit.Test
import com.kingzcheung.xime.ui.keyboard.t9LeftRailWidthDp

class HomeSpacingTest {
    @Test fun oldSpacingIsPreservedAndSectionGapCanBeZeroOrRestored() {
        val old=HomePresetCodec.decode("{}".toByteArray())
        assertNull(old.sectionSpacing);assertEquals(3f,old.spacingX);assertEquals(4f,old.spacingY)
        val p=old.copy(sectionSpacing=0f,spacingX=.05f,spacingY=.05f)
        assertEquals(p,HomePresetCodec.decode(HomePresetCodec.encode(p).toByteArray()))
        assertNull(p.copy(sectionSpacing=null).sectionSpacing)
        assertTrue(runCatching{p.copy(sectionSpacing=Float.NaN).validate()}.isFailure)
        assertTrue(runCatching{p.copy(sectionSpacing=13f).validate()}.isFailure)
    }
    @Test fun expandedT9RailSharesTheSameColumnGapMath() {
        val tight=t9LeftRailWidthDp(360f,false,0f)
        val loose=t9LeftRailWidthDp(360f,false,12f)
        assertEquals(3.84f,tight-loose,.0001f)
        assertEquals(t9LeftRailWidthDp(360f,false,2f),t9LeftRailWidthDp(360f,false),.0001f)
    }
}
