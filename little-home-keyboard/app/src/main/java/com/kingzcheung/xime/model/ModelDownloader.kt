package com.kingzcheung.xime.model

import com.kingzcheung.xime.R

import com.kingzcheung.xime.i18n.tr

import android.content.Context
import com.kingzcheung.xime.util.FileLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

object ModelDownloader {

    private const val TAG = "ModelDownloader"
    private const val CONNECT_TIMEOUT = 30L
    private const val READ_TIMEOUT = 300L
    private const val MAX_RETRIES = 3

    private val client = OkHttpClient.Builder()
        .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
        .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    suspend fun downloadModel(
        context: Context,
        modelInfo: ModelInfo,
        onProgress: (ModelDownloadState) -> Unit,
        version: ModelVersion? = null
    ) = withContext(Dispatchers.IO) {
        FileLogger.i(TAG, "Starting download: ${modelInfo.id}")

        val target = version ?: modelInfo.resolvedVersion()
        if (target == null) {
            onProgress(ModelDownloadState.Error(tr(R.string.s_moxingmeiyoukeyongbanben)))
            return@withContext
        }

        val storageDir = getStorageDir(context, modelInfo)
        storageDir.mkdirs()

        try {
            if (target.archiveUrl != null) {
                downloadAndExtractArchive(context, target.archiveUrl, storageDir, target.sha256, onProgress)
            } else {
                downloadFiles(target.files, storageDir, onProgress)
            }
            if (target.sha256.isNotBlank()) File(storageDir, ".verified-model").writeText(target.sha256)
            FileLogger.i(TAG, "Download complete: ${modelInfo.id}")
            onProgress(ModelDownloadState.Complete)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            FileLogger.e(TAG, "Download failed: ${modelInfo.id}: ${e.message}", e)
            onProgress(ModelDownloadState.Error(tr(R.string.s_xiazaishibai, e.message)))
        }
    }

    private fun getStorageDir(context: Context, modelInfo: ModelInfo): File {
        // 统一规则：所有模型一律存 filesDir/models/<id>/
        return ModelStorage.getModelDir(context, modelInfo.id)
    }

    private suspend fun downloadFiles(
        files: List<ModelFile>,
        targetDir: File,
        onProgress: (ModelDownloadState) -> Unit
    ) {
        val totalFiles = files.size

        files.forEachIndexed { index, file ->
            FileLogger.i(TAG, "Downloading ${file.name} (${index + 1}/$totalFiles)")
            downloadSingleFile(file, File(targetDir, file.name)) { fileProgress ->
                val overall = (index.toFloat() + fileProgress) / totalFiles
                onProgress(ModelDownloadState.Downloading(overall, 0, -1))
            }
        }
    }

    private suspend fun downloadSingleFile(
        file: ModelFile,
        targetFile: File,
        onProgress: (Float) -> Unit = {}
    ) {
        require(file.name == java.io.File(file.name).name && file.name !in listOf(".", ".."))
        require(file.downloadUrl.startsWith("https://")) { "模型下载需要HTTPS" }
        val temporary = File(targetFile.parentFile, targetFile.name + ".part")
        try {
            client.newCall(Request.Builder().url(file.downloadUrl).build()).execute().use { response ->
                if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
                val body = response.body ?: throw IOException("模型下载没有返回文件")
                val total = body.contentLength()
                var received = 0L
                targetFile.parentFile?.mkdirs()
                body.byteStream().use { input -> FileOutputStream(temporary).use { output ->
                    val buffer = ByteArray(65536)
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val count = input.read(buffer)
                        if (count < 0) break
                        output.write(buffer, 0, count); received += count
                        if (total > 0) onProgress(received.toFloat() / total)
                    }
                } }
                if (received == 0L || (total > 0 && received != total)) throw IOException("模型下载未完成")
            }
            verifyModelFile(temporary, file.sha256, file.bytes)
            java.nio.file.Files.move(temporary.toPath(), targetFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING)
        } finally { temporary.delete() }
    }

    private suspend fun downloadArchive(
        url: String,
        tmpFile: File,
        onProgress: (ModelDownloadState) -> Unit
    ) {
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()

        if (!response.isSuccessful) {
            throw IOException("HTTP ${response.code}")
        }

        val body = response.body ?: throw IOException("Response body is null")
        val totalBytes = body.contentLength()
        var downloadedBytes = 0L

        tmpFile.parentFile?.mkdirs()
        body.byteStream().use { input ->
            FileOutputStream(tmpFile).use { output ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                    downloadedBytes += bytesRead
                    if (totalBytes > 0) {
                        val progress = downloadedBytes.toFloat() / totalBytes.toFloat()
                        onProgress(ModelDownloadState.Downloading(progress, downloadedBytes, totalBytes))
                    }
                }
            }
        }

        if (totalBytes > 0 && downloadedBytes != totalBytes) {
            throw IOException("Download incomplete: $downloadedBytes/$totalBytes bytes")
        }
        if (downloadedBytes == 0L) {
            throw IOException("Downloaded file is empty")
        }

        FileLogger.i(TAG, "Archive downloaded: $downloadedBytes bytes")
    }

    private suspend fun downloadAndExtractArchive(
        context: Context,
        archiveUrl: String,
        targetDir: File,
        sha256: String,
        onProgress: (ModelDownloadState) -> Unit
    ) {
        val tmpFile = File(context.cacheDir, "${archiveUrl.hashCode()}.tar.bz2")

        for (attempt in 1..MAX_RETRIES) {
            try {
                if (attempt > 1) {
                    tmpFile.delete()
                    onProgress(ModelDownloadState.Downloading(0f, 0, -1))
                }

                downloadArchive(archiveUrl, tmpFile, onProgress)
                verifyModelFile(tmpFile, sha256)
                val staged = File(context.cacheDir, "model-stage-${archiveUrl.hashCode()}")
                check(staged.canonicalFile.parentFile == context.cacheDir.canonicalFile)
                staged.deleteRecursively(); staged.mkdirs()
                try {
                    extractTarBz2(tmpFile, staged)
                    File(targetDir, ".verified-model").delete()
                    staged.walkTopDown().filter { it.isFile }.forEach { source ->
                        val destination = safeModelEntry(targetDir, source.relativeTo(staged).path)
                        destination.parentFile?.mkdirs()
                        java.nio.file.Files.move(source.toPath(), destination.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING)
                    }
                } finally { staged.deleteRecursively() }
                tmpFile.delete()
                return
            } catch (e: CancellationException) {
                tmpFile.delete(); throw e
            } catch (e: Exception) {
                FileLogger.e(TAG, "Attempt $attempt/$MAX_RETRIES failed: ${e.message}")
                tmpFile.delete()
                if (attempt < MAX_RETRIES) {
                    val delayMs = 1000L * (1L shl (attempt - 1))
                    delay(delayMs)
                } else {
                    throw e
                }
            }
        }
    }

    private fun extractTarBz2(archiveFile: File, targetDir: File) {
        FileInputStream(archiveFile).use { fis ->
            BufferedInputStream(fis, 65536).use { bis ->
                BZip2CompressorInputStream(bis).use { bzIn ->
                    TarArchiveInputStream(bzIn).use { tarIn ->
                        var entry = tarIn.nextEntry
                        while (entry != null) {
                            val rawName = entry.name
                            val parts = rawName.split("/", limit = 2)
                            val entryName = if (parts.size > 1) parts[1] else rawName

                            if (entryName.isNotEmpty() && !entry.isDirectory) {
                                require(entry.isFile && !entry.isSymbolicLink && !entry.isLink) { "模型包包含不支持的链接" }
                                val outputFile = safeModelEntry(targetDir, entryName)
                                outputFile.parentFile?.mkdirs()
                                FileOutputStream(outputFile).use { out ->
                                    val buffer = ByteArray(8192)
                                    var len: Int
                                    while (tarIn.read(buffer).also { len = it } != -1) {
                                        out.write(buffer, 0, len)
                                    }
                                }
                            }
                            entry = tarIn.nextEntry
                        }
                    }
                }
            }
        }
    }

    suspend fun getDownloadSize(modelInfo: ModelInfo, version: ModelVersion? = null): Long {
        val target = version ?: modelInfo.resolvedVersion()
        val url = target?.archiveUrl ?: target?.files?.firstOrNull()?.downloadUrl ?: return -1
        return try {
            val request = Request.Builder().head().url(url).build()
            val response = client.newCall(request).execute()
            response.body?.contentLength() ?: -1
        } catch (e: Exception) {
            -1
        }
    }
}
