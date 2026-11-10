package com.flashshare.p2p.engine.socket

/**
 * Performance and protocol configuration constants for high-speed TCP streaming.
 * Optimized for zero-processor lag and maximum hardware P2P throughput.
 */
object TransferConfig {
    /**
     * Chunk buffer size optimized for Wi-Fi Direct, Local Hotspot, and LAN/Internet P2P links.
     * High-speed 512 KB buffer per read/write cycle eliminates processor thrashing and maximizes throughput.
     */
    const val BUFFER_SIZE: Int = 1024 * 512

    /**
     * Default TCP Socket port for BondhuShare P2P data stream.
     */
    const val DEFAULT_PORT: Int = 8988

    /**
     * Socket connection and read timeout in milliseconds.
     */
    const val SOCKET_TIMEOUT_MS: Int = 45000

    /**
     * Low-level TCP Socket send/receive buffer size for maximizing bandwidth (2 MB).
     */
    const val TCP_BUFFER_SIZE: Int = 1024 * 1024 * 2

    /**
     * Protocol magic header bytes: 'BNDU' (0x42, 0x4E, 0x44, 0x55) for BondhuShare.
     */
    const val PROTOCOL_MAGIC_HEADER: Int = 0x424E4455

    /**
     * Legacy protocol magic header bytes: 'FLSH' (0x46, 0x4C, 0x53, 0x48) for backward compatibility.
     */
    const val LEGACY_MAGIC_HEADER: Int = 0x464C5348

    /**
     * Acknowledgment byte sent by receiver when ready to accept byte stream.
     */
    const val ACK_BYTE: Byte = 0x06

    /**
     * Rolling update cadence for UI metrics.
     * Throttled to 200ms so high data rates don't choke Android's Main UI thread.
     */
    const val METRICS_INTERVAL_MS: Long = 200L
}
