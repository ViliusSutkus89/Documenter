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
import androidx.room.*
import com.viliussutkus89.documenter.R
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

val State.stringRes: Int get() = when(this) {
    State.Error -> R.string.state_error
    State.Init -> R.string.state_init
    State.Caching -> R.string.state_caching
    State.Cached -> R.string.state_cached
    State.Converting -> R.string.state_converting
    State.Converted -> R.string.state_converted
}

@Entity(indices = [Index(value = ["last_accessed"])])
data class Document(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // example: myDocument.pdf
    val filename: String,

    @ColumnInfo(name = "source_uri")
    val sourceUri: Uri,

    // example: myDocument.html
    @ColumnInfo(name = "converted_filename")
    val convertedFilename: String,

    @ColumnInfo(name = "last_accessed")
    val lastAccessed: Long = Date().time,

    val state: State = State.Init,

    @ColumnInfo(name = "thumbnail_available")
    val thumbnailAvailable: Boolean = false,

    @ColumnInfo(name = "copy_protected")
    val copyProtected: Boolean = false
)


data class DocumentScoped_Filename_SourceUri_ConvertedFilename_State_CopyProtected(
    val id: Long,

    val filename: String,

    @ColumnInfo(name = "source_uri")
    val sourceUri: Uri,

    @ColumnInfo(name = "converted_filename")
    val convertedFilename: String,

    val state: State,

    @ColumnInfo(name = "copy_protected")
    val copyProtected: Boolean = false
)

class StateIntConverter {
    @TypeConverter
    fun fromState(value: State) = value.ordinal

    @TypeConverter
    fun toState(value: Int) = enumValues<State>()[value]
}

class UriStringConverter {
    @TypeConverter
    fun fromUri(value: String): Uri = if (value.isEmpty()) Uri.EMPTY else Uri.parse(value)

    @TypeConverter
    fun toUri(value: Uri): String = value.toString()
}

const val DOCUMENTS_DIR_IN_CACHE = "documents"
const val DOCUMENTS_DIR_IN_FILES = "documents"

fun getDocumentCacheDir(appCacheDir: File, documentId: Long): File {
    val cacheDir = File(appCacheDir, DOCUMENTS_DIR_IN_CACHE)
    return File(cacheDir, documentId.toString())
}

fun getDocumentFilesDir(appFilesDir: File, documentId: Long): File {
    val filesDir = File(appFilesDir, DOCUMENTS_DIR_IN_FILES)
    return File(filesDir, documentId.toString())
}

fun getCachedSourceFile(appCacheDir: File, documentId: Long, filename: String): File {
    val cacheDir = File(appCacheDir, DOCUMENTS_DIR_IN_CACHE)
    val documentDir = File(cacheDir, documentId.toString())
    return File(documentDir, filename)
}

fun getConvertedHtmlFile(appFilesDir: File, documentId: Long, convertedFilename: String): File {
    val filesDir = File(appFilesDir, DOCUMENTS_DIR_IN_FILES)
    val documentDir = File(filesDir, documentId.toString())
    return File(documentDir, convertedFilename)
}

fun getThumbnail(appCacheDir: File, documentId: Long): File {
    val cacheDir = File(appCacheDir, DOCUMENTS_DIR_IN_CACHE)
    val documentDir = File(cacheDir, documentId.toString())
    return File(documentDir, "screenshot.png")
}
