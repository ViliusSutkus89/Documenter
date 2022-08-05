/*
 * DocumentDatabase.kt
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

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Document::class], version = 2, exportSchema = true)
@TypeConverters(UriStringConverter::class, StateIntConverter::class)
abstract class DocumentDatabase: RoomDatabase() {
    abstract fun documentDao(): DocumentDao

    companion object {
        private const val DB_NAME = "document_database"
        private lateinit var INSTANCE: DocumentDatabase

        private val migration_1_to_2 = object: Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // add NOT NULL constraint on converted_filename
                // add source_uri
                // add copy_protected
                db.execSQL("UPDATE `document` SET `converted_filename` = '' WHERE `converted_filename` IS NULL")
                db.execSQL("CREATE TABLE `document_2` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `filename` TEXT NOT NULL, `source_uri` TEXT NOT NULL, `converted_filename` TEXT NOT NULL, `last_accessed` INTEGER NOT NULL, `state` INTEGER NOT NULL, `thumbnail_available` INTEGER NOT NULL, `copy_protected` INTEGER NOT NULL)")
                db.execSQL("INSERT INTO `document_2` (`id`, `filename`, `source_uri`, `converted_filename`, `last_accessed`, `state`, `thumbnail_available`, `copy_protected`) SELECT `id`, `filename`,  '', `converted_filename`, `last_accessed`, `state`, `thumbnail_available`, 0 FROM `document`")
                db.execSQL("DROP TABLE `document`")
                db.execSQL("ALTER TABLE `document_2` RENAME TO `document`")
                // Add index on last_accessed
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_Document_last_accessed` ON `document` (`last_accessed`)")
            }
        }

        fun getDatabase(context: Context): DocumentDatabase {
            if (!::INSTANCE.isInitialized) {
                synchronized(DocumentDatabase::class.java) {
                    if (!::INSTANCE.isInitialized) {
                        INSTANCE = Room
                            .databaseBuilder(context.applicationContext, DocumentDatabase::class.java, DB_NAME)
                            .addMigrations(migration_1_to_2)
                            .build()
                    }
                }
            }
            return INSTANCE
        }
    }
}
