package com.kingzcheung.xime.service

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import com.kingzcheung.xime.speech.AsrModelManager
import com.kingzcheung.xime.speech.LocalAsrSession
import com.kingzcheung.xime.speech.createLocalAsrSession
import java.io.File

/** 旧模型和新增流式后端共用 :asr 生命周期，输入法主进程不加载权重。 */
class AsrInferenceService : Service() {
    companion object {
        private const val TAG = "AsrInferenceService"
        private const val IDLE_RELEASE_DELAY_MS = 60_000L
        internal fun shouldReloadSession(loaded: Boolean, loadedDir: String?, requestedDir: String): Boolean =
            !loaded || loadedDir != requestedDir
    }
    internal var sessionFactory: (String) -> LocalAsrSession = { modelDir ->
        val dir = File(modelDir)
        createLocalAsrSession(AsrModelManager(this).getModelInfo(dir.name), dir)
    }
    private var session: LocalAsrSession? = null
    private var callback: IInferenceAsrCallback? = null
    private var loadedModelDir: String? = null
    private val asrLock = Any()
    @Volatile private var keepModelAlive = false
    private val idleHandler = Handler(Looper.getMainLooper())
    private val idleReleaseRunnable = Runnable { synchronized(asrLock) { releaseLocked() } }

    private fun releaseLocked() {
        val old = session
        session = null; callback = null; loadedModelDir = null
        try { old?.close() } catch (_: Exception) { Log.w(TAG, "释放本地语音模型失败") }
    }
    private fun scheduleIdleRelease() {
        idleHandler.removeCallbacks(idleReleaseRunnable)
        if (!keepModelAlive) idleHandler.postDelayed(idleReleaseRunnable, IDLE_RELEASE_DELAY_MS)
    }
    private val binder = object : IInferenceAsrService.Stub() {
        override fun startAsr(modelDir: String, receiver: IInferenceAsrCallback): Boolean {
            idleHandler.removeCallbacks(idleReleaseRunnable)
            // Binder工作线程持锁创建，避免切换/释放与模型加载交错后复活旧会话。
            synchronized(asrLock) {
                try {
                    if (shouldReloadSession(session != null, loadedModelDir, modelDir)) {
                        releaseLocked()
                        session = sessionFactory(modelDir)
                        loadedModelDir = modelDir
                    }
                    requireNotNull(session).reset()
                    callback = receiver
                    return true
                } catch (_: Exception) {
                    releaseLocked(); Log.e(TAG, "本地语音模型启动失败")
                } catch (_: LinkageError) {
                    releaseLocked(); Log.e(TAG, "本地语音运行库加载失败")
                }
            }
            return false
        }
        override fun pushAsrAudio(audioData: ByteArray) {
            val receiver: IInferenceAsrCallback?
            val partial: String
            synchronized(asrLock) {
                val current = session ?: return
                receiver = callback
                try { current.accept(audioData); partial = current.partial() }
                catch (_: Exception) {
                    releaseLocked()
                    try { receiver?.onError("本地识别遇到问题，可以重试或切回原模型") } catch (_: Exception) {}
                    return
                }
            }
            if (partial.isNotEmpty()) try { receiver?.onPartialResult(partial) } catch (_: Exception) {}
        }
        override fun stopAsr(): String {
            val result = synchronized(asrLock) {
                try { session?.finish().orEmpty() }
                catch (_: Exception) { Log.e(TAG, "本地语音收尾失败"); "" }
                finally { callback = null }
            }
            scheduleIdleRelease()
            return result
        }
        override fun cancelAsr() {
            synchronized(asrLock) {
                try { session?.reset() } catch (_: Exception) { releaseLocked() }
                callback = null
            }
            scheduleIdleRelease()
        }
        override fun releaseAsr() {
            idleHandler.removeCallbacks(idleReleaseRunnable)
            synchronized(asrLock) { releaseLocked() }
        }
        override fun setKeepModelAlive(keepAlive: Boolean) {
            keepModelAlive = keepAlive
            idleHandler.removeCallbacks(idleReleaseRunnable)
            if (!keepAlive) synchronized(asrLock) { if (session != null) scheduleIdleRelease() }
        }
    }
    override fun onBind(intent: Intent): IBinder = binder
    override fun onDestroy() {
        idleHandler.removeCallbacks(idleReleaseRunnable)
        synchronized(asrLock) { releaseLocked() }
        super.onDestroy()
    }
}
