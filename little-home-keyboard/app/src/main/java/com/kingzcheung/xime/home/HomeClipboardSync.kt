package com.kingzcheung.xime.home

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.ContentValues
import android.content.Context
import android.net.NetworkRequest
import android.net.NetworkCapabilities
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File
import java.security.KeyStore
import java.security.MessageDigest
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import com.kingzcheung.xime.clipboard.ClipboardCapturePolicy

internal data class HomeBinding(val origin: String = "", val token: String = "", val name: String = "",
    val revision: String = UUID.randomUUID().toString(), val enabled: Boolean = false,
    val wifiOnly: Boolean = false, val enabledAt: Long = Long.MAX_VALUE,
    val excludedEditors: Set<String> = emptySet(), val settingsEpoch: String = UUID.randomUUID().toString())
internal data class HomeOutboxItem(val id: String, val text: String, val copiedAt: Long,
    val state: String, val attempts: Int, val error: String)

/** 所有调用在IO，配置和出箱正文均由AndroidKeyStore AES-GCM加密。 */
internal class HomeClipboardSync private constructor(private val context: Context) {
    companion object {
        const val JOB_ID = 0x4c484b
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        @Volatile private var instance: HomeClipboardSync? = null
        fun get(context: Context): HomeClipboardSync = instance ?: synchronized(this) {
            instance ?: HomeClipboardSync(context.applicationContext).also { instance = it }
        }
        fun capture(context: Context, text: String, copiedAt: Long, privateEditor: Boolean, editor: String?) {
            val observedAt = System.currentTimeMillis()
            if (ClipboardCapturePolicy.reject(text, privateEditor = privateEditor) != null) return
            scope.launch { runCatching { val sync = get(context); try { sync.enqueue(text, copiedAt, observedAt, editor) } catch (_: Exception) { sync.recordNotice("安全出箱暂时没收下新片段，请检查连接页；打字不受影响。") } } }
        }
    }
    private val api = HomeClipboardApi()
    private val status = context.getSharedPreferences("little-home-status", Context.MODE_PRIVATE)
    fun notice(): String = status.getString("notice", "") ?: ""
    fun recordNotice(message: String) { status.edit().putString("notice", message).apply() }
    private val configFile = AtomicFile(File(context.noBackupFilesDir, "little-home-binding.enc"))
    private val helper = object : SQLiteOpenHelper(context, "little-home-outbox.db", null, 1) {
        override fun onCreate(db: SQLiteDatabase) { db.execSQL("CREATE TABLE outbox(id TEXT PRIMARY KEY, payload BLOB NOT NULL, copied INTEGER NOT NULL, state TEXT NOT NULL, attempts INTEGER NOT NULL DEFAULT 0, due INTEGER NOT NULL DEFAULT 0, error TEXT NOT NULL DEFAULT '')") }
        override fun onUpgrade(db: SQLiteDatabase, old: Int, new: Int) { error("不支持的出箱版本") }
    }
    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val alias = "little-home-clipboard-v1"
        (store.getKey(alias, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }
    private fun encrypt(bytes: ByteArray): ByteArray = Cipher.getInstance("AES/GCM/NoPadding").run {
        init(Cipher.ENCRYPT_MODE, key()); iv + doFinal(bytes)
    }
    private fun decrypt(bytes: ByteArray): ByteArray = Cipher.getInstance("AES/GCM/NoPadding").run {
        require(bytes.size >= 28); init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
        doFinal(bytes.copyOfRange(12, bytes.size))
    }
    @Synchronized fun binding(): HomeBinding {
        if (!configFile.baseFile.exists()) return HomeBinding()
        val j = JSONObject(String(decrypt(configFile.readFully()), Charsets.UTF_8))
        return HomeBinding(j.getString("origin"), j.getString("token"), j.getString("name"), j.getString("revision"),
            j.getBoolean("enabled"), j.getBoolean("wifiOnly"), j.getLong("enabledAt"),
            j.optString("excludedEditors").lineSequence().filter { it.isNotBlank() }.toSet(), j.optString("settingsEpoch", j.getString("revision")))
    }
    @Synchronized private fun save(b: HomeBinding) {
        val j = JSONObject().put("origin", b.origin).put("token", b.token).put("name", b.name)
            .put("revision", b.revision).put("enabled", b.enabled).put("wifiOnly", b.wifiOnly)
            .put("settingsEpoch", b.settingsEpoch).put("enabledAt", b.enabledAt).put("excludedEditors", b.excludedEditors.joinToString("\n"))
        val bytes = encrypt(j.toString().toByteArray()); val out = configFile.startWrite()
        try { out.write(bytes); configFile.finishWrite(out) } catch (e: Exception) { configFile.failWrite(out); throw e }
    }
    fun bind(origin: String, code: String, name: String) {
        require(name.isNotBlank() && name.length <= 64) { "设备名字请填1到64个字" }
        require(code.matches(Regex("LHK-[A-Za-z0-9_-]{10,100}"))) { "请填写小家生成的一次性绑定码" }
        require(binding().token.isEmpty()) { "请先解绑原设备，再连接另一处小家" }
        val url = HomeSyncPolicy.origin(origin)
        val response = api.request(url, "/bind", method = "POST", body = JSONObject().put("code", code).put("deviceName", name))
        val token = response.getString("token"); require(token.startsWith("lhk_device_") && token.length <= 256)
        synchronized(this) { save(HomeBinding(origin = url, token = token, name = name)); helper.writableDatabase.delete("outbox", null, null) }
    }
    fun check(): String { val b = binding(); require(b.token.isNotEmpty()) { "先连接小家" }; api.request(b.origin, "/device/status", b.token); return "小家连接正常" }
    @Synchronized fun configure(enabled: Boolean, wifiOnly: Boolean, excludedEditors: Set<String> = binding().excludedEditors) {
        val b = binding(); require(b.token.isNotEmpty()) { "先连接小家" }
        require(excludedEditors.size <= 100 && excludedEditors.all { it.matches(Regex("[a-zA-Z0-9_]+(\\.[a-zA-Z0-9_]+)+")) }) { "请输入正确的App包名，每行一个" }
        save(b.copy(enabled = enabled, wifiOnly = wifiOnly, enabledAt = if (enabled && !b.enabled) System.currentTimeMillis() else b.enabledAt,
            excludedEditors = excludedEditors, settingsEpoch = UUID.randomUUID().toString()))
        scheduler().cancel(JOB_ID); api.cancel(); if (enabled) schedule(force = true)
    }
    @Synchronized private fun enqueue(text: String, copiedAt: Long, observedAt: Long, editor: String?) {
        val b = binding()
        if (b.token.isEmpty() || !HomeSyncPolicy.canCapture(b.enabled, b.enabledAt, copiedAt, observedAt) || editor in b.excludedEditors) return
        val db = helper.writableDatabase
        val digest = MessageDigest.getInstance("SHA-256").digest(text.toByteArray()).joinToString("") { "%02x".format(it) }
        val event = UUID.nameUUIDFromBytes("${b.revision}:$copiedAt:$digest".toByteArray()).toString()
        val payload = encrypt(JSONObject().put("text", text).put("revision", b.revision).put("editor", editor ?: "").toString().toByteArray())
        db.delete("outbox", "state='sent' AND copied<?", arrayOf((observedAt - 7 * 24 * 60 * 60_000L).toString()))
        while (true) {
            val full = db.rawQuery("SELECT COUNT(*),COALESCE(SUM(LENGTH(payload)),0) FROM outbox", null).use {
                it.moveToFirst(); it.getInt(0) >= 200 || it.getLong(1) + payload.size > 4 * 1024 * 1024
            }
            if (!full) break
            val removed = db.delete("outbox", "id IN (SELECT id FROM outbox WHERE state='sent' ORDER BY copied ASC LIMIT 1)", null)
            if (removed == 0) { recordNotice("发送出箱已满，新片段暂未收下；请先处理等待发送的记录。"); return }
        }
        db.insertWithOnConflict("outbox", null, ContentValues().apply {
            put("id", event); put("payload", payload); put("copied", copiedAt); put("state", "pending")
        }, SQLiteDatabase.CONFLICT_IGNORE)
        recordNotice(""); schedule()
    }
    @Synchronized fun items(limit: Int = 30): List<HomeOutboxItem> {
        require(limit in 1..200)
        val result = mutableListOf<HomeOutboxItem>()
        helper.readableDatabase.rawQuery("SELECT id,payload,copied,state,attempts,error FROM outbox ORDER BY copied DESC LIMIT ?", arrayOf(limit.toString())).use {
            while (it.moveToNext()) result += HomeOutboxItem(it.getString(0), JSONObject(String(decrypt(it.getBlob(1)), Charsets.UTF_8)).getString("text"), it.getLong(2), it.getString(3), it.getInt(4), it.getString(5))
        }
        return result
    }
    @Synchronized fun retry() { helper.writableDatabase.execSQL("UPDATE outbox SET state='pending', attempts=0, due=0, error='' WHERE state!='sent'"); schedule(force = true) }
    @Synchronized fun removeLocal(id: String) { require(!binding().enabled) { "先暂停，再移除发送记录" }; helper.writableDatabase.delete("outbox", "id=?", arrayOf(id)) }
    @Synchronized fun clearLocal() { val b = binding(); configure(false, b.wifiOnly); helper.writableDatabase.delete("outbox", null, null); recordNotice("") }
    fun deleteServerCopies() { val b = binding(); require(b.token.isNotEmpty()); configure(false, b.wifiOnly); api.request(b.origin, "/device/items", b.token, "DELETE") }
    fun unbind() {
        configure(false, binding().wifiOnly)
        val b = binding(); try { api.request(b.origin, "/device/binding", b.token, "DELETE") } catch (e: HomeHttpError) { if (e.status != 401) throw e }
        synchronized(this) { configFile.delete(); helper.writableDatabase.delete("outbox", null, null) }
    }
    private fun scheduler() = context.getSystemService(JobScheduler::class.java)
    @Synchronized fun schedule(force: Boolean = false) {
        val b = binding(); if (!b.enabled || b.token.isEmpty()) return
        if (!force && scheduler().getPendingJob(JOB_ID) != null) return
        val due = helper.readableDatabase.rawQuery("SELECT MIN(due) FROM outbox WHERE state='pending'", null).use { if (it.moveToFirst() && !it.isNull(0)) it.getLong(0) else return }
        val job = JobInfo.Builder(JOB_ID, ComponentName(context, HomeClipboardJob::class.java))
        if (b.wifiOnly) job.setRequiredNetwork(NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI).addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED).build())
        else job.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
        check(scheduler().schedule(job.setMinimumLatency((due - System.currentTimeMillis()).coerceAtLeast(1000)).setPersisted(true).build()) == JobScheduler.RESULT_SUCCESS) { "系统暂未安排发送，请稍后重试" }
    }
    fun cancelRequest() { api.cancel() }
    private fun networkAllowed(network: android.net.Network?, wifiOnly: Boolean): Boolean {
        if (!wifiOnly) return true
        if (network == null) return false
        val caps = context.getSystemService(android.net.ConnectivityManager::class.java).getNetworkCapabilities(network)
        return caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
    }
    fun drain(network: android.net.Network?, stopped: () -> Boolean) {
        repeat(20) {
            if (stopped()) return
            val b = binding(); if (!b.enabled || b.token.isEmpty()) return
            val row = synchronized(this) {
                helper.readableDatabase.rawQuery("SELECT id,payload,copied,attempts FROM outbox WHERE state='pending' AND due<=? ORDER BY copied LIMIT 1", arrayOf(System.currentTimeMillis().toString())).use {
                    if (!it.moveToFirst()) return
                    arrayOf(it.getString(0), it.getBlob(1), it.getLong(2), it.getInt(3))
                }
            }
            val id = row[0] as String; val payload = JSONObject(String(decrypt(row[1] as ByteArray), Charsets.UTF_8)); val attempts = (row[3] as Int) + 1
            if (payload.getString("revision") != b.revision) { update(id, "failed", attempts, 0, "这条片段属于旧的绑定，未发送"); return@repeat }
            if (payload.optString("editor") in b.excludedEditors) { update(id, "failed", attempts, 0, "当前规则排除了这条片段的输入App"); return@repeat }
            try {
                synchronized(this) { if (binding().let { !it.enabled || it.revision != b.revision }) return }
                api.request(b.origin, "/device/items", b.token, "POST", JSONObject().put("eventId", id).put("text", payload.getString("text")).put("copiedAtMs", row[2] as Long), guard = {
                    synchronized(this) { check(!stopped() && networkAllowed(network, b.wifiOnly) && binding().let { it.enabled && it.revision == b.revision && it.settingsEpoch == b.settingsEpoch }) }
                }, network = network)
                update(id, "sent", attempts, 0, "")
            } catch (e: Exception) {
                if (stopped() || binding().let { !it.enabled || it.settingsEpoch != b.settingsEpoch }) return
                val code = (e as? HomeHttpError)?.status ?: 0
                val pending = HomeSyncPolicy.retryable(code) && attempts < HomeSyncPolicy.MAX_ATTEMPTS
                update(id, if (pending) "pending" else "failed", attempts, System.currentTimeMillis() + HomeSyncPolicy.retryDelay(attempts),
                    if (code == 401) "设备授权失效，请重新连接" else if (code == 0) "网络或安全连接失败" else "小家暂未收下（$code）")
            }
        }
    }
    @Synchronized private fun update(id: String, state: String, attempts: Int, due: Long, error: String) {
        helper.writableDatabase.update("outbox", ContentValues().apply { put("state", state); put("attempts", attempts); put("due", due); put("error", error) }, "id=?", arrayOf(id))
    }
}
