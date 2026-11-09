package com.flashshare.p2p.engine.nearby

import android.content.Context
import android.os.Build
import android.util.Log
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import com.flashshare.p2p.data.model.DiscoveredPeer
import com.flashshare.p2p.data.model.PeerConnectionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.charset.StandardCharsets

/**
 * NearbyConnectionEngine handles zero-configuration device discovery and pairing
 * using Google Nearby Connections API with Strategy.P2P_STAR (High Bandwidth).
 */
class NearbyConnectionEngine(
    private val context: Context
) {
    private val connectionsClient: ConnectionsClient = Nearby.getConnectionsClient(context)

    private val _discoveredPeers = MutableStateFlow<List<DiscoveredPeer>>(emptyList())
    val discoveredPeers: StateFlow<List<DiscoveredPeer>> = _discoveredPeers.asStateFlow()

    private val _connectionState = MutableStateFlow<String>("IDLE")
    val connectionState: StateFlow<String> = _connectionState.asStateFlow()

    var onEndpointConnected: ((endpointId: String, endpointName: String) -> Unit)? = null
    var onPayloadReceived: ((endpointId: String, data: ByteArray) -> Unit)? = null
    var onError: ((String) -> Unit)? = null

    val deviceName: String = "${Build.MANUFACTURER} ${Build.MODEL}".trim()

    // Endpoint Discovery Callback
    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            Log.d(TAG, "Nearby Endpoint discovered: $endpointId (${info.endpointName})")
            val peer = DiscoveredPeer(
                id = endpointId,
                name = info.endpointName,
                connectionType = PeerConnectionType.NEARBY_P2P
            )
            val current = _discoveredPeers.value.toMutableList()
            if (current.none { it.id == endpointId }) {
                current.add(peer)
                _discoveredPeers.value = current
            }
        }

        override fun onEndpointLost(endpointId: String) {
            Log.d(TAG, "Nearby Endpoint lost: $endpointId")
            _discoveredPeers.value = _discoveredPeers.value.filter { it.id != endpointId }
        }
    }

    // Connection Lifecycle Callback
    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {
            Log.i(TAG, "Connection initiated with: $endpointId (${connectionInfo.endpointName}). Auto-accepting...")
            _connectionState.value = "CONNECTING"
            // Auto accept for high-speed P2P handshake
            connectionsClient.acceptConnection(endpointId, payloadCallback)
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to accept connection: ${e.message}", e)
                    onError?.invoke("Failed to accept connection: ${e.message}")
                }
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            when (result.status.statusCode) {
                ConnectionsStatusCodes.STATUS_OK -> {
                    Log.i(TAG, "Connected successfully to: $endpointId")
                    _connectionState.value = "CONNECTED"
                    val peerName = _discoveredPeers.value.find { it.id == endpointId }?.name ?: "Remote Peer"
                    onEndpointConnected?.invoke(endpointId, peerName)
                }
                ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> {
                    Log.w(TAG, "Connection rejected by: $endpointId")
                    _connectionState.value = "REJECTED"
                    onError?.invoke("Connection was rejected")
                }
                else -> {
                    Log.w(TAG, "Connection broken or timed out: ${result.status.statusCode}")
                    _connectionState.value = "DISCONNECTED"
                    onError?.invoke("Connection failed with code: ${result.status.statusCode}")
                }
            }
        }

        override fun onDisconnected(endpointId: String) {
            Log.i(TAG, "Disconnected from: $endpointId")
            _connectionState.value = "DISCONNECTED"
        }
    }

    // Payload Callback for control & IP/Port handshake messages
    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            if (payload.type == Payload.Type.BYTES) {
                payload.asBytes()?.let { bytes ->
                    Log.d(TAG, "Received payload of size ${bytes.size} from $endpointId")
                    onPayloadReceived?.invoke(endpointId, bytes)
                }
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            // Handshake messages are small bytes, no stream tracking needed here
        }
    }

    /**
     * Starts advertising this device as a Receiver using Strategy.P2P_STAR.
     */
    fun startAdvertising() {
        val advertisingOptions = AdvertisingOptions.Builder()
            .setStrategy(Strategy.P2P_STAR)
            .build()

        connectionsClient.startAdvertising(
            deviceName,
            SERVICE_ID,
            connectionLifecycleCallback,
            advertisingOptions
        ).addOnSuccessListener {
            Log.i(TAG, "Nearby advertising started successfully ($deviceName)")
            _connectionState.value = "ADVERTISING"
        }.addOnFailureListener { e ->
            Log.e(TAG, "Nearby advertising failed: ${e.message}", e)
            onError?.invoke("Advertising failed: ${e.message}")
        }
    }

    /**
     * Starts discovery of nearby devices using Strategy.P2P_STAR.
     */
    fun startDiscovery() {
        _discoveredPeers.value = emptyList()
        val discoveryOptions = DiscoveryOptions.Builder()
            .setStrategy(Strategy.P2P_STAR)
            .build()

        connectionsClient.startDiscovery(
            SERVICE_ID,
            endpointDiscoveryCallback,
            discoveryOptions
        ).addOnSuccessListener {
            Log.i(TAG, "Nearby discovery started successfully")
            _connectionState.value = "DISCOVERING"
        }.addOnFailureListener { e ->
            Log.e(TAG, "Nearby discovery failed: ${e.message}", e)
            onError?.invoke("Discovery failed: ${e.message}")
        }
    }

    /**
     * Connects to a discovered remote endpoint.
     */
    fun connectToPeer(endpointId: String) {
        _connectionState.value = "REQUESTING"
        connectionsClient.requestConnection(
            deviceName,
            endpointId,
            connectionLifecycleCallback
        ).addOnFailureListener { e ->
            Log.e(TAG, "Nearby requestConnection failed: ${e.message}", e)
            onError?.invoke("Connection request failed: ${e.message}")
        }
    }

    /**
     * Sends control byte message (e.g., IP address or TCP coordination info) to peer.
     */
    fun sendControlMessage(endpointId: String, message: String) {
        val payload = Payload.fromBytes(message.toByteArray(StandardCharsets.UTF_8))
        connectionsClient.sendPayload(endpointId, payload)
    }

    /**
     * Shuts down all Nearby discovery, advertising, and connections.
     */
    fun stopAll() {
        try {
            connectionsClient.stopAdvertising()
            connectionsClient.stopDiscovery()
            connectionsClient.stopAllEndpoints()
            _discoveredPeers.value = emptyList()
            _connectionState.value = "IDLE"
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping Nearby client: ${e.message}", e)
        }
    }

    companion object {
        private const val TAG = "NearbyConnectionEngine"
        const val SERVICE_ID = "com.flashshare.p2p"
    }
}
