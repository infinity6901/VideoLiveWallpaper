package com.infinity.videowallpaper.data

import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File

/**
 * Handles safe SAF metadata queries, persistable URI permission lifecycle,
 * and optional/fallback copying into app-private storage so gallery deletes
 * or non-persistable providers never break the live wallpaper.
 */
object VideoStorageHelper {

    private const val LOCAL_DIR = "wallpapers"

    fun queryDisplayName(context: Context, uri: Uri): String {
        return runCatching {
            context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null,
            )?.use { cursor ->
                val colIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (colIdx >= 0 && cursor.moveToFirst()) {
                    cursor.getString(colIdx)?.takeIf { it.isNotBlank() }
                } else {
                    null
                }
            }
        }.getOrNull()
            ?: uri.lastPathSegment?.substringAfterLast('/')?.takeIf { it.isNotBlank() }
            ?: "video"
    }

    fun queryDurationMs(context: Context, uri: Uri): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()
                ?.coerceAtLeast(0L)
                ?: 0L
        } catch (_: Exception) {
            0L
        } finally {
            runCatching { retriever.release() }
        }
    }

    /**
     * Tries to take a persistable read URI permission. Returns true if granted.
     */
    fun takePersistableReadPermission(context: Context, uri: Uri): Boolean {
        if (uri.scheme != "content") return true
        return try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
            true
        } catch (_: SecurityException) {
            false
        }
    }

    /**
     * Releases a previously held persistable URI permission (or deletes a local copy)
     * when a video is removed and no longer referenced by the playlist.
     */
    fun releaseVideoResource(context: Context, uriString: String?, isLocalCopy: Boolean) {
        if (uriString.isNullOrBlank()) return
        val uri = runCatching { Uri.parse(uriString) }.getOrNull() ?: return
        if (isLocalCopy && uri.scheme == "file") {
            uri.path?.let { path ->
                val target = File(path)
                val allowedDir = File(context.filesDir, LOCAL_DIR)
                if (target.canonicalPath.startsWith(allowedDir.canonicalPath)) {
                    runCatching { target.delete() }
                }
            }
        } else if (uri.scheme == "content") {
            runCatching {
                context.contentResolver.releasePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
        }
    }

    /**
     * Copies the video from [sourceUri] into private app storage.
     * Returns a `file://` URI string on success, or null if copying fails.
     */
    fun copyToLocalStorage(context: Context, sourceUri: Uri, displayName: String): String? {
        return runCatching {
            val dir = File(context.filesDir, LOCAL_DIR).apply { mkdirs() }
            val ext = displayName.substringAfterLast('.', "mp4")
                .filter { it.isLetterOrDigit() }
                .take(5)
                .ifBlank { "mp4" }
            val destFile = File(dir, "wp_${System.currentTimeMillis()}.$ext")
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output, bufferSize = 64 * 1024)
                }
            } ?: return null
            if (destFile.length() == 0L) {
                destFile.delete()
                return null
            }
            Uri.fromFile(destFile).toString()
        }.getOrNull()
    }
}
