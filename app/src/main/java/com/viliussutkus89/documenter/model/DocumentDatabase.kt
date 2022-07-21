/*
 * DocumentDatabase.kt
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
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [Document::class], version = 1, exportSchema = false)
@TypeConverters(UriStringConverter::class, StateIntConverter::class)
abstract class DocumentDatabase: RoomDatabase() {
    abstract fun documentDao(): DocumentDao

    companion object {
        private const val DB_NAME = "document_database"
        private lateinit var INSTANCE: DocumentDatabase

        fun getDatabase(context: Context): DocumentDatabase {
            if (!::INSTANCE.isInitialized) {
                synchronized(DocumentDatabase::class.java) {
                    if (!::INSTANCE.isInitialized) {
                        INSTANCE = Room
                            .databaseBuilder(context.applicationContext, DocumentDatabase::class.java, DB_NAME)
                            .build()
                    }
                }
            }
            return INSTANCE
        }
    }
}
