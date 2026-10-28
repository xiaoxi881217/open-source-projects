package com.kingzcheung.xime.clipboard

internal object HomeDraftModel {
 fun showDrafts(tab:Int,explicit:Boolean,picked:Boolean,count:Int)=tab==0 && (explicit || (!picked && count>0))
 const val MAX_TEXT=65536
 const val MAX_DRAFTS=1000
 const val MAX_BYTES=8*1024*1024
 fun validate(title:String,text:String){require(title.length<=80 && text.isNotBlank() && text.length<=MAX_TEXT){"草稿请写1到65536字，标题不超过80字"}}
 fun categoryLabel(label:String):String=label.trim().also{require(it.isNotEmpty() && it.length<=30){"分类请写1到30字"}}
 fun selected(text:String,start:Int,end:Int):String {val a=minOf(start,end).coerceIn(0,text.length);val b=maxOf(start,end).coerceIn(0,text.length);return text.substring(a,b)}
}

internal object HomeDraftMigration {
 val sql=listOf(
  "CREATE TABLE IF NOT EXISTS home_draft_categories (id TEXT NOT NULL, label TEXT NOT NULL, PRIMARY KEY(id))",
  "CREATE UNIQUE INDEX IF NOT EXISTS index_home_draft_categories_label ON home_draft_categories(label)",
  "CREATE TABLE IF NOT EXISTS home_drafts (id TEXT NOT NULL, title TEXT NOT NULL, text TEXT NOT NULL, categoryId TEXT, updatedAt INTEGER NOT NULL, PRIMARY KEY(id), FOREIGN KEY(categoryId) REFERENCES home_draft_categories(id) ON UPDATE NO ACTION ON DELETE SET NULL)",
  "CREATE INDEX IF NOT EXISTS index_home_drafts_categoryId ON home_drafts(categoryId)"
 )
}
