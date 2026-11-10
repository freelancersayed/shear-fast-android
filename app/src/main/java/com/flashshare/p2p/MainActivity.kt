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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flashshare.p2p.data.model.PeerRole
import com.flashshare.p2p.data.model.TransferState
import com.flashshare.p2p.permission.PermissionManager
import com.flashshare.p2p.ui.components.PermissionRationaleDialog
import com.flashshare.p2p.ui.screens.DashboardScreen
import com.flashshare.p2p.ui.screens.DiscoveryScreen
import com.flashshare.p2p.ui.screens.TransferScreen
import com.flashshare.p2p.ui.theme.BackgroundDark
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
    val context = androidx.compose.ui.platform.LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var showPermissionDialog by remember { mutableStateOf(false) }
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    // System Media Picker for Native Resolution Videos and Images
    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.onFileSelectedForSend(uri)
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
        containerColor = BackgroundDark,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        when (val state = uiState) {
            is TransferState.Idle -> {
                DashboardScreen(
                    onSendClicked = {
                        executeWithPermissions {
                            mediaPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                            )
                        }
                    },
                    onReceiveClicked = {
                        executeWithPermissions {
                            viewModel.startReceiving()
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
                            mediaPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                            )
                        }
                    },
                    onReceiveClicked = {
                        executeWithPermissions {
                            viewModel.startReceiving()
                        }
                    },
                    modifier = Modifier.padding(innerPadding)
                )
            }
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
