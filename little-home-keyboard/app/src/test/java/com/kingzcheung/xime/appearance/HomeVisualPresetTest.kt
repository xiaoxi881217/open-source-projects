package com.kingzcheung.xime.appearance

import org.junit.Assert.*
import org.junit.Test
import androidx.compose.ui.graphics.toArgb

class HomeVisualPresetTest {
    private fun rejected(block: () -> Unit) {
        assertTrue(runCatching(block).isFailure)
    }

    @Test fun roundTripKeepsEverySupportedProperty() {
        val preset = HomeVisualPreset(name = "羽茜的小键盘", gradient = true, gradientAngle = 275,
            cornerRadius = 20, spacingX = 2.5f, spacingY = 6f, candidateTextSize = 25,
            light = HomePalette(key = 0xFFFEDCBA), dark = HomePalette(text = 0xFFABCDEF))
        assertEquals(preset, HomePresetCodec.decode(HomePresetCodec.encode(preset).toByteArray()))
    }

    @Test fun rendererReceivesBothPalettesAndGradientDirection() {
        val preset = HomeVisualPreset(gradient = true, gradientAngle = 135)
        val scheme = HomePresetStore.scheme("preview", preset)
        assertEquals(preset.light.key, scheme.keyBgLight.toArgb().toLong() and 0xFFFFFFFF)
        assertEquals(preset.dark.special, scheme.specialKeyDark.toArgb().toLong() and 0xFFFFFFFF)
        assertEquals(135, scheme.keyboardBackground?.angle)
        assertEquals(listOf(preset.dark.background and 0xFFFFFF, preset.dark.gradientEnd and 0xFFFFFF),
            scheme.keyboardBackground?.colorsDark)
    }

    @Test fun importRejectsCredentialsAndNewerVersions() {
        val json = HomePresetCodec.encode(HomeVisualPreset())
        rejected { HomePresetCodec.decode(json.replaceFirst("{", "{\"token\":\"private\",").toByteArray()) }
        rejected { HomePresetCodec.decode(json.replace("\"schemaVersion\": 1", "\"schemaVersion\": 99").toByteArray()) }
        assertFalse(json.contains("clipboard"))
        assertFalse(json.contains("server"))
    }

    @Test fun geometryAndColorsCannotMakeInvisibleOrUnusableKeys() {
        rejected { HomeVisualPreset(light = HomePalette(key = 0x00FFFFFF)).validate() }
        rejected { HomeVisualPreset(spacingX = Float.NaN).validate() }
        rejected { HomeVisualPreset(spacingX = 40f).validate() }
        rejected { HomeVisualPreset(candidateTextSize = 1000).validate() }
        rejected { HomeVisualPreset(cornerRadius = -1).validate() }
    }

    @Test fun importHasBoundedReadAndRejectsInvalidUtf8() {
        var count = 0
        val endless = object : java.io.InputStream() {
            override fun read(): Int { count++; return 32 }
        }
        rejected { HomePresetCodec.read(endless) }
        assertTrue(count <= HomePresetCodec.MAX_BYTES + 1)
        rejected { HomePresetCodec.decode(byteArrayOf(0xC3.toByte(), 0x28)) }
        val valid = HomePresetCodec.encode(HomeVisualPreset()).byteInputStream()
        assertEquals(HomeVisualPreset(), HomePresetCodec.read(valid))
    }
}
