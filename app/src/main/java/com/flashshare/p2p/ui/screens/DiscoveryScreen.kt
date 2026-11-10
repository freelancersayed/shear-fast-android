package com.flashshare.p2p.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.Surface
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
import com.flashshare.p2p.ui.theme.AccentAzure
import com.flashshare.p2p.ui.theme.AccentAzureLight
import com.flashshare.p2p.ui.theme.AccentMint
import com.flashshare.p2p.ui.theme.AccentMintText
import com.flashshare.p2p.ui.theme.AppBackground
import com.flashshare.p2p.ui.theme.CardBorder
import com.flashshare.p2p.ui.theme.SuccessEmerald
import com.flashshare.p2p.ui.theme.SurfaceElevated
import com.flashshare.p2p.ui.theme.TextBody
import com.flashshare.p2p.ui.theme.TextMuted
import com.flashshare.p2p.ui.theme.TextTitle

/**
 * DiscoveryScreen - Clean, human-centered device search interface.
 * Midnight Navy + Mint Emerald theme.
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
            .background(AppBackground)
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

            Surface(
                shape = CircleShape,
                color = SurfaceElevated,
                border = BorderStroke(1.dp, CardBorder)
            ) {
                IconButton(
                    onClick = onCancel,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextBody,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        if (hardwareWarnings.isNotEmpty()) {
            Spacer(modifier = Modifier.height(14.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF24180A),
                border = BorderStroke(1.dp, Color(0xFFF59E0B)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    hardwareWarnings.forEach { warning ->
                        Text(
                            text = "⚠️ $warning",
                            color = Color(0xFFFDE68A),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Center Radar
        PeerRadarComponent(radarColor = AccentMint)

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = statusMessage,
            color = TextTitle,
            fontSize = 16.sp,
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

        Spacer(modifier = Modifier.height(20.dp))

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
                        color = AccentMint,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (peers.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = SurfaceElevated,
                    border = BorderStroke(1.dp, CardBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = stringResource(R.string.searching_devices),
                            color = TextMuted,
                            fontSize = 13.sp
                        )
                    }
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

            Spacer(modifier = Modifier.height(14.dp))

            // Direct IP Option Button
            if (onSendToIp != null) {
                Button(
                    onClick = { showIpDialog = true },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentAzure,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Send to PC / IP Address Directly",
                        fontWeight = FontWeight.Bold,
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
                        color = TextTitle,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column {
                        Text(
                            text = "Enter the destination PC or phone IP address:",
                            color = TextBody,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = targetIpInput,
                            onValueChange = { targetIpInput = it },
                            placeholder = { Text("e.g. 192.168.2.106", color = TextMuted) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AccentMint,
                                unfocusedBorderColor = CardBorder,
                                focusedTextColor = TextTitle,
                                unfocusedTextColor = TextTitle,
                                cursorColor = AccentMint
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
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentMint,
                            contentColor = AccentMintText
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Send Now", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showIpDialog = false }) {
                        Text("Cancel", color = TextMuted)
                    }
                },
                containerColor = SurfaceElevated
            )
        }
    }
}

@Composable
private fun PeerListItem(
    peer: DiscoveredPeer,
    onConnect: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = SurfaceElevated,
        border = BorderStroke(1.dp, CardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
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
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(AppBackground),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PhoneAndroid,
                        contentDescription = null,
                        tint = AccentAzureLight,
                        modifier = Modifier.size(22.dp)
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
                    Spacer(modifier = Modifier.height(2.dp))
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
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentMint,
                    contentColor = AccentMintText
                )
            ) {
                Text(
                    text = "Send",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}
