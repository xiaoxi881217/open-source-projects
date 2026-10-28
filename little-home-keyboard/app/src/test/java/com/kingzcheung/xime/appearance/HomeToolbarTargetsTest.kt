package com.kingzcheung.xime.appearance

import org.junit.Assert.*
import org.junit.Test

class HomeToolbarTargetsTest {
    @Test fun menuAndHideDoNotCollideWithHomeCursorActionOrPluginIds() {
        val targets=homeToolbarTargets(listOf("home" to "段首","clipboard" to "剪贴板","plugin:one" to "小工具"))
        assertEquals("更多",targets["more_menu"]);assertEquals("段首",targets["home"])
        assertEquals("收起键盘",targets["hide_keyboard"])
        assertEquals("小工具",targets[HomeKeyIds.token("plugin:one")])
        assertTrue(targets.keys.all{it.matches(Regex("[a-z0-9_-]{1,40}"))})
    }
    @Test fun iconStickersSupportIsolatedSyncAndFullBundle() {
        val source=HomeSticker(asset="builtin:bow",scale=4f,clipToKey=true,layer=HomeStickerLayer.BELOW_SHADOW)
        val target=HomeSticker(asset="builtin:star",scale=1f)
        val p=HomeVisualPreset(keys=mapOf("toolbar:more_menu" to HomeKeyStyle(sticker=source),"toolbar:clipboard" to HomeKeyStyle(sticker=target)))
        val next=p.synchronize("toolbar:more_menu",setOf("toolbar:clipboard"),HomeStyleProperty.STICKER_SCALE)
        assertEquals(target.copy(scale=4f),next.keys["toolbar:clipboard"]!!.sticker)
        assertEquals(next,HomeStickerBundleCodec.decode(HomeStickerBundleCodec.encode(next,emptyMap())).preset)
    }
}
