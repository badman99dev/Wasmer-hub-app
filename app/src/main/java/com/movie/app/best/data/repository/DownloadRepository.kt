package com.movie.app.best.data.repository

import android.content.Context
import android.net.Uri
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
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadRepository @Inject constructor(
    private val bypassApiService: BypassApiService,
    private val ketch: Ketch,
    @ApplicationContext private val context: Context
) {
    fun resolveDownloadUrl(linkUrl: String): Flow<Resource<String>> = flow {
        emit(Resource.Loading())
        try {
            NetworkLogger.logAction("RESOLVE", "linkUrl=$linkUrl")
            val bypassResponse = bypassApiService.bypassUrl(BypassRequest(linkUrl))
            if (!bypassResponse.success || bypassResponse.data.isEmpty()) {
                emit(Resource.Error("Bypass API failed - no direct link returned"))
                return@flow
            }
            val directUrl = bypassResponse.data.firstOrNull { it.status == "SUCCESS" }?.jackpot
                ?: bypassResponse.data.firstOrNull()?.jackpot
            if (directUrl.isNullOrEmpty()) {
                emit(Resource.Error("No direct download URL found"))
                return@flow
            }
            if (!directUrl.startsWith("http")) {
                emit(Resource.Error("Invalid URL returned: $directUrl"))
                return@flow
            }
            NetworkLogger.logAction("RESOLVE", "directUrl=$directUrl")
            emit(Resource.Success(directUrl))
        } catch (e: Exception) {
            NetworkLogger.logAction("RESOLVE_ERR", e.message ?: "unknown")
            emit(Resource.Error(e.message ?: "Failed to resolve download URL"))
        }
    }

    fun startDownload(url: String, fileName: String, title: String): Int {
        val safeFileName = sanitizeFileName(fileName)
        val actualFileName = if (safeFileName.isNotBlank()) safeFileName else extractFileNameFromUrl(url)

        NetworkLogger.logAction("DOWNLOAD_START", "url=$url file=$actualFileName title=$title")

        val headers = HashMap<String, String>().apply {
            put("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Mobile Safari/537.36")
            put("Referer", "https://wasmer-hub.vercel.app/")
            put("Accept", "*/*")
        }

        val id = ketch.download(
            url = url,
            fileName = actualFileName,
            path = "WasmerHub",
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
