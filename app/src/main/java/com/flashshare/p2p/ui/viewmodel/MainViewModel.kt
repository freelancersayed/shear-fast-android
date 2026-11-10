package com.flashshare.p2p.ui.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.flashshare.p2p.data.model.DiscoveredPeer
import com.flashshare.p2p.data.model.FileTransferPayload
import com.flashshare.p2p.data.model.PeerConnectionType
import com.flashshare.p2p.data.model.PeerRole
import com.flashshare.p2p.data.model.TransferProgress
import com.flashshare.p2p.data.model.TransferState
import com.flashshare.p2p.data.repository.FileStorageRepository
import com.flashshare.p2p.data.repository.ReceivedFileInfo
import com.flashshare.p2p.engine.nearby.NearbyConnectionEngine
import com.flashshare.p2p.engine.socket.SocketManager
import com.flashshare.p2p.engine.socket.TransferConfig
import com.flashshare.p2p.engine.wifidirect.WifiDirectEngine
import com.flashshare.p2p.permission.PermissionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * MainViewModel manages high-speed peer discovery, dual-engine lifecycle
 * (Nearby Connections + Wi-Fi Direct), batch file sending, and received file management for BondhuShare.
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val storageRepo = FileStorageRepository(application)
    val socketManager = SocketManager(storageRepo)
    private val nearbyEngine = NearbyConnectionEngine(application, storageRepo)
    private val wifiDirectEngine = WifiDirectEngine(application)

    private val _uiState = MutableStateFlow<TransferState>(TransferState.Idle)
    val uiState: StateFlow<TransferState> = _uiState.asStateFlow()

    private val _selectedFileUris = MutableStateFlow<List<Uri>>(emptyList())
    val selectedFileUris: StateFlow<List<Uri>> = _selectedFileUris.asStateFlow()

    private val _filePayloads = MutableStateFlow<List<FileTransferPayload>>(emptyList())
    val filePayloads: StateFlow<List<FileTransferPayload>> = _filePayloads.asStateFlow()

    private val _allDiscoveredPeers = MutableStateFlow<List<DiscoveredPeer>>(emptyList())
    val discoveredPeers: StateFlow<List<DiscoveredPeer>> = _allDiscoveredPeers.asStateFlow()

    private val _hardwareWarnings = MutableStateFlow<List<String>>(emptyList())
    val hardwareWarnings: StateFlow<List<String>> = _hardwareWarnings.asStateFlow()

    private val _receivedFiles = MutableStateFlow<List<ReceivedFileInfo>>(emptyList())
    val receivedFiles: StateFlow<List<ReceivedFileInfo>> = _receivedFiles.asStateFlow()

    private var activeTransferJob: Job? = null
    private var isCurrentlySender = false

    init {
        setupEngines()
        loadReceivedFiles()
    }

    private fun setupEngines() {
        wifiDirectEngine.initialize()

        // Combine peer discovery streams from both Nearby Connections and Wi-Fi Direct
        viewModelScope.launch {
            combine(
                nearbyEngine.discoveredPeers,
                wifiDirectEngine.discoveredPeers
            ) { nearbyList, wifiList ->
                (nearbyList + wifiList).distinctBy { it.id }
            }.collect { combined ->
                _allDiscoveredPeers.value = combined
                val currentState = _uiState.value
                if (currentState is TransferState.Discovering && combined.isNotEmpty()) {
                    _uiState.value = TransferState.PeersDiscovered(
                        role = currentState.role,
                        peers = combined
                    )
                } else if (currentState is TransferState.PeersDiscovered) {
                    _uiState.value = currentState.copy(peers = combined)
                }
            }
        }

        // Nearby Connections transfer callbacks
        nearbyEngine.onTransferProgress = { progress ->
            _uiState.value = TransferState.Transferring(
                progress = progress,
                isSender = isCurrentlySender,
                peerName = "Nearby Peer"
            )
        }

        nearbyEngine.onTransferCompleted = { fileName, totalBytes, durationSec, speedMBps, destUri, isSender ->
            _uiState.value = TransferState.Completed(
                fileName = fileName,
                fileSize = totalBytes,
                durationSeconds = durationSec,
                averageSpeedMBps = speedMBps,
                destinationUri = destUri,
                isSender = isSender
            )
            loadReceivedFiles()
        }

        nearbyEngine.onError = { error ->
            Log.e(TAG, "Nearby Engine Error: $error")
            val currentState = _uiState.value
            if (currentState is TransferState.Connecting || currentState is TransferState.Transferring) {
                _uiState.value = TransferState.Error(error, isSender = isCurrentlySender)
            }
        }

        // Wi-Fi Direct Connection Handshake
        wifiDirectEngine.onConnectionEstablished = { groupOwnerAddress, isGroupOwner ->
            Log.i(TAG, "Wi-Fi Direct connected. GO: $groupOwnerAddress, isGO: $isGroupOwner")
            if (!isGroupOwner) {
                val uris = _selectedFileUris.value
                if (uris.isNotEmpty()) {
                    startSocketSend(groupOwnerAddress, TransferConfig.DEFAULT_PORT, uris)
                }
            }
        }

        wifiDirectEngine.onError = { error ->
            Log.e(TAG, "Wi-Fi Direct Error: $error")
        }
    }

    /**
     * Loads or refreshes the list of files stored in Downloads/BondhuShare.
     */
    fun loadReceivedFiles() {
        viewModelScope.launch(Dispatchers.IO) {
            val list = storageRepo.getReceivedFiles()
            _receivedFiles.value = list
        }
    }

    /**
     * Opens or plays a received file with device default player/viewer.
     */
    fun openReceivedFile(file: ReceivedFileInfo): Boolean {
        return storageRepo.openReceivedFile(file)
    }

    /**
     * Checks and updates hardware warnings (Bluetooth, Wi-Fi, Location).
     */
    fun refreshHardwareStatus() {
        val warnings = PermissionManager.getHardwareWarnings(getApplication())
        _hardwareWarnings.value = warnings
    }

    /**
     * Handles selection of multiple files/photos for high-speed transmission.
     */
    fun onFilesSelectedForSend(uris: List<Uri>) {
        if (uris.isEmpty()) return
        isCurrentlySender = true
        refreshHardwareStatus()

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val metadatas = uris.map { storageRepo.getFileMetadata(it) }
                _selectedFileUris.value = uris
                _filePayloads.value = metadatas

                val title = if (uris.size > 1) "${uris.size} files selected. Scanning for devices…" else "Scanning for receiving devices…"

                _uiState.value = TransferState.Discovering(
                    role = PeerRole.SENDER,
                    statusMessage = title
                )

                // Activate discovery on both engines concurrently
                nearbyEngine.startDiscovery()
                wifiDirectEngine.startDiscovery()
            } catch (e: Exception) {
                Log.e(TAG, "Error resolving file metadata", e)
                _uiState.value = TransferState.Error("Failed to access selected files: ${e.message}", isSender = true)
            }
        }
    }

    fun onFileSelectedForSend(uri: Uri) {
        onFilesSelectedForSend(listOf(uri))
    }

    /**
     * Initiates Receiver flow: advertises via Nearby and creates Wi-Fi Direct Group.
     */
    fun startReceiving() {
        isCurrentlySender = false
        refreshHardwareStatus()

        _uiState.value = TransferState.Discovering(
            role = PeerRole.RECEIVER,
            statusMessage = "Ready to receive. Waiting for sender…"
        )

        nearbyEngine.startAdvertising()
        wifiDirectEngine.createGroup()

        // Also launch fallback socket listener on port 8988 for Wi-Fi Direct & Local/Remote transfers
        activeTransferJob?.cancel()
        activeTransferJob = viewModelScope.launch(Dispatchers.IO) {
            val result = socketManager.receiveOriginalFile(
                context = getApplication(),
                port = TransferConfig.DEFAULT_PORT,
                onProgress = { progress ->
                    _uiState.value = TransferState.Transferring(
                        progress = progress,
                        isSender = false,
                        peerName = "Sender Device"
                    )
                }
            )

            result.fold(
                onSuccess = { stats ->
                    _uiState.value = TransferState.Completed(
                        fileName = stats.fileName,
                        fileSize = stats.totalBytes,
                        durationSeconds = stats.durationSeconds,
                        averageSpeedMBps = stats.averageSpeedMBps,
                        destinationUri = stats.destinationUri,
                        isSender = false
                    )
                    loadReceivedFiles()
                },
                onFailure = { error ->
                    if (error !is InterruptedException && _uiState.value !is TransferState.Completed) {
                        Log.w(TAG, "Socket listener ended: ${error.message}")
                    }
                }
            )
        }
    }

    /**
     * Connects to selected peer and triggers transmission.
     */
    fun connectAndSend(peer: DiscoveredPeer) {
        _uiState.value = TransferState.Connecting(peer)

        val uris = _selectedFileUris.value
        val metas = _filePayloads.value

        if (uris.isEmpty() || metas.isEmpty()) {
            _uiState.value = TransferState.Error("No files selected for transfer", isSender = true)
            return
        }

        if (peer.connectionType == PeerConnectionType.WIFI_DIRECT) {
            wifiDirectEngine.connectToDevice(peer.id)
        } else {
            nearbyEngine.connectAndSend(peer.id, uris, metas)
        }
    }

    /**
     * Sends selected files directly to a remote IP address over LAN or Internet.
     */
    fun sendViaDirectIp(targetIp: String, port: Int = TransferConfig.DEFAULT_PORT) {
        val uris = _selectedFileUris.value
        if (uris.isEmpty()) {
            _uiState.value = TransferState.Error("Please select files first before sending", isSender = true)
            return
        }
        isCurrentlySender = true
        startSocketSend(targetIp.trim(), port, uris)
    }

    private fun startSocketSend(targetIp: String, port: Int, uris: List<Uri>) {
        activeTransferJob?.cancel()
        activeTransferJob = viewModelScope.launch(Dispatchers.IO) {
            val summaryTitle = if (uris.size > 1) "${uris.size} Files" else (_filePayloads.value.firstOrNull()?.fileName ?: "File")
            _uiState.value = TransferState.Transferring(
                progress = TransferProgress(fileName = summaryTitle),
                isSender = true,
                peerName = targetIp
            )

            val result = socketManager.sendOriginalFiles(
                context = getApplication(),
                targetHost = targetIp,
                port = port,
                sourceUris = uris,
                onProgress = { progress ->
                    _uiState.value = TransferState.Transferring(
                        progress = progress,
                        isSender = true,
                        peerName = targetIp
                    )
                }
            )

            result.fold(
                onSuccess = { stats ->
                    _uiState.value = TransferState.Completed(
                        fileName = stats.fileName,
                        fileSize = stats.totalBytes,
                        durationSeconds = stats.durationSeconds,
                        averageSpeedMBps = stats.averageSpeedMBps,
                        destinationUri = null,
                        isSender = true
                    )
                },
                onFailure = { error ->
                    if (error !is InterruptedException) {
                        _uiState.value = TransferState.Error(
                            message = error.message ?: "Transmission failed",
                            isSender = true
                        )
                    }
                }
            )
        }
    }

    /**
     * Aborts active transfer and resets engines.
     */
    fun cancelTransfer() {
        activeTransferJob?.cancel()
        socketManager.cancelTransfer()
        nearbyEngine.stopAll()
        wifiDirectEngine.tearDown()
        _uiState.value = TransferState.Idle
        _allDiscoveredPeers.value = emptyList()
        _selectedFileUris.value = emptyList()
        _filePayloads.value = emptyList()
    }

    /**
     * Resets state back to dashboard.
     */
    fun resetToIdle() {
        cancelTransfer()
    }

    override fun onCleared() {
        super.onCleared()
        cancelTransfer()
    }

    companion object {
        private const val TAG = "MainViewModel"
    }
}
