package com.kingzcheung.xime.clipboard

import com.kingzcheung.xime.home.HomeOutboxItem
import java.net.URI

internal enum class HistoryFilter(val label: String) { ALL("全部文字"), LINKS("链接"), PINNED("已收藏") }

internal object ClipboardHistoryModel {
    private val links=Regex("https?://[^\\s]+",RegexOption.IGNORE_CASE)
    fun hasLink(text: String): Boolean = links.findAll(text).any {
        runCatching { URI(it.value.trimEnd('.',',','。','，','！','!','?', '？',')','）',']')).host?.isNotBlank()==true }.getOrDefault(false)
    }
    fun visible(items: List<ClipboardItem>, query: String, filter: HistoryFilter): List<ClipboardItem> {
        val search=query.trim()
        return items.asSequence().filter { !it.isImage && !it.isQuickSend }
            .filter { search.isEmpty() || it.text.contains(search,ignoreCase=true) }
            .filter { when(filter) { HistoryFilter.ALL -> true;HistoryFilter.LINKS -> hasLink(it.text);HistoryFilter.PINNED -> it.isPinned } }
            .sortedWith(compareByDescending<ClipboardItem> { it.timestamp }.thenByDescending { it.id }).toList()
    }
    // 发送与本地历史没有共享事件ID，只能如实说明“同内容最近一次”。
    fun latestByText(items: List<HomeOutboxItem>): Map<String,HomeOutboxItem> = items.groupBy { it.text }
        .mapValues { (_,rows)->rows.maxBy { it.copiedAt } }
    fun status(item: HomeOutboxItem?): String = when(item?.state) {
        "sent" -> "同内容最近一次：小家收到了"
        "pending" -> "同内容最近一次：等待发送"
        "failed" -> "同内容最近一次：没送达"
        null -> "最近发送记录里没有同内容"
        else -> "同内容最近一次：状态待确认"
    }
}
