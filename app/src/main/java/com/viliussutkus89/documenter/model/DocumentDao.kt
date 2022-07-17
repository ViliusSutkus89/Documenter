/*
 * DocumentDao.kt
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

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {
    @Query("SELECT * FROM `document` ORDER BY `last_accessed` DESC")
    fun getDocuments(): Flow<List<Document>>

    @Query("SELECT * FROM `document` WHERE id = :id")
    fun getDocument(id: Long): Document

    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun insert(document: Document): Long

    @Update
    fun update(document: Document)

    @Delete
    fun delete(document: Document)

    @Query("SELECT `id`, `filename`, `state` FROM `document` WHERE `id` = :id")
    fun getFilenameAndState(id: Long): Flow<DocumentScoped_Filename_State>

    @Query("SELECT `id`, `filename`, `converted_filename` FROM `document` WHERE `id` = :id")
    fun getFilenameConvertedFilename(id: Long): Flow<DocumentScoped_Filename_ConvertedFilename>

    @Query("UPDATE `document` SET `last_accessed` = :lastAccessed WHERE `id` = :id")
    fun updateLastAccessed(id: Long, lastAccessed: Long)
}
