package com.viliussutkus89.documenter.utils

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File

// https://developer.android.com/training/secure-file-sharing/retrieve-info
fun Uri.getFilename(contentResolver: ContentResolver): String? {
    if ("file" == scheme) {
        return path?.let { File(it).name }
    } else {
        contentResolver.query(this, null, null, null, null).use { crs ->
            crs?.let {
                crs.moveToFirst()
                val nameIndex = crs.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0)
                    return crs.getString(nameIndex)
            }
        }
        return null
    }
}
