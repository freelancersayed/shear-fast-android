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
import com.flashshare.p2p.engine.nearby.NearbyConnectionEngine
import com.flashshare.p2p.engine.socket.SocketManager
import com.flashshare.p2p.engine.socket.TransferConfig
import com.flashshare.p2p.engine.wifidirect.WifiDirectEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * MainViewModel manages high-speed peer discovery, socket life-cycles,
 * and reactive UI updates for FlashShare P2P.
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val storageRepo = FileStorageRepository(application)
    val socketManager = SocketManager(storageRepo)
    private val nearbyEngine = NearbyConnectionEngine(application)
    private val wifiDirectEngine = WifiDirectEngine(application)

    private val _uiState = MutableStateFlow<TransferState>(TransferState.Idle)
    val uiState: StateFlow<TransferState> = _uiState.asStateFlow()

    private val _selectedFileUri = MutableStateFlow<Uri?>(null)
    val selectedFileUri: StateFlow<Uri?> = _selectedFileUri.asStateFlow()

    private val _filePayload = MutableStateFlow<FileTransferPayload?>(null)
    val filePayload: StateFlow<FileTransferPayload?> = _filePayload.asStateFlow()

    private val _allDiscoveredPeers = MutableStateFlow<List<DiscoveredPeer>>(emptyList())
    val discoveredPeers: StateFlow<List<DiscoveredPeer>> = _allDiscoveredPeers.asStateFlow()

    private var activeTransferJob: Job? = null

    init {
        setupEngines()
    }

    private fun setupEngines() {
        wifiDirectEngine.initialize()

        // Combine peer discovery streams from both Nearby and Wi-Fi Direct
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

        // Handle Wi-Fi Direct Connection Handshake
        wifiDirectEngine.onConnectionEstablished = { groupOwnerAddress, isGroupOwner ->
            Log.i(TAG, "Wi-Fi Direct connection established. GO: $groupOwnerAddress, isGO: $isGroupOwner")
            if (!isGroupOwner) {
                // Client side initiates sending
                val uri = _selectedFileUri.value
                if (uri != null) {
                    startSocketSend(groupOwnerAddress, TransferConfig.DEFAULT_PORT, uri)
                }
            }
        }

        // Handle Nearby Payload control messages (e.g. IP sync)
        nearbyEngine.onPayloadReceived = { endpointId, data ->
            val message = String(data)
            Log.d(TAG, "Control payload from $endpointId: $message")
            if (message.startsWith("IP:")) {
                val ip = message.substringAfter("IP:")
                val uri = _selectedFileUri.value
                if (uri != null) {
                    startSocketSend(ip, TransferConfig.DEFAULT_PORT, uri)
                }
            }
        }

        nearbyEngine.onError = { error ->
            Log.e(TAG, "Nearby Engine Error: $error")
        }

        wifiDirectEngine.onError = { error ->
            Log.e(TAG, "Wi-Fi Direct Error: $error")
        }
    }

    /**
     * Prepares Sender flow when a file is selected from system media picker.
     */
    fun onFileSelectedForSend(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val metadata = storageRepo.getFileMetadata(uri)
                _selectedFileUri.value = uri
                _filePayload.value = metadata

                _uiState.value = TransferState.Discovering(
                    role = PeerRole.SENDER,
                    statusMessage = "Scanning for receiving devices…"
                )

                nearbyEngine.startDiscovery()
                wifiDirectEngine.startDiscovery()
            } catch (e: Exception) {
                Log.e(TAG, "Error resolving file metadata", e)
                _uiState.value = TransferState.Error("Failed to access selected file: ${e.message}", isSender = true)
            }
        }
    }

    /**
     * Initiates Receiver flow: advertises, creates Wi-Fi Direct Group, and awaits raw TCP socket stream.
     */
    fun startReceiving() {
        _uiState.value = TransferState.Discovering(
            role = PeerRole.RECEIVER,
            statusMessage = "Ready to receive. Waiting for sender…"
        )

        nearbyEngine.startAdvertising()
        wifiDirectEngine.createGroup()

        // Launch persistent background socket listener on Dispatchers.IO
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
                },
                onFailure = { error ->
                    if (error !is InterruptedException) {
                        _uiState.value = TransferState.Error(
                            message = error.message ?: "Failed to receive file",
                            isSender = false
                        )
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

        if (peer.connectionType == PeerConnectionType.WIFI_DIRECT) {
            wifiDirectEngine.connectToDevice(peer.id)
        } else {
            nearbyEngine.connectToPeer(peer.id)
            // If peer IP is directly available, connect socket immediately
            peer.ipAddress?.let { ip ->
                val uri = _selectedFileUri.value ?: return
                startSocketSend(ip, peer.port, uri)
            }
        }
    }

    private fun startSocketSend(targetIp: String, port: Int, uri: Uri) {
        activeTransferJob?.cancel()
        activeTransferJob = viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = TransferState.Transferring(
                progress = TransferProgress(fileName = _filePayload.value?.fileName ?: "File"),
                isSender = true,
                peerName = targetIp
            )

            val result = socketManager.sendOriginalFile(
                context = getApplication(),
                targetHost = targetIp,
                port = port,
                sourceUri = uri,
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
