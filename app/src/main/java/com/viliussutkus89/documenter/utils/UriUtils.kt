/*
 * UriUtils.kt
 *
 * Copyright (C) 2022 ViliusSutkus89.com
 *
 * Documenter is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.viliussutkus89.documenter.utils

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.net.toFile
import com.viliussutkus89.documenter.background.pdf2htmlEXWorker
import com.viliussutkus89.documenter.background.wvWareWorker
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

fun Uri.getMimeType(contentResolver: ContentResolver): String? {
    return when (scheme) {
        // Should look at file "magic" prefix to determine the actual MIME type
        "file" -> {
            when (toFile().name.substringAfterLast(".")) {
                "pdf" -> pdf2htmlEXWorker.SUPPORTED_MIME_TYPES[0]
                "doc" -> wvWareWorker.SUPPORTED_MIME_TYPES[0]
                else -> ""
            }
        }
        else -> contentResolver.getType(this)
    }
}
