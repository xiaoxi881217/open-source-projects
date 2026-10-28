package com.kingzcheung.xime.appearance

import com.kingzcheung.xime.ui.keyboard.*
import com.kingzcheung.xime.viewmodel.ShiftMode
import org.junit.Assert.*
import org.junit.Test

class HomePreviewPanelTest {
 @Test fun visibleSymbolSyncUsesSelectedKeyOrAnExistingLetterStyle() {
  val p=HomeVisualPreset(keys=mapOf("qwerty:a" to HomeKeyStyle(frost=.4f)))
  assertEquals("qwerty:a",p.panelSyncSource(null))
  assertEquals("qwerty:period",p.panelSyncSource("qwerty:period"))
  assertEquals("qwerty:q",HomeVisualPreset().panelSyncSource(null))
  assertEquals(.4f,p.withPanelStyle(p.panelSyncSource(null)).styleFor("common_symbols:!")?.frost)
 }
 @Test fun actualBrowserPanelStyleDecodesWithDecorations(){
  val name="tools/appearance-preview/fixtures/browser-panels.json";val p=HomeStickerBundleCodec.decode(listOf(java.io.File(name),java.io.File("../$name")).first{it.isFile}.readBytes()).preset
  assertEquals(.6f,p.panelStyle!!.frost);assertEquals("builtin:heart",p.styleFor("symbol:，")!!.sticker!!.asset);assertEquals(p.panelStyle,p.styleFor("number:n1"))
 }
 @Test fun liveLayoutAndMeasuredSizeRemainInSync(){
  HomePreviewRuntime.update(HomePreviewState(layout=KeyboardLayoutState.T9Pinyin,dark=true))
  HomePreviewRuntime.updateSize(411f,247f)
  HomePreviewRuntime.update(HomePreviewState(layout=KeyboardLayoutState.English,dark=false,shifted=true,shift=ShiftMode.CAPS))
  val result=HomePreviewRuntime.state.value!!
  assertEquals(KeyboardLayoutState.English,result.layout);assertEquals(411f,result.width);assertEquals(247,result.height);assertFalse(result.dark);assertTrue(result.shifted)
  HomePreviewRuntime.updateSize(Float.NaN,1f);assertEquals(result,HomePreviewRuntime.state.value)
 }
 @Test fun oneStyleCoversNumbersAndSymbolsWithoutClobberingChineseKeys(){
  val style=HomeKeyStyle(cornerRadius=9,keyOpacity=.35f,frost=.8f,shadows=mapOf(HomeShadowSide.TOP to HomeSideShadow(enabled=true)),sticker=HomeSticker(),motion=HomePressMotion.JELLY)
  val p=HomeVisualPreset(keys=mapOf("qwerty:q" to style,"t9:2" to HomeKeyStyle(cornerRadius=4)))
  val copied=p.withPanelStyle("qwerty:q")
  for(id in listOf("number:n1","common_symbols:dot","symbol:，")){assertEquals(copied.panelStyle,copied.styleFor(id));assertEquals(style.sticker,copied.styleFor(id)?.sticker)}
  assertEquals(p.keys,copied.keys);assertEquals(4,copied.styleFor("t9:2")?.cornerRadius);assertNull(copied.styleFor("qwerty:w"))
  assertEquals(copied,HomePresetCodec.decode(HomePresetCodec.encode(copied).toByteArray()))
  assertNull(copied.copy(panelStyle=null).styleFor("symbol:，"))
 }
 @Test fun panelStickerResourceIsIncludedAndChecked(){
  val bytes=byteArrayOf(1,2,3);val id=HomeStickerBundleCodec.hash(bytes);val p=HomeVisualPreset(panelStyle=HomeKeyStyle(sticker=HomeSticker(asset="image:$id")))
  assertEquals(setOf(id),HomeStickerBundleCodec.imageIds(p));assertEquals(p,HomeStickerBundleCodec.decode(HomeStickerBundleCodec.encode(p,mapOf(id to bytes))).preset)
  assertThrows(IllegalArgumentException::class.java){HomeStickerBundleCodec.encode(p,emptyMap())}
 }
}
