package com.movie.app.best.data.repository

import android.app.DownloadManager
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import com.movie.app.best.data.debug.DebugInterceptor
import com.movie.app.best.data.debug.NetworkLogger
import com.movie.app.best.data.model.Resource
import com.movie.app.best.data.remote.BypassApiService
import com.movie.app.best.data.remote.BypassRequest
import com.movie.app.best.data.remote.MovieApiService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class DownloadProgress(
    val downloadId: Long,
    val fileName: String,
    val bytesDownloaded: Long,
    val totalBytes: Long,
    val status: Int,
    val reason: Int
) {
    val percentage: Int
        get() = if (totalBytes > 0) ((bytesDownloaded * 100) / totalBytes).toInt() else 0

    val isCompleted: Boolean
        get() = status == DownloadManager.STATUS_SUCCESSFUL

    val isFailed: Boolean
        get() = status == DownloadManager.STATUS_FAILED

    val isPaused: Boolean
        get() = status == DownloadManager.STATUS_PAUSED

    val isPending: Boolean
        get() = status == DownloadManager.STATUS_PENDING

    val isRunning: Boolean
        get() = status == DownloadManager.STATUS_RUNNING
}

@Singleton
class DownloadRepository @Inject constructor(
    private val apiService: MovieApiService,
    private val bypassApiService: BypassApiService,
    @ApplicationContext private val context: Context
) {
    private val downloadManager: DownloadManager by lazy {
        context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    }

    private val speedReadings = mutableMapOf<Long, MutableList<Pair<Long, Long>>>()

    fun resolveDownloadUrl(slug: String, linkId: Int): Flow<Resource<String>> = flow {
        emit(Resource.Loading())
        try {
            NetworkLogger.logAction("RESOLVE", "slug=$slug linkId=$linkId")
            val downloadResponse = apiService.getDownloadInfo(slug, linkId)
            if (downloadResponse.status != "success" || downloadResponse.data == null) {
                emit(Resource.Error(downloadResponse.message ?: "Failed to get download info"))
                return@flow
            }
            val downloadData = downloadResponse.data
            val targetUrl = downloadData.targetUrl
            NetworkLogger.logAction("RESOLVE", "targetUrl=$targetUrl")
            if (targetUrl.isEmpty()) {
                emit(Resource.Error("No target URL found"))
                return@flow
            }
            val bypassResponse = bypassApiService.bypassUrl(BypassRequest(targetUrl))
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

    fun startDownload(url: String, fileName: String, title: String): Long {
        val safeFileName = sanitizeFileName(fileName)
        val actualFileName = if (safeFileName.isNotBlank()) safeFileName else extractFileNameFromUrl(url)

        NetworkLogger.logAction("DOWNLOAD_START", "url=$url file=$actualFileName title=$title")

        val request = DownloadManager.Request(Uri.parse(url)).apply {
            setTitle(title)
            setDescription("Downloading via WasmerHub")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "WasmerHub/$actualFileName")
            setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
            setAllowedOverMetered(true)
            setAllowedOverRoaming(true)
            addRequestHeader("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Mobile Safari/537.36")
            addRequestHeader("Referer", "https://wasmer-jhns970ko-badals-projects-03fab3df.vercel.app/")
            addRequestHeader("Accept", "*/*")
        }

        val downloadId = downloadManager.enqueue(request)
        speedReadings[downloadId] = mutableListOf()

        NetworkLogger.logAction("DOWNLOAD_QUEUED", "downloadId=$downloadId file=$actualFileName")

        return downloadId
    }

    fun getDownloadProgress(downloadId: Long): DownloadProgress? {
        val query = DownloadManager.Query().setFilterById(downloadId)
        val cursor: Cursor? = downloadManager.query(query)
        cursor?.use {
            if (it.moveToFirst()) {
                val bytesDownloaded = it.getLong(it.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                val totalBytes = it.getLong(it.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                val status = it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                val reason = it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
                val fileName = try {
                    val localUri = it.getString(it.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI))
                    localUri?.substringAfterLast("/") ?: ""
                } catch (_: Exception) {
                    ""
                }

                return DownloadProgress(
                    downloadId = downloadId,
                    fileName = fileName,
                    bytesDownloaded = bytesDownloaded,
                    totalBytes = totalBytes,
                    status = status,
                    reason = reason
                )
            }
        }
        return null
    }

    fun calculateSpeed(downloadId: Long): Double {
        val progress = getDownloadProgress(downloadId) ?: return 0.0
        val now = System.currentTimeMillis()
        val readings = speedReadings.getOrPut(downloadId) { mutableListOf() }
        readings.add(Pair(now, progress.bytesDownloaded))

        val cutoff = now - 5000
        readings.removeAll { it.first < cutoff }

        if (readings.size < 2) return 0.0

        val oldest = readings.first()
        val newest = readings.last()
        val timeDiff = (newest.first - oldest.first) / 1000.0
        val bytesDiff = newest.second - oldest.second

        return if (timeDiff > 0) bytesDiff / timeDiff else 0.0
    }

    fun calculateEta(downloadId: Long): String {
        val progress = getDownloadProgress(downloadId) ?: return "--"
        val avgSpeed = calculateSpeed(downloadId)
        if (avgSpeed <= 0) return "--"
        val remaining = progress.totalBytes - progress.bytesDownloaded
        if (remaining <= 0) return "0s"
        val etaSeconds = (remaining / avgSpeed).toLong()
        return formatEta(etaSeconds)
    }

    fun cancelDownload(downloadId: Long) {
        downloadManager.remove(downloadId)
        speedReadings.remove(downloadId)
        NetworkLogger.logAction("DOWNLOAD_CANCEL", "downloadId=$downloadId")
    }

    fun getCompletedDownloads(): List<DownloadProgress> {
        val query = DownloadManager.Query().setFilterByStatus(DownloadManager.STATUS_SUCCESSFUL)
        val results = mutableListOf<DownloadProgress>()
        val cursor: Cursor? = downloadManager.query(query)
        cursor?.use {
            while (it.moveToNext()) {
                val id = it.getLong(it.getColumnIndexOrThrow(DownloadManager.COLUMN_ID))
                val bytesDownloaded = it.getLong(it.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                val totalBytes = it.getLong(it.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                val status = it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                val reason = it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
                val fileName = try {
                    val localUri = it.getString(it.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI))
                    localUri?.substringAfterLast("/") ?: ""
                } catch (_: Exception) {
                    ""
                }
                results.add(DownloadProgress(id, fileName, bytesDownloaded, totalBytes, status, reason))
            }
        }
        return results.filter { it.fileName.contains("WasmerHub") || it.fileName.isNotEmpty() }
    }

    private fun formatEta(seconds: Long): String {
        return when {
            seconds < 60 -> "${seconds}s"
            seconds < 3600 -> "${seconds / 60}m ${seconds % 60}s"
            else -> "${seconds / 3600}h ${(seconds % 3600) / 60}m"
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
