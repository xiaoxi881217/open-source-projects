package com.kingzcheung.xime.clipboard.db
import androidx.room3.*
import com.kingzcheung.xime.clipboard.HomeDraftModel
import kotlinx.coroutines.flow.Flow

@Dao
abstract class HomeDraftDao {
 @Query("SELECT COUNT(*) FROM home_drafts") abstract fun observeCount():Flow<Int>
 @Query("SELECT * FROM home_drafts ORDER BY updatedAt DESC, id ASC") abstract fun observeDrafts():Flow<List<HomeDraftEntry>>
 @Query("SELECT * FROM home_draft_categories ORDER BY label COLLATE NOCASE, id") abstract fun observeCategories():Flow<List<HomeDraftCategory>>
 @Query("SELECT * FROM home_drafts WHERE id=:id") abstract suspend fun find(id:String):HomeDraftEntry?
 @Query("SELECT * FROM home_draft_categories WHERE label=:label") abstract suspend fun findCategory(label:String):HomeDraftCategory?
 @Query("SELECT COUNT(*) FROM home_draft_categories WHERE id=:id") abstract suspend fun categoryExists(id:String):Int
 @Query("SELECT COUNT(*) FROM home_draft_categories") abstract suspend fun categoryCount():Int
 @Query("SELECT COUNT(*) FROM home_drafts") abstract suspend fun count():Int
 @Query("SELECT COALESCE(SUM(LENGTH(CAST(text AS BLOB))),0) FROM home_drafts") abstract suspend fun bytes():Long
 @Insert(onConflict=OnConflictStrategy.ABORT) abstract suspend fun insertDraft(value:HomeDraftEntry)
 @Insert(onConflict=OnConflictStrategy.IGNORE) abstract suspend fun insertCategory(value:HomeDraftCategory):Long
 @Query("UPDATE home_drafts SET title=:title,text=:text,categoryId=:categoryId,updatedAt=:updatedAt WHERE id=:id AND updatedAt=:expected") abstract suspend fun update(id:String,title:String,text:String,categoryId:String?,updatedAt:Long,expected:Long):Int
 @Query("DELETE FROM home_drafts WHERE id=:id AND updatedAt=:expected") abstract suspend fun delete(id:String,expected:Long):Int
 @Query("DELETE FROM home_draft_categories WHERE id=:id") abstract suspend fun deleteCategory(id:String)
 @Transaction open suspend fun save(value:HomeDraftEntry,expected:Long?){
  HomeDraftModel.validate(value.title,value.text);val old=find(value.id)
  require(if(old==null)expected==null else old.updatedAt==expected){"这篇草稿在别处改过了，请重新打开后再保存"}
  require(value.categoryId==null || categoryExists(value.categoryId)>0){"分类已经变了，请重新选择"}
  require((old!=null || count()<HomeDraftModel.MAX_DRAFTS) && bytes()-(old?.text?.toByteArray(Charsets.UTF_8)?.size?:0)+value.text.toByteArray(Charsets.UTF_8).size<=HomeDraftModel.MAX_BYTES){"草稿本已满，请先整理一些草稿"}
  val next=value.copy(updatedAt=maxOf(System.currentTimeMillis(),(old?.updatedAt?:0)+1))
  if(old==null)insertDraft(next) else require(update(next.id,next.title,next.text,next.categoryId,next.updatedAt,old.updatedAt)==1){"草稿暂时没有保存，请重新打开"}
 }
 @Transaction open suspend fun addCategory(label:String):HomeDraftCategory {
  val name=HomeDraftModel.categoryLabel(label);findCategory(name)?.let{return it};require(categoryCount()<100){"分类最多100个"}
  val value=HomeDraftCategory(java.util.UUID.randomUUID().toString(),name);insertCategory(value);return requireNotNull(findCategory(name))
 }
}
