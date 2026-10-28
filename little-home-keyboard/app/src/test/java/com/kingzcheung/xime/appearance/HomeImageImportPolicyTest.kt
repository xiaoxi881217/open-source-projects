package com.kingzcheung.xime.appearance

import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream

class HomeImageImportPolicyTest {
    @Test fun cameraSizedImagesAreSampledBeforeDecoding() {
        assertEquals(4,HomeImageImportPolicy.sampleSize(8000,6000,1024))
        assertEquals(1,HomeImageImportPolicy.sampleSize(1024,768,1024))
        try{HomeImageImportPolicy.sampleSize(32768,32768,1024);fail()}catch(_:IllegalArgumentException){}
    }
    @Test fun inputLargerThanOldTwoMbIsAllowedAndStreamLimitIsExact() {
        val bytes=ByteArray(2*1024*1024+1){7}
        assertArrayEquals(bytes,HomeImageImportPolicy.read(ByteArrayInputStream(bytes)))
        assertArrayEquals(byteArrayOf(1,2,3),HomeImageImportPolicy.read(ByteArrayInputStream(byteArrayOf(1,2,3)),3))
        try{HomeImageImportPolicy.read(ByteArrayInputStream(byteArrayOf(1,2,3,4)),3);fail()}catch(_:IllegalArgumentException){}
    }
    @Test fun twelveTimesStickerScaleIsPortableAndOldPresetsStillWork() {
        val p=HomeVisualPreset(keys=mapOf("qwerty:q" to HomeKeyStyle(sticker=HomeSticker(scale=12f))))
        assertEquals(p,HomePresetCodec.decode(HomePresetCodec.encode(p).toByteArray()))
        assertEquals(12f,HomeSticker().transformed(0f,0f,100f,0f).scale)
        try{HomeSticker(scale=12.1f).validate();fail()}catch(_:IllegalArgumentException){}
    }
}
