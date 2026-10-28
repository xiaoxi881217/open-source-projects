package com.kingzcheung.xime.appearance

import java.io.ByteArrayOutputStream
import java.io.InputStream

internal object HomeImageImportPolicy {
    const val MAX_INPUT_BYTES=24*1024*1024
    const val STICKER_EDGE=1024
    fun sampleSize(width:Int,height:Int,edge:Int):Int {
        require(width in 1..32768 && height in 1..32768 && width.toLong()*height<=100_000_000L){"图片尺寸太大，请选择一亿像素以内的图片"}
        var sample=1
        while(maxOf(width,height)/sample>edge*2)sample*=2
        return sample
    }
    fun read(input:InputStream,limit:Int=MAX_INPUT_BYTES):ByteArray {
        val out=ByteArrayOutputStream();val buffer=ByteArray(8192)
        while(true){val n=input.read(buffer);if(n<0)break;require(n>0 && out.size().toLong()+n<=limit){"图片请控制在24MB以内"};out.write(buffer,0,n)}
        return out.toByteArray()
    }
}
