package com.kingzcheung.xime.clipboard
import org.junit.Assert.*
import org.junit.Test
class HomeDraftModelTest {
 @Test fun keyboardClipboardOpensSavedDraftsUnlessAHistoryTabWasChosen(){
  assertTrue(HomeDraftModel.showDrafts(0,false,false,1));assertFalse(HomeDraftModel.showDrafts(0,false,true,1));assertFalse(HomeDraftModel.showDrafts(1,true,false,10));assertTrue(HomeDraftModel.showDrafts(0,true,true,0));assertFalse(HomeDraftModel.showDrafts(0,false,false,0))
 }

 @Test fun selectedPassageOnlyAndDirectionDoesNotMatter(){
  val text="第一段的话。\n只想发这一小段。\n最后一段留着。";val start=text.indexOf("只想");val end=text.indexOf("\n",start)
  assertEquals("只想发这一小段。",HomeDraftModel.selected(text,start,end));assertEquals("只想发这一小段。",HomeDraftModel.selected(text,end,start))
  assertEquals("",HomeDraftModel.selected(text,4,4));assertEquals(text,HomeDraftModel.selected(text,-3,Int.MAX_VALUE))
 }
 @Test fun editorSnapshotRestoresIdentityBodyCategoryAndExpectedVersion(){
  val entry=com.kingzcheung.xime.clipboard.db.HomeDraftEntry("id","标题","一段长话".repeat(500),"category",123L)
  val saver=com.kingzcheung.xime.ui.settings.HomeDraftEntrySaver
  val scope=object:androidx.compose.runtime.saveable.SaverScope{override fun canBeSaved(value:Any)=value is String || value is Long}
  val saved=with(saver){scope.save(entry)};assertNotNull(saved);assertEquals(entry,saver.restore(saved!!))
  val blank=entry.copy(categoryId=null);assertEquals(blank,saver.restore(with(saver){scope.save(blank)}!!))
 }
 @Test fun limitsAndCategoryNames(){
  HomeDraftModel.validate("标题","字".repeat(65536));assertThrows(IllegalArgumentException::class.java){HomeDraftModel.validate("","字".repeat(65537))}
  assertThrows(IllegalArgumentException::class.java){HomeDraftModel.validate("","  ")};assertEquals("想说的话",HomeDraftModel.categoryLabel("  想说的话  "))
  assertThrows(IllegalArgumentException::class.java){HomeDraftModel.categoryLabel(" ")}
 }
}
