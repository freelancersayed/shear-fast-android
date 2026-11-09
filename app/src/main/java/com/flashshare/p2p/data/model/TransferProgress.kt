package com.flashshare.p2p.data.model

import java.util.Locale

/**
 * Real-time transfer metrics snapshot updated at 100ms intervals.
 */
data class TransferProgress(
    val bytesTransferred: Long = 0L,
    val totalBytes: Long = 0L,
    val speedMBps: Double = 0.0,
    val etaSeconds: Long = 0L,
    val fileName: String = ""
) {
    val progressFraction: Float
        get() = if (totalBytes > 0) (bytesTransferred.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 0f

    val percentage: Int
        get() = (progressFraction * 100).toInt()

    val formattedSpeed: String
        get() = String.format(Locale.US, "%.1f MB/s", speedMBps)

    val formattedEta: String
        get() = when {
            etaSeconds <= 0L -> "--"
            etaSeconds < 60L -> "${etaSeconds}s"
            etaSeconds < 3600L -> "${etaSeconds / 60}m ${etaSeconds % 60}s"
            else -> "${etaSeconds / 3600}h ${(etaSeconds % 3600) / 60}m"
        }

    val formattedTransferred: String
        get() = formatBytes(bytesTransferred)

    val formattedTotal: String
        get() = formatBytes(totalBytes)

    companion object {
        fun formatBytes(bytes: Long): String {
            if (bytes < 1024) return "$bytes B"
            val z = (63 - java.lang.Long.numberOfLeadingZeros(bytes)) / 10
            return String.format(Locale.US, "%.2f %sB", bytes.toDouble() / (1L shl (z * 10)), " KMGTPE"[z])
        }
    }
}
