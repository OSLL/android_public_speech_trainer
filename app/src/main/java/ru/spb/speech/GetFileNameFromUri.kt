package ru.spb.speech

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns

@SuppressLint("Recycle")
fun getFileName(uri: Uri, cr: ContentResolver): String {
    var result: String? = null
    if (uri.scheme == "content") {
        val cursor = cr.query(uri, null, null, null, null)
        try {
            if (cursor != null && cursor.moveToFirst()) {
                result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
            }
        } finally {
            cursor!!.close()
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result!!.lastIndexOf('/')
        if (cut != -1) {
            result = result.substring(cut + 1)
        }
    }
    return result.substring(0, result.indexOf(".pdf"))
}