package com.flashshare.p2p.engine.socket

/**
 * Performance and protocol configuration constants for high-speed TCP streaming.
 */
object TransferConfig {
    /**
     * Chunk buffer size optimized for Wi-Fi Direct and 5GHz P2P links.
     * Strict requirement: ByteArray(1024 * 64) = 64 KB per read/write cycle.
     */
    const val BUFFER_SIZE: Int = 1024 * 64

    /**
     * Default TCP Socket port for FlashShare P2P data stream.
     */
    const val DEFAULT_PORT: Int = 8988

    /**
     * Socket connection and read timeout in milliseconds.
     */
    const val SOCKET_TIMEOUT_MS: Int = 45000

    /**
     * Low-level TCP Socket send/receive buffer size for maximizing bandwidth.
     */
    const val TCP_BUFFER_SIZE: Int = 512 * 1024

    /**
     * Protocol magic header bytes: 'FLSH' (0x46, 0x4C, 0x53, 0x48).
     */
    const val PROTOCOL_MAGIC_HEADER: Int = 0x464C5348

    /**
     * Acknowledgment byte sent by receiver when ready to accept byte stream.
     */
    const val ACK_BYTE: Byte = 0x06

    /**
     * Rolling update cadence for UI metrics.
     */
    const val METRICS_INTERVAL_MS: Long = 100L
}
