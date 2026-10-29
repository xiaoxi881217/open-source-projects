package com.kingzcheung.xime.appearance

import org.junit.Assert.*
import org.junit.Test

class HomeKeyStickerStackTest {
    private val free=HomeSticker(asset="builtin:heart",scale=2.5f)
    private val cover=HomeSticker(asset="builtin:star",clipToKey=true,layer=HomeStickerLayer.BELOW_SHADOW,opacity=.4f)
    private fun preset(style:HomeKeyStyle)=HomeVisualPreset(keys=mapOf("qwerty:q" to style))
    private fun rejects(block:()->Unit){assertTrue(runCatching(block).isFailure)}

    @Test fun legacySingleStickerImportsWithoutMigrationLoss() {
        val old="""{"name":"旧装扮","keys":{"qwerty:q":{"sticker":{"asset":"builtin:star","scale":2.5}}}}"""
        val style=HomePresetCodec.decode(old.toByteArray()).keys.getValue("qwerty:q")
        assertEquals(listOf(HomeSticker(asset="builtin:star",scale=2.5f)),style.stickers())
        assertTrue(style.extraStickers.isEmpty())
    }
    @Test fun mixedModesAndOrderingSurviveJsonAndDiyTxt() {
        val p=preset(HomeKeyStyle(sticker=free,extraStickers=listOf(cover)))
        assertEquals(p,HomePresetCodec.decode(HomePresetCodec.encode(p).toByteArray()))
        val bundle=HomeStickerBundleCodec.encode(p,emptyMap())
        assertEquals(p,HomeDiyCode.decode(HomeDiyCode.encode(bundle)).preset)
        assertEquals(p,HomeDiyCode.readImport(HomeDiyCode.encodeTextFile(bundle).inputStream()).preset)
        assertEquals(listOf(free,cover),p.stickers())
    }
    @Test fun changingOrDeletingSelectedLayerPreservesOthersAndKeyStyle() {
        val style=HomeKeyStyle(sticker=free,extraStickers=listOf(cover),keyOpacity=.3f,cornerRadius=8)
        val moved=style.withSticker(1,cover.transformed(.2f,-.1f,1.3f,25f))
        assertEquals(free,moved.sticker)
        assertTrue(moved.stickerAt(1)!!.clipToKey)
        assertEquals(cover.layer,moved.stickerAt(1)!!.layer)
        assertEquals(style.copy(sticker=cover,extraStickers=emptyList()),style.withSticker(0,null))
        assertEquals(style.copy(extraStickers=emptyList()),style.withSticker(1,null))
        assertEquals(style,style.withSticker(1,cover))
    }
    @Test fun stackAndIndividualItemSyncHaveDifferentScopes() {
        val source=HomeKeyStyle(sticker=free,extraStickers=listOf(cover))
        val target=HomeKeyStyle(sticker=cover,extraStickers=listOf(free),keyOpacity=.25f)
        val p=HomeVisualPreset(keys=mapOf("qwerty:q" to source,"qwerty:w" to target))
        fun synced(property:HomeStyleProperty)=p.synchronize("qwerty:q",setOf("qwerty:w"),property,stickerIndex=1).keys.getValue("qwerty:w")
        assertEquals(target.withStickers(source.stickers()),synced(HomeStyleProperty.STICKERS))
        assertEquals(target.withSticker(1,cover),synced(HomeStyleProperty.STICKER))
        assertEquals(target.withSticker(1,free.copy(opacity=cover.opacity)),synced(HomeStyleProperty.STICKER_OPACITY))
        assertEquals(target.withSticker(1,free.copy(clipToKey=true)),synced(HomeStyleProperty.STICKER_CLIP))
    }
    @Test fun numericSyncNeverCreatesMissingLayersButImageSyncDoes() {
        val source=HomeKeyStyle(sticker=free,extraStickers=listOf(cover))
        val p=preset(source)
        val numeric=p.synchronize("qwerty:q",setOf("qwerty:w"),HomeStyleProperty.STICKER_SCALE,stickerIndex=1)
        assertTrue(numeric.keys.getValue("qwerty:w").stickers().isEmpty())
        val image=p.synchronize("qwerty:q",setOf("qwerty:w"),HomeStyleProperty.STICKER_ASSET,stickerIndex=1)
        assertEquals(listOf(HomeSticker(asset=cover.asset)),image.keys.getValue("qwerty:w").stickers())
    }
    @Test fun everyLayerImageIsIncludedAndMissingOrCorruptImagesAreRejected() {
        val a=byteArrayOf(1,2,3);val b=byteArrayOf(4,5,6)
        val x=HomeStickerBundleCodec.hash(a);val y=HomeStickerBundleCodec.hash(b)
        val p=preset(HomeKeyStyle(sticker=free.copy(asset="image:$x"),extraStickers=listOf(cover.copy(asset="image:$y"))))
        val images=mapOf(x to a,y to b)
        val decoded=HomeStickerBundleCodec.decode(HomeStickerBundleCodec.encode(p,images))
        assertEquals(setOf(x,y),HomeStickerBundleCodec.imageIds(p))
        assertArrayEquals(a,decoded.images[x]);assertArrayEquals(b,decoded.images[y])
        rejects{HomeStickerBundleCodec.encode(p,mapOf(x to a))}
        rejects{HomeStickerBundleCodec.encode(p,images+(y to a))}
    }
    @Test fun reorderAndCapacityAreBoundedWithoutChangingInputSettings() {
        val style=HomeKeyStyle(sticker=free,extraStickers=listOf(cover))
        assertEquals(listOf(cover,free),style.moveSticker(1,-1).stickers())
        assertEquals(style,style.moveSticker(0,1).moveSticker(1,-1))
        rejects{style.moveSticker(0,-1)}
        val full=style.withStickers(List(8){free});full.validate()
        rejects{full.withSticker(8,cover)}
        rejects{full.copy(extraStickers=List(8){cover}).validate()}
        rejects{style.withSticker(-1,free)}
        assertFalse(HomePresetCodec.encode(preset(full)).contains("inputSchema"))
    }
    @Test fun extraStickerValidationCannotBypassAssetOrGeometryLimits() {
        rejects{HomeKeyStyle(extraStickers=listOf(cover.copy(asset="file:///private"))).validate()}
        rejects{HomeKeyStyle(extraStickers=listOf(cover.copy(scale=Float.NaN))).validate()}
    }
}
