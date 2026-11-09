# FlashShare P2P ⚡

**FlashShare P2P** is a native, ultra-high-throughput Android peer-to-peer file transfer application written in **Kotlin** and **Jetpack Compose**. It is engineered to transfer massive 4K/8K videos and high-resolution RAW images at maximum hardware link speeds (target **100MB/s+** on 5GHz Wi-Fi Direct) with **100% original fidelity** — completely free of compression, downscaling, or metadata stripping.

---

## 🚀 Key Architectural Highlights

### 1. Zero-Loss Direct Stream Engine
* **Raw Byte Pipelining**: Uses Android Scoped Storage to read raw bytes via `FileInputStream` directly from the source URI.
* **64KB Optimized Chunk Buffer**: Transmits data using a dedicated `ByteArray(1024 * 64)` chunk stream over low-level TCP Sockets (`tcpNoDelay = true`, `sendBufferSize = 512KB`, `receiveBufferSize = 512KB`) to saturate local link capacity.
* **Non-destructive Storage Ingestion**: Receiver writes directly into the public `Downloads/FlashShare` directory via Android's `MediaStore` API (`IS_PENDING` workflow for instant gallery discovery) with 0% transcoding.

### 2. Hybrid Connectivity Core
* **Google Nearby Connections API**: Uses `Strategy.P2P_STAR` (high bandwidth star topology) for instant device discovery, cryptographic handshake, and high-speed Wi-Fi Direct channel negotiation without internet or cellular connectivity.
* **Hardware Wi-Fi Direct (Wi-Fi P2P) Fallback**: Autonomous Group formation using `WifiP2pManager` providing complete offline fallback when Google Play Services is unavailable.

### 3. Jetpack Compose & Material 3 UI
* **High-Tech Dashboard**: Dedicated "Send (Original Quality)" and "Receive Fast" action cards.
* **Telemetry HUD**: Real-time throughput gauge in **MB/s**, Estimated Time Remaining (**ETA**), and linear progress indicator.
* **Radar Visualizer**: Animated pulse waves for peer discovery.
* **Reactive Coroutine Architecture**: Fully asynchronous on `Dispatchers.IO` to ensure a consistent 60/120 FPS UI.

### 4. Modern Android Permissions
* Full support for Android 13+ (API 33/34):
  - `NEARBY_WIFI_DEVICES` (with `usesPermissionFlags="neverForLocation"`)
  - `READ_MEDIA_IMAGES` & `READ_MEDIA_VIDEO`
  - `BLUETOOTH_SCAN`, `BLUETOOTH_ADVERTISE`, `BLUETOOTH_CONNECT`
  - Backward compatibility down to Android 8.0 (API 26).

---

## 📁 System Architecture
```
FlashShare/
├── app/
│   ├── build.gradle.kts
│   ├── src/main/
│   │   ├── AndroidManifest.xml
│   │   ├── java/com/flashshare/p2p/
│   │   │   ├── FlashShareApp.kt
│   │   │   ├── MainActivity.kt
│   │   │   ├── data/
│   │   │   │   ├── model/
│   │   │   │   │   ├── DiscoveredPeer.kt
│   │   │   │   │   ├── FileTransferPayload.kt
│   │   │   │   │   ├── TransferProgress.kt
│   │   │   │   │   └── TransferState.kt
│   │   │   │   └── repository/
│   │   │   │       └── FileStorageRepository.kt
│   │   │   ├── engine/
│   │   │   │   ├── nearby/
│   │   │   │   │   └── NearbyConnectionEngine.kt
│   │   │   │   ├── socket/
│   │   │   │   │   ├── SocketManager.kt
│   │   │   │   │   └── TransferConfig.kt
│   │   │   │   └── wifidirect/
│   │   │   │       └── WifiDirectEngine.kt
│   │   │   ├── permission/
│   │   │   │   └── PermissionManager.kt
│   │   │   └── ui/
│   │   │       ├── components/
│   │   │       │   ├── HighTechSpeedGauge.kt
│   │   │       │   ├── LinearTransferProgress.kt
│   │   │       │   ├── PeerRadarComponent.kt
│   │   │       │   └── PermissionRationaleDialog.kt
│   │   │       ├── screens/
│   │   │       │   ├── DashboardScreen.kt
│   │   │       │   ├── DiscoveryScreen.kt
│   │   │       │   └── TransferScreen.kt
│   │   │       ├── theme/
│   │   │       │   ├── Color.kt
│   │   │       │   ├── Theme.kt
│   │   │       │   └── Type.kt
│   │   │       └── viewmodel/
│   │   │           └── MainViewModel.kt
│   │   └── res/
│   │       ├── drawable/ic_flash_logo.xml
│   │       └── values/{colors, strings, themes}.xml
```

---

## 🛠️ Build and Deployment

### Requirements
* Android Studio Iguana (2023.2.1+) or newer
* JDK 17
* Android SDK 34 (Android 14)

### Building via Terminal
```bash
# Debug APK Build
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug
```
