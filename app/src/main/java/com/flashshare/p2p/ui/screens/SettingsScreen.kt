package com.flashshare.p2p.ui.screens

import android.os.Build
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flashshare.p2p.ui.theme.ButtonAquaGreen
import com.flashshare.p2p.ui.theme.CorporateBlue

/**
 * SettingsScreen - Displays application configuration, hardware speed optimizations,
 * zero-cost Remote P2P transfer tools, and copyright info.
 */
@Composable
fun SettingsScreen(
    onSendDirectIp: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDirectIpDialog by remember { mutableStateOf(false) }
    var targetIpInput by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CorporateBlue)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Title
        Text(
            text = "Settings & Optimizer",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "High-speed zero-server transfer configuration",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 13.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 1. Device & Storage Section
        SettingsCard(title = "Device & Storage") {
            SettingsInfoRow(
                icon = Icons.Default.PhoneAndroid,
                label = "Device Name",
                value = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}"
            )
            Spacer(modifier = Modifier.height(12.dp))
            SettingsInfoRow(
                icon = Icons.Default.FolderSpecial,
                label = "Save Directory",
                value = "Internal Storage/Download/BondhuShare"
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. High-Speed Optimizer Section
        SettingsCard(title = "Hardware Speed Optimizations") {
            SettingsInfoRow(
                icon = Icons.Default.Speed,
                label = "Streaming I/O Buffer",
                value = "256 KB High Throughput",
                valueColor = ButtonAquaGreen
            )
            Spacer(modifier = Modifier.height(12.dp))
            SettingsInfoRow(
                icon = Icons.Default.Bolt,
                label = "Wi-Fi Bandwidth Upgrade",
                value = "5GHz Wi-Fi Direct Auto-Lock",
                valueColor = ButtonAquaGreen
            )
            Spacer(modifier = Modifier.height(12.dp))
            SettingsInfoRow(
                icon = Icons.Default.CloudDone,
                label = "TCP Latency Engine",
                value = "Zero-Delay (TCP_NODELAY Active)",
                valueColor = ButtonAquaGreen
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Free Remote Transfer (Two Cities) Section
        SettingsCard(title = "Free Remote Transfer (Two Cities)") {
            Text(
                text = "Transfer files to a friend in another city or on the same Wi-Fi router directly via IP without any paid cloud servers.",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 13.sp,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = { showDirectIpDialog = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = ButtonAquaGreen,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Language,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Direct IP Send (Remote P2P)",
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. App & Copyright Info
        SettingsCard(title = "About BondhuShare") {
            SettingsInfoRow(
                icon = Icons.Default.Info,
                label = "Version",
                value = "v1.0.3 (High Speed Engine)"
            )
            Spacer(modifier = Modifier.height(12.dp))
            SettingsInfoRow(
                icon = Icons.Default.Speed,
                label = "License & Cost",
                value = "100% Free • No Cloud Server Required"
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Copyright by: Sayed Hossain",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // Direct IP Send Dialog
    if (showDirectIpDialog) {
        AlertDialog(
            onDismissRequest = { showDirectIpDialog = false },
            title = {
                Text(
                    text = "Send via Direct IP",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "Enter your friend's Wi-Fi / Public IP address. Ensure their app is on 'Receive Fast'.",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = targetIpInput,
                        onValueChange = { targetIpInput = it },
                        placeholder = { Text("e.g. 192.168.1.15", color = Color.White.copy(alpha = 0.4f)) },
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
                            showDirectIpDialog = false
                            onSendDirectIp(targetIpInput.trim())
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ButtonAquaGreen)
                ) {
                    Text("Connect & Send")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDirectIpDialog = false }) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.7f))
                }
            },
            containerColor = Color(0xFF0D3B75)
        )
    }
}

@Composable
private fun SettingsCard(
    title: String,
    content: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.White.copy(alpha = 0.12f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
private fun SettingsInfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    valueColor: Color = Color.White.copy(alpha = 0.9f)
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp
            )
            Text(
                text = value,
                color = valueColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
