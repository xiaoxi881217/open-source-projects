package com.kingzcheung.xime.appearance

/** 只保存外观；连接凭据、剪贴板与输入历史没有进入此类型的入口。 */
data class HomeThemeDocument(
    val schemaVersion: Int = 1,
    val name: String = "我的小家键盘",
    val profiles: List<ThemeProfile> = emptyList(),
) {
    fun validate() {
        require(schemaVersion == 1) { "这份主题需要其他版本的小家键盘" }
        require(name.isNotBlank() && name.length <= 80) { "主题名称请填1到80个字" }
        require(profiles.size in 1..4 && profiles.map { it.id }.distinct().size == profiles.size) {
            "每种键盘和屏幕方向只保存一份布局"
        }
        profiles.forEach { it.validate() }
    }
}

enum class ThemeProfileId { QWERTY_PORTRAIT, QWERTY_LANDSCAPE, T9_PORTRAIT, T9_LANDSCAPE }
enum class KeyRole { LETTER, DIGIT, SPACE, DELETE, ENTER, SHIFT, LANGUAGE, SYMBOL, FUNCTION }

data class ThemeBackground(
    val colors: List<Long> = listOf(0xFFFFF8F5),
    val gradientAngle: Float = 0f,
    val imageAsset: String? = null,
    val imageAlpha: Float = 1f,
    val imageBlurDp: Float = 0f,
    val imageScale: Float = 1f,
    val imageOffsetX: Float = 0f,
    val imageOffsetY: Float = 0f,
    val overlayColor: Long = 0x00000000,
) {
    fun validate() {
        require(colors.size in 1..8 && colors.all { it in 0..0xFFFFFFFF }) { "背景色无效" }
        require(gradientAngle.isFinite() && gradientAngle in 0f..360f) { "渐变角度无效" }
        require(imageAsset == null || imageAsset.matches(Regex("[a-zA-Z0-9_-]{1,80}"))) {
            "图片须从主题资源库选择"
        }
        bounded(imageAlpha, 0f..1f, "图片透明度")
        bounded(imageBlurDp, 0f..24f, "图片模糊")
        bounded(imageScale, 0.25f..4f, "图片缩放")
        bounded(imageOffsetX, -1f..1f, "图片横向位置")
        bounded(imageOffsetY, -1f..1f, "图片纵向位置")
        require(overlayColor in 0..0xFFFFFFFF) { "遮罩颜色无效" }
    }
}

/** nullable字段表示继承；优先级为整体 → 按键类别 → 单键 → 按下/长按状态。 */
data class KeyAppearance(
    val background: ThemeBackground? = null,
    val textColor: Long? = null,
    val fontAsset: String? = null,
    val fontSizeSp: Float? = null,
    val fontWeight: Int? = null,
    val letterSpacingSp: Float? = null,
    val textOffsetX: Float? = null,
    val textOffsetY: Float? = null,
    val cornerRadiusDp: Float? = null,
    val borderColor: Long? = null,
    val borderWidthDp: Float? = null,
    val shadowDp: Float? = null,
    val opacity: Float? = null,
    val pressScale: Float? = null,
) {
    fun validate() {
        background?.validate()
        listOfNotNull(textColor, borderColor).forEach { require(it in 0..0xFFFFFFFF) { "按键颜色无效" } }
        require(fontAsset == null || fontAsset.matches(Regex("[a-zA-Z0-9_-]{1,80}"))) { "字体须从资源库选择" }
        fontSizeSp?.let { bounded(it, 8f..48f, "字号") }
        fontWeight?.let { require(it in 100..900) { "字体粗细无效" } }
        letterSpacingSp?.let { bounded(it, -2f..8f, "字间距") }
        textOffsetX?.let { bounded(it, -12f..12f, "文字横向位置") }
        textOffsetY?.let { bounded(it, -12f..12f, "文字纵向位置") }
        cornerRadiusDp?.let { bounded(it, 0f..40f, "圆角") }
        borderWidthDp?.let { bounded(it, 0f..6f, "边框") }
        shadowDp?.let { bounded(it, 0f..12f, "阴影") }
        opacity?.let { bounded(it, 0.15f..1f, "按键透明度") }
        pressScale?.let { bounded(it, 0.9f..1f, "按下缩放") }
    }
}

data class ThemeKey(val id: String, val role: KeyRole, val width: Float = 1f)
data class ThemeRow(val keys: List<ThemeKey>, val height: Float = 1f)

data class ThemeProfile(
    val id: ThemeProfileId,
    val rows: List<ThemeRow>,
    val heightDp: Float = 260f,
    val widthFraction: Float = 1f,
    val gapDp: Float = 4f,
    val rowGapDp: Float = 6f,
    val paddingDp: Float = 6f,
    val background: ThemeBackground = ThemeBackground(),
    val baseStyle: KeyAppearance = KeyAppearance(),
    val roles: Map<KeyRole, KeyAppearance> = emptyMap(),
    val keys: Map<String, KeyAppearance> = emptyMap(),
    val pressed: KeyAppearance = KeyAppearance(),
    val longPressed: KeyAppearance = KeyAppearance(),
    val candidateStyle: KeyAppearance = KeyAppearance(),
    val candidateHeightDp: Float = 44f,
    val candidateGapDp: Float = 12f,
    val toolbar: List<String> = listOf("clipboard", "emoji", "settings"),
    val toolbarStyle: KeyAppearance = KeyAppearance(),
    val animationEnabled: Boolean = false,
    val animationDurationMs: Int = 120,
) {
    fun validate() {
        bounded(heightDp, 180f..420f, "键盘高度")
        bounded(widthFraction, 0.65f..1f, "键盘宽度")
        bounded(gapDp, 0f..12f, "按键间距")
        bounded(rowGapDp, 0f..16f, "行间距")
        bounded(paddingDp, 0f..24f, "键盘边距")
        bounded(candidateHeightDp, 36f..100f, "候选栏高度")
        bounded(candidateGapDp, 0f..32f, "候选词间距")
        require(rows.size in 3..6 && rows.all { it.keys.size in 1..14 }) { "请保留3到6行可用按键" }
        val allKeys = rows.flatMap { it.keys }
        val ids = allKeys.map { it.id }
        require(ids.distinct().size == ids.size && ids.all { it.matches(Regex("[a-z0-9_-]{1,40}")) }) {
            "每个按键需要独立的有效编号"
        }
        val qwerty = id == ThemeProfileId.QWERTY_PORTRAIT || id == ThemeProfileId.QWERTY_LANDSCAPE
        require(if (qwerty) ('a'..'z').all { letter ->
            allKeys.any { it.id == letter.toString() && it.role == KeyRole.LETTER }
        } else ('2'..'9').all { digit ->
            allKeys.any { it.id == digit.toString() && it.role == KeyRole.DIGIT }
        }) { "请保留完整的字母或九宫格数字键" }
        rows.forEach { row ->
            bounded(row.height, 0.5f..2f, "行高比例")
            row.keys.forEach { bounded(it.width, 0.5f..8f, "按键宽度比例") }
        }
        require(setOf(KeyRole.SPACE, KeyRole.DELETE, KeyRole.ENTER, KeyRole.LANGUAGE).all { role ->
            allKeys.any { it.role == role }
        }) { "请保留空格、删除、回车和语言切换" }
        require(toolbar.size in 1..12 && toolbar.distinct().size == toolbar.size && "settings" in toolbar &&
            toolbar.all { it in TOOLBAR_IDS }) { "工具栏必须保留设置入口" }
        require(keys.size <= 100 && keys.keys.all { it in ids }) { "单键样式只能引用当前布局中的按键" }
        background.validate()
        listOf(baseStyle, pressed, longPressed, candidateStyle, toolbarStyle).forEach { it.validate() }
        roles.values.forEach { it.validate() }
        keys.values.forEach { it.validate() }
        require(animationDurationMs in 0..250) { "动画时长请保持在250毫秒以内" }
    }

    /** 以实际可用窗口检查最小触控面积，预览与渲染必须使用同份比例结果。 */
    fun validateWindow(windowWidthDp: Float, windowHeightDp: Float) {
        validate()
        require(windowWidthDp.isFinite() && windowHeightDp.isFinite() && windowWidthDp > 0 && windowHeightDp > 0)
        require(heightDp + candidateHeightDp <= windowHeightDp * 0.75f) { "键盘不能遮住过多输入区域" }
        val availableHeight = heightDp - paddingDp * 2 - rowGapDp * (rows.size - 1)
        val totalHeight = rows.sumOf { it.height.toDouble() }.toFloat()
        rows.forEach { row ->
            val availableWidth = windowWidthDp * widthFraction - paddingDp * 2 - gapDp * (row.keys.size - 1)
            val totalWidth = row.keys.sumOf { it.width.toDouble() }.toFloat()
            require(availableHeight * row.height / totalHeight >= 36f &&
                row.keys.all { availableWidth * it.width / totalWidth >= 24f }) {
                "部分按键太小，请减小间距或恢复布局"
            }
        }
    }

    companion object {
        private val TOOLBAR_IDS = setOf("settings", "clipboard", "emoji", "phrases", "layout", "home", "pause")
    }
}

private fun bounded(value: Float, range: ClosedFloatingPointRange<Float>, label: String) {
    require(value.isFinite() && value in range) { "$label 超出安全范围" }
}
