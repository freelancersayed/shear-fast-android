package com.flashshare.p2p.data.repository

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import com.flashshare.p2p.data.model.FileTransferPayload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Model representing a received file displayed in the Files History tab.
 */
data class ReceivedFileInfo(
    val name: String,
    val size: Long,
    val lastModified: Long,
    val uri: Uri,
    val mimeType: String,
    val filePath: String = ""
)

/**
 * FileStorageRepository handles zero-loss file reads and writes using Android Scoped Storage.
 * Saves received files into 'Downloads/BondhuShare'.
 * Guarantees 100% bit-exact transmission without transcoding, recompression, or metadata stripping.
 */
class FileStorageRepository(private val context: Context) {

    private val contentResolver: ContentResolver = context.contentResolver

    /**
     * Resolves display name, size, and mime type from a content URI.
     */
    suspend fun getFileMetadata(uri: Uri): FileTransferPayload = withContext(Dispatchers.IO) {
        val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"
        var resolvedName: String? = null
        var fileSize = 0L

        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)

            if (cursor.moveToFirst()) {
                if (nameIndex != -1 && !cursor.isNull(nameIndex)) {
                    resolvedName = cursor.getString(nameIndex)
                }
                if (sizeIndex != -1 && !cursor.isNull(sizeIndex)) {
                    fileSize = cursor.getLong(sizeIndex)
                }
            }
        }

        // Generate clean fallback name if display name query was empty
        val finalFileName = if (!resolvedName.isNullOrBlank()) {
            resolvedName!!
        } else {
            val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: "dat"
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            "file_$timestamp.$extension"
        }

        // Secondary fallback to measure file size if cursor was null
        if (fileSize <= 0L) {
            runCatching {
                contentResolver.openAssetFileDescriptor(uri, "r")?.use { afd ->
                    fileSize = afd.length
                }
            }
        }

        FileTransferPayload(
            fileName = finalFileName,
            fileSize = fileSize,
            mimeType = mimeType
        )
    }

    /**
     * Opens a raw byte stream directly from the selected URI.
     */
    fun openInputStream(uri: Uri): InputStream {
        return contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("Unable to open stream for URI: $uri")
    }

    /**
     * Creates an original-quality destination target inside 'Downloads/BondhuShare'.
     */
    suspend fun createDestinationTarget(
        fileName: String,
        mimeType: String,
        fileSize: Long
    ): DestinationTarget = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/BondhuShare")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
                if (fileSize > 0) {
                    put(MediaStore.MediaColumns.SIZE, fileSize)
                }
            }

            val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val itemUri = contentResolver.insert(collection, contentValues)
                ?: throw IllegalStateException("Failed to create destination in MediaStore")

            val outputStream = contentResolver.openOutputStream(itemUri, "w")
                ?: throw IllegalStateException("Failed to open output stream for: $itemUri")

            DestinationTarget.ScopedMediaStoreTarget(itemUri, outputStream, contentResolver)
        } else {
            // Legacy Storage Fallback for Android 9 and lower
            val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val targetDir = File(downloadDir, "BondhuShare").apply {
                if (!exists()) mkdirs()
            }
            var targetFile = File(targetDir, fileName)
            var counter = 1
            val baseName = fileName.substringBeforeLast(".")
            val extension = if (fileName.contains(".")) ".${fileName.substringAfterLast(".")}" else ""

            while (targetFile.exists()) {
                targetFile = File(targetDir, "${baseName}_$counter$extension")
                counter++
            }

            val outputStream = FileOutputStream(targetFile)
            DestinationTarget.LegacyFileTarget(targetFile, outputStream, context)
        }
    }

    /**
     * Retrieves all files received in 'Downloads/BondhuShare' (and legacy 'Downloads/FlashShare').
     */
    suspend fun getReceivedFiles(): List<ReceivedFileInfo> = withContext(Dispatchers.IO) {
        val fileList = mutableListOf<ReceivedFileInfo>()

        // 1. Query Android MediaStore Downloads collection (Android 10+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                val projection = arrayOf(
                    MediaStore.MediaColumns._ID,
                    MediaStore.MediaColumns.DISPLAY_NAME,
                    MediaStore.MediaColumns.SIZE,
                    MediaStore.MediaColumns.DATE_MODIFIED,
                    MediaStore.MediaColumns.MIME_TYPE,
                    MediaStore.MediaColumns.RELATIVE_PATH
                )
                val selection = "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ? OR ${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?"
                val selectionArgs = arrayOf("%BondhuShare%", "%FlashShare%")
                val sortOrder = "${MediaStore.MediaColumns.DATE_MODIFIED} DESC"

                contentResolver.query(collection, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                    val nameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                    val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                    val dateCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
                    val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)

                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idCol)
                        val name = cursor.getString(nameCol) ?: "Unknown File"
                        val size = cursor.getLong(sizeCol)
                        val dateSec = cursor.getLong(dateCol)
                        val mime = cursor.getString(mimeCol) ?: "application/octet-stream"
                        val contentUri = ContentUris.withAppendedId(collection, id)

                        fileList.add(
                            ReceivedFileInfo(
                                name = name,
                                size = size,
                                lastModified = dateSec * 1000L,
                                uri = contentUri,
                                mimeType = mime
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error querying MediaStore for BondhuShare files", e)
            }
        }

        // 2. Direct File System Scan in Download/BondhuShare & Download/FlashShare
        val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val bondhuDir = File(downloadDir, "BondhuShare")
        val flashDir = File(downloadDir, "FlashShare")

        val targetFolders = listOf(bondhuDir, flashDir)
        for (folder in targetFolders) {
            if (folder.exists() && folder.isDirectory) {
                folder.listFiles()?.forEach { file ->
                    if (file.isFile && file.length() > 0) {
                        // Avoid duplicates if already queried via MediaStore
                        if (fileList.none { it.name == file.name }) {
                            val mime = getMimeType(file.name)
                            val fileUri = try {
                                FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    file
                                )
                            } catch (e: Exception) {
                                Uri.fromFile(file)
                            }

                            fileList.add(
                                ReceivedFileInfo(
                                    name = file.name,
                                    size = file.length(),
                                    lastModified = file.lastModified(),
                                    uri = fileUri,
                                    mimeType = mime,
                                    filePath = file.absolutePath
                                )
                            )
                        }
                    }
                }
            }
        }

        fileList.sortedByDescending { it.lastModified }
    }

    /**
     * Opens or plays a received file using the device's default application.
     */
    fun openReceivedFile(file: ReceivedFileInfo): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(file.uri, file.mimeType.ifBlank { "*/*" })
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            try {
                val fallbackIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(file.uri, "*/*")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
                true
            } catch (e2: Exception) {
                Log.e(TAG, "Failed to open file: ${file.name}", e2)
                false
            }
        }
    }

    private fun getMimeType(fileName: String): String {
        val extension = fileName.substringAfterLast('.', "")
        return if (extension.isNotEmpty()) {
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase(Locale.ROOT))
                ?: "application/octet-stream"
        } else {
            "application/octet-stream"
        }
    }

    companion object {
        private const val TAG = "FileStorageRepository"
    }
}

/**
 * Encapsulates the destination output stream and completion lifecycle.
 */
sealed class DestinationTarget(val outputStream: OutputStream) {

    abstract fun finish()
    abstract fun discard()
    abstract val resultUriString: String

    class ScopedMediaStoreTarget(
        val uri: Uri,
        outputStream: OutputStream,
        private val contentResolver: ContentResolver
    ) : DestinationTarget(outputStream) {

        override fun finish() {
            try {
                outputStream.flush()
                outputStream.close()
            } catch (e: Exception) {
                Log.e("DestinationTarget", "Stream close error", e)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.IS_PENDING, 0)
                }
                contentResolver.update(uri, values, null, null)
            }
        }

        override fun discard() {
            try { outputStream.close() } catch (ignored: Exception) {}
            contentResolver.delete(uri, null, null)
        }

        override val resultUriString: String
            get() = uri.toString()
    }

    class LegacyFileTarget(
        val file: File,
        outputStream: OutputStream,
        private val context: Context
    ) : DestinationTarget(outputStream) {

        override fun finish() {
            try {
                outputStream.flush()
                outputStream.close()
            } catch (e: Exception) {
                Log.e("DestinationTarget", "Stream close error", e)
            }
            MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), null, null)
        }

        override fun discard() {
            try { outputStream.close() } catch (ignored: Exception) {}
            if (file.exists()) file.delete()
        }

        override val resultUriString: String
            get() = file.absolutePath
    }
}
