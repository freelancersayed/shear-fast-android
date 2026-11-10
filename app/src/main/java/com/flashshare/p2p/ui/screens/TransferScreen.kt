package com.flashshare.p2p.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flashshare.p2p.R
import com.flashshare.p2p.data.model.TransferProgress
import com.flashshare.p2p.data.model.TransferState
import com.flashshare.p2p.ui.components.HighTechSpeedGauge
import com.flashshare.p2p.ui.components.LinearTransferProgress
import com.flashshare.p2p.ui.theme.AccentMint
import com.flashshare.p2p.ui.theme.AccentMintText
import com.flashshare.p2p.ui.theme.AppBackground
import com.flashshare.p2p.ui.theme.CardBackground
import com.flashshare.p2p.ui.theme.CardBorder
import com.flashshare.p2p.ui.theme.ErrorRose
import com.flashshare.p2p.ui.theme.SuccessEmerald
import com.flashshare.p2p.ui.theme.SurfaceElevated
import com.flashshare.p2p.ui.theme.TextBody
import com.flashshare.p2p.ui.theme.TextMuted
import com.flashshare.p2p.ui.theme.TextTitle
import java.util.Locale

/**
 * TransferScreen - Clean, production-grade real-time transfer telemetry.
 */
@Composable
fun TransferScreen(
    state: TransferState,
    onCancel: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppBackground)
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        when (state) {
            is TransferState.Transferring -> {
                TransferringContent(
                    progress = state.progress,
                    isSender = state.isSender,
                    peerName = state.peerName,
                    onCancel = onCancel
                )
            }
            is TransferState.Completed -> {
                CompletedContent(
                    completed = state,
                    onDone = onDone
                )
            }
            is TransferState.Error -> {
                ErrorContent(
                    error = state,
                    onDismiss = onDone
                )
            }
            is TransferState.Connecting -> {
                ConnectingContent(
                    peerName = state.peer.name,
                    onCancel = onCancel
                )
            }
            else -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = "Preparing transfer…", color = TextBody)
                }
            }
        }
    }
}

@Composable
private fun TransferringContent(
    progress: TransferProgress,
    isSender: Boolean,
    peerName: String,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Clean Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (isSender) stringResource(R.string.transferring_file) else stringResource(R.string.receiving_file),
                    color = TextTitle,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Connected with $peerName",
                    color = TextBody,
                    fontSize = 13.sp
                )
            }

            OutlinedButton(
                onClick = onCancel,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = stringResource(R.string.cancel_transfer),
                    color = TextBody,
                    fontSize = 13.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Speed & ETA Gauge
        HighTechSpeedGauge(
            speedMBps = progress.speedMBps,
            etaFormatted = progress.formattedEta
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Linear Progress Bar & File Details
        LinearTransferProgress(progress = progress)
    }
}

@Composable
private fun CompletedContent(
    completed: TransferState.Completed,
    onDone: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(SuccessEmerald.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = SuccessEmerald,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = stringResource(R.string.transfer_complete),
            color = TextTitle,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Transferred in original quality with zero loss",
            color = TextBody,
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Summary Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CardBackground)
                .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            DetailRow(label = "File", value = completed.fileName)
            DetailRow(label = "Size", value = TransferProgress.formatBytes(completed.fileSize))
            DetailRow(label = "Average speed", value = String.format(Locale.US, "%.1f MB/s", completed.averageSpeedMBps))
            DetailRow(label = "Time taken", value = String.format(Locale.US, "%.1f s", completed.durationSeconds))

            if (!completed.isSender) {
                DetailRow(label = "Saved to", value = "Download/BondhuShare")
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = onDone,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AccentMint,
                contentColor = AccentMintText
            )
        ) {
            Text(
                text = stringResource(R.string.done),
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }
    }
}

@Composable
private fun ErrorContent(
    error: TransferState.Error,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(ErrorRose.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = ErrorRose,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = stringResource(R.string.transfer_failed),
            color = TextTitle,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = error.message,
            color = TextBody,
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onDismiss,
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = CardBorder,
                contentColor = TextTitle
            )
        ) {
            Text(text = stringResource(R.string.try_again))
        }
    }
}

@Composable
private fun ConnectingContent(
    peerName: String,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Connecting to $peerName…",
            color = TextTitle,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Establishing high-speed local link",
            color = TextBody,
            fontSize = 13.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedButton(
            onClick = onCancel,
            shape = RoundedCornerShape(10.dp)
        ) {
            Text(text = stringResource(R.string.cancel_transfer), color = TextBody)
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = TextMuted, fontSize = 13.sp)
        Text(text = value, color = TextTitle, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}
