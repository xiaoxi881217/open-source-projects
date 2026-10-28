package com.kingzcheung.xime.ui

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.OnBackPressedDispatcherOwner
import androidx.activity.OnBackPressedDispatcher
import androidx.navigationevent.NavigationEventDispatcherOwner
import androidx.navigationevent.NavigationEventDispatcher
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.activity.compose.setContent
import com.kingzcheung.xime.MainActivity
import androidx.compose.ui.unit.dp
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import com.kingzcheung.xime.clipboard.db.HomeDraftEntry
import com.kingzcheung.xime.ui.menubar.HomeKeyboardDraftBody
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** 合成草稿，不接触手机的Room、剪贴板或同步设置。 */
class HomeKeyboardDraftTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun imeWithoutActivityBackOwnerCanOpenSelectInsertAndReturn() {
        val text = "前面的话只发这一段后面的话"
        var inserted = ""
        compose.activityRule.scenario.onActivity { activity -> activity.setContent {
            CompositionLocalProvider(LocalOnBackPressedDispatcherOwner provides object : OnBackPressedDispatcherOwner {
                override val lifecycle get() = activity.lifecycle
                override val onBackPressedDispatcher: OnBackPressedDispatcher get() = error("IME has no Activity back dispatcher")
            }, LocalNavigationEventDispatcherOwner provides object : NavigationEventDispatcherOwner {
                override val navigationEventDispatcher: NavigationEventDispatcher get() = error("IME has no Activity navigation dispatcher")
            }) {
                MaterialTheme {
                    Box(Modifier.width(350.dp).height(300.dp)) {
                        HomeKeyboardDraftBody(listOf(HomeDraftEntry("fixture", "合成小草稿", text, null, 1)), emptyList(),
                            Color.White, Color(0xffeeeeee), Color.Black, { inserted = it }, {})
                    }
                }
            }
        }
        }
        compose.onNodeWithText("合成小草稿").performClick()
        compose.onNodeWithText("插入全文").performClick()
        compose.runOnIdle { assertEquals(text, inserted) }
        compose.onNode(hasSetTextAction()).performSemanticsAction(SemanticsActions.SetSelection) { it(4, 9, false) }
        compose.onNodeWithText("插入选中这段（5字）").performClick()
        compose.runOnIdle { assertEquals(text.substring(4, 9), inserted) }
        compose.onNodeWithText("返回草稿").performClick()
        compose.onNodeWithText("合成小草稿").assertIsDisplayed()
    }

    @Test fun emptyPanelHasVisibleManageEntryWithoutActivityBackOwner() {
        var managed = false
        compose.activityRule.scenario.onActivity { activity -> activity.setContent {
            CompositionLocalProvider(LocalOnBackPressedDispatcherOwner provides object : OnBackPressedDispatcherOwner {
                override val lifecycle get() = activity.lifecycle
                override val onBackPressedDispatcher: OnBackPressedDispatcher get() = error("IME has no Activity back dispatcher")
            }, LocalNavigationEventDispatcherOwner provides object : NavigationEventDispatcherOwner {
                override val navigationEventDispatcher: NavigationEventDispatcher get() = error("IME has no Activity navigation dispatcher")
            }) {
                MaterialTheme { Box(Modifier.width(350.dp).height(300.dp)) {
                    HomeKeyboardDraftBody(emptyList(), emptyList(), Color.White, Color.White, Color.Black, {}, { managed = true })
                } }
            }
        }
        }
        compose.onNodeWithText("写 / 管理").performClick()
        compose.runOnIdle { assertEquals(true, managed) }
    }
}
