package com.kingzcheung.xime.home

import okhttp3.OkHttpClient
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.security.KeyStore
import java.security.cert.CertificateFactory
import java.util.UUID
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

/** 只信任本机隔离夹具的单张证书，保持默认主机名校验；不会进入APK。 */
class HomeClipboardTlsContractTest {
    @Test fun actualReceiverAcceptsClientOverVerifiedTlsAndHonorsRevocation() {
        val origin = System.getenv("HOME_SYNC_FIXTURE_ORIGIN")
        assumeTrue("需要tools/home-sync-fixture.py启动隔离接收器", origin != null)
        val certificate = File(requireNotNull(System.getenv("HOME_SYNC_FIXTURE_CERT"))).inputStream().use {
            CertificateFactory.getInstance("X.509").generateCertificate(it)
        }
        val keys = KeyStore.getInstance(KeyStore.getDefaultType()).apply { load(null); setCertificateEntry("local-fixture", certificate) }
        val trust = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).apply { init(keys) }.trustManagers.single() as X509TrustManager
        val ssl = SSLContext.getInstance("TLS").apply { init(null, arrayOf(trust), null) }
        val api = HomeClipboardApi(OkHttpClient.Builder().sslSocketFactory(ssl.socketFactory, trust).build())
        val code = File(requireNotNull(System.getenv("HOME_SYNC_FIXTURE_CODE"))).readText().trim()
        val bound = api.request(origin!!, "/bind", method = "POST", body = JSONObject().put("code", code).put("deviceName", "隔离测试手机"))
        val token = bound.getString("token"); assertTrue(token.startsWith("lhk_device_"))
        api.request(origin, "/device/status", token)
        val body = JSONObject().put("eventId", UUID.randomUUID().toString()).put("text", "测试用的窗边花开了").put("copiedAtMs", System.currentTimeMillis())
        val first = api.request(origin, "/device/items", token, "POST", body)
        val again = api.request(origin, "/device/items", token, "POST", body)
        assertEquals(first.getString("id"), again.getString("id")); assertTrue(again.getBoolean("duplicate"))
        val listed = api.request(origin, "/device/items", token).getJSONArray("items")
        assertEquals(1, listed.length()); assertTrue(listed.getJSONObject(0).isNull("sourceApp"))
        val denied = runCatching { api.request(origin, "/owner/items", token) }.exceptionOrNull()
        assertEquals(401, (denied as HomeHttpError).status)
        api.request(origin, "/device/items", token, "DELETE")
        assertEquals(0, api.request(origin, "/device/items", token).getJSONArray("items").length())
        api.request(origin, "/device/binding", token, "DELETE")
        val revoked = runCatching { api.request(origin, "/device/status", token) }.exceptionOrNull()
        assertEquals(401, (revoked as HomeHttpError).status)
    }
}
