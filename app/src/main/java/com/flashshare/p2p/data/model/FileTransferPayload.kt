package com.flashshare.p2p.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Metadata header for zero-loss file streaming.
 * Transferred before raw binary payload over TCP socket.
 */
@Serializable
data class FileTransferPayload(
    val fileName: String,
    val fileSize: Long,
    val mimeType: String,
    val checksum: Long = 0L,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toJson(): String = Json.encodeToString(this)

    companion object {
        fun fromJson(jsonStr: String): FileTransferPayload =
            Json.decodeFromString(serializer(), jsonStr)
    }
}
