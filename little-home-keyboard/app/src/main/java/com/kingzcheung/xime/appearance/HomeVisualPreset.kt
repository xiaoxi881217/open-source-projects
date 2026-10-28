package com.kingzcheung.xime.appearance

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class HomePalette(
    val background: Long = 0xFFFFF3F5,
    val gradientEnd: Long = 0xFFFFE3EB,
    val key: Long = 0xFFFFFFFF,
    val special: Long = 0xFFF4D4DF,
    val text: Long = 0xFF493C42,
    val candidate: Long = 0xFF493C42,
    val accent: Long = 0xFFAD5978,
) {
    fun validate() {
        require(listOf(background, gradientEnd, key, special, text, candidate, accent)
            .all { it in 0xFF000000..0xFFFFFFFF }) { "请选择不透明的有效颜色" }
    }
}

/** 这一版只包含已接入真实渲染的属性；后续版本以新增可选字段扩展。 */
@Serializable
data class HomeKeyStyle(
    val backgroundLight: Long? = null,
    val backgroundDark: Long? = null,
    val textLight: Long? = null,
    val textDark: Long? = null,
    val cornerRadius: Int? = null,
    val shadows: Map<HomeShadowSide, HomeSideShadow> = emptyMap(),
    val shadowShape: HomeShadowShape = HomeShadowShape.ROUNDED,
    val motion: HomePressMotion = HomePressMotion.NONE,
    val motionStrength: Float = 0.65f,
    val motionDurationMs: Int = 120,
    val sticker: HomeSticker? = null,
    val labelOffsetX: Float = 0f,
    val labelOffsetY: Float = 0f,
    val labelFontSize: Float? = null,
    val hintOffsetX: Float = 0f,
    val hintOffsetY: Float = 0f,
    val hintFontSize: Float? = null,
    val hintLight: Long? = null,
    val hintDark: Long? = null,
    val hintOpacity: Float = 1f,
    val hintVisible: Boolean = true,
    val keyOpacity: Float = 1f,
    val frost: Float = 0f,
    val shape:HomeKeyShape?=null,
    val border:HomeBorder?=null,
) {
    fun movedLabel(dx: Float,dy: Float): HomeKeyStyle = if(!dx.isFinite() || !dy.isFinite())this else
        copy(labelOffsetX=(labelOffsetX+dx).coerceIn(-.4f,.4f),labelOffsetY=(labelOffsetY+dy).coerceIn(-.4f,.4f))
    fun resizedLabel(zoom: Float): HomeKeyStyle = if(!zoom.isFinite() || zoom<=0 || kotlin.math.abs(zoom-1f)<.0001f)this else
        copy(labelFontSize=((labelFontSize?:18f)*zoom).coerceIn(2f,32f))
    fun movedHint(dx: Float,dy: Float): HomeKeyStyle = if(!dx.isFinite() || !dy.isFinite())this else
        copy(hintOffsetX=(hintOffsetX+dx).coerceIn(-.8f,.8f),hintOffsetY=(hintOffsetY+dy).coerceIn(-.8f,.8f))
    fun resizedHint(zoom: Float): HomeKeyStyle = if(!zoom.isFinite() || zoom<=0 || kotlin.math.abs(zoom-1f)<.0001f)this else
        copy(hintFontSize=((hintFontSize?:9f)*zoom).coerceIn(2f,24f))
    fun validate() {
        require(listOfNotNull(backgroundLight, backgroundDark, textLight, textDark, hintLight, hintDark)
            .all { it in 0xFF000000..0xFFFFFFFF }) { "按键颜色无效" }
        require(cornerRadius == null || cornerRadius in 0..24) { "按键圆角超出范围" }
        require(shadows.size <= 4) { "每颗键最多四边阴影" }
        shadows.values.forEach { it.validate() }
        sticker?.validate()
        border?.validate()
        require(labelOffsetX.isFinite() && labelOffsetX in -0.4f..0.4f && labelOffsetY.isFinite() && labelOffsetY in -0.4f..0.4f) { "文字位置超出按键范围" }
        require(labelFontSize==null || labelFontSize.isFinite() && labelFontSize in 2f..32f) { "文字字号请保持在2到32之间" }
        require(hintOffsetX.isFinite() && hintOffsetX in -.8f.. .8f && hintOffsetY.isFinite() && hintOffsetY in -.8f.. .8f) { "小字位置超出安全范围" }
        require(hintFontSize==null || hintFontSize.isFinite() && hintFontSize in 2f..24f) { "小字字号请保持在2到24之间" }
        require(hintOpacity.isFinite() && hintOpacity in 0f..1f) { "小字不透明度超出范围" }
        require(frost.isFinite() && frost in 0f..1f) { "磨砂度超出范围" }
        require(keyOpacity.isFinite() && keyOpacity in 0f..1f) { "键帽透明度超出范围" }
        require(motionStrength.isFinite() && motionStrength in 0f..4f && motionDurationMs in 40..600) {
            "按压动态超出安全范围"
        }
    }
}

enum class HomeStyleProperty(val label: String) {
    SHAPE("按键形状"), BORDER("整套边框"), BORDER_KIND("边框线型"), BORDER_WIDTH("边框粗细"), BORDER_OPACITY("边框不透明度"), BORDER_LIGHT("浅色边框颜色"), BORDER_DARK("深色边框颜色"),
    HINT_ALL("上方小字整套样式"), HINT_POSITION("小字位置"), HINT_X("小字横向位置"), HINT_Y("小字纵向位置"), HINT_FONT_SIZE("小字字号"), HINT_LIGHT("浅色小字颜色"), HINT_DARK("深色小字颜色"), HINT_OPACITY("小字不透明度"), HINT_VISIBLE("小字显示开关"),
    ALL("整套已支持的样式"), BACKGROUND_LIGHT("浅色按键颜色"), BACKGROUND_DARK("深色按键颜色"),
    TEXT_LIGHT("浅色文字颜色"), TEXT_DARK("深色文字颜色"), KEY_OPACITY("键帽底色不透明度"), FROST("键帽磨砂度"), LABEL_POSITION("文字位置"), LABEL_X("文字横向位置"), LABEL_Y("文字纵向位置"), LABEL_FONT_SIZE("文字字号"), CORNER_RADIUS("按键圆角"),
    SHADOW_ALL("四边整套阴影"), SHADOW_SIDE("这一边整套阴影"), SHADOW_SHAPE("阴影形状"), SHADOW_ENABLED("阴影开关"), SHADOW_INSET("阴影内外侧"), SHADOW_COLOR("阴影颜色"),
    SHADOW_OPACITY("阴影深浅"), SHADOW_BLUR("阴影模糊"), SHADOW_SPREAD("阴影扩散"),
    SHADOW_DISTANCE("阴影距离"), SHADOW_ANGLE("阴影角度"),
    STICKER("整张贴纸"), STICKER_ASSET("贴纸图片"), STICKER_X("贴纸横向位置"), STICKER_Y("贴纸纵向位置"),
    STICKER_SCALE("贴纸大小"), STICKER_ROTATION("贴纸旋转"), STICKER_OPACITY("贴纸透明度"), STICKER_CLIP("贴纸覆盖模式"), STICKER_LAYER("贴纸图层"),
    MOTION("按压动态类型"), MOTION_STRENGTH("动态强度"), MOTION_DURATION("动态时长")
}

@Serializable
data class HomeVisualPreset(
    val schemaVersion: Int = 1,
    val name: String = "我的软萌键盘",
    val light: HomePalette = HomePalette(),
    val dark: HomePalette = HomePalette(0xFF29242A, 0xFF3B2A36, 0xFF453D46,
        0xFF684856, 0xFFF7EAF0, 0xFFF7EAF0, 0xFFF0B3CC),
    val gradient: Boolean = false,
    val gradientAngle: Int = 90,
    val cornerRadius: Int = 12,
    val spacingX: Float = 3f,
    val spacingY: Float = 4f,
    val candidateTextSize: Int = 19,
    val keys: Map<String, HomeKeyStyle> = emptyMap(),
    val animationsEnabled: Boolean = false,
    val pressPopupEnabled: Boolean = false,
    val wallpaper: HomeWallpaper? = null,
    val panelStyle: HomeKeyStyle? = null,
    val floatingStickers: List<HomeFloatingSticker> = emptyList(),
    val keyShape:HomeKeyShape=HomeKeyShape.ROUNDED,
    val keyBorder:HomeBorder=HomeBorder(),
    val frameBorder:HomeBorder=HomeBorder(),
    val sectionSpacing:Float?=null,
) {
    fun permitsKeyBubble(pressed:Boolean,swiping:Boolean,longPress:Boolean):Boolean = swiping || longPress || (pressed && pressPopupEnabled)
    fun validate() {
        require(schemaVersion == 1) { "请使用支持这份预设的小家键盘版本" }
        require(name.isNotBlank() && name.length <= 80) { "预设名称请填1到80个字" }
        panelStyle?.validate()
        keyBorder.validate();frameBorder.validate()
        require(sectionSpacing==null || sectionSpacing.isFinite()&&sectionSpacing in 0f..12f){"分区留白超出范围"}
        wallpaper?.validate()
        require(floatingStickers.size<=12 && floatingStickers.map{it.id}.toSet().size==floatingStickers.size){"自由贴纸最多12张，编号不能重复"}
        floatingStickers.forEach{it.validate()}
        light.validate()
        dark.validate()
        require(gradientAngle in 0..360 && cornerRadius in 0..24) { "渐变角度或圆角超出范围" }
        require(spacingX.isFinite() && spacingX in 0f..4f &&
            spacingY.isFinite() && spacingY in 0f..8f) { "间距超出安全范围" }
        require(candidateTextSize in 14..28) { "候选词字号请保持在14到28之间" }
        require(keys.size <= 200 && keys.keys.all { it.matches(Regex("(qwerty|t9|toolbar|chrome):[a-z0-9_-]{1,40}")) }) {
            "单键设置需要有效的键盘与按键编号"
        }
        keys.values.forEach { it.validate() }
    }

    fun stickers():List<HomeSticker> = keys.values.mapNotNull{it.sticker}+listOfNotNull(panelStyle?.sticker)+floatingStickers.map{it.sticker}
    fun panelBackground():HomeVisualPreset=copy(floatingStickers=emptyList(),frameBorder=HomeBorder())

    fun withGlobalRadius(value: Int): HomeVisualPreset = copy(cornerRadius=value, panelStyle=panelStyle?.copy(cornerRadius=null), keys=keys.mapValues { it.value.copy(cornerRadius=null) }).also { it.validate() }

    fun withGlobalShape(value:HomeKeyShape):HomeVisualPreset=copy(keyShape=value,panelStyle=panelStyle?.copy(shape=null),keys=keys.mapValues{it.value.copy(shape=null)}).also{it.validate()}

    fun styleFor(id:String):HomeKeyStyle?=keys[id]?:panelStyle.takeIf{id.substringBefore(':') in setOf("number","common_symbols","symbol")}
    fun panelSyncSource(preferred:String?):String = preferred?.takeIf { it.matches(Regex("(qwerty|t9):[a-z0-9_-]{1,40}")) }
        ?: keys.keys.firstOrNull { it.matches(Regex("(qwerty|t9):[a-z0-9]")) } ?: "qwerty:q"

    fun withPanelStyle(sourceId:String,functionKey:Boolean=false):HomeVisualPreset {
        require(sourceId.startsWith("qwerty:") || sourceId.startsWith("t9:")) { "先选一颗来源按键" }
        val source=keys[sourceId]?:HomeKeyStyle()
        return copy(panelStyle=source.copy(backgroundLight=source.backgroundLight?:if(functionKey)light.special else light.key,
            backgroundDark=source.backgroundDark?:if(functionKey)dark.special else dark.key,textLight=source.textLight?:light.text,textDark=source.textDark?:dark.text,cornerRadius=source.cornerRadius?:cornerRadius)).also{it.validate()}
    }

    fun synchronize(sourceId: String, targetIds: Set<String>, property: HomeStyleProperty,
        side: HomeShadowSide? = null): HomeVisualPreset {
        require(sourceId.matches(Regex("(qwerty|t9|toolbar|chrome):[a-z0-9_-]{1,40}"))) { "来源按键编号无效" }
        require(targetIds.isNotEmpty() && targetIds.all { it.substringBefore(':') == sourceId.substringBefore(':') }) {
            "请在同一种键盘中选择要同步的按键"
        }
        val source = keys[sourceId] ?: HomeKeyStyle()
        val updated = keys.toMutableMap()
        targetIds.forEach { target ->
            val previous = updated[target] ?: HomeKeyStyle()
            updated[target] = when (property) {
                HomeStyleProperty.ALL -> source
                HomeStyleProperty.SHAPE -> previous.copy(shape=source.shape?:keyShape)
                HomeStyleProperty.BORDER -> previous.copy(border=source.border?:keyBorder)
                HomeStyleProperty.BORDER_KIND,HomeStyleProperty.BORDER_WIDTH,HomeStyleProperty.BORDER_OPACITY,HomeStyleProperty.BORDER_LIGHT,HomeStyleProperty.BORDER_DARK -> {
                    val from=source.border?:keyBorder;val into=previous.border?:keyBorder
                    previous.copy(border=when(property){
                        HomeStyleProperty.BORDER_KIND->into.copy(kind=from.kind)
                        HomeStyleProperty.BORDER_WIDTH->into.copy(widthDp=from.widthDp)
                        HomeStyleProperty.BORDER_OPACITY->into.copy(opacity=from.opacity)
                        HomeStyleProperty.BORDER_LIGHT->into.copy(colorLight=from.colorLight)
                        else->into.copy(colorDark=from.colorDark)
                    })
                }
                HomeStyleProperty.BACKGROUND_LIGHT -> previous.copy(backgroundLight = source.backgroundLight)
                HomeStyleProperty.BACKGROUND_DARK -> previous.copy(backgroundDark = source.backgroundDark)
                HomeStyleProperty.TEXT_LIGHT -> previous.copy(textLight = source.textLight)
                HomeStyleProperty.TEXT_DARK -> previous.copy(textDark = source.textDark)
                HomeStyleProperty.FROST -> previous.copy(frost=source.frost)
                HomeStyleProperty.KEY_OPACITY -> previous.copy(keyOpacity=source.keyOpacity)
                HomeStyleProperty.LABEL_POSITION -> previous.copy(labelOffsetX=source.labelOffsetX,labelOffsetY=source.labelOffsetY)
                HomeStyleProperty.LABEL_X -> previous.copy(labelOffsetX=source.labelOffsetX)
                HomeStyleProperty.LABEL_Y -> previous.copy(labelOffsetY=source.labelOffsetY)
                HomeStyleProperty.LABEL_FONT_SIZE -> previous.copy(labelFontSize=source.labelFontSize)
                HomeStyleProperty.HINT_ALL -> previous.copy(hintOffsetX=source.hintOffsetX,hintOffsetY=source.hintOffsetY,hintFontSize=source.hintFontSize,hintLight=source.hintLight,hintDark=source.hintDark,hintOpacity=source.hintOpacity,hintVisible=source.hintVisible)
                HomeStyleProperty.HINT_POSITION -> previous.copy(hintOffsetX=source.hintOffsetX,hintOffsetY=source.hintOffsetY)
                HomeStyleProperty.HINT_X -> previous.copy(hintOffsetX=source.hintOffsetX)
                HomeStyleProperty.HINT_Y -> previous.copy(hintOffsetY=source.hintOffsetY)
                HomeStyleProperty.HINT_FONT_SIZE -> previous.copy(hintFontSize=source.hintFontSize)
                HomeStyleProperty.HINT_LIGHT -> previous.copy(hintLight=source.hintLight)
                HomeStyleProperty.HINT_DARK -> previous.copy(hintDark=source.hintDark)
                HomeStyleProperty.HINT_OPACITY -> previous.copy(hintOpacity=source.hintOpacity)
                HomeStyleProperty.HINT_VISIBLE -> previous.copy(hintVisible=source.hintVisible)
                HomeStyleProperty.CORNER_RADIUS -> previous.copy(cornerRadius = source.cornerRadius ?: cornerRadius)
                HomeStyleProperty.STICKER -> previous.copy(sticker = source.sticker)
                HomeStyleProperty.STICKER_ASSET -> previous.copy(sticker = source.sticker?.let { from -> (previous.sticker ?: HomeSticker()).copy(asset=from.asset) })
                HomeStyleProperty.STICKER_X, HomeStyleProperty.STICKER_Y, HomeStyleProperty.STICKER_SCALE,
                HomeStyleProperty.STICKER_ROTATION, HomeStyleProperty.STICKER_OPACITY, HomeStyleProperty.STICKER_CLIP, HomeStyleProperty.STICKER_LAYER -> {
                    val from = requireNotNull(source.sticker) { "先给来源按键选一张贴纸" }
                    val into = previous.sticker
                    if (into == null) previous else previous.copy(sticker = when(property) {
                        HomeStyleProperty.STICKER_X -> into.copy(offsetX=from.offsetX)
                        HomeStyleProperty.STICKER_Y -> into.copy(offsetY=from.offsetY)
                        HomeStyleProperty.STICKER_SCALE -> into.copy(scale=from.scale)
                        HomeStyleProperty.STICKER_ROTATION -> into.copy(rotation=from.rotation)
                        HomeStyleProperty.STICKER_CLIP -> into.copy(clipToKey=from.clipToKey)
                        HomeStyleProperty.STICKER_LAYER -> into.copy(layer=from.layer)
                        else -> into.copy(opacity=from.opacity)
                    })
                }
                HomeStyleProperty.SHADOW_ALL -> previous.copy(shadows=source.shadows, shadowShape=source.shadowShape)
                HomeStyleProperty.SHADOW_SIDE -> {
                    requireNotNull(side) { "请选择要同步的阴影边" }
                    previous.copy(shadows=previous.shadows+(side to (source.shadows[side] ?: HomeSideShadow.forSide(side))))
                }
                HomeStyleProperty.SHADOW_SHAPE -> previous.copy(shadowShape = source.shadowShape)
                HomeStyleProperty.MOTION -> previous.copy(motion = source.motion)
                HomeStyleProperty.MOTION_STRENGTH -> previous.copy(motionStrength = source.motionStrength)
                HomeStyleProperty.MOTION_DURATION -> previous.copy(motionDurationMs = source.motionDurationMs)
                else -> {
                    requireNotNull(side) { "请选择要同步的阴影边" }
                    val from = source.shadows[side] ?: HomeSideShadow.forSide(side)
                    val into = previous.shadows[side] ?: HomeSideShadow.forSide(side)
                    val shadow = when (property) {
                        HomeStyleProperty.SHADOW_ENABLED -> into.copy(enabled = from.enabled)
                        HomeStyleProperty.SHADOW_INSET -> into.copy(inset = from.inset)
                        HomeStyleProperty.SHADOW_COLOR -> into.copy(color = from.color)
                        HomeStyleProperty.SHADOW_OPACITY -> into.copy(opacity = from.opacity)
                        HomeStyleProperty.SHADOW_BLUR -> into.copy(blurDp = from.blurDp)
                        HomeStyleProperty.SHADOW_SPREAD -> into.copy(spreadDp = from.spreadDp)
                        HomeStyleProperty.SHADOW_DISTANCE -> into.copy(distanceDp = from.distanceDp)
                        HomeStyleProperty.SHADOW_ANGLE -> into.copy(angleDegrees = from.angleDegrees)
                        else -> error("不支持的同步属性")
                    }
                    previous.copy(shadows = previous.shadows + (side to shadow))
                }
            }
        }
        return copy(keys = updated).also { it.validate() }
    }
}

object HomePresetCodec {
    const val MAX_BYTES = 128 * 1024
    private val json = Json { encodeDefaults = true; prettyPrint = true }

    fun encode(preset: HomeVisualPreset): String {
        preset.validate()
        return json.encodeToString(preset).also {
            require(it.toByteArray(Charsets.UTF_8).size <= MAX_BYTES) { "这份预设太大了，请减少单键覆盖项" }
        }
    }

    fun decode(bytes: ByteArray): HomeVisualPreset {
        require(bytes.size <= MAX_BYTES) { "预设文件太大了" }
        return json.decodeFromString<HomeVisualPreset>(bytes.decodeToString(throwOnInvalidSequence = true))
            .also { it.validate() }
    }

    fun read(input: java.io.InputStream): HomeVisualPreset {
        val output = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(4096)
        while (true) {
            val count = input.read(buffer, 0, minOf(buffer.size, MAX_BYTES + 1 - output.size()))
            if (count < 0) break
            require(count > 0) { "无法读取预设" }
            output.write(buffer, 0, count)
            require(output.size() <= MAX_BYTES) { "预设文件太大了" }
        }
        return decode(output.toByteArray())
    }
}
