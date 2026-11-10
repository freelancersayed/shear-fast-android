package com.flashshare.p2p.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flashshare.p2p.data.model.TransferProgress
import com.flashshare.p2p.ui.theme.AccentAzureLight
import com.flashshare.p2p.ui.theme.AccentMint
import com.flashshare.p2p.ui.theme.CardBackground
import com.flashshare.p2p.ui.theme.CardBorder
import com.flashshare.p2p.ui.theme.TextBody
import com.flashshare.p2p.ui.theme.TextTitle

/**
 * Minimal, polished transfer progress indicator.
 * Displays accurate file name, transferred bytes, and animated progress.
 */
@Composable
fun LinearTransferProgress(
    progress: TransferProgress,
    modifier: Modifier = Modifier
) {
    val animatedFraction by animateFloatAsState(
        targetValue = progress.progressFraction,
        label = "progressFraction"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(CardBackground)
            .border(1.dp, CardBorder, RoundedCornerShape(18.dp))
            .padding(18.dp)
    ) {
        // File detail row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(CardBorder.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                val icon = when {
                    progress.fileName.endsWith(".mp4", ignoreCase = true) ||
                    progress.fileName.endsWith(".mkv", ignoreCase = true) ||
                    progress.fileName.endsWith(".mov", ignoreCase = true) -> Icons.Default.Movie

                    progress.fileName.endsWith(".jpg", ignoreCase = true) ||
                    progress.fileName.endsWith(".png", ignoreCase = true) ||
                    progress.fileName.endsWith(".heic", ignoreCase = true) ||
                    progress.fileName.endsWith(".dng", ignoreCase = true) -> Icons.Default.Image

                    else -> Icons.Default.InsertDriveFile
                }
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = AccentAzureLight,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = progress.fileName.ifEmpty { "File" },
                    color = TextTitle,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${progress.formattedTransferred} of ${progress.formattedTotal}",
                    color = TextBody,
                    fontSize = 12.sp
                )
            }

            // Percentage badge
            Text(
                text = "${progress.percentage}%",
                color = AccentMint,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Clean Material Progress Track
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape)
                .background(CardBorder)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedFraction)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(AccentMint)
            )
        }
    }
}
