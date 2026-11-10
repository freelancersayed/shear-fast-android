package com.flashshare.p2p.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flashshare.p2p.data.repository.ReceivedFileInfo
import com.flashshare.p2p.ui.theme.AccentAzureLight
import com.flashshare.p2p.ui.theme.AccentMint
import com.flashshare.p2p.ui.theme.AppBackground
import com.flashshare.p2p.ui.theme.CardBorder
import com.flashshare.p2p.ui.theme.SurfaceElevated
import com.flashshare.p2p.ui.theme.TextBody
import com.flashshare.p2p.ui.theme.TextMuted
import com.flashshare.p2p.ui.theme.TextTitle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * FilesScreen - Displays all files received by BondhuShare in 'Download/BondhuShare'.
 * Beautiful, cohesive Midnight Navy + Mint Emerald theme.
 * Allows users to view details (name, size, date) and tap to directly play/open them.
 */
@Composable
fun FilesScreen(
    files: List<ReceivedFileInfo>,
    onFileClick: (ReceivedFileInfo) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppBackground)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(18.dp))

        // Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Received Files",
                    color = TextTitle,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Saved in: Download/BondhuShare",
                    color = TextBody,
                    fontSize = 13.sp
                )
            }

            Surface(
                shape = CircleShape,
                color = SurfaceElevated,
                border = BorderStroke(1.dp, CardBorder)
            ) {
                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier.size(42.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh Files",
                        tint = AccentMint,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (files.isEmpty()) {
            // Empty State
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = SurfaceElevated,
                    border = BorderStroke(1.dp, CardBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .clip(CircleShape)
                                .background(AppBackground),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                tint = AccentMint,
                                modifier = Modifier.size(40.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "No Received Files Yet",
                            color = TextTitle,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Files sent to you will appear here and are stored in Download/BondhuShare at 100% original quality.",
                            color = TextBody,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 19.sp
                        )
                    }
                }
            }
        } else {
            // File List
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(files, key = { it.uri.toString() + it.lastModified }) { file ->
                    ReceivedFileItem(
                        file = file,
                        onClick = { onFileClick(file) }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun ReceivedFileItem(
    file: ReceivedFileInfo,
    onClick: () -> Unit
) {
    val (icon, iconBg) = getFileTypeIconAndColor(file.mimeType, file.name)
    val formattedSize = formatFileSize(file.size)
    val formattedDate = formatFileDate(file.lastModified)

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = SurfaceElevated,
        border = BorderStroke(1.dp, CardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // File Type Icon
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconBg.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconBg,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // File Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    color = TextTitle,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formattedSize,
                        color = AccentMint,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = " • $formattedDate",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Play / Open Icon
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(AppBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayCircle,
                    contentDescription = "Open File",
                    tint = AccentAzureLight,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

private fun getFileTypeIconAndColor(mimeType: String, fileName: String): Pair<ImageVector, Color> {
    val lower = fileName.lowercase(Locale.ROOT)
    return when {
        mimeType.startsWith("image/") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") || lower.endsWith(".webp") -> {
            Pair(Icons.Default.Image, Color(0xFF14B8A6)) // Teal
        }
        mimeType.startsWith("video/") || lower.endsWith(".mp4") || lower.endsWith(".mkv") || lower.endsWith(".mov") || lower.endsWith(".avi") -> {
            Pair(Icons.Default.VideoFile, Color(0xFFF43F5E)) // Rose
        }
        mimeType.startsWith("audio/") || lower.endsWith(".mp3") || lower.endsWith(".m4a") || lower.endsWith(".wav") || lower.endsWith(".aac") -> {
            Pair(Icons.Default.AudioFile, Color(0xFFA855F7)) // Purple
        }
        lower.endsWith(".pdf") || mimeType == "application/pdf" || lower.endsWith(".doc") || lower.endsWith(".docx") || lower.endsWith(".txt") -> {
            Pair(Icons.Default.Description, Color(0xFF38BDF8)) // Sky
        }
        else -> {
            Pair(Icons.Default.InsertDriveFile, Color(0xFF94A3B8)) // Slate
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0

    return when {
        gb >= 1.0 -> String.format(Locale.US, "%.2f GB", gb)
        mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
        kb >= 1.0 -> String.format(Locale.US, "%.0f KB", kb)
        else -> "$bytes B"
    }
}

private fun formatFileDate(timestamp: Long): String {
    if (timestamp <= 0L) return ""
    return SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(timestamp))
}
