package com.icadd.records.ui

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns

/** Reads the display name of a picked file (falls back to the URI's last segment). */
fun queryFileName(context: Context, uri: Uri): String {
    var name = ""
    try {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && cursor.moveToFirst()) name = cursor.getString(idx) ?: ""
        }
    } catch (_: Exception) { }
    if (name.isBlank()) name = uri.lastPathSegment ?: "attachment"
    return name
}

fun readBytes(context: Context, uri: Uri): ByteArray? = try {
    context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
} catch (_: Exception) { null }
