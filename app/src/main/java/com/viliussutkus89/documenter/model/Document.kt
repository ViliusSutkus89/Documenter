/*
 * Document.kt
 *
 * Copyright (C) 2022 ViliusSutkus89.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
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

import android.content.Context
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

    // example: /sdcard/Downloads/myDocument.pdf
    val sourceUri: Uri,

    // example: myDocument.pdf
    val filename: String,

    // example: myDocument.html
    @ColumnInfo(name = "converted_filename")
    val convertedFilename: String? = "",

    @ColumnInfo(name = "last_accessed")
    val lastAccessed: Long = Date().time,

    val state: State = State.Init
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

fun Document.getCachedDir(cacheDir: File): File {
    return File(File(cacheDir, DOCUMENTS_DIR_IN_CACHE), id.toString())
}

fun Document.getFilesDir(filesDir: File): File {
    return File(File(filesDir, DOCUMENTS_DIR_IN_CACHE), id.toString())
}

fun Document.getCachedSourceFile(context: Context): File {
    return File(getCachedDir(context.cacheDir), filename)
}

fun Document.getConvertedHtmlFile(context: Context): File? {
    return convertedFilename?.let {
        File(getFilesDir(context.filesDir), convertedFilename)
    }
}
