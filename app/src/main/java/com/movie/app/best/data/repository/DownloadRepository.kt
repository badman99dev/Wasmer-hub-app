package com.movie.app.best.data.repository

import android.content.Context
import android.net.Uri
import android.os.Environment
import com.movie.app.best.data.debug.NetworkLogger
import com.movie.app.best.data.model.Resource
import com.movie.app.best.data.remote.BypassApiService
import com.movie.app.best.data.remote.BypassRequest
import com.ketch.Ketch
import com.ketch.Status
import com.ketch.DownloadModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class ResolvedMirror(
    val jackpot: String,
    val fileName: String,
    val size: String,
    val sizeBytes: Long?,
    val resumable: Boolean,
    val quality: String,
    val sourceLabel: String
)

@Singleton
class DownloadRepository @Inject constructor(
    private val bypassApiService: BypassApiService,
    private val ketch: Ketch,
    @ApplicationContext private val context: Context
) {
    fun resolveDownloadUrls(linkUrl: String): Flow<Resource<List<ResolvedMirror>>> = flow {
        emit(Resource.Loading())
        try {
            NetworkLogger.logAction("RESOLVE", "linkUrl=$linkUrl")
            val bypassResponse = bypassApiService.bypassUrl(BypassRequest(linkUrl, fetchInfo = true))
            if (!bypassResponse.success || bypassResponse.data.isEmpty()) {
                emit(Resource.Error("Bypass API failed - no direct link returned"))
                return@flow
            }
            val successItems = bypassResponse.data.filter { it.status == "SUCCESS" && !it.jackpot.isNullOrEmpty() }
            if (successItems.isEmpty()) {
                emit(Resource.Error("No direct download URL found"))
                return@flow
            }
            val mirrors = successItems.mapIndexedNotNull { idx, item ->
                val jackpot = item.jackpot ?: return@mapIndexedNotNull null
                if (!jackpot.startsWith("http")) return@mapIndexedNotNull null
                val fi = item.fileInfo
                val fileName = fi?.filename ?: extractFileNameFromUrl(jackpot)
                val quality = detectQuality(fileName)
                val sourceLabel = if (successItems.size == 1) "Download Now" else "Server ${idx + 1}"
                ResolvedMirror(
                    jackpot = jackpot,
                    fileName = fileName,
                    size = fi?.size ?: "",
                    sizeBytes = fi?.sizeBytes,
                    resumable = fi?.resumable ?: false,
                    quality = quality,
                    sourceLabel = sourceLabel
                )
            }
            if (mirrors.isEmpty()) {
                emit(Resource.Error("No valid direct download URLs found"))
                return@flow
            }
            NetworkLogger.logAction("RESOLVE", "mirrors=${mirrors.size}")
            emit(Resource.Success(mirrors))
        } catch (e: Exception) {
            NetworkLogger.logAction("RESOLVE_ERR", e.message ?: "unknown")
            emit(Resource.Error(e.message ?: "Failed to resolve download URL"))
        }
    }

    fun startDownload(url: String, fileName: String, title: String): Int {
        val safeFileName = sanitizeFileName(fileName)
        val actualFileName = if (safeFileName.isNotBlank()) safeFileName else extractFileNameFromUrl(url)

        NetworkLogger.logAction("DOWNLOAD_START", "url=$url file=$actualFileName title=$title")

        val downloadPath = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "WasmerHub"
        ).apply { if (!exists()) mkdirs() }.path

        val headers = HashMap<String, String>().apply {
            put("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Mobile Safari/537.36")
            put("Referer", "https://wasmer-hub.vercel.app/")
            put("Accept", "*/*")
        }

        val id = ketch.download(
            url = url,
            path = downloadPath,
            fileName = actualFileName,
            tag = "wasmerhub",
            headers = headers
        )

        NetworkLogger.logAction("DOWNLOAD_QUEUED", "id=$id file=$actualFileName")
        return id
    }

    fun observeDownloads(): Flow<List<DownloadModel>> {
        return ketch.observeDownloads()
    }

    fun pauseDownload(id: Int) {
        ketch.pause(id)
        NetworkLogger.logAction("DOWNLOAD_PAUSE", "id=$id")
    }

    fun resumeDownload(id: Int) {
        ketch.resume(id)
        NetworkLogger.logAction("DOWNLOAD_RESUME", "id=$id")
    }

    fun cancelDownload(id: Int) {
        ketch.cancel(id)
        NetworkLogger.logAction("DOWNLOAD_CANCEL", "id=$id")
    }

    fun retryDownload(id: Int) {
        ketch.retry(id)
        NetworkLogger.logAction("DOWNLOAD_RETRY", "id=$id")
    }

    fun deleteDownload(id: Int) {
        ketch.clearDb(id)
        NetworkLogger.logAction("DOWNLOAD_DELETE", "id=$id")
    }

    private fun detectQuality(filename: String): String {
        val f = filename.lowercase()
        return when {
            f.contains("4k") || f.contains("2160p") || f.contains("uhd") -> "4K"
            f.contains("2k") || f.contains("1440p") -> "2K"
            f.contains("1080p") || f.contains("fhd") -> "1080p"
            f.contains("720p") || f.contains("hd") -> "720p"
            f.contains("480p") -> "480p"
            f.contains("360p") -> "360p"
            else -> "Unknown"
        }
    }

    private fun sanitizeFileName(name: String): String {
        return name.replace(Regex("[\\\\/:*?\"<>|]"), "_")
    }

    private fun extractFileNameFromUrl(url: String): String {
        return try {
            val path = Uri.parse(url).path ?: "download"
            path.substringAfterLast("/").ifEmpty { "download" }
        } catch (_: Exception) {
            "download"
        }
    }
}
