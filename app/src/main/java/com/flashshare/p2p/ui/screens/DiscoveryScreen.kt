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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flashshare.p2p.R
import com.flashshare.p2p.data.model.DiscoveredPeer
import com.flashshare.p2p.data.model.PeerRole
import com.flashshare.p2p.ui.components.PeerRadarComponent
import com.flashshare.p2p.ui.theme.BackgroundDark
import com.flashshare.p2p.ui.theme.BrandBlue
import com.flashshare.p2p.ui.theme.ButtonAquaGreen
import com.flashshare.p2p.ui.theme.CardBackground
import com.flashshare.p2p.ui.theme.CardBorder
import com.flashshare.p2p.ui.theme.SuccessEmerald
import com.flashshare.p2p.ui.theme.TextBody
import com.flashshare.p2p.ui.theme.TextMuted
import com.flashshare.p2p.ui.theme.TextTitle

/**
 * DiscoveryScreen - Clean, human-centered device search interface.
 */
@Composable
fun DiscoveryScreen(
    role: PeerRole,
    statusMessage: String,
    peers: List<DiscoveredPeer>,
    hardwareWarnings: List<String> = emptyList(),
    onPeerSelected: (DiscoveredPeer) -> Unit,
    onSendToIp: ((String) -> Unit)? = null,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showIpDialog by remember { mutableStateOf(false) }
    var targetIpInput by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(horizontal = 20.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (role == PeerRole.SENDER) stringResource(R.string.send_title) else stringResource(R.string.ready_to_receive),
                color = TextTitle,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            IconButton(
                onClick = onCancel,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(CardBackground)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = TextBody,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        if (hardwareWarnings.isNotEmpty()) {
            Spacer(modifier = Modifier.height(14.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF3E2723))
                    .padding(12.dp)
            ) {
                hardwareWarnings.forEach { warning ->
                    Text(
                        text = warning,
                        color = Color(0xFFFFCC80),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Center Radar
        PeerRadarComponent(radarColor = BrandBlue)

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = statusMessage,
            color = TextTitle,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )

        Text(
            text = if (role == PeerRole.SENDER) {
                stringResource(R.string.no_devices_found)
            } else {
                stringResource(R.string.waiting_for_sender)
            },
            color = TextMuted,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 4.dp, start = 20.dp, end = 20.dp),
            lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Discovered Devices Section
        if (role == PeerRole.SENDER) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.nearby_devices),
                    color = TextBody,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (peers.isNotEmpty()) {
                    Text(
                        text = "${peers.size} found",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (peers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(CardBackground)
                        .border(1.dp, CardBorder, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.searching_devices),
                        color = TextMuted,
                        fontSize = 13.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(peers, key = { it.id }) { peer ->
                        PeerListItem(peer = peer, onConnect = { onPeerSelected(peer) })
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Direct IP Option Button
            if (onSendToIp != null) {
                Button(
                    onClick = { showIpDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ButtonAquaGreen,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Send to PC / IP Address Directly",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }

        // Direct IP Dialog
        if (showIpDialog) {
            AlertDialog(
                onDismissRequest = { showIpDialog = false },
                title = {
                    Text(
                        text = "Send Directly to IP Address",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column {
                        Text(
                            text = "Enter the destination PC or phone IP address:",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = targetIpInput,
                            onValueChange = { targetIpInput = it },
                            placeholder = { Text("e.g. 192.168.2.106", color = Color.White.copy(alpha = 0.4f)) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ButtonAquaGreen,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (targetIpInput.isNotBlank()) {
                                showIpDialog = false
                                onSendToIp?.invoke(targetIpInput.trim())
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ButtonAquaGreen)
                    ) {
                        Text("Send Now")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showIpDialog = false }) {
                        Text("Cancel", color = Color.White.copy(alpha = 0.7f))
                    }
                },
                containerColor = Color(0xFF0D3B75)
            )
        }
    }
}
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(androidx.compose.ui.graphics.Color(0xFF2D2010))
                    .border(1.dp, androidx.compose.ui.graphics.Color(0xFFF59E0B), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⚠️ Notice: ${hardwareWarnings.joinToString(", ")}. Turn ON in settings for discovery.",
                    color = androidx.compose.ui.graphics.Color(0xFFFDE68A),
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Center Radar
        PeerRadarComponent(radarColor = BrandBlue)

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = statusMessage,
            color = TextTitle,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )

        Text(
            text = if (role == PeerRole.SENDER) {
                stringResource(R.string.no_devices_found)
            } else {
                stringResource(R.string.waiting_for_sender)
            },
            color = TextMuted,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 4.dp, start = 20.dp, end = 20.dp),
            lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Discovered Devices Section
        if (role == PeerRole.SENDER) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.nearby_devices),
                    color = TextBody,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (peers.isNotEmpty()) {
                    Text(
                        text = "${peers.size} found",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (peers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(CardBackground)
                        .border(1.dp, CardBorder, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.searching_devices),
                        color = TextMuted,
                        fontSize = 13.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(peers, key = { it.id }) { peer ->
                        PeerListItem(peer = peer, onConnect = { onPeerSelected(peer) })
                    }
                }
            }
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun PeerListItem(
    peer: DiscoveredPeer,
    onConnect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CardBackground)
            .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(BrandBlue.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PhoneAndroid,
                    contentDescription = null,
                    tint = BrandBlue,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = peer.name.ifBlank { "Nearby device" },
                    color = TextTitle,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(SuccessEmerald)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Ready to connect",
                        color = TextBody,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Button(
            onClick = onConnect,
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = BrandBlue,
                contentColor = TextTitle
            )
        ) {
            Text(
                text = "Send",
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )
        }
    }
}
