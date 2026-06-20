package com.movie.app.best.ui.screens.downloads

import android.Manifest
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoFile
import com.ketch.DownloadModel
import com.ketch.Status
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import java.io.File

data class LocalVideoFile(
    val name: String,
    val path: String,
    val size: Long,
    val extension: String,
    val contentUri: String = ""
)

internal fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024L * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
        else -> String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
    }
}

fun scanWasmerHubVideos(context: Context): List<LocalVideoFile> {
    val results = mutableListOf<LocalVideoFile>()
    val videoExts = setOf("mp4", "mkv", "avi", "webm", "mov", "flv", "3gp", "ts", "m4v")

    val collection = if (Build.VERSION.SDK_INT >= 29) {
        MediaStore.Downloads.EXTERNAL_CONTENT_URI
    } else {
        MediaStore.Files.getContentUri("external")
    }

    val projection = arrayOf(
        MediaStore.Downloads._ID,
        MediaStore.Downloads.DISPLAY_NAME,
        MediaStore.Downloads.SIZE,
        MediaStore.Downloads.DATA
    )

    val selection = "${MediaStore.Downloads.DISPLAY_NAME} LIKE ?"
    val selectionArgs = arrayOf("%.mkv")
    val sortOrder = "${MediaStore.Downloads.DATE_MODIFIED} DESC"

    try {
        val cursor = context.contentResolver.query(
            collection, projection, null, null, sortOrder
        ) ?: return emptyList()

        cursor.use {
            val idCol = it.getColumnIndexOrThrow(MediaStore.Downloads._ID)
            val nameCol = it.getColumnIndexOrThrow(MediaStore.Downloads.DISPLAY_NAME)
            val sizeCol = it.getColumnIndexOrThrow(MediaStore.Downloads.SIZE)
            val dataCol = it.getColumnIndexOrThrow(MediaStore.Downloads.DATA)

            while (it.moveToNext()) {
                val filePath = it.getString(dataCol) ?: continue
                val name = it.getString(nameCol) ?: continue

                if (!filePath.contains("WasmerHub")) continue

                val ext = name.substringAfterLast(".", "").lowercase()
                if (ext !in videoExts) continue

                val size = it.getLong(sizeCol)
                val contentUri = ContentUris.withAppendedId(collection, it.getLong(idCol))

                results.add(LocalVideoFile(name, filePath, size, ext, contentUri.toString()))
            }
        }
    } catch (_: Exception) {}

    return results
}

@Composable
fun DownloadsScreen(
    viewModel: DownloadsViewModel = hiltViewModel(),
    onLocalVideosClick: () -> Unit = {},
    onPlayFile: (String, String) -> Unit = { _, _ -> },
    onSettingsClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var scannedVideos by remember { mutableStateOf<List<LocalVideoFile>>(emptyList()) }
    var showDeleteDialog by remember { mutableStateOf<LocalVideoFile?>(null) }
    var hasStoragePermission by remember { mutableStateOf(true) }
    var hasNotificationPermission by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(true) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        scannedVideos = scanWasmerHubVideos(context)
    }

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotificationPermission = granted
    }

    LaunchedEffect(Unit) {
        isRefreshing = true
        scannedVideos = scanWasmerHubVideos(context)
        isRefreshing = false

        if (Build.VERSION.SDK_INT >= 33) {
            if (context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                hasNotificationPermission = false
                notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    if (!hasStoragePermission) {
        Column(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(imageVector = Icons.Default.Download, contentDescription = null,
                modifier = Modifier.size(64.dp), tint = Color.White.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(16.dp))
            Text("Storage Permission Required", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Needed to download videos & scan downloads", color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp)
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = {
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_EXTERNAL_STORAGE
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914))
            ) {
                Text("Grant Permission", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
        return
    }

    LaunchedEffect(uiState.completedDownloads) {
        scannedVideos = scanWasmerHubVideos(context)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Downloads",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
            if (isRefreshing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 2.dp
                )
            } else {
                IconButton(onClick = {
                    isRefreshing = true
                    scannedVideos = scanWasmerHubVideos(context)
                    isRefreshing = false
                }) {
                    Icon(imageVector = Icons.Default.FolderOpen, contentDescription = "Refresh",
                        tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
                }
            }
            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        if (uiState.isResolving) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Resolving download link...",
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        if (uiState.resolveError != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2A0000))
            ) {
                Text(
                    text = uiState.resolveError ?: "Error",
                    color = Color(0xFFFF5252),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        if (uiState.activeDownloads.isEmpty() && scannedVideos.isEmpty() && !uiState.isResolving) {
            EmptyDownloadsState()
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                if (uiState.activeDownloads.isNotEmpty()) {
                    item {
                        Text(
                            text = "DOWNLOADING",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
                        )
                    }
                    items(uiState.activeDownloads, key = { it.id }) { download ->
                        ActiveDownloadItem(
                            download = download,
                            onPause = { viewModel.pauseDownload(download.id) },
                            onResume = { viewModel.resumeDownload(download.id) },
                            onCancel = { viewModel.cancelDownload(download.id) },
                            onRetry = { viewModel.retryDownload(download.id) }
                        )
                    }
                }

                if (uiState.completedDownloads.isNotEmpty()) {
                    item {
                        Text(
                            text = "COMPLETED",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4CAF50),
                            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                        )
                    }
                    items(uiState.completedDownloads, key = { it.id }) { download ->
                        CompletedDownloadItem(
                            download = download,
                            onPlay = { onPlayFile("file://${download.path}/${download.fileName}", download.fileName) },
                            onDelete = { viewModel.deleteDownload(download.id) }
                        )
                    }
                }

                if (scannedVideos.isNotEmpty()) {
                    item {
                        Text(
                            text = "LOCAL FILES",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4CAF50),
                            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                        )
                    }
                    items(scannedVideos, key = { it.path }) { video ->
                        ScannedVideoItem(
                            video = video,
                            onPlay = { onPlayFile("file://${video.path}", video.name) },
                            onDelete = { showDeleteDialog = video }
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }

    showDeleteDialog?.let { video ->
        DeleteConfirmationDialog(
            fileName = video.name,
            onConfirm = {
                try {
                    if (video.contentUri.isNotEmpty()) {
                        context.contentResolver.delete(Uri.parse(video.contentUri), null, null)
                    } else {
                        File(video.path).delete()
                    }
                } catch (_: Exception) {
                    File(video.path).delete()
                }
                scannedVideos = scanWasmerHubVideos(context)
                showDeleteDialog = null
            },
            onDismiss = { showDeleteDialog = null }
        )
    }
}

@Composable
private fun EmptyDownloadsState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Download,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = Color.White.copy(alpha = 0.3f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No downloads yet",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Downloads will appear here",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.3f)
            )
        }
    }
}

@Composable
private fun ActiveDownloadItem(
    download: DownloadModel,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (download.status == Status.FAILED) Icons.Default.Refresh else Icons.Default.Download,
                    contentDescription = null,
                    tint = if (download.status == Status.FAILED) Color(0xFFFF5252) else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = download.fileName.ifEmpty { download.url.substringAfterLast("/") },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            val progressFloat = if (download.progress > 0) download.progress / 100f else 0f
            val progressColor = when (download.status) {
                Status.PAUSED -> Color(0xFFFFA000)
                Status.FAILED -> Color(0xFFFF5252)
                else -> MaterialTheme.colorScheme.primary
            }

            LinearProgressIndicator(
                progress = { progressFloat },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = progressColor,
                trackColor = Color(0xFF333333)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val statusText = when (download.status) {
                    Status.QUEUED -> "Queued..."
                    Status.STARTED -> "Starting..."
                    Status.PROGRESS -> {
                        val speedBytesPerSec = (download.speedInBytePerMs * 1000).toLong()
                        if (speedBytesPerSec > 0) "${formatFileSize(speedBytesPerSec)}/s" else "Connecting..."
                    }
                    Status.PAUSED -> "Paused"
                    Status.FAILED -> "Failed: ${download.failureReason}"
                    else -> ""
                }
                val downloadedBytes = (download.progress.toLong() * download.total) / 100
                val sizeText = "${formatFileSize(downloadedBytes)} / ${formatFileSize(download.total)}"

                Column {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                    Text(
                        text = sizeText,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 11.sp
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "${download.progress}%",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = progressColor
                    )
                    Spacer(modifier = Modifier.width(4.dp))

                    when (download.status) {
                        Status.PROGRESS, Status.STARTED -> {
                            IconButton(onClick = onPause, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Pause, "Pause", tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                            }
                        }
                        Status.PAUSED -> {
                            IconButton(onClick = onResume, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.PlayArrow, "Resume", tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                            }
                        }
                        Status.FAILED -> {
                            IconButton(onClick = onRetry, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Refresh, "Retry", tint = Color(0xFFFFA000), modifier = Modifier.size(18.dp))
                            }
                        }
                        else -> {}
                    }

                    IconButton(onClick = onCancel, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, "Cancel", tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun CompletedDownloadItem(
    download: DownloadModel,
    onPlay: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onPlay),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = Color(0xFFE50914),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = download.fileName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = formatFileSize(download.total),
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 11.sp
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun ScannedVideoItem(
    video: LocalVideoFile,
    onPlay: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onPlay),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = Color(0xFFE50914),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = video.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = video.extension.uppercase(),
                        color = Color(0xFFE50914).copy(alpha = 0.7f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = formatFileSize(video.size),
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 11.sp
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun DeleteConfirmationDialog(
    fileName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Video") },
        text = { Text("Are you sure you want to delete \"$fileName\"?") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Delete", color = Color(0xFFFF5252))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
