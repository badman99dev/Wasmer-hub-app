package com.movie.app.best.ui.screens.moviedetail.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.movie.app.best.ui.theme.WasmerRed

@Composable
fun DetailActionButtons(
    hasStream: Boolean,
    streamRequested: Boolean,
    isInMyList: Boolean,
    isLiked: Boolean,
    onPlayClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onMyListClick: () -> Unit,
    onLikeClick: () -> Unit,
    onRequestStream: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (hasStream) {
            Button(
                onClick  = onPlayClick,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape    = RoundedCornerShape(8.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor = WasmerRed,
                    contentColor   = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(6.dp))
                Text("Play Now", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        } else {
            OutlinedButton(
                onClick  = { if (!streamRequested) onRequestStream() },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape    = RoundedCornerShape(8.dp),
                enabled  = !streamRequested,
                colors   = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) {
                Icon(Icons.Default.SmartDisplay, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (streamRequested) "Requested ✓" else "Request Stream",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            }
        }

        ActionIconButton(
            icon    = Icons.Default.Download,
            label   = "Download",
            onClick = onDownloadClick
        )

        ActionIconButton(
            icon    = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            label   = if (isLiked) "Liked" else "Like",
            tint    = if (isLiked) Color(0xFFFF1744) else Color.White,
            onClick = onLikeClick
        )

        ActionIconButton(
            icon    = if (isInMyList) Icons.Default.BookmarkAdded else Icons.Default.BookmarkAdd,
            label   = if (isInMyList) "Saved" else "My List",
            tint    = if (isInMyList) WasmerRed else Color.White,
            onClick = onMyListClick
        )
    }
}

@Composable
private fun ActionIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color = Color.White,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(60.dp)
    ) {
        IconButton(
            onClick  = onClick,
            modifier = Modifier.size(44.dp)
        ) {
            Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(26.dp))
        }
        Text(
            text     = label,
            color    = tint.copy(alpha = 0.75f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
