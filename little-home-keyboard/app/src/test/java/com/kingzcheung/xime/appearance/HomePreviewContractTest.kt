package com.kingzcheung.xime.appearance

import java.io.File
import org.junit.Assert.*
import org.junit.Test

class HomePreviewContractTest {
    private fun projectFile(path: String) = listOf(File(path), File("../$path")).first { it.isFile }

    @Test fun previewDefaultsMatchNativeDefaults() {
        val bytes = projectFile("tools/appearance-preview/default.json").readBytes()
        assertEquals(HomeVisualPreset(), HomePresetCodec.decode(bytes))
    }

    @Test fun browserExportIsAcceptedAndKeepsUntouchedShadowFields() {
        val preset = HomePresetCodec.decode(projectFile("build/appearance-preview-export.json").readBytes())
        val source = preset.keys.getValue("qwerty:q")
        val target = preset.keys.getValue("qwerty:w")
        assertEquals(HomePressMotion.JELLY, source.motion)
        assertEquals(0.82f, target.shadows.getValue(HomeShadowSide.RIGHT).opacity)
        assertEquals(0xFF123456, target.shadows.getValue(HomeShadowSide.RIGHT).color)
        assertEquals(4, target.cornerRadius)
        assertTrue(target.shadows.getValue(HomeShadowSide.TOP).enabled)
    }
}
