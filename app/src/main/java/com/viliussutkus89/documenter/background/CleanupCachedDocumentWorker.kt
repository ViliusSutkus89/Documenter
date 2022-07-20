/*
 * CleanupCachedDocumentWorker.kt
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

package com.viliussutkus89.documenter.background

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.viliussutkus89.documenter.model.DocumentDatabase
import com.viliussutkus89.documenter.model.getCachedSourceFile
import java.io.IOException

class CleanupCachedDocumentWorker(context: Context, params: WorkerParameters): Worker(context, params) {
    companion object {
        const val INPUT_KEY_DOCUMENT_ID = "key_id"
    }

    private val documentId = inputData.getLong(INPUT_KEY_DOCUMENT_ID, -1)
    private val documentDao by lazy { DocumentDatabase.getDatabase(applicationContext).documentDao() }

    override fun doWork(): Result {
        if (documentId == (-1).toLong()) {
            return Result.failure()
        }
        val document = documentDao.getDocument(documentId)
        val outputFile = document.getCachedSourceFile(appCacheDir = applicationContext.cacheDir)
        try {
            outputFile.delete()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return Result.success()
    }
}
