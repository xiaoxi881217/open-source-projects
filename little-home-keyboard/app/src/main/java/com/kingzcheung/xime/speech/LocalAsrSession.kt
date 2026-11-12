package com.kingzcheung.xime.speech

import com.kingzcheung.xime.speech.models.AsrEngine
import com.k2fsa.sherpa.onnx.*
import java.io.File

/** 所有本地模型只在 :asr 中加载；旧的 Zipformer 实现保持原样。 */
internal interface LocalAsrSession : AutoCloseable {
    fun reset()
    fun accept(pcm: ByteArray)
    fun partial(): String
    fun finish(): String
}

internal fun createLocalAsrSession(info: AsrModelManager.AsrModelInfo, dir: File): LocalAsrSession {
    fun file(name: String): String = requireNotNull(findAsrFile(dir, name)) { "模型文件不完整：$name" }.absolutePath
    return when (info.engine) {
        AsrEngine.LEGACY_ZIPFORMER -> LegacyAsrSession(AsrNative.nativeCreate(file(info.encoderFile), file(info.decoderFile), file(info.joinerFile), file("tokens.txt")))
        AsrEngine.STREAMING_PARAFORMER, AsrEngine.STREAMING_CTC -> SherpaAsrSession(info, dir)
        AsrEngine.DOLPHIN_STREAMING -> DolphinAsrSession(file(info.modelFile), file("tokens.txt"))
    }
}

internal fun findAsrFile(dir: File, name: String): File? {
    require(name.isNotBlank() && File(name).name == name) { "无效的模型文件名" }
    val direct = File(dir, name)
    if (direct.isFile) return direct
    return dir.listFiles()?.filter { it.isDirectory && !java.nio.file.Files.isSymbolicLink(it.toPath()) }
        ?.firstNotNullOfOrNull { findAsrFile(it, name) }
}

private class LegacyAsrSession(private var handle: Long) : LocalAsrSession {
    init { check(handle != 0L) { "原本地模型加载失败" } }
    override fun reset() = AsrNative.nativeReset(handle)
    override fun accept(pcm: ByteArray) = AsrNative.nativeAcceptPcm(handle, pcm)
    override fun partial(): String = AsrNative.nativeGetPartial(handle)
    override fun finish(): String = AsrNative.nativeFinalize(handle)
    override fun close() { if (handle != 0L) AsrNative.nativeRelease(handle); handle = 0L }
}

internal class SherpaAsrSession(info: AsrModelManager.AsrModelInfo, dir: File) : LocalAsrSession {
    private val pcm = Pcm16Stream()
    private val recognizer: OnlineRecognizer
    private var stream: OnlineStream? = null
    private var finished = false
    init {
        fun file(name: String) = requireNotNull(findAsrFile(dir, name)).absolutePath
        val model = OnlineModelConfig(tokens = file("tokens.txt"), numThreads = 2, provider = "cpu", debug = false)
        when (info.engine) {
            AsrEngine.STREAMING_PARAFORMER -> model.paraformer = OnlineParaformerModelConfig(file(info.encoderFile), file(info.decoderFile))
            AsrEngine.STREAMING_CTC -> model.zipformer2Ctc = OnlineZipformer2CtcModelConfig(file(info.modelFile))
            else -> error("不支持的流式模型")
        }
        recognizer = OnlineRecognizer(config = OnlineRecognizerConfig(modelConfig = model, enableEndpoint = false))
        try { reset() } catch (error: Throwable) { recognizer.release(); throw error }
    }
    override fun reset() { stream?.release(); stream = recognizer.createStream(); pcm.reset(); finished = false }
    override fun accept(pcm: ByteArray) {
        check(!finished)
        requireNotNull(stream).acceptWaveform(this.pcm.accept(pcm), 16000)
        decode()
    }
    private fun decode() { val s = requireNotNull(stream); while (recognizer.isReady(s)) recognizer.decode(s) }
    override fun partial(): String = recognizer.getResult(requireNotNull(stream)).text
    override fun finish(): String {
        if (!finished) {
            val s = requireNotNull(stream)
            s.acceptWaveform(FloatArray(16000), 16000)
            s.inputFinished(); decode(); finished = true
        }
        return partial()
    }
    override fun close() { stream?.release(); stream = null; recognizer.release() }
}

internal object DolphinNative {
    init { System.loadLibrary("asr_jni") }
    external fun create(model: String, tokens: String): Long
    external fun reset(handle: Long)
    external fun accept(handle: Long, samples: FloatArray)
    external fun text(handle: Long, finish: Boolean): ByteArray
    external fun release(handle: Long)
}
private class DolphinAsrSession(model: String, tokens: String) : LocalAsrSession {
    private var handle = DolphinNative.create(model, tokens)
    private val pcm = Pcm16Stream()
    init { check(handle != 0L) }
    override fun reset() { DolphinNative.reset(handle); pcm.reset() }
    override fun accept(pcm: ByteArray) = DolphinNative.accept(handle, this.pcm.accept(pcm))
    override fun partial(): String = DolphinNative.text(handle, false).toString(Charsets.UTF_8)
    override fun finish(): String = DolphinNative.text(handle, true).toString(Charsets.UTF_8)
    override fun close() { if (handle != 0L) DolphinNative.release(handle); handle = 0L }
}
