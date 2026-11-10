package com.flashshare.p2p

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flashshare.p2p.data.model.PeerRole
import com.flashshare.p2p.data.model.TransferState
import com.flashshare.p2p.permission.PermissionManager
import com.flashshare.p2p.ui.components.PermissionRationaleDialog
import com.flashshare.p2p.ui.screens.DashboardScreen
import com.flashshare.p2p.ui.screens.DiscoveryScreen
import com.flashshare.p2p.ui.screens.TransferScreen
import com.flashshare.p2p.ui.theme.BackgroundDark
import com.flashshare.p2p.ui.theme.ButtonAquaGreen
import com.flashshare.p2p.ui.theme.ButtonSlateGrey
import com.flashshare.p2p.ui.theme.CorporateBlue
import com.flashshare.p2p.ui.theme.FlashShareTheme
import com.flashshare.p2p.ui.viewmodel.MainViewModel

/**
 * MainActivity - Primary entry point orchestrating Compose UI,
 * modern runtime permissions, and system media picker integration.
 */
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            FlashShareTheme {
                FlashShareMainContent(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun FlashShareMainContent(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val peers by viewModel.discoveredPeers.collectAsStateWithLifecycle()
    val hardwareWarnings by viewModel.hardwareWarnings.collectAsStateWithLifecycle()
    val receivedFiles by viewModel.receivedFiles.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var showPermissionDialog by remember { mutableStateOf(false) }
    var showFileTypePicker by remember { mutableStateOf(false) }
    var pendingTargetIp by remember { mutableStateOf<String?>(null) }
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    // Multi-select Visual Media Picker for Native Resolution Videos and Photos
    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 100)
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            val ip = pendingTargetIp
            if (ip != null) {
                pendingTargetIp = null
                viewModel.onFilesSelectedForSend(uris)
                viewModel.sendViaDirectIp(ip)
            } else {
                viewModel.onFilesSelectedForSend(uris)
            }
        }
    }

    // Generic Multi-File Document Picker for Apps, APKs, Documents, Audio, ZIPs
    val allFilesPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            val ip = pendingTargetIp
            if (ip != null) {
                pendingTargetIp = null
                viewModel.onFilesSelectedForSend(uris)
                viewModel.sendViaDirectIp(ip)
            } else {
                viewModel.onFilesSelectedForSend(uris)
            }
        }
    }

    // Dynamic Multi-Permission Launcher for Modern Android
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        viewModel.refreshHardwareStatus()
        pendingAction?.invoke()
        pendingAction = null
    }

    // Permission Verification Helper
    fun executeWithPermissions(action: () -> Unit) {
        viewModel.refreshHardwareStatus()
        if (PermissionManager.hasAllPermissions(context)) {
            action()
        } else {
            pendingAction = action
            showPermissionDialog = true
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = if (uiState is TransferState.Idle) CorporateBlue else BackgroundDark,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        when (val state = uiState) {
            is TransferState.Idle -> {
                DashboardScreen(
                    onSendClicked = {
                        pendingTargetIp = null
                        executeWithPermissions {
                            showFileTypePicker = true
                        }
                    },
                    onReceiveClicked = {
                        executeWithPermissions {
                            viewModel.startReceiving()
                        }
                    },
                    receivedFiles = receivedFiles,
                    onOpenFile = { file -> viewModel.openReceivedFile(file) },
                    onRefreshFiles = { viewModel.loadReceivedFiles() },
                    onSendDirectIp = { ip ->
                        pendingTargetIp = ip
                        executeWithPermissions {
                            showFileTypePicker = true
                        }
                    },
                    modifier = Modifier.padding(innerPadding)
                )
            }

            is TransferState.Discovering -> {
                DiscoveryScreen(
                    role = state.role,
                    statusMessage = state.statusMessage,
                    peers = peers,
                    hardwareWarnings = hardwareWarnings,
                    onPeerSelected = { peer ->
                        viewModel.connectAndSend(peer)
                    },
                    onSendToIp = { ip ->
                        viewModel.sendViaDirectIp(ip)
                    },
                    onCancel = { viewModel.cancelTransfer() },
                    modifier = Modifier.padding(innerPadding)
                )
            }

            is TransferState.PeersDiscovered -> {
                DiscoveryScreen(
                    role = state.role,
                    statusMessage = if (state.role == PeerRole.SENDER) "Receivers found! Select a device:" else "Awaiting transfer stream…",
                    peers = state.peers,
                    hardwareWarnings = hardwareWarnings,
                    onPeerSelected = { peer ->
                        viewModel.connectAndSend(peer)
                    },
                    onSendToIp = { ip ->
                        viewModel.sendViaDirectIp(ip)
                    },
                    onCancel = { viewModel.cancelTransfer() },
                    modifier = Modifier.padding(innerPadding)
                )
            }

            is TransferState.Connecting,
            is TransferState.Transferring,
            is TransferState.Completed,
            is TransferState.Error -> {
                TransferScreen(
                    state = state,
                    onCancel = { viewModel.cancelTransfer() },
                    onDone = { viewModel.resetToIdle() },
                    modifier = Modifier.padding(innerPadding)
                )
            }

            is TransferState.PermissionsRequired -> {
                DashboardScreen(
                    onSendClicked = {
                        executeWithPermissions {
                            showFileTypePicker = true
                        }
                    },
                    onReceiveClicked = {
                        executeWithPermissions {
                            viewModel.startReceiving()
                        }
                    },
                    receivedFiles = receivedFiles,
                    onOpenFile = { file -> viewModel.openReceivedFile(file) },
                    onRefreshFiles = { viewModel.loadReceivedFiles() },
                    onSendDirectIp = { ip -> viewModel.sendViaDirectIp(ip) },
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }

        // File Type Chooser Dialog
        if (showFileTypePicker) {
            AlertDialog(
                onDismissRequest = { showFileTypePicker = false },
                title = {
                    Text(
                        text = "Select Files to Send",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Choose what type of files you want to transfer at original native quality:",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 14.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                showFileTypePicker = false
                                mediaPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ButtonAquaGreen),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Text(
                                text = "Photos & Videos (Multiple)",
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = {
                                showFileTypePicker = false
                                allFilesPickerLauncher.launch(arrayOf("*/*"))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ButtonSlateGrey),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Text(
                                text = "All Files (Apps, Docs, Music)",
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showFileTypePicker = false }) {
                        Text("Cancel", color = Color.White.copy(alpha = 0.7f))
                    }
                },
                containerColor = Color(0xFF0D3B75)
            )
        }

        // Rationale Dialog
        if (showPermissionDialog) {
            PermissionRationaleDialog(
                onConfirm = {
                    showPermissionDialog = false
                    permissionLauncher.launch(PermissionManager.getRequiredPermissions())
                },
                onDismiss = {
                    showPermissionDialog = false
                    pendingAction = null
                }
            )
        }
    }
}
