package com.kingzcheung.xime.appearance

import android.content.Context
import android.util.AtomicFile
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.kingzcheung.xime.settings.BackgroundConfig
import com.kingzcheung.xime.settings.SettingsPreferences
import com.kingzcheung.xime.ui.theme.KeyboardColorScheme
import java.io.File
import java.util.UUID

object HomePresetStore {
    private lateinit var directory: File
    var presets by mutableStateOf<Map<String, HomeVisualPreset>>(emptyMap())
        private set
    var activeId by mutableStateOf("")
    private var schemes by mutableStateOf<Map<String, KeyboardColorScheme>>(emptyMap())
    val active: HomeVisualPreset? get() = presets[activeId]

    private fun publish(next: Map<String, HomeVisualPreset>) {
        schemes = next.mapValues { (id, preset) -> scheme(id, preset) }
        presets = next
    }

    fun initialize(context: Context) {
        HomeStickerAssets.initialize(context)
        directory = File(context.filesDir, "home-appearance").apply { mkdirs() }
        val loaded = directory.listFiles().orEmpty().filter { validId(it.nameWithoutExtension) && it.extension == "json" }
            .take(100).mapNotNull { file ->
                runCatching {
                    require(file.length() <= HomePresetCodec.MAX_BYTES)
                    file.nameWithoutExtension to HomePresetCodec.decode(AtomicFile(file).readFully())
                }.getOrNull()
            }.toMap()
        publish(loaded)
        activeId = SettingsPreferences.getKeyboardTheme(context)
    }

    private fun validId(id: String) = id.matches(Regex("home_[a-f0-9-]{36}"))

    @Synchronized
    fun save(preset: HomeVisualPreset, existingId: String? = null): String {
        preset.validate()
        HomeStickerAssets.validateReferences(preset)
        val id = existingId ?: "home_${UUID.randomUUID()}"
        require(validId(id) && (existingId == null || id in presets)) { "预设编号无效" }
        require(existingId != null || presets.size < 100) { "先整理一些不用的预设吧，最多保存100份" }
        val bytes = HomePresetCodec.encode(preset).toByteArray(Charsets.UTF_8)
        val file = AtomicFile(File(directory, "$id.json"))
        val stream = file.startWrite()
        try {
            stream.write(bytes)
            file.finishWrite(stream)
        } catch (error: Exception) {
            file.failWrite(stream)
            throw error
        }
        publish(presets + (id to preset))
        return id
    }

    @Synchronized
    fun delete(context: Context, id: String) {
        require(validId(id))
        // 先切回底座主题，删除当前预设也不会丢失设置入口。
        if (activeId == id) SettingsPreferences.setKeyboardTheme(context, "lavender_purple")
        AtomicFile(File(directory, "$id.json")).delete()
        publish(presets - id)
    }

    fun scheme(id: String): KeyboardColorScheme? = schemes[id]

    fun scheme(id: String, preset: HomeVisualPreset): KeyboardColorScheme {
        val l = preset.light
        val d = preset.dark
        return KeyboardColorScheme(
            id = id, name = preset.name,
            specialKeyLight = Color(l.special), specialKeyDark = Color(d.special),
            accentLight = Color(l.accent), accentDark = Color(d.accent),
            keyboardBgLight = Color(l.background), keyboardBgDark = Color(d.background),
            keyBgLight = Color(l.key), keyBgDark = Color(d.key),
            candidateBarBgLight = Color(l.background), candidateBarBgDark = Color(d.background),
            keyTextColorLight = Color(l.text), keyTextColorDark = Color(d.text),
            candidateTextColorLight = Color(l.candidate), candidateTextColorDark = Color(d.candidate),
            candidateSelectedTextColorLight = Color(l.accent), candidateSelectedTextColorDark = Color(d.accent),
            keyboardBackground = if (preset.gradient) BackgroundConfig(type = "gradient",
                colors = listOf(l.background and 0xFFFFFF, l.gradientEnd and 0xFFFFFF),
                colorsDark = listOf(d.background and 0xFFFFFF, d.gradientEnd and 0xFFFFFF),
                angle = preset.gradientAngle) else null,
        )
    }
}
