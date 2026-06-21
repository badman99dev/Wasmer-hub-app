package com.movie.app.best.data.repository

import com.movie.app.best.data.debug.NetworkLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ZipExtractor @Inject constructor() {

    private val videoExtensions = listOf("mp4", "mkv", "avi", "webm", "mov", "flv", "3gp", "ts", "m4v")

    suspend fun extractZip(zipPath: String, baseDir: File): List<String> = withContext(Dispatchers.IO) {
        val zipFile = File(zipPath)
        if (!zipFile.exists()) return@withContext emptyList()

        val folderName = zipFile.nameWithoutExtension
        val extractDir = File(baseDir, folderName).apply { if (!exists()) mkdirs() }
        val extractedVideos = mutableListOf<String>()

        try {
            when {
                zipPath.lowercase().endsWith(".zip") -> extractZipFile(zipFile, extractDir, extractedVideos)
                else -> {
                    NetworkLogger.logAction("ZIP_EXTRACT", "Unsupported archive format: $zipPath")
                    return@withContext emptyList()
                }
            }
        } catch (e: Exception) {
            NetworkLogger.logAction("ZIP_EXTRACT_ERR", e.message ?: "unknown")
        }

        extractedVideos.sortBy { File(it).name }
        NetworkLogger.logAction("ZIP_EXTRACT", "Extracted ${extractedVideos.size} videos to ${extractDir.path}")
        extractedVideos
    }

    private fun extractZipFile(zipFile: File, extractDir: File, extractedVideos: MutableList<String>) {
        FileInputStream(zipFile).use { fis ->
            ZipInputStream(fis).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        val entryName = entry.name.substringAfterLast("/")
                        val outFile = File(extractDir, entryName)

                        outFile.parentFile?.mkdirs()

                        if (isVideoFile(entryName)) {
                            FileOutputStream(outFile).use { fos ->
                                zis.copyTo(fos)
                            }
                            extractedVideos.add(outFile.absolutePath)
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
        }
    }

    private fun isVideoFile(fileName: String): Boolean {
        val ext = fileName.substringAfterLast(".", "").lowercase()
        return videoExtensions.contains(ext)
    }
}
