package com.flashshare.p2p.data.model

/**
 * High-level state representing the entire lifecycle of a FlashShare session.
 */
sealed interface TransferState {
    data object Idle : TransferState

    data class PermissionsRequired(val missingPermissions: List<String>) : TransferState

    data class Discovering(
        val role: PeerRole,
        val statusMessage: String = "Searching for nearby peers…"
    ) : TransferState

    data class PeersDiscovered(
        val role: PeerRole,
        val peers: List<DiscoveredPeer>
    ) : TransferState

    data class Connecting(
        val peer: DiscoveredPeer,
        val statusMessage: String = "Establishing high-speed P2P link…"
    ) : TransferState

    data class Transferring(
        val progress: TransferProgress,
        val isSender: Boolean,
        val peerName: String
    ) : TransferState

    data class Completed(
        val fileName: String,
        val fileSize: Long,
        val durationSeconds: Double,
        val averageSpeedMBps: Double,
        val destinationUri: String?,
        val isSender: Boolean
    ) : TransferState

    data class Error(
        val message: String,
        val isSender: Boolean = false,
        val canRetry: Boolean = true
    ) : TransferState
}
