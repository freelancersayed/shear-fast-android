package com.flashshare.p2p.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flashshare.p2p.R
import com.flashshare.p2p.data.repository.ReceivedFileInfo
import com.flashshare.p2p.ui.theme.AccentAzure
import com.flashshare.p2p.ui.theme.AccentAzureLight
import com.flashshare.p2p.ui.theme.AccentMint
import com.flashshare.p2p.ui.theme.AccentMintText
import com.flashshare.p2p.ui.theme.AppBackground
import com.flashshare.p2p.ui.theme.BottomBarBackground
import com.flashshare.p2p.ui.theme.BottomBarBorder
import com.flashshare.p2p.ui.theme.BottomBarIconActive
import com.flashshare.p2p.ui.theme.BottomBarIconInactive
import com.flashshare.p2p.ui.theme.CardBorder
import com.flashshare.p2p.ui.theme.SurfaceElevated
import com.flashshare.p2p.ui.theme.TextBody
import com.flashshare.p2p.ui.theme.TextMuted
import com.flashshare.p2p.ui.theme.TextTitle

/**
 * DashboardScreen - Modern, world-class UI for BondhuShare.
 * Cohesive Midnight Navy + Mint Emerald + Electric Azure theme.
 */
@Composable
fun DashboardScreen(
    onSendClicked: () -> Unit,
    onReceiveClicked: () -> Unit,
    receivedFiles: List<ReceivedFileInfo> = emptyList(),
    onOpenFile: (ReceivedFileInfo) -> Unit = {},
    onRefreshFiles: () -> Unit = {},
    onSendDirectIp: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedNavIndex by remember { mutableIntStateOf(0) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        // Tab Content Switching
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (selectedNavIndex) {
                0 -> {
                    // Home Screen
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(36.dp))

                        // 1. Centered Handshake Logo inside glowing badge
                        Box(
                            modifier = Modifier
                                .size(130.dp)
                                .clip(CircleShape)
                                .background(SurfaceElevated)
                                .border(1.5.dp, CardBorder, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_bondhushare_handshake),
                                contentDescription = "BondhuShare Logo",
                                modifier = Modifier.size(96.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // 2. App Title & Subtitle Badge
                        Text(
                            text = "BondhuShare",
                            color = TextTitle,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = SurfaceElevated,
                            border = BorderStroke(1.dp, CardBorder)
                        ) {
                            Text(
                                text = "⚡ 100% Free • High-Speed P2P • No Cloud",
                                color = AccentAzureLight,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(36.dp))

                        // 3. Send (Original Quality) Button - Mint Emerald (#00D29E)
                        Button(
                            onClick = onSendClicked,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(62.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AccentMint,
                                contentColor = AccentMintText
                            ),
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = 4.dp,
                                pressedElevation = 1.dp
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowUpward,
                                contentDescription = null,
                                tint = AccentMintText,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Send (Original Quality)",
                                color = AccentMintText,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // 4. Receive Fast Button - Electric Azure (#2563EB)
                        Button(
                            onClick = onReceiveClicked,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(62.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AccentAzure,
                                contentColor = Color.White
                            ),
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = 4.dp,
                                pressedElevation = 1.dp
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowDownward,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Receive Fast",
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // 5. Connection Helper Card
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = SurfaceElevated,
                            border = BorderStroke(1.dp, CardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(AccentAzure.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Wifi,
                                        contentDescription = null,
                                        tint = AccentAzureLight,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "High-Speed Offline Transfer",
                                        color = TextTitle,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Ensure Wi-Fi or Bluetooth is on for direct phone-to-phone speed",
                                        color = TextBody,
                                        fontSize = 11.sp,
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        // 6. Persistent Copyright Text
                        Text(
                            text = "Copyright by: Sayed Hossain",
                            color = TextMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }
                }

                1 -> {
                    // Files Screen (Downloads/BondhuShare History)
                    FilesScreen(
                        files = receivedFiles,
                        onFileClick = onOpenFile,
                        onRefresh = onRefreshFiles,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                2 -> {
                    // Settings Screen
                    SettingsScreen(
                        onSendDirectIp = onSendDirectIp,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        // 7. Polished Bottom Navigation Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = BottomBarBackground,
            border = BorderStroke(1.dp, BottomBarBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(62.dp)
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BottomNavItem(
                    icon = Icons.Default.Home,
                    label = "Home",
                    isSelected = selectedNavIndex == 0,
                    onClick = { selectedNavIndex = 0 }
                )

                BottomNavItem(
                    icon = Icons.Default.Folder,
                    label = "Files",
                    isSelected = selectedNavIndex == 1,
                    onClick = {
                        selectedNavIndex = 1
                        onRefreshFiles()
                    }
                )

                BottomNavItem(
                    icon = Icons.Default.Settings,
                    label = "Settings",
                    isSelected = selectedNavIndex == 2,
                    onClick = { selectedNavIndex = 2 }
                )
            }
        }
    }
}

@Composable
private fun BottomNavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) BottomBarIconActive else BottomBarIconInactive,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            color = if (isSelected) BottomBarIconActive else BottomBarIconInactive,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
