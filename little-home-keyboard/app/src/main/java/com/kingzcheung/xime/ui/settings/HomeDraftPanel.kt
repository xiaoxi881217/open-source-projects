package com.kingzcheung.xime.ui.settings

import android.content.Intent
import com.kingzcheung.xime.ui.keyboard.homeBackdropCover
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kingzcheung.xime.MainActivity
import com.kingzcheung.xime.clipboard.HomeDraftModel
import com.kingzcheung.xime.clipboard.db.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.util.UUID

internal val HomeDraftEntrySaver=listSaver<HomeDraftEntry?,Any>(
 save={e->if(e==null)emptyList() else listOf(e.id,e.title,e.text,e.categoryId?:"",e.updatedAt)},
 restore={p->if(p.isEmpty())null else HomeDraftEntry(p[0] as String,p[1] as String,p[2] as String,(p[3] as String).ifEmpty{null},p[4] as Long)}
)

/** App中写草稿，IME中使用原生文字选择手柄只插入选中段；不经过系统剪贴板。 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun HomeDraftPanel(onBack:()->Unit,onInsert:((String)->Unit)?=null,embedded:Boolean=false) {
 Box(Modifier.fillMaxSize().homeBackdropCover(MaterialTheme.colorScheme.background)) {
  if(onInsert!=null)HomeDraftContent(onBack,onInsert,embedded)
  else Scaffold(containerColor=androidx.compose.ui.graphics.Color.Transparent,contentWindowInsets=WindowInsets.safeDrawing){safe->
   Box(Modifier.fillMaxSize().padding(safe).consumeWindowInsets(safe)){HomeDraftContent(onBack,null,false)}
  }
 }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HomeDraftContent(onBack:()->Unit,onInsert:((String)->Unit)?,embedded:Boolean) {
 val context=LocalContext.current;val scope=rememberCoroutineScope();val dao=remember{ClipboardDatabase.getInstance(context).homeDraftDao()}
 val draftFlow=remember(dao){dao.observeDrafts()};val categoryFlow=remember(dao){dao.observeCategories()}
 val drafts by draftFlow.collectAsStateWithLifecycle(emptyList());val categories by categoryFlow.collectAsStateWithLifecycle(emptyList())
 var query by rememberSaveable{mutableStateOf("")};var category by rememberSaveable{mutableStateOf<String?>(null)}
 var chosen by remember{mutableStateOf<HomeDraftEntry?>(null)};var edit by rememberSaveable(stateSaver=HomeDraftEntrySaver){mutableStateOf<HomeDraftEntry?>(null)};var creating by rememberSaveable{mutableStateOf(false)}
 var categoryDialog by remember{mutableStateOf(false)};var categoryText by rememberSaveable{mutableStateOf("")};var remove by remember{mutableStateOf<HomeDraftEntry?>(null)};var deleteCategory by remember{mutableStateOf<HomeDraftCategory?>(null)}
 var busy by remember{mutableStateOf(false)};var message by remember{mutableStateOf("")}
 fun task(work:suspend ()->Unit){if(busy)return;busy=true;scope.launch{try{work()}catch(e:CancellationException){throw e}catch(e:IllegalArgumentException){message=e.message?:"没有完成，请再试一次"}catch(_:Exception){message="这次没有存好，原草稿仍然保留"}finally{busy=false}}}
 if(creating || edit!=null){
  HomeDraftEditor(edit,categories,busy,onBack={creating=false;edit=null},onAddCategory={categoryDialog=true},onSave={title,text,cat->
   val old=edit;task{dao.save(HomeDraftEntry(old?.id?:UUID.randomUUID().toString(),title,text,cat,0),old?.updatedAt);creating=false;edit=null;message="草稿已存在本机"}
  })
 }else{
  if(onInsert==null)BackHandler(chosen!=null){chosen=null}
  Column(Modifier.fillMaxSize().padding(horizontal=if(onInsert==null)20.dp else 10.dp,vertical=8.dp),verticalArrangement=Arrangement.spacedBy(if(onInsert==null)12.dp else 6.dp)){
   if(!embedded || chosen!=null)Row(verticalAlignment=androidx.compose.ui.Alignment.CenterVertically){
    HomeActionButton(onClick={if(chosen!=null)chosen=null else onBack()},modifier=Modifier.heightIn(min=48.dp)){Text(if(chosen!=null)"回到草稿" else "返回")}
    Column(Modifier.weight(1f)){Text("我的草稿",style=if(onInsert==null)MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium);if(onInsert==null)Text("写下来，留到想说的时候",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}
    if(onInsert==null)HomeTonalAction(onClick={creating=true}){Text("写一篇")}
   }
   if(embedded && chosen==null)Row(verticalAlignment=androidx.compose.ui.Alignment.CenterVertically){
    Text("${drafts.size}篇草稿",Modifier.weight(1f),style=MaterialTheme.typography.labelMedium)
    HomeActionButton(onClick={context.startActivity(Intent(context,MainActivity::class.java).putExtra("open_fragment","home_drafts").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))}){Text("写 / 管理")}
   }
   if(message.isNotEmpty())Text(message,style=MaterialTheme.typography.bodySmall)
   val item=chosen
   if(item!=null){
    Text(item.title.ifBlank{"没有标题的小草稿"},style=MaterialTheme.typography.titleMedium)
    var selection by remember(item.id,item.updatedAt){mutableStateOf(TextFieldValue(item.text,TextRange.Zero))}
    BasicTextField(value=selection,onValueChange={selection=it.copy(text=item.text)},readOnly=true,
     textStyle=MaterialTheme.typography.bodyLarge.copy(color=MaterialTheme.colorScheme.onSurface),modifier=Modifier.weight(1f).fillMaxWidth().background(MaterialTheme.colorScheme.surface,RoundedCornerShape(16.dp)).verticalScroll(rememberScrollState()).padding(16.dp))
    val selected=HomeDraftModel.selected(item.text,selection.selection.start,selection.selection.end)
    Text("长按并拖动选区 · 已选${selected.length}字",style=MaterialTheme.typography.bodySmall)
    Row{
     if(onInsert!=null){HomePrimaryAction(enabled=selected.isNotEmpty(),onClick={onInsert(selected)}){Text("插入选中这段")};HomeActionButton(onClick={onInsert(item.text)}){Text("插入全文")}}
     else HomeActionButton(onClick={edit=item;chosen=null}){Text("继续写 / 修改分类")}
    }
   }else{
    if(onInsert==null)OutlinedTextField(query,{query=it.take(100)},label={Text("找标题或草稿内容")},singleLine=true,modifier=Modifier.fillMaxWidth())
    Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)){
     HomeChoiceChip(category==null,{category=null},label={Text("全部")});HomeChoiceChip(category=="",{category=""},label={Text("未分类")})
     categories.forEach{c->HomeChoiceChip(category==c.id,{category=c.id},label={Text(c.label)})}
     if(onInsert==null)HomeActionButton(onClick={categoryDialog=true}){Text("＋分类")}
    }
    if(onInsert==null && category?.isNotEmpty()==true)categories.firstOrNull{it.id==category}?.let{c->HomeActionButton(onClick={deleteCategory=c}){Text("删除这个分类（保留草稿）")}}
    val visible=remember(drafts,category,query){drafts.filter{(category==null || (it.categoryId?:"")==category) && (query.isBlank()||it.text.contains(query,true)||it.title.contains(query,true))}}
    LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(10.dp),contentPadding=PaddingValues(bottom=12.dp)){
     if(visible.isEmpty())item{Text("这里还没有草稿，可以先去写一篇。",Modifier.padding(16.dp))}
     items(visible,key={it.id}){d->Surface(color=MaterialTheme.colorScheme.surface,shape=MaterialTheme.shapes.medium,onClick={chosen=d}){
      Column(Modifier.fillMaxWidth().padding(if(onInsert==null)18.dp else 12.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
       Text(d.title.ifBlank{d.text.take(24)},style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Medium,maxLines=1,overflow=TextOverflow.Ellipsis);Text(d.text,maxLines=if(onInsert==null)3 else 2,overflow=TextOverflow.Ellipsis,style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
       Text(categories.firstOrNull{it.id==d.categoryId}?.label?:"未分类",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
       if(onInsert==null)Row{HomeActionButton(onClick={edit=d}){Text("继续写")};HomeActionButton(onClick={remove=d}){Text("删除")}}
      }
     }}
    }
    if(onInsert==null)Text("只存在这部手机。打开键盘剪贴板的草稿标签，可长按选一小段插入。",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
   }
  }
 }
 if(categoryDialog)AlertDialog(onDismissRequest={if(!busy)categoryDialog=false},title={Text("给分类起个名字")},text={OutlinedTextField(categoryText,{categoryText=it.take(30)},singleLine=true,label={Text("例如：想说的话、工作、灵感")})},confirmButton={HomeActionButton(enabled=!busy&&categoryText.isNotBlank(),onClick={task{val c=dao.addCategory(categoryText);category=c.id;categoryText="";categoryDialog=false}}){Text("加好")}},dismissButton={HomeActionButton(onClick={categoryDialog=false}){Text("先不加")}})
 remove?.let{d->AlertDialog(onDismissRequest={remove=null},title={Text("删除这篇草稿？")},text={Text("只删除这一篇本地草稿，不能撤销。")},confirmButton={HomeActionButton(enabled=!busy,onClick={task{require(dao.delete(d.id,d.updatedAt)==1){"草稿已经变了，请重新查看"};remove=null}}){Text("删除")}},dismissButton={HomeActionButton(onClick={remove=null}){Text("留着")}})}
 deleteCategory?.let{c->AlertDialog(onDismissRequest={deleteCategory=null},title={Text("删除“${c.label}”？")},text={Text("草稿会回到未分类，不会删除内容。")},confirmButton={HomeActionButton(enabled=!busy,onClick={task{dao.deleteCategory(c.id);category=null;deleteCategory=null}}){Text("删除分类")}},dismissButton={HomeActionButton(onClick={deleteCategory=null}){Text("留着")}})}
}

@Composable
private fun HomeDraftEditor(entry:HomeDraftEntry?,categories:List<HomeDraftCategory>,busy:Boolean,onBack:()->Unit,onAddCategory:()->Unit,onSave:(String,String,String?)->Unit){
 val key=entry?.id?:"new";var title by rememberSaveable(key){mutableStateOf(entry?.title?:"")}
 var value by rememberSaveable(key,stateSaver=TextFieldValue.Saver){mutableStateOf(TextFieldValue(entry?.text?:""))}
 var category by rememberSaveable(key){mutableStateOf(entry?.categoryId)};var confirmLeave by remember{mutableStateOf(false)}
 val dirty=title!=(entry?.title?:"") || value.text!=(entry?.text?:"") || category!=entry?.categoryId
 fun back(){if(dirty)confirmLeave=true else onBack()}
 BackHandler{back()}
 Column(Modifier.fillMaxSize().imePadding().padding(horizontal=20.dp,vertical=10.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
  Row{HomeActionButton(onClick=::back,enabled=!busy){Text("返回")};Text("写一篇小草稿",Modifier.weight(1f).padding(horizontal=6.dp),style=MaterialTheme.typography.titleLarge);HomePrimaryAction(enabled=!busy&&value.text.isNotBlank(),onClick={onSave(title,value.text,category)}){Text(if(busy)"存着…" else "保存")}}
  OutlinedTextField(title,{title=it.take(80)},label={Text("标题（可以不写）")},singleLine=true,modifier=Modifier.fillMaxWidth())
  Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)){
   HomeChoiceChip(category==null,{category=null},label={Text("未分类")});categories.forEach{c->HomeChoiceChip(category==c.id,{category=c.id},label={Text(c.label)})};HomeActionButton(onClick=onAddCategory){Text("＋分类")}
  }
  OutlinedTextField(value,{if(it.text.length<=HomeDraftModel.MAX_TEXT)value=it},label={Text("想写的话，慢慢写在这里")},modifier=Modifier.weight(1f).fillMaxWidth(),shape=RoundedCornerShape(18.dp))
  Text("${value.text.length}/${HomeDraftModel.MAX_TEXT}字 · 保存后，可在键盘里长按选中一小段插入",style=MaterialTheme.typography.bodySmall)
 }
 if(confirmLeave)AlertDialog(onDismissRequest={confirmLeave=false},title={Text("草稿还没存好")},text={Text("保存后再离开，或放弃这次修改。")},confirmButton={HomeActionButton(enabled=!busy&&value.text.isNotBlank(),onClick={onSave(title,value.text,category)}){Text("保存")}},dismissButton={HomeActionButton(enabled=!busy,onClick={confirmLeave=false;onBack()}){Text("放弃本次修改")}})
}
