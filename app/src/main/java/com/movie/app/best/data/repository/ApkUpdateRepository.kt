package com.movie.app.best.data.repository

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApkUpdateRepository @Inject constructor() {

    companion object {
        private const val APK_NAME = "wasmer-hub-update.apk"
    }

    fun downloadApk(context: Context, url: String): Long {
        cleanupOldApk(context)

        val request = DownloadManager.Request(Uri.parse(url)).apply {
            setMimeType("application/vnd.android.package-archive")
            setTitle("Wasmer Hub Update")
            setDescription("Downloading latest version...")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            setDestinationInExternalFilesDir(context, null, APK_NAME)
        }

        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        return dm.enqueue(request)
    }

    fun observeUpdateProgress(context: Context, downloadId: Long): Flow<ApkUpdateState> = flow {
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

        while (true) {
            val cursor = dm.query(DownloadManager.Query().setFilterById(downloadId))
            if (cursor.moveToFirst()) {
                val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                val reason = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))

                when (status) {
                    DownloadManager.STATUS_RUNNING -> {
                        val total = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                        val downloaded = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                        val progress = if (total > 0) ((downloaded * 100) / total).toInt() else 0
                        emit(ApkUpdateState.Downloading(progress))
                    }
                    DownloadManager.STATUS_SUCCESSFUL -> {
                        val uriStr = cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI))
                        val file = if (uriStr != null) {
                            val file = File(Uri.parse(uriStr).path ?: "")
                            if (file.isFile) file else getDownloadedApk(context)
                        } else {
                            getDownloadedApk(context)
                        }
                        if (file != null && file.isFile) {
                            emit(ApkUpdateState.Completed(file))
                        } else {
                            emit(ApkUpdateState.Error("APK file not found after download"))
                        }
                        cursor.close()
                        break
                    }
                    DownloadManager.STATUS_FAILED -> {
                        emit(ApkUpdateState.Error("Download failed: reason=$reason"))
                        cursor.close()
                        break
                    }
                    DownloadManager.STATUS_PAUSED -> {
                        emit(ApkUpdateState.Paused(0))
                    }
                    DownloadManager.STATUS_PENDING -> {
                        emit(ApkUpdateState.Downloading(0))
                    }
                }
            } else {
                emit(ApkUpdateState.Error("Download cancelled or not found"))
                cursor.close()
                break
            }
            cursor.close()
            delay(500)
        }
    }

    fun installApk(context: Context, apkFile: File) {
        if (!apkFile.isFile) return

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(intent)
    }

    fun getDownloadedApk(context: Context): File? {
        val file = File(context.getExternalFilesDir(null), APK_NAME)
        return if (file.isFile) file else null
    }

    fun cleanupOldApk(context: Context) {
        val file = File(context.getExternalFilesDir(null), APK_NAME)
        if (file.exists()) file.delete()
    }
}

sealed class ApkUpdateState {
    data object Idle : ApkUpdateState()
    data class Downloading(val progress: Int) : ApkUpdateState()
    data class Paused(val progress: Int) : ApkUpdateState()
    data class Completed(val file: File) : ApkUpdateState()
    data class Error(val message: String) : ApkUpdateState()
    data object Cancelled : ApkUpdateState()
}
