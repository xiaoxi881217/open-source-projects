package com.kingzcheung.xime.clipboard.db
import androidx.room3.*

@Entity(tableName="home_draft_categories",indices=[Index(value=["label"],unique=true)])
data class HomeDraftCategory(@PrimaryKey val id:String,val label:String)

@Entity(tableName="home_drafts",indices=[Index(value=["categoryId"])],foreignKeys=[ForeignKey(entity=HomeDraftCategory::class,parentColumns=["id"],childColumns=["categoryId"],onDelete=ForeignKey.SET_NULL)])
data class HomeDraftEntry(@PrimaryKey val id:String,val title:String,val text:String,val categoryId:String?,val updatedAt:Long)
