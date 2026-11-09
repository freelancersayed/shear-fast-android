package com.flashshare.p2p.engine.wifidirect

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.NetworkInfo
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.util.Log
import com.flashshare.p2p.data.model.DiscoveredPeer
import com.flashshare.p2p.data.model.PeerConnectionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * WifiDirectEngine provides hardware-level Wi-Fi Direct (Wi-Fi P2P) fallback
 * when Google Play Services or Nearby Connections is unavailable.
 */
class WifiDirectEngine(private val context: Context) {

    private val wifiP2pManager: WifiP2pManager? =
        context.getSystemService(Context.WIFI_P2P_SERVICE) as? WifiP2pManager
    private var channel: WifiP2pManager.Channel? = null

    private val _discoveredPeers = MutableStateFlow<List<DiscoveredPeer>>(emptyList())
    val discoveredPeers: StateFlow<List<DiscoveredPeer>> = _discoveredPeers.asStateFlow()

    private val _isWifiP2pEnabled = MutableStateFlow(false)
    val isWifiP2pEnabled: StateFlow<Boolean> = _isWifiP2pEnabled.asStateFlow()

    var onConnectionEstablished: ((groupOwnerAddress: String, isGroupOwner: Boolean) -> Unit)? = null
    var onError: ((String) -> Unit)? = null

    private var receiverRegistered = false

    private val wifiDirectReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(ctx: Context?, intent: Intent?) {
            when (intent?.action) {
                WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                    val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
                    _isWifiP2pEnabled.value = (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED)
                    Log.d(TAG, "Wi-Fi Direct State: ${_isWifiP2pEnabled.value}")
                }
                WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                    wifiP2pManager?.requestPeers(channel) { peersList: WifiP2pDeviceList? ->
                        val peers = peersList?.deviceList?.map { device: WifiP2pDevice ->
                            DiscoveredPeer(
                                id = device.deviceAddress,
                                name = device.deviceName.ifBlank { "Wi-Fi Direct Peer" },
                                connectionType = PeerConnectionType.WIFI_DIRECT
                            )
                        } ?: emptyList()
                        _discoveredPeers.value = peers
                        Log.d(TAG, "Wi-Fi Direct peers found: ${peers.size}")
                    }
                }
                WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                    @Suppress("DEPRECATION")
                    val networkInfo = intent.getParcelableExtra<NetworkInfo>(WifiP2pManager.EXTRA_NETWORK_INFO)
                    if (networkInfo?.isConnected == true) {
                        wifiP2pManager?.requestConnectionInfo(channel) { info: WifiP2pInfo? ->
                            if (info != null && info.groupFormed) {
                                val hostAddress = info.groupOwnerAddress?.hostAddress ?: "192.168.49.1"
                                Log.i(TAG, "Wi-Fi Direct connected! GO Address: $hostAddress, isGO: ${info.isGroupOwner}")
                                onConnectionEstablished?.invoke(hostAddress, info.isGroupOwner)
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Initializes Wi-Fi Direct channel and registers broadcast listeners.
     */
    fun initialize() {
        if (wifiP2pManager == null) {
            Log.w(TAG, "Wi-Fi P2P Service not supported on this device")
            onError?.invoke("Wi-Fi Direct not supported on this device")
            return
        }

        channel = wifiP2pManager.initialize(context, context.mainLooper, null)

        val intentFilter = IntentFilter().apply {
            addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
        }

        if (!receiverRegistered) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(wifiDirectReceiver, intentFilter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                context.registerReceiver(wifiDirectReceiver, intentFilter)
            }
            receiverRegistered = true
        }
    }

    /**
     * Starts peer discovery for Wi-Fi Direct.
     */
    @SuppressLint("MissingPermission")
    fun startDiscovery() {
        val ch = channel ?: return
        wifiP2pManager?.discoverPeers(ch, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d(TAG, "Wi-Fi Direct peer discovery initiated")
            }

            override fun onFailure(reasonCode: Int) {
                Log.e(TAG, "Wi-Fi Direct peer discovery failed: $reasonCode")
                onError?.invoke("Wi-Fi Direct discovery failed ($reasonCode)")
            }
        })
    }

    /**
     * Creates an autonomous Wi-Fi Direct Group (Receiver becomes Group Owner).
     */
    @SuppressLint("MissingPermission")
    fun createGroup() {
        val ch = channel ?: return
        wifiP2pManager?.createGroup(ch, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.i(TAG, "Wi-Fi Direct Autonomous Group Created")
            }

            override fun onFailure(reasonCode: Int) {
                Log.e(TAG, "Wi-Fi Direct createGroup failed: $reasonCode")
                onError?.invoke("Failed to create Wi-Fi Direct group ($reasonCode)")
            }
        })
    }

    /**
     * Connects to a specific Wi-Fi Direct peer.
     */
    @SuppressLint("MissingPermission")
    fun connectToDevice(deviceAddress: String) {
        val ch = channel ?: return
        val config = WifiP2pConfig().apply {
            this.deviceAddress = deviceAddress
        }

        wifiP2pManager?.connect(ch, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.i(TAG, "Connecting to Wi-Fi Direct device: $deviceAddress")
            }

            override fun onFailure(reasonCode: Int) {
                Log.e(TAG, "Wi-Fi Direct connection failed: $reasonCode")
                onError?.invoke("Wi-Fi Direct connection failed ($reasonCode)")
            }
        })
    }

    /**
     * Closes the active group and unregisters the receiver.
     */
    fun tearDown() {
        val ch = channel
        if (ch != null && wifiP2pManager != null) {
            wifiP2pManager.removeGroup(ch, null)
            wifiP2pManager.stopPeerDiscovery(ch, null)
        }
        if (receiverRegistered) {
            try {
                context.unregisterReceiver(wifiDirectReceiver)
            } catch (e: Exception) {
                Log.w(TAG, "Receiver already unregistered")
            }
            receiverRegistered = false
        }
        _discoveredPeers.value = emptyList()
    }

    companion object {
        private const val TAG = "WifiDirectEngine"
    }
}
