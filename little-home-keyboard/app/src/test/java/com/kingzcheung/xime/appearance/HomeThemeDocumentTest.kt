package com.kingzcheung.xime.appearance

import org.junit.Assert.*
import org.junit.Test

class HomeThemeDocumentTest {
    private fun profile() = ThemeProfile(ThemeProfileId.QWERTY_PORTRAIT, listOf(
        ThemeRow("qwertyuiop".map { ThemeKey(it.toString(), KeyRole.LETTER) }),
        ThemeRow("asdfghjkl".map { ThemeKey(it.toString(), KeyRole.LETTER) }),
        ThemeRow("zxcvbnm".map { ThemeKey(it.toString(), KeyRole.LETTER) }),
        ThemeRow(listOf(ThemeKey("language", KeyRole.LANGUAGE), ThemeKey("space", KeyRole.SPACE, 4f),
            ThemeKey("delete", KeyRole.DELETE), ThemeKey("enter", KeyRole.ENTER)))
    ))

    private fun rejected(block: () -> Unit) {
        try { block(); fail("应该拒绝无效主题") } catch (_: IllegalArgumentException) { }
    }

    @Test fun normalPhoneLayoutHasUsableTouchAreas() {
        val p = profile()
        HomeThemeDocument(profiles = listOf(p)).validate()
        p.validateWindow(390f, 844f)
        p.validateWindow(320f, 640f)
    }

    @Test fun essentialsAndSettingsCannotDisappear() {
        rejected { profile().copy(rows = profile().rows.dropLast(1)).validate() }
        rejected { profile().copy(toolbar = listOf("emoji")).validate() }
    }

    @Test fun nonFiniteAndTinyTouchAreasAreRejected() {
        rejected { profile().copy(heightDp = Float.NaN).validate() }
        rejected { profile().copy(widthFraction = Float.POSITIVE_INFINITY).validate() }
        rejected { profile().copy(gapDp = 12f, paddingDp = 24f).validateWindow(320f, 640f) }
        rejected { profile().validateWindow(390f, 300f) }
    }

    @Test fun unknownVersionAndAssetPathsAreRejected() {
        rejected { HomeThemeDocument(schemaVersion = 2, profiles = listOf(profile())).validate() }
        rejected { ThemeBackground(imageAsset = "../../secret").validate() }
        rejected { KeyAppearance(fontAsset = "/data/user/secret").validate() }
        rejected { profile().copy(keys = mapOf("missing" to KeyAppearance())).validate() }
    }
}
