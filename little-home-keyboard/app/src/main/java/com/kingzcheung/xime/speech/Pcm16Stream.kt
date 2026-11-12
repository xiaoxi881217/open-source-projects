package com.kingzcheung.xime.speech

/** little-endian PCM16转浮点；跨音频块的半个采样只保留一个字节。 */
internal class Pcm16Stream {
    private var low: Int? = null
    fun reset() { low = null }
    fun accept(bytes: ByteArray): FloatArray {
        val result = FloatArray((bytes.size + if (low == null) 0 else 1) / 2)
        var out = 0
        bytes.forEach { value ->
            val first = low
            if (first == null) low = value.toInt() and 255
            else { result[out++] = ((first or ((value.toInt() and 255) shl 8)).toShort().toInt() / 32768f); low = null }
        }
        return result
    }
}
