package com.kingzcheung.xime.home

import android.app.job.JobParameters
import android.app.job.JobService
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean

class HomeClipboardJob : JobService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var stopped = AtomicBoolean(false)
    override fun onStartJob(params: JobParameters): Boolean {
        val flag = AtomicBoolean(false); stopped = flag
        scope.launch {
            val sync = HomeClipboardSync.get(applicationContext)
            var completed = false
            try { sync.drain(params.network) { flag.get() }; completed = true } catch (_: Exception) { sync.recordNotice("安全发送任务未能完成，请打开连接页检查；打字不受影响。") }
            finally {
                if (!flag.get()) {
                    jobFinished(params, false)
                    if (completed) runCatching { sync.schedule(force = true) }
                }
            }
        }
        return true
    }
    override fun onStopJob(params: JobParameters): Boolean {
        stopped.set(true); HomeClipboardSync.get(applicationContext).cancelRequest()
        return true
    }
    override fun onDestroy() { stopped.set(true); scope.cancel(); super.onDestroy() }
}
