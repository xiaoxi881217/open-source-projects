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

    @Test fun borderSurvivesIdleToCandidateAndBackWithoutCopyingOtherStyles() {
        val border=HomeBorder(HomeBorderKind.DASHED,widthDp=4f,opacity=.6f)
        val p=HomeVisualPreset(keys=mapOf("chrome:toolbar" to HomeKeyStyle(border=border),"chrome:candidate" to HomeKeyStyle(textOpacity=.4f)))
        assertEquals(border,p.chromeBorder("chrome:toolbar"));assertEquals(border,p.chromeBorder("chrome:candidate"))
        assertEquals(.4f,p.keys.getValue("chrome:candidate").textOpacity)
    }
    @Test fun explicitBordersIncludingDisabledRemainIndependent() {
        val a=HomeBorder(HomeBorderKind.DASHED);val b=HomeBorder(HomeBorderKind.SOLID,widthDp=3f)
        val p=HomeVisualPreset(frameBorder=a,keys=mapOf("chrome:toolbar" to HomeKeyStyle(border=b),"chrome:candidate" to HomeKeyStyle(border=HomeBorder())))
        assertEquals(b,p.chromeBorder("chrome:toolbar"));assertEquals(HomeBorder(),p.chromeBorder("chrome:candidate"))
        assertEquals(a,HomeVisualPreset(frameBorder=a).chromeBorder("chrome:candidate"))
    }
    @Test fun candidateFontFitsThickFrameAndSystemFontScale() {
        val border=HomeBorder(HomeBorderKind.DASHED,widthDp=6f)
        for(scale in listOf(1f,1.5f,2f)) {
            val fitted=fittedHomeCandidateTextSize(28f,scale,border)
            assertTrue(fitted<=28f);assertTrue(fitted*scale*1.35f+4f<=44f-2*homeChromeInset(border)+.001f)
        }
        assertEquals(19f,fittedHomeCandidateTextSize(19f,1f,HomeBorder()))
        assertEquals(0f,homeChromeInset(HomeBorder()))
    }
    @Test fun fullToolbarIncludesMandatoryEntrypointsAndKeepsOldMiddleOrder() {
        assertEquals(listOf("more_menu","emoji","clipboard","hide_keyboard"),homeToolbarOrder(listOf("clipboard","emoji"),listOf("emoji","clipboard")))
        assertEquals(listOf("clipboard","more_menu","hide_keyboard","emoji"),homeToolbarOrder(listOf("clipboard","emoji"),listOf("clipboard","more_menu","hide_keyboard")))
    }
    @Test fun movingAcrossEntireRowChangesOrderWithoutLosingActions() {
        val original=homeToolbarOrder(listOf("clipboard","emoji"),emptyList())
        val moved=moveHomeToolbar(original,"more_menu","hide_keyboard")
        assertEquals(listOf("clipboard","emoji","hide_keyboard","more_menu"),moved)
        assertEquals(original.toSet(),moved.toSet());assertEquals(original,moveHomeToolbar(original,"unknown","emoji"))
        assertEquals(moved,HomeDiyCode.decode(HomeDiyCode.encode(HomeStickerBundleCodec.encode(HomeVisualPreset(toolbarOrder=moved),emptyMap()))).preset.toolbarOrder)
    }
    @Test fun staleOrUnenabledActionsCannotBeCreatedByImportedOrdering() {
        val result=homeToolbarOrder(listOf("clipboard","clipboard"),listOf("plugin:disabled","clipboard","clipboard","more_menu"))
        assertEquals(listOf("clipboard","more_menu","hide_keyboard"),result)
        assertEquals(listOf("clipboard"),homeToolbarOrder(listOf("clipboard"),emptyList(),false,false))
    }
    @Test fun fullOrderCanHoldThirtyActionsPlusTwoEntrypoints() {
        val order=homeToolbarOrder(List(30){"tool$it"},listOf("hide_keyboard","more_menu"))
        assertEquals(32,order.size);HomeVisualPreset(toolbarOrder=order).validate()
        assertTrue(runCatching{HomeVisualPreset(toolbarOrder=List(65){"tool$it"}).validate()}.isFailure)
    }
}
