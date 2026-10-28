package com.kingzcheung.xime.appearance

import java.io.File
import org.junit.Assert.*
import org.junit.Test

class HomeEditorInteractionTest {
    @Test fun previewFitsWithoutStretchingOrUpscaling() {
        assertEquals(.5f,HomeEditorViewport.scale(400f,300f,400f,150f))
        assertEquals(.5f,HomeEditorViewport.scale(400f,300f,200f,400f))
        assertEquals(1f,HomeEditorViewport.scale(400f,300f,800f,600f))
        assertEquals(1f,HomeEditorViewport.scale(0f,300f,400f,150f))
    }
    @Test fun preferenceBoundsAndPhoneFrameAreSafe() {
        assertEquals(10,HomeEditorViewport.percent(-1));assertEquals(40,HomeEditorViewport.percent(100))
        assertEquals(118.16f,HomeEditorViewport.frameHeight(844f,14),.01f)
        assertEquals(220f,HomeEditorViewport.frameHeight(844f,50))
        assertEquals(96f,HomeEditorViewport.frameHeight(Float.NaN,14))
    }
    @Test fun draggingTextChangesOnlyPositionAndClamps() {
        val key=HomeKeyStyle(sticker=HomeSticker(),cornerRadius=5,labelOffsetY=.1f)
        assertEquals(key.copy(labelOffsetX=.4f,labelOffsetY=-.4f),key.movedLabel(100f,-100f))
        assertEquals(key,key.movedLabel(Float.NaN,1f))
        assertEquals(key.copy(labelOffsetX=.2f,labelOffsetY=.3f),key.movedLabel(.2f,.2f))
    }
    @Test fun syncSingleAxisPreservesOtherAxisAndSticker() {
        val source=HomeKeyStyle(labelOffsetX=.2f,labelOffsetY=.3f)
        val target=HomeKeyStyle(labelOffsetX=-.1f,labelOffsetY=-.2f,sticker=HomeSticker(asset="builtin:star"),cornerRadius=6)
        val p=HomeVisualPreset(keys=mapOf("qwerty:q" to source,"qwerty:w" to target))
        assertEquals(target.copy(labelOffsetX=.2f),p.synchronize("qwerty:q",setOf("qwerty:w"),HomeStyleProperty.LABEL_X).keys["qwerty:w"])
        assertEquals(target.copy(labelOffsetX=.2f,labelOffsetY=.3f),p.synchronize("qwerty:q",setOf("qwerty:w"),HomeStyleProperty.LABEL_POSITION).keys["qwerty:w"])
        assertEquals(target,p.keys["qwerty:w"])
    }
    @Test fun oldPresetsDefaultToOriginalLabelAndRejectBadCoordinates() {
        assertEquals(0f,HomePresetCodec.decode("{\"keys\":{\"qwerty:q\":{}}}".toByteArray()).keys.getValue("qwerty:q").labelOffsetX)
        assertThrows(IllegalArgumentException::class.java){HomeKeyStyle(labelOffsetX=Float.NaN).validate()}
        assertThrows(IllegalArgumentException::class.java){HomeKeyStyle(labelOffsetY=.41f).validate()}
    }
    @Test fun browserExportKeepsLabelAxesAndUnrelatedStyle() {
        val name="tools/appearance-preview/fixtures/browser-atelier.json"
        val p=HomeStickerBundleCodec.decode(listOf(File(name),File("../$name")).first{it.isFile}.readBytes()).preset
        assertEquals(.190476f,p.keys.getValue("qwerty:w").labelOffsetX,.00001f)
        assertTrue(p.keys.getValue("qwerty:w").labelOffsetY>0f)
        assertEquals(p.keys.getValue("qwerty:q").labelOffsetY,p.keys.getValue("qwerty:w").labelOffsetY)
        assertEquals("builtin:cloud",p.keys.getValue("qwerty:w").sticker!!.asset)
    }
}
