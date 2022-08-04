/*
 * DocumentDao.kt
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
import kotlinx.coroutines.flow.Flow

// Now() unavailable in SQLite
// unixepoch() added only recently
// strftime('%s') is current timestamp
private const val CURRENT_TIMESTAMP = "strftime('%s')"

@Dao
interface DocumentDao {
    @Query("SELECT * FROM `document` ORDER BY `last_accessed` DESC")
    fun getDocuments(): Flow<List<Document>>

    @Query("SELECT * FROM `document` WHERE `id` = :id")
    fun getDocument(id: Long): Document

    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun insert(document: Document): Long

    @Delete
    fun delete(document: Document)

    @Query("SELECT `id`, `filename`, `converted_filename`, `source_uri`, `state` FROM `document` WHERE `id` = :id")
    fun getFilenameSourceUriConvertedFilenameState(id: Long): Flow<DocumentScoped_Filename_SourceUri_ConvertedFilename_State>

    @Query("UPDATE `document` SET `last_accessed` = $CURRENT_TIMESTAMP WHERE `id` = :id")
    fun updateLastAccessed(id: Long)

    @Query("UPDATE `document` SET `last_accessed` = $CURRENT_TIMESTAMP, `thumbnail_available` = 1 WHERE `id` = :id")
    fun updateLastAccessedAndSetThumbnailAvailable(id: Long)

    @Query("UPDATE `document` SET `state` = :state WHERE `id` = :id AND `state` < :state AND `state` != :internal_errorState")
    fun progressState(id: Long, state: State, internal_errorState: State = State.Error)

    @Query("UPDATE `document` SET `state` = :internal_errorState WHERE `id` = :id")
    fun errorState(id: Long, internal_errorState: State = State.Error)

    @Query("Update `document` SET `state` = :internal_initState, `thumbnail_available` = 0 WHERE `id` = :id")
    fun reloadDocument(id: Long, internal_initState: State = State.Init)

    @Query("UPDATE `document` SET `converted_filename` = :convertedFilename WHERE `id` = :id")
    fun updateConvertedFilename(id: Long, convertedFilename: String)

    @Query("SELECT CASE WHEN COUNT(`source_uri`) = 1 THEN 1 ELSE 0 END FROM `document` WHERE `source_uri` = :uri")
    fun isUriUnique(uri: Uri): Boolean
}
