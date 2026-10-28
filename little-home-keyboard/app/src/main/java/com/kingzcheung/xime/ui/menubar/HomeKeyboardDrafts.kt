package com.kingzcheung.xime.ui.menubar

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kingzcheung.xime.MainActivity
import com.kingzcheung.xime.clipboard.HomeDraftModel
import com.kingzcheung.xime.clipboard.db.*

@Composable
internal fun HomeKeyboardDrafts(background: Color, card: Color, text: Color, onInsert: (String) -> Unit) {
    val context = LocalContext.current
    val dao = remember { ClipboardDatabase.getInstance(context).homeDraftDao() }
    val draftFlow = remember(dao) { dao.observeDrafts() }
    val categoryFlow = remember(dao) { dao.observeCategories() }
    val drafts by draftFlow.collectAsState(emptyList())
    val categories by categoryFlow.collectAsState(emptyList())
    HomeKeyboardDraftBody(drafts, categories, background, card, text, onInsert) {
        context.startActivity(Intent(context, MainActivity::class.java).putExtra("open_fragment", "home_drafts")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

/** IME内保持三栏共同的卡片尺寸与配色，不依赖Activity返回分发器。 */
@Composable
internal fun HomeKeyboardDraftBody(
    drafts: List<HomeDraftEntry>, categories: List<HomeDraftCategory>, background: Color, card: Color,
    text: Color, onInsert: (String) -> Unit, onManage: () -> Unit,
) {
    var chosenId by rememberSaveable { mutableStateOf<String?>(null) }
    var category by rememberSaveable { mutableStateOf<String?>(null) }
    val chosen = drafts.firstOrNull { it.id == chosenId }
    val scheme = MaterialTheme.colorScheme.copy(background = background, surface = card, onSurface = text,
        onBackground = text, onSurfaceVariant = text.copy(alpha = .7f))
    MaterialTheme(colorScheme = scheme) {
        Column(Modifier.fillMaxSize().background(background).padding(horizontal = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (chosen == null) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onManage) { Text("写 / 管理") }
                }
                if (categories.isNotEmpty()) Row(Modifier.horizontalScroll(rememberScrollState())) {
                    FilterChip(category == null, { category = null }, label = { Text("全部") })
                    categories.forEach { c -> FilterChip(category == c.id, { category = c.id }, label = { Text(c.label) }) }
                }
                LazyVerticalGrid(GridCells.Adaptive(145.dp), Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 8.dp)) {
                    if (drafts.isEmpty()) item { Text("写下的小草稿会留在这里，点“写 / 管理”开始。", color = text, modifier = Modifier.padding(12.dp)) }
                    items(drafts.filter { category == null || it.categoryId == category }, key = { it.id }) { draft ->
                        Surface(onClick = { chosenId = draft.id }, color = card, shape = RoundedCornerShape(12.dp)) {
                            Text(draft.title.ifBlank { "没有标题的小草稿" }, color = text, maxLines = 1,
                                overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(horizontal = 12.dp, vertical = 14.dp))
                        }
                    }
                }
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = { chosenId = null }) { Text("返回草稿") }
                    TextButton(onClick = { onInsert(chosen.text) }) { Text("插入全文") }
                }
                var selection by remember(chosen.id, chosen.updatedAt) { mutableStateOf(TextFieldValue(chosen.text, TextRange.Zero)) }
                BasicTextField(selection, { selection = it.copy(text = chosen.text) }, readOnly = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = text),
                    modifier = Modifier.weight(1f).fillMaxWidth().background(card, RoundedCornerShape(12.dp))
                        .verticalScroll(rememberScrollState()).padding(12.dp))
                val selected = HomeDraftModel.selected(chosen.text, selection.selection.start, selection.selection.end)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("长按拖动选一段", Modifier.padding(vertical = 12.dp), color = text.copy(alpha = .7f), style = MaterialTheme.typography.labelSmall)
                    TextButton(enabled = selected.isNotEmpty(), onClick = { onInsert(selected) }) { Text("插入选中这段（${selected.length}字）") }
                }
            }
        }
    }
}
