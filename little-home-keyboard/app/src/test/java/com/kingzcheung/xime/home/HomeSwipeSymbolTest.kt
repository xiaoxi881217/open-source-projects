package com.kingzcheung.xime.home

import com.kingzcheung.xime.keyboard.GestureAction
import com.kingzcheung.xime.settings.*
import com.kingzcheung.xime.ui.keyboard.swipeUpHandlerFor
import org.junit.Assert.*
import org.junit.Test

class HomeSwipeSymbolTest {
    @Test fun displayAndActualSwipeUseTheSameSymbolInsteadOfOldCapsAction() {
        val base=KeyBinding(swipeUp=KeyAction(GestureAction.SEND_RIME,"A","A"))
        val changed=HomeSwipeSymbolPolicy.bind(base,"@")!!
        assertEquals("@",changed.swipeUp!!.label);assertEquals(DisplayMode.BOTH,changed.swipeUp!!.display)
        val committed=mutableListOf<String>();val pressed=mutableListOf<String>();val actions=mutableListOf<GestureAction>()
        swipeUpHandlerFor(changed.swipeUp,changed.swipeUp!!.value,{pressed+=it},{committed+=it},{action,_->actions+=action},literalCommit=true)!!.invoke("old-label")
        assertEquals(listOf("@"),committed);assertTrue(pressed.isEmpty());assertTrue(actions.isEmpty())
    }
    @Test fun tapLongPressOtherDirectionsWidthAndBubbleChoiceArePreserved() {
        val tap=KeyAction(GestureAction.COMMIT,"q");val hold=LongPressAction();val side=KeyAction(GestureAction.COMMIT,"x")
        val base=KeyBinding(tap,tap,hold,KeyAction(bubble=false),side,side,side,1.4f)
        val changed=HomeSwipeSymbolPolicy.bind(base,"?")!!
        assertSame(tap,changed.tap);assertSame(tap,changed.doubleTap);assertSame(hold,changed.longPress)
        assertSame(side,changed.swipeDown);assertSame(side,changed.swipeLeft);assertSame(side,changed.swipeRight)
        assertEquals(1.4f,changed.width!!);assertFalse(changed.swipeUp!!.bubble)
        assertSame(base,HomeSwipeSymbolPolicy.bind(base,null))
    }
    @Test fun shortLiteralSymbolsEmojiAndJsonRoundTripAreAllowed() {
        val text="""{"q":"@","w":"👀","e":"?!"}"""
        val map=HomeSwipeSymbolPolicy.decode(text)
        assertEquals("👀",map["w"]);assertEquals("@",HomeSwipeSymbolPolicy.bind(null,map["q"])!!.swipeUp!!.value)
        HomeSwipeSymbolPolicy.common.forEach{HomeSwipeSymbolPolicy.validate("q",it)}
    }
    @Test fun functionKeysControlsBrokenUnicodeAndLongPayloadsAreRejected() {
        listOf("space","delete","voice","qw","Q").forEach{assertTrue(runCatching{HomeSwipeSymbolPolicy.validate(it,"@")}.isFailure)}
        listOf(""," ","\n","abcde","\uD800").forEach{assertTrue(runCatching{HomeSwipeSymbolPolicy.validate("q",it)}.isFailure)}
        assertTrue(runCatching{HomeSwipeSymbolPolicy.decode("x".repeat(8193))}.isFailure)
    }
}
