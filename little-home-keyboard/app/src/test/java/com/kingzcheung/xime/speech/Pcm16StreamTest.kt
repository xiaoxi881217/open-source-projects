package com.kingzcheung.xime.speech

import org.junit.Assert.*
import org.junit.Test

class Pcm16StreamTest {
    @Test fun signedLittleEndianSamplesKeepTheirScale() {
        val pcm=Pcm16Stream()
        assertArrayEquals(floatArrayOf(0f,32767f/32768f,-1f,-1f/32768f),pcm.accept(byteArrayOf(0,0,-1,127,0,-128,-1,-1)),0f)
    }
    @Test fun halfSamplesAcrossBlocksAreNotLost() {
        val pcm=Pcm16Stream()
        assertEquals(0,pcm.accept(byteArrayOf(0)).size)
        assertArrayEquals(floatArrayOf(-1f),pcm.accept(byteArrayOf(-128)),0f)
        assertArrayEquals(floatArrayOf(1f/32768f),pcm.accept(byteArrayOf(1,0,2)),0f)
        assertArrayEquals(floatArrayOf(2f/32768f),pcm.accept(byteArrayOf(0)),0f)
    }
    @Test fun resetDropsOnlyTheUnfinishedSample() {
        val pcm=Pcm16Stream();pcm.accept(byteArrayOf(-1));pcm.reset()
        assertArrayEquals(floatArrayOf(0f),pcm.accept(byteArrayOf(0,0)),0f)
        assertEquals(0,pcm.accept(byteArrayOf()).size)
    }
}
