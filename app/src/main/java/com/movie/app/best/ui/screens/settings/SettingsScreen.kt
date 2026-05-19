package com.movie.app.best.ui.screens.settings

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CopyAll
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.movie.app.best.data.debug.NetworkLogger
import com.movie.app.best.data.repository.LibraryRepository
import com.movie.app.best.data.settings.ModerationSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URLEncoder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit = {}
) {
    var debugEnabled by remember { mutableStateOf(NetworkLogger.isEnabled()) }
    var showLogs by remember { mutableStateOf(false) }
    var logs by remember { mutableStateOf("") }
    var isUploading by remember { mutableStateOf(false) }
    var uploadedLink by remember { mutableStateOf("") }
    var uploadError by remember { mutableStateOf("") }
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    var moderationEnabled by remember { mutableStateOf(ModerationSettings.isEnabled(context)) }
    var moderationMode by remember { mutableStateOf(ModerationSettings.getMode(context)) }

    LaunchedEffect(debugEnabled) {
        NetworkLogger.setEnabled(debugEnabled)
    }

    if (showLogs) {
        LogViewerScreen(
            logs = logs,
            onBackClick = {
                showLogs = false
            },
            onRefresh = {
                logs = NetworkLogger.getLogs()
            },
            onCopy = {
                clipboardManager.setText(AnnotatedString(logs))
            },
            onClear = {
                NetworkLogger.clear()
                logs = ""
            },
            onUpload = {
                if (logs.isEmpty()) return@LogViewerScreen
                isUploading = true
                uploadedLink = ""
                uploadError = ""
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val file = File(context.cacheDir, "wasmer_debug_${System.currentTimeMillis()}.log")
                        file.writeText(logs)
                        val fileName = URLEncoder.encode(file.name, "UTF-8")
                        val connection = java.net.URL("https://tempserv.badman993944.workers.dev/api/upload")
                            .openConnection() as java.net.HttpURLConnection
                        connection.requestMethod = "POST"
                        connection.doOutput = true
                        val boundary = "----FetchBoundary${System.currentTimeMillis()}"
                        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
                        val output = connection.outputStream
                        output.write("--$boundary\r\n".toByteArray())
                        output.write("Content-Disposition: form-data; name=\"mode\"\r\n\r\nfile\r\n".toByteArray())
                        output.write("--$boundary\r\n".toByteArray())
                        output.write("Content-Disposition: form-data; name=\"expiry\"\r\n\r\n6hr\r\n".toByteArray())
                        output.write("--$boundary\r\n".toByteArray())
                        output.write("Content-Disposition: form-data; name=\"paths\"\r\n\r\n[\"$fileName\"]\r\n".toByteArray())
                        output.write("--$boundary\r\n".toByteArray())
                        output.write("Content-Disposition: form-data; name=\"files\"; filename=\"$fileName\"\r\n".toByteArray())
                        output.write("Content-Type: text/plain\r\n\r\n".toByteArray())
                        output.write(file.readBytes())
                        output.write("\r\n--$boundary--\r\n".toByteArray())
                        output.flush()
                        val responseCode = connection.responseCode
                        val responseBody = if (responseCode == 201) {
                            connection.inputStream.bufferedReader().readText()
                        } else {
                            connection.errorStream?.bufferedReader()?.readText() ?: "HTTP $responseCode"
                        }
                        withContext(Dispatchers.Main) {
                            if (responseCode == 201) {
                                val slug = try {
                                    val json = org.json.JSONObject(responseBody)
                                    json.optString("slug", "")
                                } catch (_: Exception) { "" }
                                if (slug.isNotEmpty()) {
                                    uploadedLink = "https://tempserv.badman993944.workers.dev/file/$slug/dl"
                                } else {
                                    uploadError = "Upload succeeded but no link returned"
                                }
                            } else {
                                uploadError = "Upload failed: $responseBody"
                            }
                            isUploading = false
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            uploadError = "Upload error: ${e.message}"
                            isUploading = false
                        }
                    }
                }
            },
            isUploading = isUploading,
            uploadedLink = uploadedLink,
            uploadError = uploadError,
            onCopyLink = {
                clipboardManager.setText(AnnotatedString(uploadedLink))
            }
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        TopAppBar(
            title = {
                Text(
                    text = "Settings",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        tint = Color.White
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            SettingsSectionTitle("General")

            val libraryRepo = remember { LibraryRepository(context) }
            var selectedPlayer by remember { mutableIntStateOf(libraryRepo.getDefaultPlayer()) }

            SettingsItem(
                icon = Icons.Default.PlayCircle,
                title = "Player",
                subtitle = when(selectedPlayer) {
                    1 -> "Player 1 (Native)"
                    2 -> "Player 2 (Web)"
                    else -> "Ask every time"
                },
                onClick = {
                    val next = (selectedPlayer + 1) % 3
                    selectedPlayer = next
                    libraryRepo.setDefaultPlayer(next)
                }
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PlayerOptionChip("Ask", selectedPlayer == 0) { selectedPlayer = 0; libraryRepo.setDefaultPlayer(0) }
                PlayerOptionChip("P1 Native", selectedPlayer == 1) { selectedPlayer = 1; libraryRepo.setDefaultPlayer(1) }
                PlayerOptionChip("P2 Web", selectedPlayer == 2) { selectedPlayer = 2; libraryRepo.setDefaultPlayer(2) }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayCircle,
                        contentDescription = null,
                        tint = Color(0xFFE50914),
                        modifier = Modifier.size(22.dp)
                    )
                    Column(modifier = Modifier.weight(1f).padding(start = 14.dp)) {
                        Text(
                            text = "Content Moderation Filter",
                            color = Color.White,
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp
                        )
                        Text(
                            text = if (!moderationEnabled) "Off — show all content" else if (moderationMode == ModerationSettings.MODE_BLUR) "Blur sexual/inappropriate content" else "Hide sexual/inappropriate content",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 12.sp
                        )
                    }
                    Switch(
                        checked = moderationEnabled,
                        onCheckedChange = {
                            moderationEnabled = it
                            ModerationSettings.setEnabled(context, it)
                        },
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = Color(0xFFE50914),
                            checkedThumbColor = Color.White
                        )
                    )
                }
            }

            if (moderationEnabled) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PlayerOptionChip("Blur", moderationMode == ModerationSettings.MODE_BLUR) {
                        moderationMode = ModerationSettings.MODE_BLUR
                        ModerationSettings.setMode(context, ModerationSettings.MODE_BLUR)
                    }
                    PlayerOptionChip("Hide", moderationMode == ModerationSettings.MODE_HIDE) {
                        moderationMode = ModerationSettings.MODE_HIDE
                        ModerationSettings.setMode(context, ModerationSettings.MODE_HIDE)
                    }
                }
            }

            SettingsSectionTitle("Developer")

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.BugReport,
                        contentDescription = null,
                        tint = Color(0xFFFF9800),
                        modifier = Modifier.size(22.dp)
                    )
                    Column(modifier = Modifier.weight(1f).padding(start = 14.dp)) {
                        Text(
                            text = "Debug Mode",
                            color = Color.White,
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp
                        )
                        Text(
                            text = if (debugEnabled) "Capturing ${NetworkLogger.getLogCount()} log entries" else "Record network requests & errors",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 12.sp
                        )
                    }
                    Switch(
                        checked = debugEnabled,
                        onCheckedChange = { debugEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = Color(0xFFE50914),
                            checkedThumbColor = Color.White
                        )
                    )
                }
            }

            if (debugEnabled) {
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            logs = NetworkLogger.getLogs()
                            showLogs = true
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A1A1A))
                    ) {
                        Icon(imageVector = Icons.Default.BugReport, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White.copy(alpha = 0.7f))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("View Logs", color = Color.White, fontSize = 13.sp)
                    }

                    Button(
                        onClick = {
                            NetworkLogger.clear()
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A1A1A))
                    ) {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White.copy(alpha = 0.7f))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Clear", color = Color.White, fontSize = 13.sp)
                    }
                }
            }

            SettingsSectionTitle("About")

            SettingsItem(
                icon = Icons.Default.Info,
                title = "About Us",
                subtitle = "Learn more about Wasmer Hub",
                onClick = {}
            )

            SettingsItem(
                icon = Icons.AutoMirrored.Filled.HelpOutline,
                title = "Help & Feedback",
                subtitle = "Coming soon",
                onClick = {},
                enabled = false
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Wasmer Hub v1.0",
                color = Color.White.copy(alpha = 0.2f),
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LogViewerScreen(
    logs: String,
    onBackClick: () -> Unit,
    onRefresh: () -> Unit,
    onCopy: () -> Unit,
    onClear: () -> Unit,
    onUpload: () -> Unit,
    isUploading: Boolean,
    uploadedLink: String,
    uploadError: String,
    onCopyLink: () -> Unit
) {
    var copied by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A))
    ) {
        TopAppBar(
            title = {
                Text(
                    text = "Debug Logs (${NetworkLogger.getLogCount()} entries)",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 14.sp
                )
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.White)
                }
            },
            actions = {
                IconButton(onClick = onRefresh) {
                    Icon(imageVector = Icons.Default.Clear, contentDescription = "Refresh", tint = Color.White.copy(alpha = 0.7f))
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF111111))
        )

        if (uploadedLink.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1B5E20))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Link, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(uploadedLink, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                    Text("COPY", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.clickable { onCopyLink() })
                }
            }
        }

        if (uploadError.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF3E0000))
            ) {
                Text(uploadError, color = Color(0xFFFF5252), fontSize = 12.sp, modifier = Modifier.padding(12.dp))
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = logs.ifEmpty { "No logs captured yet. Enable debug mode and use the app." },
                color = Color(0xFF00FF41),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                lineHeight = 14.sp
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF111111))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    onCopy()
                    copied = true
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A1A1A))
            ) {
                Icon(imageVector = Icons.Default.CopyAll, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White.copy(alpha = 0.7f))
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (copied) "Copied!" else "Copy", color = Color.White, fontSize = 12.sp)
            }

            Button(
                onClick = onUpload,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914))
            ) {
                if (isUploading) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Icon(imageVector = Icons.Default.Link, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text("Get Link", color = Color.White, fontSize = 12.sp)
            }

            Button(
                onClick = onClear,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A1A1A))
            ) {
                Text("Clear", color = Color(0xFFFF5252), fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        color = MaterialTheme.colorScheme.primary,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp, start = 4.dp)
    )
}

@Composable
private fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    val tint = if (enabled) Color.White else Color.White.copy(alpha = 0.3f)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clickable(enabled = enabled) { onClick() },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = if (enabled) 0.04f else 0.02f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(22.dp)
            )
            Column(modifier = Modifier.weight(1f).padding(start = 14.dp)) {
                Text(
                    text = title,
                    color = tint,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp
                )
                Text(
                    text = subtitle,
                    color = tint.copy(alpha = 0.5f),
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun PlayerOptionChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) Color(0xFFE50914).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.04f)
        )
    ) {
        Text(
            text = label,
            color = if (selected) Color(0xFFE50914) else Color.White.copy(alpha = 0.5f),
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}
