/*
 * Document.kt
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

package com.viliussutkus89.documenter.model

import android.net.Uri
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import java.io.File
import java.util.*

enum class State(val value: Int) {
    Error(-1),
    Init(0),
    Caching(100),
    Cached(150),
    Converting(200),
    Converted(300)
}

@Entity
data class Document(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // example: myDocument.pdf
    val filename: String,

    // example: myDocument.html
    @ColumnInfo(name = "converted_filename")
    val convertedFilename: String? = null,

    @ColumnInfo(name = "last_accessed")
    val lastAccessed: Long = Date().time,

    val state: State = State.Init,

    @ColumnInfo(name = "thumbnail_available")
    val thumbnailAvailable: Boolean = false
)

data class DocumentScoped_Filename_State(
    val id: Long,

    val filename: String,

    val state: State = State.Init
)

data class DocumentScoped_Filename_ConvertedFilename(
    val id: Long = 0,

    val filename: String,

    @ColumnInfo(name = "converted_filename")
    val convertedFilename: String? = null
)

class StateIntConverter {
    @TypeConverter
    fun fromState(value: State) = value.ordinal

    @TypeConverter
    fun toState(value: Int) = enumValues<State>()[value]
}

class UriStringConverter {
    @TypeConverter
    fun fromUri(value: String): Uri = value.let { Uri.parse(value) }

    @TypeConverter
    fun toUri(value: Uri): String = value.let { value.toString() }
}

const val DOCUMENTS_DIR_IN_CACHE = "documents"
const val DOCUMENTS_DIR_IN_FILES = "documents"

fun Document.getCachedDir(appCacheDir: File): File {
    val cacheDir = File(appCacheDir, DOCUMENTS_DIR_IN_CACHE)
    return File(cacheDir, id.toString())
}

fun Document.getFilesDir(appFilesDir: File): File {
    val filesDir = File(appFilesDir, DOCUMENTS_DIR_IN_FILES)
    return File(filesDir, id.toString())
}

fun Document.getCachedSourceFile(appCacheDir: File): File {
    val cacheDir = File(appCacheDir, DOCUMENTS_DIR_IN_CACHE)
    val documentDir = File(cacheDir, id.toString())
    return File(documentDir, filename)
}

fun Document.getConvertedHtmlFile(appFilesDir: File): File? {
    val filesDir = File(appFilesDir, DOCUMENTS_DIR_IN_FILES)
    val documentDir = File(filesDir, id.toString())
    return convertedFilename?.let { File(documentDir, convertedFilename) }
}

fun DocumentScoped_Filename_ConvertedFilename.getConvertedHtmlFile(appFilesDir: File): File? {
    val filesDir = File(appFilesDir, DOCUMENTS_DIR_IN_FILES)
    val documentDir = File(filesDir, id.toString())
    return convertedFilename?.let { File(documentDir, convertedFilename) }
}

fun DocumentScoped_Filename_ConvertedFilename.getScreenshotFile(appCacheDir: File): File {
    val cacheDir = File(appCacheDir, DOCUMENTS_DIR_IN_CACHE)
    val documentDir = File(cacheDir, id.toString())
    return File(documentDir, "screenshot.png")
}

fun Document.getThumbnailFile(appCacheDir: File): File? {
    return if (thumbnailAvailable) {
        val cacheDir = File(appCacheDir, DOCUMENTS_DIR_IN_CACHE)
        val documentDir = File(cacheDir, id.toString())
        File(documentDir, "screenshot.png")
    } else null
}

fun getCachedSourceFile(appCacheDir: File, documentId: Long, filename: String): File {
    val cacheDir = File(appCacheDir, DOCUMENTS_DIR_IN_CACHE)
    val documentDir = File(cacheDir, documentId.toString())
    return File(documentDir, filename)
}
