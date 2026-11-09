package com.flashshare.p2p.engine.socket

import android.content.Context
import android.net.Uri
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
 * Implements high-throughput binary streaming using TCP Sockets and a strictly sized
 * 64KB chunk buffer (ByteArray(1024 * 64)) directly over Dispatchers.IO.
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
     *
     * @param context Application context
     * @param targetHost IP address of the receiving peer
     * @param port Destination port (defaults to 8988)
     * @param sourceUri Original media Uri from Android picker
     * @param onProgress Real-time progress callback invoked periodically
     */
    suspend fun sendOriginalFile(
        context: Context,
        targetHost: String,
        port: Int = TransferConfig.DEFAULT_PORT,
        sourceUri: Uri,
        onProgress: (TransferProgress) -> Unit
    ): Result<TransferStats> = withContext(Dispatchers.IO) {
        isCancelled.set(false)
        val startTime = System.currentTimeMillis()
        var inputStream: InputStream? = null
        var socket: Socket? = null

        try {
            // 1. Resolve raw metadata without downscaling or stripping
            val metadata = storageRepository.getFileMetadata(sourceUri)
            val totalBytes = metadata.fileSize
            val jsonHeader = metadata.toJson()
            val headerBytes = jsonHeader.toByteArray(StandardCharsets.UTF_8)

            Log.i(TAG, "Initiating zero-loss send: ${metadata.fileName} ($totalBytes bytes) to $targetHost:$port")

            // 2. Open raw InputStream
            inputStream = storageRepository.openInputStream(sourceUri)
            val bufferedInput = BufferedInputStream(inputStream, TransferConfig.BUFFER_SIZE)

            // 3. Establish optimized TCP socket
            socket = Socket().apply {
                tcpNoDelay = true
                sendBufferSize = TransferConfig.TCP_BUFFER_SIZE
                receiveBufferSize = TransferConfig.TCP_BUFFER_SIZE
                soTimeout = TransferConfig.SOCKET_TIMEOUT_MS
                connect(InetSocketAddress(targetHost, port), TransferConfig.SOCKET_TIMEOUT_MS)
            }
            activeSocket = socket

            val rawOutput = socket.getOutputStream()
            val bufferedOutput = BufferedOutputStream(rawOutput, TransferConfig.BUFFER_SIZE)
            val dataOutput = DataOutputStream(bufferedOutput)
            val dataInput = DataInputStream(BufferedInputStream(socket.getInputStream()))

            // 4. Protocol Handshake: Magic Header + Metadata Payload
            dataOutput.writeInt(TransferConfig.PROTOCOL_MAGIC_HEADER)
            dataOutput.writeInt(headerBytes.size)
            dataOutput.write(headerBytes)
            dataOutput.flush()

            // 5. Wait for receiver ACK
            val ack = dataInput.readByte()
            if (ack != TransferConfig.ACK_BYTE) {
                throw IllegalStateException("Invalid ACK received from target: $ack")
            }

            // 6. Zero-Loss Raw Byte Streaming using strict 64KB buffer
            val buffer = ByteArray(TransferConfig.BUFFER_SIZE)
            var bytesTransferred = 0L
            var lastSampleTime = System.currentTimeMillis()
            var bytesSinceSample = 0L
            var currentSpeedMBps = 0.0

            while (bytesTransferred < totalBytes || totalBytes <= 0) {
                currentCoroutineContext().ensureActive()
                if (isCancelled.get()) {
                    throw InterruptedException("Transfer cancelled by user")
                }

                val bytesToRead = if (totalBytes > 0) {
                    minOf(buffer.size.toLong(), totalBytes - bytesTransferred).toInt()
                } else {
                    buffer.size
                }

                val bytesRead = bufferedInput.read(buffer, 0, bytesToRead)
                if (bytesRead == -1) break

                bufferedOutput.write(buffer, 0, bytesRead)
                bytesTransferred += bytesRead
                bytesSinceSample += bytesRead

                // Real-time metrics sampling
                val now = System.currentTimeMillis()
                val elapsedSinceSample = now - lastSampleTime
                if (elapsedSinceSample >= TransferConfig.METRICS_INTERVAL_MS) {
                    val instantSpeed = (bytesSinceSample.toDouble() / (1024.0 * 1024.0)) / (elapsedSinceSample.toDouble() / 1000.0)
                    // Smooth speed via exponential moving average (0.3 weight to instant)
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

            // Flush all residual frames
            bufferedOutput.flush()

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

            Log.i(TAG, "Send complete: $bytesTransferred bytes in $totalElapsedSec s (Avg: $avgSpeedMBps MB/s)")

            Result.success(
                TransferStats(
                    fileName = metadata.fileName,
                    totalBytes = bytesTransferred,
                    durationSeconds = totalElapsedSec,
                    averageSpeedMBps = avgSpeedMBps
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Transmission failed: ${e.message}", e)
            Result.failure(e)
        } finally {
            try { inputStream?.close() } catch (ignored: Exception) {}
            try { socket?.close() } catch (ignored: Exception) {}
            activeSocket = null
        }
    }

    /**
     * Receives original file and saves it bit-for-bit into Downloads/FlashShare.
     *
     * @param context Application context
     * @param port Local listening port (defaults to 8988)
     * @param onProgress Real-time progress callback
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
            val bufferedInput = BufferedInputStream(rawInput, TransferConfig.BUFFER_SIZE)
            val dataInput = DataInputStream(bufferedInput)
            val dataOutput = DataOutputStream(BufferedOutputStream(clientSocket.getOutputStream()))

            // 2. Validate Magic Header
            val magic = dataInput.readInt()
            if (magic != TransferConfig.PROTOCOL_MAGIC_HEADER) {
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

            // 4. Create zero-loss target inside Scoped Storage (Downloads/FlashShare)
            destinationTarget = storageRepository.createDestinationTarget(
                fileName = metadata.fileName,
                mimeType = metadata.mimeType,
                fileSize = totalBytes
            )
            val bufferedOutput = BufferedOutputStream(destinationTarget.outputStream, TransferConfig.BUFFER_SIZE)

            // 5. Send ACK to Sender to unblock stream
            dataOutput.writeByte(TransferConfig.ACK_BYTE.toInt())
            dataOutput.flush()

            // 6. Read Raw Bytes Stream using strict 64KB buffer
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

                val bytesRead = bufferedInput.read(buffer, 0, bytesToRead)
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
        }
    }

    companion object {
        private const val TAG = "SocketManager"
    }
}
