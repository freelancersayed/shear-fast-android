package com.flashshare.p2p.engine.socket

import android.content.Context
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.os.PowerManager
import android.util.Log
import com.flashshare.p2p.data.model.FileTransferPayload
import com.flashshare.p2p.data.model.TransferProgress
import com.flashshare.p2p.data.repository.DestinationTarget
import com.flashshare.p2p.data.repository.FileStorageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.InputStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Results and statistics upon successful completion of a file transfer session.
 */
data class TransferStats(
    val fileName: String,
    val totalBytes: Long,
    val durationSeconds: Double,
    val averageSpeedMBps: Double,
    val destinationUri: String? = null
)

/**
 * SocketManager - Core Zero-Loss Streaming Engine.
 * Implements high-throughput binary streaming using TCP Sockets, high-performance
 * Wi-Fi Lock, CPU WakeLock, and 512KB chunk buffer streaming directly over Dispatchers.IO.
 */
class SocketManager(
    private val storageRepository: FileStorageRepository
) {
    private val isCancelled = AtomicBoolean(false)
    private var activeSocket: Socket? = null
    private var activeServerSocket: ServerSocket? = null

    /**
     * Cancels any active socket session and unblocks pending read/write calls.
     */
    fun cancelTransfer() {
        isCancelled.set(true)
        try {
            activeSocket?.close()
        } catch (ignored: Exception) {}
        try {
            activeServerSocket?.close()
        } catch (ignored: Exception) {}
    }

    /**
     * Sends original file at native quality over TCP socket.
     */
    suspend fun sendOriginalFile(
        context: Context,
        targetHost: String,
        port: Int = TransferConfig.DEFAULT_PORT,
        sourceUri: Uri,
        onProgress: (TransferProgress) -> Unit
    ): Result<TransferStats> = sendOriginalFiles(
        context = context,
        targetHost = targetHost,
        port = port,
        sourceUris = listOf(sourceUri),
        onProgress = onProgress
    )

    /**
     * Sends original files at native quality over TCP socket with 512 KB chunk streaming,
     * low-latency Wi-Fi Lock, and CPU WakeLock to guarantee maximum hardware throughput.
     */
    suspend fun sendOriginalFiles(
        context: Context,
        targetHost: String,
        port: Int = TransferConfig.DEFAULT_PORT,
        sourceUris: List<Uri>,
        onProgress: (TransferProgress) -> Unit
    ): Result<TransferStats> = withContext(Dispatchers.IO) {
        isCancelled.set(false)
        val startTime = System.currentTimeMillis()
        var totalBytesTransferredAllFiles = 0L

        // Acquire High-Performance Wi-Fi Lock and WakeLock
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        val wifiLock = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                wifiManager?.createWifiLock(WifiManager.WIFI_MODE_FULL_LOW_LATENCY, "BondhuShare:LowLatencyWifiLock")
            } else {
                @Suppress("DEPRECATION")
                wifiManager?.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "BondhuShare:HighPerfWifiLock")
            }?.apply {
                setReferenceCounted(false)
                acquire()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to acquire WifiLock: ${e.message}")
            null
        }

        val powerManager = context.applicationContext.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val wakeLock = try {
            powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "BondhuShare:TransferWakeLock")?.apply {
                setReferenceCounted(false)
                acquire(30 * 60 * 1000L) // 30 minutes max safeguard
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to acquire WakeLock: ${e.message}")
            null
        }

        try {
            for ((index, uri) in sourceUris.withIndex()) {
                if (isCancelled.get()) {
                    return@withContext Result.failure(InterruptedException("Transfer cancelled by user"))
                }

                var inputStream: InputStream? = null
                var socket: Socket? = null

                try {
                    val metadata = storageRepository.getFileMetadata(uri)
                    val totalBytes = metadata.fileSize
                    val jsonHeader = metadata.toJson()
                    val headerBytes = jsonHeader.toByteArray(StandardCharsets.UTF_8)

                    Log.i(TAG, "Sending [${index + 1}/${sourceUris.size}]: ${metadata.fileName} ($totalBytes bytes) to $targetHost:$port")

                    inputStream = storageRepository.openInputStream(uri)
                    val bufferedInput = BufferedInputStream(inputStream, TransferConfig.BUFFER_SIZE)

                    socket = Socket().apply {
                        tcpNoDelay = true
                        sendBufferSize = TransferConfig.TCP_BUFFER_SIZE
                        receiveBufferSize = TransferConfig.TCP_BUFFER_SIZE
                        soTimeout = TransferConfig.SOCKET_TIMEOUT_MS
                        connect(InetSocketAddress(targetHost, port), TransferConfig.SOCKET_TIMEOUT_MS)
                    }
                    activeSocket = socket

                    val rawOutput = socket.getOutputStream()
                    val dataOutput = DataOutputStream(BufferedOutputStream(rawOutput, 64 * 1024))
                    val dataInput = DataInputStream(BufferedInputStream(socket.getInputStream(), 64 * 1024))

                    // Protocol Handshake
                    dataOutput.writeInt(TransferConfig.PROTOCOL_MAGIC_HEADER)
                    dataOutput.writeInt(headerBytes.size)
                    dataOutput.write(headerBytes)
                    dataOutput.flush()

                    // Wait for receiver ACK
                    val ack = dataInput.readByte()
                    if (ack != TransferConfig.ACK_BYTE) {
                        throw IllegalStateException("Invalid ACK received from target: $ack")
                    }

                    // High-performance 512 KB zero-copy streaming
                    val buffer = ByteArray(TransferConfig.BUFFER_SIZE)
                    var fileBytesTransferred = 0L
                    var lastSampleTime = System.currentTimeMillis()
                    var bytesSinceSample = 0L
                    var currentSpeedMBps = 0.0

                    while (fileBytesTransferred < totalBytes || totalBytes <= 0) {
                        currentCoroutineContext().ensureActive()
                        if (isCancelled.get()) {
                            throw InterruptedException("Transfer cancelled by user")
                        }

                        val bytesToRead = if (totalBytes > 0) {
                            minOf(buffer.size.toLong(), totalBytes - fileBytesTransferred).toInt()
                        } else {
                            buffer.size
                        }

                        val bytesRead = bufferedInput.read(buffer, 0, bytesToRead)
                        if (bytesRead == -1) break

                        rawOutput.write(buffer, 0, bytesRead)
                        fileBytesTransferred += bytesRead
                        bytesSinceSample += bytesRead

                        val now = System.currentTimeMillis()
                        val elapsedSinceSample = now - lastSampleTime
                        if (elapsedSinceSample >= TransferConfig.METRICS_INTERVAL_MS) {
                            val instantSpeed = (bytesSinceSample.toDouble() / (1024.0 * 1024.0)) / (elapsedSinceSample.toDouble() / 1000.0)
                            currentSpeedMBps = if (currentSpeedMBps <= 0.0) instantSpeed else (currentSpeedMBps * 0.7) + (instantSpeed * 0.3)

                            val remainingBytes = if (totalBytes > fileBytesTransferred) totalBytes - fileBytesTransferred else 0L
                            val etaSeconds = if (currentSpeedMBps > 0.01) {
                                (remainingBytes.toDouble() / (currentSpeedMBps * 1024.0 * 1024.0)).toLong()
                            } else 0L

                            val prefix = if (sourceUris.size > 1) "(${index + 1}/${sourceUris.size}) " else ""

                            onProgress(
                                TransferProgress(
                                    bytesTransferred = fileBytesTransferred,
                                    totalBytes = totalBytes,
                                    speedMBps = currentSpeedMBps,
                                    etaSeconds = etaSeconds,
                                    fileName = "$prefix${metadata.fileName}"
                                )
                            )

                            lastSampleTime = now
                            bytesSinceSample = 0L
                        }
                    }

                    rawOutput.flush()
                    totalBytesTransferredAllFiles += fileBytesTransferred

                } catch (e: Exception) {
                    Log.e(TAG, "Transmission failed on file [${index + 1}]: ${e.message}", e)
                    return@withContext Result.failure(e)
                } finally {
                    try { inputStream?.close() } catch (ignored: Exception) {}
                    try { socket?.close() } catch (ignored: Exception) {}
                    activeSocket = null
                }
            }

            val totalElapsedSec = ((System.currentTimeMillis() - startTime).toDouble() / 1000.0).coerceAtLeast(0.1)
            val avgSpeedMBps = (totalBytesTransferredAllFiles.toDouble() / (1024.0 * 1024.0)) / totalElapsedSec

            val summaryName = if (sourceUris.size > 1) "${sourceUris.size} Files" else "File"

            onProgress(
                TransferProgress(
                    bytesTransferred = totalBytesTransferredAllFiles,
                    totalBytes = totalBytesTransferredAllFiles,
                    speedMBps = avgSpeedMBps,
                    etaSeconds = 0L,
                    fileName = summaryName
                )
            )

            Log.i(TAG, "All files sent successfully: $totalBytesTransferredAllFiles bytes in $totalElapsedSec s ($avgSpeedMBps MB/s)")

            Result.success(
                TransferStats(
                    fileName = summaryName,
                    totalBytes = totalBytesTransferredAllFiles,
                    durationSeconds = totalElapsedSec,
                    averageSpeedMBps = avgSpeedMBps
                )
            )
        } finally {
            try {
                if (wifiLock?.isHeld == true) wifiLock.release()
            } catch (ignored: Exception) {}
            try {
                if (wakeLock?.isHeld == true) wakeLock.release()
            } catch (ignored: Exception) {}
        }
    }

    /**
     * Receives original file and saves it bit-for-bit into Downloads/BondhuShare.
     */
    suspend fun receiveOriginalFile(
        context: Context,
        port: Int = TransferConfig.DEFAULT_PORT,
        onProgress: (TransferProgress) -> Unit
    ): Result<TransferStats> = withContext(Dispatchers.IO) {
        isCancelled.set(false)
        val startTime = System.currentTimeMillis()
        var serverSocket: ServerSocket? = null
        var clientSocket: Socket? = null
        var destinationTarget: DestinationTarget? = null

        // Acquire High-Performance Wi-Fi Lock and WakeLock
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        val wifiLock = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                wifiManager?.createWifiLock(WifiManager.WIFI_MODE_FULL_LOW_LATENCY, "BondhuShare:LowLatencyWifiLock")
            } else {
                @Suppress("DEPRECATION")
                wifiManager?.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "BondhuShare:HighPerfWifiLock")
            }?.apply {
                setReferenceCounted(false)
                acquire()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to acquire WifiLock: ${e.message}")
            null
        }

        val powerManager = context.applicationContext.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val wakeLock = try {
            powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "BondhuShare:TransferWakeLock")?.apply {
                setReferenceCounted(false)
                acquire(30 * 60 * 1000L)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to acquire WakeLock: ${e.message}")
            null
        }

        try {
            // 1. Initialize TCP Server Socket
            serverSocket = ServerSocket().apply {
                reuseAddress = true
                receiveBufferSize = TransferConfig.TCP_BUFFER_SIZE
                bind(InetSocketAddress(port))
            }
            activeServerSocket = serverSocket

            Log.i(TAG, "Receiver listening on port $port...")
            clientSocket = serverSocket.accept().apply {
                tcpNoDelay = true
                sendBufferSize = TransferConfig.TCP_BUFFER_SIZE
                receiveBufferSize = TransferConfig.TCP_BUFFER_SIZE
                soTimeout = TransferConfig.SOCKET_TIMEOUT_MS
            }
            activeSocket = clientSocket
            Log.i(TAG, "Sender connected from: ${clientSocket.inetAddress.hostAddress}")

            val rawInput = clientSocket.getInputStream()
            val dataInput = DataInputStream(BufferedInputStream(rawInput, 64 * 1024))
            val dataOutput = DataOutputStream(BufferedOutputStream(clientSocket.getOutputStream(), 64 * 1024))

            // 2. Validate Magic Header (BondhuShare or legacy)
            val magic = dataInput.readInt()
            if (magic != TransferConfig.PROTOCOL_MAGIC_HEADER && magic != TransferConfig.LEGACY_MAGIC_HEADER) {
                throw IllegalStateException("Invalid protocol magic header: 0x${Integer.toHexString(magic)}")
            }

            // 3. Read Metadata Header
            val headerLen = dataInput.readInt()
            val headerBytes = ByteArray(headerLen)
            dataInput.readFully(headerBytes)
            val jsonHeader = String(headerBytes, StandardCharsets.UTF_8)
            val metadata = FileTransferPayload.fromJson(jsonHeader)
            val totalBytes = metadata.fileSize

            Log.i(TAG, "Receiving native file: ${metadata.fileName} ($totalBytes bytes, mime: ${metadata.mimeType})")

            // 4. Create zero-loss target inside Scoped Storage (Download/BondhuShare)
            destinationTarget = storageRepository.createDestinationTarget(
                fileName = metadata.fileName,
                mimeType = metadata.mimeType,
                fileSize = totalBytes
            )
            val targetOutputStream = destinationTarget.outputStream
            val bufferedOutput = BufferedOutputStream(targetOutputStream, TransferConfig.BUFFER_SIZE)

            // 5. Send ACK to Sender to unblock stream
            dataOutput.writeByte(TransferConfig.ACK_BYTE.toInt())
            dataOutput.flush()

            // 6. Read Raw Bytes Stream using high-performance 512KB buffer
            val buffer = ByteArray(TransferConfig.BUFFER_SIZE)
            var bytesTransferred = 0L
            var lastSampleTime = System.currentTimeMillis()
            var bytesSinceSample = 0L
            var currentSpeedMBps = 0.0

            while (bytesTransferred < totalBytes || totalBytes <= 0) {
                currentCoroutineContext().ensureActive()
                if (isCancelled.get()) {
                    throw InterruptedException("Transfer cancelled by receiver")
                }

                val bytesToRead = if (totalBytes > 0) {
                    minOf(buffer.size.toLong(), totalBytes - bytesTransferred).toInt()
                } else {
                    buffer.size
                }

                val bytesRead = rawInput.read(buffer, 0, bytesToRead)
                if (bytesRead == -1) break

                bufferedOutput.write(buffer, 0, bytesRead)
                bytesTransferred += bytesRead
                bytesSinceSample += bytesRead

                // Periodic Metrics Calculation
                val now = System.currentTimeMillis()
                val elapsedSinceSample = now - lastSampleTime
                if (elapsedSinceSample >= TransferConfig.METRICS_INTERVAL_MS) {
                    val instantSpeed = (bytesSinceSample.toDouble() / (1024.0 * 1024.0)) / (elapsedSinceSample.toDouble() / 1000.0)
                    currentSpeedMBps = if (currentSpeedMBps <= 0.0) instantSpeed else (currentSpeedMBps * 0.7) + (instantSpeed * 0.3)

                    val remainingBytes = if (totalBytes > bytesTransferred) totalBytes - bytesTransferred else 0L
                    val etaSeconds = if (currentSpeedMBps > 0.01) {
                        (remainingBytes.toDouble() / (currentSpeedMBps * 1024.0 * 1024.0)).toLong()
                    } else 0L

                    onProgress(
                        TransferProgress(
                            bytesTransferred = bytesTransferred,
                            totalBytes = totalBytes,
                            speedMBps = currentSpeedMBps,
                            etaSeconds = etaSeconds,
                            fileName = metadata.fileName
                        )
                    )

                    lastSampleTime = now
                    bytesSinceSample = 0L
                }
            }

            // 7. Flush and finalize the received file in MediaStore
            bufferedOutput.flush()
            destinationTarget.finish()

            val totalElapsedSec = (System.currentTimeMillis() - startTime).toDouble() / 1000.0
            val avgSpeedMBps = if (totalElapsedSec > 0) {
                (bytesTransferred.toDouble() / (1024.0 * 1024.0)) / totalElapsedSec
            } else 0.0

            onProgress(
                TransferProgress(
                    bytesTransferred = bytesTransferred,
                    totalBytes = totalBytes,
                    speedMBps = avgSpeedMBps,
                    etaSeconds = 0L,
                    fileName = metadata.fileName
                )
            )

            Log.i(TAG, "Receive complete: saved to ${destinationTarget.resultUriString} in $totalElapsedSec s")

            Result.success(
                TransferStats(
                    fileName = metadata.fileName,
                    totalBytes = bytesTransferred,
                    durationSeconds = totalElapsedSec,
                    averageSpeedMBps = avgSpeedMBps,
                    destinationUri = destinationTarget.resultUriString
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Reception failed: ${e.message}", e)
            destinationTarget?.discard()
            Result.failure(e)
        } finally {
            try { clientSocket?.close() } catch (ignored: Exception) {}
            try { serverSocket?.close() } catch (ignored: Exception) {}
            activeSocket = null
            activeServerSocket = null
            try {
                if (wifiLock?.isHeld == true) wifiLock.release()
            } catch (ignored: Exception) {}
            try {
                if (wakeLock?.isHeld == true) wakeLock.release()
            } catch (ignored: Exception) {}
        }
    }

    companion object {
        private const val TAG = "SocketManager"
    }
}
