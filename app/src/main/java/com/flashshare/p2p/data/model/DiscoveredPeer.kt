package com.flashshare.p2p.data.model

enum class PeerConnectionType {
    NEARBY_P2P,
    WIFI_DIRECT
}

enum class PeerRole {
    SENDER,
    RECEIVER
}

/**
 * Represents a remote device discovered via Nearby Connections or Wi-Fi Direct.
 */
data class DiscoveredPeer(
    val id: String,
    val name: String,
    val connectionType: PeerConnectionType,
    val ipAddress: String? = null,
    val port: Int = 8988,
    val isConnected: Boolean = false
)
