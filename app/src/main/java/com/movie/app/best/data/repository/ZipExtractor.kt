package com.movie.app.best.data.repository

import com.movie.app.best.data.debug.NetworkLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import net.lingala.zip4j.ZipFile
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class ExtractionProgress(
    val percent: Int,
    val currentFile: String,
    val state: ExtractionState
)

enum class ExtractionState {
    IN_PROGRESS,
    COMPLETE,
    FAILED
}

@Singleton
class ZipExtractor @Inject constructor() {

    private val videoExtensions = listOf("mp4", "mkv", "avi", "webm", "mov", "flv", "3gp", "ts", "m4v")

    fun extractZipWithProgress(zipPath: String, baseDir: File): Flow<ExtractionProgress> = flow {
        val zipFile = File(zipPath)
        if (!zipFile.exists()) {
            NetworkLogger.logAction("ZIP_EXTRACT_ERR", "ZIP file not found: $zipPath")
            emit(ExtractionProgress(0, "", ExtractionState.FAILED))
            return@flow
        }

        val folderName = zipFile.nameWithoutExtension
        val extractDir = File(baseDir, folderName).apply { if (!exists()) mkdirs() }

        try {
            val zip = ZipFile(zipFile)
            zip.setRunInThread(true)

            zip.extractAll(extractDir.absolutePath)

            val monitor = zip.progressMonitor
            var stuckCount = 0
            var lastPercent = -1

            while (true) {
                val stateName = monitor.state.name
                val percent = if (monitor.percentDone < 0) 0 else monitor.percentDone

                if (stateName == "SUCCESS" || stateName == "CANCELLED" || stateName == "ERROR") break
                if (percent >= 100) break

                if (percent == lastPercent) {
                    stuckCount++
                    if (stuckCount > 300) {
                        NetworkLogger.logAction("ZIP_EXTRACT_TIMEOUT", "Stuck at $percent% for 30s, breaking")
                        break
                    }
                } else {
                    stuckCount = 0
                    lastPercent = percent
                }

                emit(ExtractionProgress(
                    percent = percent,
                    currentFile = monitor.fileName ?: "",
                    state = ExtractionState.IN_PROGRESS
                ))
                delay(100)
            }

            val extractedVideos = mutableListOf<String>()
            scanForVideos(extractDir, extractedVideos)
            extractedVideos.sortBy { File(it).name }

            if (extractedVideos.isNotEmpty()) {
                NetworkLogger.logAction("ZIP_EXTRACT", "Extracted ${extractedVideos.size} videos to ${extractDir.path}")
                emit(ExtractionProgress(100, "", ExtractionState.COMPLETE))
            } else {
                NetworkLogger.logAction("ZIP_EXTRACT_ERR", "No videos found in ${extractDir.path}")
                emit(ExtractionProgress(0, "", ExtractionState.FAILED))
            }
        } catch (e: Exception) {
            NetworkLogger.logAction("ZIP_EXTRACT_ERR", e.message ?: "unknown")
            emit(ExtractionProgress(0, "", ExtractionState.FAILED))
        }
    }.flowOn(Dispatchers.IO)

    private fun scanForVideos(dir: File, result: MutableList<String>) {
        dir.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                scanForVideos(file, result)
            } else if (isVideoFile(file.name)) {
                result.add(file.absolutePath)
            }
        }
    }

    private fun isVideoFile(fileName: String): Boolean {
        val ext = fileName.substringAfterLast(".", "").lowercase()
        return videoExtensions.contains(ext)
    }
}
