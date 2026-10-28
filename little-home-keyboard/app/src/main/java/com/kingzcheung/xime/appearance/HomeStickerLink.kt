package com.kingzcheung.xime.appearance

import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.util.concurrent.TimeUnit

internal object HomeStickerLink {
    private val client=OkHttpClient.Builder().followRedirects(false).followSslRedirects(false)
        .connectTimeout(10,TimeUnit.SECONDS).readTimeout(15,TimeUnit.SECONDS).callTimeout(30,TimeUnit.SECONDS).build()
    fun fetch(value:String,execute:(Request)->Response={client.newCall(it).execute()}):ByteArray {
        fun checked(value:String)=value.trim().toHttpUrl().also{
            require(it.scheme=="https" && it.username.isEmpty() && it.password.isEmpty() && it.fragment==null){"请填写不带账号或片段的HTTPS图片直链"}
        }
        var url=checked(value)
        repeat(4){attempt->
            val request=Request.Builder().url(url).header("Accept","image/png,image/jpeg,image/webp,application/octet-stream").build()
            execute(request).use{response->
                if(response.code in setOf(301,302,303,307,308)){
                    require(attempt<3){"图片链接跳转太多"}
                    val location=requireNotNull(response.header("Location")){"图片链接没有有效地址"}
                    url=checked(requireNotNull(url.resolve(location)).toString())
                }else{
                    require(response.isSuccessful){"图片链接暂时无法打开"}
                    val body=requireNotNull(response.body){"图片链接没有内容"}
                    require(body.contentLength()<=HomeImageImportPolicy.MAX_INPUT_BYTES){"图片请控制在24MB以内"}
                    val type=response.header("Content-Type").orEmpty().substringBefore(';').trim().lowercase()
                    require(type in setOf("","image/png","image/jpeg","image/jpg","image/webp","application/octet-stream","binary/octet-stream")){"这个链接不是PNG、JPEG或WebP图片直链"}
                    return body.byteStream().use{HomeImageImportPolicy.read(it)}
                }
            }
        }
        error("图片链接没有打开")
    }
}
