package com.kingzcheung.xime.clipboard

import com.kingzcheung.xime.home.HomeOutboxItem
import org.junit.Assert.*
import org.junit.Test

class ClipboardHistoryModelTest {
    private val sample=listOf(
        ClipboardItem(id=1,text="今天看见小猫",timestamp=30),
        ClipboardItem(id=2,text="想留着的文字",timestamp=10,isPinned=true),
        ClipboardItem(id=3,text="看看 HTTPS://example.com/a。",timestamp=20),
        ClipboardItem(id=4,text="图片不进入文字管理",timestamp=40,type="image"),
        ClipboardItem(id=5,text="常用语",timestamp=50,isQuickSend=true),
    )
    @Test fun textOnlyAndNewestFirstWithoutMovingFavoritesToTop() {
        assertEquals(listOf(1L,3L,2L),ClipboardHistoryModel.visible(sample,"",HistoryFilter.ALL).map { it.id })
        assertEquals(5,sample.size)
    }
    @Test fun searchAndFiltersCombineWithoutExpandingDeleteScope() {
        assertEquals(listOf(3L),ClipboardHistoryModel.visible(sample," EXAMPLE ",HistoryFilter.LINKS).map { it.id })
        assertEquals(listOf(2L),ClipboardHistoryModel.visible(sample,"文字",HistoryFilter.PINNED).map { it.id })
        assertTrue(ClipboardHistoryModel.visible(sample,"小猫",HistoryFilter.PINNED).isEmpty())
        assertFalse(ClipboardHistoryModel.hasLink("只是写了 https://"))
    }
    @Test fun timestampTiesRemainStableById() {
        val items=listOf(ClipboardItem(id=7,text="a",timestamp=1),ClipboardItem(id=8,text="b",timestamp=1))
        assertEquals(listOf(8L,7L),ClipboardHistoryModel.visible(items,"",HistoryFilter.ALL).map { it.id })
    }
    @Test fun matchingTextShowsMostRecentAttemptRatherThanOldSuccess() {
        val old=HomeOutboxItem("a","同一段",10,"sent",1,"")
        val latest=old.copy(id="b",copiedAt=20,state="failed")
        val unrelated=old.copy(id="c",text="另一段",copiedAt=30)
        val map=ClipboardHistoryModel.latestByText(listOf(unrelated,old,latest))
        assertEquals(latest,map["同一段"])
        assertEquals("同内容最近一次：没送达",ClipboardHistoryModel.status(map["同一段"]))
        assertEquals("最近发送记录里没有同内容",ClipboardHistoryModel.status(map["未知"]))
        assertFalse(ClipboardHistoryModel.status(null).contains("未同步"))
    }
    @Test fun pendingAndUnknownStatesAreNotCalledSuccess() {
        val row=HomeOutboxItem("a","片段",1,"pending",2,"网络失败")
        assertEquals("同内容最近一次：等待发送",ClipboardHistoryModel.status(row))
        assertEquals("同内容最近一次：状态待确认",ClipboardHistoryModel.status(row.copy(state="future")))
    }
}
