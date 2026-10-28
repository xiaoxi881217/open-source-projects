package com.kingzcheung.xime.home

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import okio.Buffer

class HomeClipboardApiTest {
    @Test fun newBindingAllowsCellularButStillRequiresConsentAndKeepsExplicitWifiChoice() {
        val defaults=HomeBinding()
        assertFalse(defaults.wifiOnly)
        assertFalse(defaults.enabled)
        assertTrue(defaults.copy(wifiOnly=true).copy(name="手机").wifiOnly)
    }

    @Test fun acceptsOnlyHttpsOriginAndNeverCredentialBearingAddresses() {
        assertEquals("https://home.example", HomeSyncPolicy.origin(" https://home.example/ "))
        listOf("http://home.example", "https://name:secret@home.example", "https://home.example/path", "https://home.example/?token=private", "https://home.example/#fragment").forEach {
            assertTrue(runCatching { HomeSyncPolicy.origin(it) }.isFailure)
        }
    }
    @Test fun onlyNewClipsAfterConsentMayBeQueued() {
        assertFalse(HomeSyncPolicy.canCapture(false, 100, 110, 120))
        assertFalse(HomeSyncPolicy.canCapture(true, 100, 90, 120))
        assertFalse(HomeSyncPolicy.canCapture(true, 100, 121, 120))
        assertFalse(HomeSyncPolicy.canCapture(true, 100, 0, 120))
        assertTrue(HomeSyncPolicy.canCapture(true, 100, 110, 120))
    }
    @Test fun onlyTransientFailuresRetryAndBackoffIsBounded() {
        listOf(0, 408, 429, 500, 503).forEach { assertTrue(HomeSyncPolicy.retryable(it)) }
        listOf(301, 401, 403, 409, 413, 422).forEach { assertFalse(HomeSyncPolicy.retryable(it)) }
        assertEquals(listOf(30000L,60000L,120000L,240000L,480000L), (1..5).map(HomeSyncPolicy::retryDelay))
        assertEquals(5, HomeSyncPolicy.MAX_ATTEMPTS)
    }
    @Test fun actualHttpRequestUsesScopedBodyAndNoClipboardInUrl() {
        var captured: Request? = null
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            captured = chain.request()
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(201).message("Created")
                .body("{\"id\":\"test-id\",\"duplicate\":false}".toResponseBody("application/json".toMediaType())).build()
        }.build()
        val payload = JSONObject().put("eventId", "event-test").put("text", "测试生活片段").put("copiedAtMs", 12345L)
        val result = HomeClipboardApi(client).request("https://home.example", "/device/items", "test-device-token", "POST", payload)
        assertEquals("test-id", result.getString("id"))
        val r = captured!!; assertEquals("https://home.example/api/our-home/clipboard/device/items", r.url.toString())
        assertEquals("Bearer test-device-token", r.header("Authorization"))
        val buffer = Buffer(); r.body!!.writeTo(buffer)
        assertEquals("测试生活片段", JSONObject(buffer.readUtf8()).getString("text"))
    }
    @Test fun redirectsCannotForwardAuthorizationAndErrorsHideResponseContent() {
        var requests = 0
        val client = OkHttpClient.Builder().addInterceptor { chain -> requests++
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(302).message("Found")
                .header("Location", "https://third-party.example")
                .body("private server body".toResponseBody()).build()
        }.build()
        val error = runCatching { HomeClipboardApi(client).request("https://home.example", "/device/status", "test-token") }.exceptionOrNull()
        assertEquals(1, requests); assertEquals(302, (error as HomeHttpError).status)
        assertFalse(error.message!!.contains("private"))
    }
    @Test fun pausedGuardPreventsAnyRequestAndCancellationHappensBeforeNetwork() {
        var requests = 0
        val api = HomeClipboardApi(OkHttpClient.Builder().addNetworkInterceptor { requests++; error("unexpected network") }.build())
        assertTrue(runCatching { api.request("https://home.example", "/device/status", guard = { error("paused") }) }.isFailure)
        assertEquals(0, requests)
        assertTrue(runCatching { api.request("https://home.example", "/device/status", guard = { api.cancel() }) }.isFailure)
        assertEquals(0, requests)
    }
    @Test fun refusesOversizedResponses() {
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("OK")
                .body("x".repeat(512 * 1024 + 1).toResponseBody()).build()
        }.build()
        assertTrue(runCatching { HomeClipboardApi(client).request("https://home.example", "/device/status") }.isFailure)
    }
}
