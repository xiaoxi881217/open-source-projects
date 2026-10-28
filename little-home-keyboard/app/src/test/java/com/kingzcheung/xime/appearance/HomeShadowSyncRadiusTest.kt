package com.kingzcheung.xime.appearance
import org.junit.Assert.*
import org.junit.Test
class HomeShadowSyncRadiusTest {
 @Test fun allEdgesIncludeDisabledAndPreserveOtherFields(){
  val source=HomeKeyStyle(shadows=mapOf(HomeShadowSide.TOP to HomeSideShadow.forSide(HomeShadowSide.TOP).copy(enabled=true,opacity=.61f),HomeShadowSide.BOTTOM to HomeSideShadow.forSide(HomeShadowSide.BOTTOM).copy(enabled=false,blurDp=9f)))
  val target=HomeKeyStyle(cornerRadius=4,shadows=mapOf(HomeShadowSide.LEFT to HomeSideShadow.forSide(HomeShadowSide.LEFT).copy(enabled=true)))
  val p=HomeVisualPreset(keys=mapOf("qwerty:q" to source,"qwerty:w" to target))
  val copied=p.synchronize("qwerty:q",setOf("qwerty:w"),HomeStyleProperty.SHADOW_ALL).keys.getValue("qwerty:w")
  assertEquals(source.shadows,copied.shadows);assertEquals(4,copied.cornerRadius)
  val single=p.synchronize("qwerty:q",setOf("qwerty:w"),HomeStyleProperty.SHADOW_SIDE,HomeShadowSide.TOP).keys.getValue("qwerty:w")
  assertEquals(source.shadows[HomeShadowSide.TOP],single.shadows[HomeShadowSide.TOP]);assertEquals(target.shadows[HomeShadowSide.LEFT],single.shadows[HomeShadowSide.LEFT])
 }
 @Test fun globalRadiusReplacesOverridesWithoutLosingDecorations(){
  val s=HomeKeyStyle(cornerRadius=20,labelFontSize=26f,sticker=HomeSticker())
  val p=HomeVisualPreset(keys=mapOf("qwerty:q" to s));val changed=p.withGlobalRadius(2)
  assertEquals(2,changed.cornerRadius);assertEquals(s.copy(cornerRadius=null),changed.keys["qwerty:q"]);assertEquals(20,p.keys["qwerty:q"]?.cornerRadius)
 }
}
