package com.kingzcheung.xime.appearance

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.Assert.*
import org.junit.Test

class HomeStickerLinkTest {
    private fun response(request:Request,code:Int=200,location:String?=null,type:String="image/png",body:ResponseBody="fixture".toByteArray().toResponseBody()):Response {
        val builder=Response.Builder().request(request).protocol(Protocol.HTTP_1_1).code(code).message("fixture").body(body).header("Content-Type",type)
        location?.let{builder.header("Location",it)}
        return builder.build()
    }
    private fun rejects(block:()->Unit){try{block();fail("expected rejection")}catch(_:IllegalArgumentException){}}
    @Test fun directDownloadKeepsBytesAndSendsNoHomeCredentials() {
        val bytes=HomeStickerLink.fetch("https://example.org/sticker.png"){request->
            assertNull(request.header("Authorization"));assertNull(request.header("Cookie"))
            response(request)
        }
        assertArrayEquals("fixture".toByteArray(),bytes)
    }
    @Test fun httpsRedirectWorksButDowngradeAndEmbeddedCredentialsAreRejected() {
        var calls=0
        val bytes=HomeStickerLink.fetch("https://example.org/first"){request->calls++;if(calls==1)response(request,302,"/second")else response(request)}
        assertEquals(2,calls);assertTrue(bytes.isNotEmpty())
        rejects{HomeStickerLink.fetch("https://example.org/first"){response(it,302,"http://example.org/image.png")}}
        rejects{HomeStickerLink.fetch("https://name:secret@example.org/image.png"){error("must not send")}}
    }
    @Test fun redirectLoopsHtmlAndOversizedBodiesAreRejected() {
        rejects{HomeStickerLink.fetch("https://example.org/loop"){response(it,302,"/loop")}}
        rejects{HomeStickerLink.fetch("https://example.org/page"){response(it,type="text/html")}}
        val body=object:ResponseBody(){override fun contentType()="image/png".toMediaType();override fun contentLength()=HomeImageImportPolicy.MAX_INPUT_BYTES.toLong()+1;override fun source()=Buffer().writeUtf8("small fixture")}
        rejects{HomeStickerLink.fetch("https://example.org/large"){response(it,body=body)}}
    }
}
