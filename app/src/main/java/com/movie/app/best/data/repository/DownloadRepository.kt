package com.movie.app.best.data.repository

import android.content.Context
import android.net.Uri
import android.os.Environment
import com.movie.app.best.data.debug.NetworkLogger
import com.movie.app.best.data.model.DownloadMetadata
import com.movie.app.best.data.model.DownloadPhase
import com.movie.app.best.data.model.Resource
import com.movie.app.best.data.remote.BypassApiService
import com.movie.app.best.data.remote.BypassRequest
import com.ketch.Ketch
import com.ketch.Status
import com.ketch.DownloadModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
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
    val sourceLabel: String,
    val isZip: Boolean = false
)

data class DownloadStatusInfo(
    val phase: DownloadPhase,
    val progress: Int = 0,
    val ketchId: Int = -1,
    val failureReason: String? = null
)

@Singleton
class DownloadRepository @Inject constructor(
    private val bypassApiService: BypassApiService,
    private val ketch: Ketch,
    private val metadataStore: DownloadMetadataStore,
    private val zipExtractor: ZipExtractor,
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
                    sourceLabel = sourceLabel,
                    isZip = isZipFileName(fileName)
                )
            }.distinctBy { it.jackpot }
            if (mirrors.size == 1) {
                mirrors.mapIndexed { idx, m -> m.copy(sourceLabel = "Download Now") }
            } else {
                mirrors.mapIndexed { idx, m -> m.copy(sourceLabel = "Server ${idx + 1}") }
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

    suspend fun startDownloadWithMetadata(
        mirror: ResolvedMirror,
        slug: String,
        posterUrl: String,
        title: String,
        contentType: String = "movie",
        episodeId: Int? = null,
        episodeLabel: String? = null
    ): Int {
        val safeFileName = sanitizeFileName(mirror.fileName)
        val isZip = metadataStore.isZipFile(safeFileName)

        val downloadPath = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "WasmerHub"
        ).apply { if (!exists()) mkdirs() }.path

        val filePath = File(downloadPath, safeFileName).path
        val metaKey = slug + (episodeLabel ?: "")

        NetworkLogger.logAction("DOWNLOAD_INIT", "slug=$slug poster=$posterUrl isZip=$isZip")

        val localPosterPath = metadataStore.downloadPoster(posterUrl, slug)

        val metadata = DownloadMetadata(
            slug = slug,
            title = title,
            posterUrl = posterUrl,
            localPosterPath = localPosterPath,
            fileName = safeFileName,
            filePath = filePath,
            isZip = isZip,
            contentType = contentType,
            episodeId = episodeId,
            episodeLabel = episodeLabel,
            status = "initializing"
        )
        metadataStore.saveMetadata(metadata)

        val headers = HashMap<String, String>().apply {
            put("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Mobile Safari/537.36")
            put("Referer", "https://wasmer-hub.vercel.app/")
            put("Accept", "*/*")
        }

        val id = ketch.download(
            url = mirror.jackpot,
            path = downloadPath,
            fileName = safeFileName,
            tag = "wasmerhub",
            headers = headers
        )

        metadataStore.updateMetadata(metaKey) { it.copy(ketchId = id, status = "downloading") }
        NetworkLogger.logAction("DOWNLOAD_QUEUED", "id=$id file=$safeFileName slug=$slug")
        return id
    }

    fun startDownload(url: String, fileName: String, title: String): Int {
        val safeFileName = sanitizeFileName(fileName)
        val actualFileName = if (safeFileName.isNotBlank()) safeFileName else extractFileNameFromUrl(url)

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

    fun observeDownloadStatus(ketchId: Int): Flow<DownloadStatusInfo> {
        return ketch.observeDownloads().map { downloads ->
            val dl = downloads.find { it.id == ketchId }
            if (dl == null) {
                DownloadStatusInfo(phase = DownloadPhase.NONE, ketchId = ketchId)
            } else {
                val phase = when (dl.status) {
                    Status.QUEUED -> DownloadPhase.DOWNLOADING
                    Status.STARTED -> DownloadPhase.DOWNLOADING
                    Status.PROGRESS -> DownloadPhase.DOWNLOADING
                    Status.SUCCESS -> DownloadPhase.COMPLETE
                    Status.CANCELLED -> DownloadPhase.CANCELLED
                    Status.FAILED -> DownloadPhase.FAILED
                    Status.PAUSED -> DownloadPhase.DOWNLOADING
                    Status.DEFAULT -> DownloadPhase.NONE
                }
                DownloadStatusInfo(
                    phase = phase,
                    progress = dl.progress,
                    ketchId = ketchId,
                    failureReason = if (dl.status == Status.FAILED) dl.failureReason else null
                )
            }
        }
    }

    suspend fun postProcessDownload(ketchId: Int, metaKey: String) {
        val meta = metadataStore.getMetadata(metaKey) ?: return
        if (!meta.isZip) {
            metadataStore.updateMetadata(metaKey) { it.copy(status = "complete") }
            return
        }

        metadataStore.updateMetadata(metaKey) { it.copy(status = "extracting") }

        val downloadDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "WasmerHub"
        )

        val extractedVideos = zipExtractor.extractZip(meta.filePath, downloadDir)
        val extractPath = if (extractedVideos.isNotEmpty()) {
            File(extractedVideos.first()).parent
        } else null

        if (extractPath != null) {
            try { File(meta.filePath).delete() } catch (_: Exception) {}
            try { ketch.clearDb(ketchId) } catch (_: Exception) {}
            metadataStore.updateMetadata(metaKey) { it.copy(status = "complete", extractPath = extractPath, filePath = extractPath) }
            NetworkLogger.logAction("ZIP_POSTPROCESS", "key=$metaKey extractPath=$extractPath videos=${extractedVideos.size} ZIP_DELETED=true")
        } else {
            metadataStore.updateMetadata(metaKey) { it.copy(status = "failed") }
            NetworkLogger.logAction("ZIP_POSTPROCESS", "key=$metaKey extraction FAILED, ZIP kept")
        }
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

    fun getAllMetadata(): List<DownloadMetadata> = metadataStore.getAllMetadata()

    fun getMetadata(key: String): DownloadMetadata? = metadataStore.getMetadata(key)

    fun saveMetadataDirect(key: String, metadata: DownloadMetadata) {
        metadataStore.saveMetadataByKey(key, metadata)
    }

    fun rescanDownloads() {
        val downloadDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "WasmerHub"
        )
        metadataStore.rescanAndCleanup(downloadDir)
    }

    private fun isZipFileName(fileName: String): Boolean {
        val f = fileName.lowercase()
        return f.endsWith(".zip") || f.endsWith(".rar") || f.endsWith(".7z")
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
