package com.kingzcheung.xime.home

import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

object HomeSyncPolicy {
    const val PREFIX = "/api/our-home/clipboard"
    const val MAX_ATTEMPTS = 5
    fun origin(value: String): String {
        val u = value.trim().toHttpUrl()
        require(u.scheme == "https" && u.username.isEmpty() && u.password.isEmpty() &&
            u.encodedPath == "/" && u.query == null && u.fragment == null) { "请填写纯HTTPS小家地址，不带路径、账号或参数" }
        return u.toString().removeSuffix("/")
    }
    fun retryable(code: Int) = code == 0 || code == 408 || code == 429 || code in 500..599
    fun retryDelay(attempts: Int): Long = (30_000L shl (attempts.coerceIn(1, MAX_ATTEMPTS) - 1)).coerceAtMost(30 * 60_000L)
    fun canCapture(enabled: Boolean, enabledAt: Long, copiedAt: Long, observedAt: Long): Boolean =
        enabled && copiedAt >= enabledAt && copiedAt <= observedAt && observedAt - copiedAt < 24 * 60 * 60_000L
}

internal class HomeHttpError(val status: Int) : Exception("小家请求未完成（$status）")

/** 不共享插件网络客户端，不重定向，不输出请求体/凭证或服务端正文。 */
internal class HomeClipboardApi(client: OkHttpClient = OkHttpClient()) {
    private val http = client.newBuilder().followRedirects(false).followSslRedirects(false)
        .connectTimeout(15, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS)
        .callTimeout(30, TimeUnit.SECONDS).build()
    private val active = java.util.concurrent.ConcurrentHashMap.newKeySet<okhttp3.Call>()
    fun cancel() { active.forEach { it.cancel() } }
    fun request(origin: String, path: String, token: String? = null, method: String = "GET", body: JSONObject? = null, guard: (() -> Unit)? = null, network: android.net.Network? = null): JSONObject {
        require(path.startsWith("/"))
        val builder = Request.Builder().url(HomeSyncPolicy.origin(origin) + HomeSyncPolicy.PREFIX + path)
            .header("Accept", "application/json").header("Cache-Control", "no-store")
        if (token != null) builder.header("Authorization", "Bearer $token")
        builder.method(method, body?.toString()?.toRequestBody("application/json; charset=utf-8".toMediaType()))
        val transport = if (network == null) http else http.newBuilder().socketFactory(network.socketFactory)
            .dns(object : okhttp3.Dns { override fun lookup(hostname: String) = network.getAllByName(hostname).toList() }).build()
        val call = transport.newCall(builder.build()); active.add(call)
        try {
            guard?.invoke()
            call.execute().use { response ->
                if (!response.isSuccessful) throw HomeHttpError(response.code)
                val output = ByteArrayOutputStream()
                response.body?.byteStream()?.use { input ->
                    val bytes = ByteArray(4096)
                    while (true) {
                        val n = input.read(bytes); if (n < 0) break
                        require(output.size() + n <= 512 * 1024) { "小家返回内容过大" }
                        output.write(bytes, 0, n)
                    }
                }
                return if (output.size() == 0) JSONObject() else JSONObject(output.toString("UTF-8"))
            }
        } finally { active.remove(call) }
    }
}
