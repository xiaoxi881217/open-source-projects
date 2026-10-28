package com.kingzcheung.xime.appearance
import org.junit.Assert.*
import org.junit.Test
class HomeWallpaperFrostTest {
 @org.junit.Test fun appPanelBackgroundKeepsWallpaperButDoesNotRepeatKeyboardDecorations(){
  val p=HomeVisualPreset(wallpaper=HomeWallpaper("wallpaper:"+"a".repeat(64)),floatingStickers=listOf(HomeFloatingSticker("one")),frameBorder=HomeBorder(HomeBorderKind.DASHED))
  val background=p.panelBackground()
  org.junit.Assert.assertEquals(p.wallpaper,background.wallpaper)
  org.junit.Assert.assertTrue(background.floatingStickers.isEmpty())
  org.junit.Assert.assertEquals(HomeBorderKind.NONE,background.frameBorder.kind)
  org.junit.Assert.assertEquals(1,p.floatingStickers.size)
 }
 private val asset="wallpaper:"+"a".repeat(64)
 @Test fun actualBrowserExportContainsWallpaperFrostAndShadowSwitches(){
  val name="tools/appearance-preview/fixtures/browser-wallpaper.json"
  val bundle=HomeStickerBundleCodec.decode(listOf(java.io.File(name),java.io.File("../$name")).first{it.isFile}.readBytes());val p=bundle.preset
  assertNotNull(p.wallpaper);assertEquals(1f,p.keys.getValue("qwerty:q").frost);assertEquals(0f,p.keys.getValue("qwerty:w").frost)
  assertEquals(.15f,p.keys.getValue("qwerty:q").keyOpacity);assertEquals(2,p.cornerRadius)
  assertEquals(true,p.keys.getValue("qwerty:w").shadows[HomeShadowSide.TOP]?.enabled);assertFalse(p.keys.getValue("qwerty:w").shadows[HomeShadowSide.BOTTOM]?.enabled?:false)
  assertEquals(.61f,p.keys.getValue("qwerty:w").shadows.getValue(HomeShadowSide.TOP).opacity)
  assertTrue(bundle.images.values.single().take(4).toByteArray().contentEquals(byteArrayOf(-119,80,78,71)))
 }
 @Test fun placementAndBounds(){
  val p=HomeWallpaper(asset);val r=wallpaperRect(100f,100f,400f,200f,p)
  assertEquals(HomeWallpaperRect(0f,-100f,400f,400f),r)
  assertEquals(HomeWallpaperRect(40f,-60f,400f,400f),wallpaperRect(100f,100f,400f,200f,p.transformed(.1f,.2f,1f)))
  assertEquals(4f,p.transformed(9f,-9f,9f).scale);assertEquals(p,p.transformed(Float.NaN,0f,1f))
  assertThrows(IllegalArgumentException::class.java){p.copy(scale=0f).validate()}
  assertThrows(IllegalArgumentException::class.java){HomeKeyStyle(frost=Float.NaN).validate()}
 }
 @Test fun frostOnlySyncAndOldDefaults(){
  val target=HomeKeyStyle(keyOpacity=.35f,labelFontSize=26f,sticker=HomeSticker())
  val p=HomeVisualPreset(keys=mapOf("qwerty:q" to HomeKeyStyle(frost=.8f),"qwerty:w" to target))
  assertEquals(target.copy(frost=.8f),p.synchronize("qwerty:q",setOf("qwerty:w"),HomeStyleProperty.FROST).keys["qwerty:w"])
  assertEquals(null,HomePresetCodec.decode("{}".toByteArray()).wallpaper)
 }
 @Test fun wallpaperRoundTripAndResourceIntegrity(){
  val bytes=ByteArray(1024*1024+1){37};val id=HomeStickerBundleCodec.hash(bytes);val p=HomeVisualPreset(wallpaper=HomeWallpaper("wallpaper:$id",.2f,-.1f,2f,.5f))
  assertEquals(setOf(id),HomeStickerBundleCodec.imageIds(p));val bundle=HomeStickerBundleCodec.decode(HomeStickerBundleCodec.encode(p,mapOf(id to bytes)))
  assertEquals(p,bundle.preset);assertArrayEquals(bytes,bundle.images[id])
  assertThrows(IllegalArgumentException::class.java){HomeStickerBundleCodec.encode(p,emptyMap())}
  assertThrows(IllegalArgumentException::class.java){HomeStickerBundleCodec.encode(p,mapOf(id to byteArrayOf(1)))}
 }
 @Test fun blurSmoothsEdgesPreservesColorAndAlpha(){
  val pixels=IntArray(25){if(it%5<2)0xff000000.toInt() else 0xffffffff.toInt()};val blur=blurBackdrop(pixels,5,5,1)
  assertTrue((blur[2] and 255) in 1..254);assertTrue(blur.all{it ushr 24==255});assertEquals(0xff000000.toInt(),pixels[0])
  val solid=IntArray(25){0xff73452a.toInt()};assertArrayEquals(solid,blurBackdrop(solid,5,5,2));assertArrayEquals(pixels,blurBackdrop(pixels,5,5,0))
 }
}
