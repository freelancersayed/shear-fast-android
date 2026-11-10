package com.flashshare.p2p.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flashshare.p2p.R
import com.flashshare.p2p.ui.theme.BottomBarIconActive
import com.flashshare.p2p.ui.theme.BottomBarIconInactive
import com.flashshare.p2p.ui.theme.BottomBarMutedTeal
import com.flashshare.p2p.ui.theme.ButtonAquaGreen
import com.flashshare.p2p.ui.theme.ButtonSlateGrey
import com.flashshare.p2p.ui.theme.CorporateBlue

/**
 * DashboardScreen - Flat, human-designed UI matching the BondhuShare brand design.
 * Features:
 * - Solid Deep Corporate Blue background (#1251A1)
 * - Centered flat vector handshake logo
 * - Bold white "BondhuShare" app title
 * - Flat "Send (Original Quality)" button in Aqua Green (#4DB6AC)
 * - Flat "Receive Fast" button in Slate Grey (#B0BEC5)
 * - Centered connection helper text
 * - Persistent "Copyright by: Sayed Hossain" at the bottom
 * - Slate/teal bottom navigation bar with Home, Files, Settings icons
 */
@Composable
fun DashboardScreen(
    onSendClicked: () -> Unit,
    onReceiveClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedNavIndex by remember { mutableIntStateOf(0) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CorporateBlue)
    ) {
        // Main Scrollable / Centered Area
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(44.dp))

            // 1. Centered Handshake Logo
            Image(
                painter = painterResource(id = R.drawable.ic_bondhushare_handshake),
                contentDescription = "BondhuShare Logo",
                modifier = Modifier
                    .size(140.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 2. App Title
            Text(
                text = "BondhuShare",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(40.dp))

            // 3. Send (Original Quality) Button - Aqua Green (#4DB6AC)
            Button(
                onClick = onSendClicked,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ButtonAquaGreen,
                    contentColor = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 2.dp
                )
            ) {
                Text(
                    text = "Send (Original Quality)",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Receive Fast Button - Flat Slate Grey (#B0BEC5)
            Button(
                onClick = onReceiveClicked,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ButtonSlateGrey,
                    contentColor = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 2.dp
                )
            ) {
                Text(
                    text = "Receive Fast",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(26.dp))

            // 5. Helper Text
            Text(
                text = "Ensure Wi-Fi or Bluetooth is enabled\nfor best connection",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.weight(1f))

            // 6. Persistent Copyright Text
            Text(
                text = "Copyright by: Sayed Hossain",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        // 7. Clean Bottom Navigation Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(BottomBarMutedTeal)
                .padding(horizontal = 32.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Home Icon (Active)
            IconButton(
                onClick = { selectedNavIndex = 0 },
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "Home",
                    tint = if (selectedNavIndex == 0) BottomBarIconActive else BottomBarIconInactive,
                    modifier = Modifier.size(26.dp)
                )
            }

            // Files Icon
            IconButton(
                onClick = { selectedNavIndex = 1 },
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = "Files",
                    tint = if (selectedNavIndex == 1) BottomBarIconActive else BottomBarIconInactive,
                    modifier = Modifier.size(26.dp)
                )
            }

            // Settings Icon
            IconButton(
                onClick = { selectedNavIndex = 2 },
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = if (selectedNavIndex == 2) BottomBarIconActive else BottomBarIconInactive,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}
