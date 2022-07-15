/*
 * SaveToCacheWorker.kt
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

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.viliussutkus89.documenter.model.DocumentDatabase
import com.viliussutkus89.documenter.model.State
import com.viliussutkus89.documenter.model.getCachedSourceFile
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class SaveToCacheWorker(context: Context, params: WorkerParameters): Worker(context, params) {
    companion object {
        const val INPUT_KEY_DOCUMENT_ID = "key_id"
    }

    private val documentId = inputData.getLong(INPUT_KEY_DOCUMENT_ID, -1)
    private val documentDao by lazy { DocumentDatabase.getDatabase(applicationContext).documentDao() }

    override fun doWork(): Result {
        if (documentId == (-1).toLong()) {
            return Result.failure()
        }
        val document = documentDao.getDocument(documentId).copy(state = State.Caching)
        documentDao.update(document)

        val cached = document.getCachedSourceFile(applicationContext)
        if (!copyFromUriToFile(applicationContext.contentResolver, document.sourceUri, cached)){
            documentDao.update(document.copy(state = State.Error))
            return Result.failure()
        }

        documentDao.update(document.copy(state = State.Cached))
        return Result.success()
    }

    private fun copyFromUriToFile(contentResolver: ContentResolver, inputUri: Uri, outputFile: File): Boolean {
        try {
            contentResolver.openInputStream(inputUri).use { inputStream ->
                if (null == inputStream) {
                    return false
                }
                FileOutputStream(outputFile).use { outputStream ->
                    val readBufferSize = 2048
                    val buffer = ByteArray(readBufferSize)
                    var didRead: Int
                    while (-1 != inputStream.read(buffer, 0, readBufferSize).also { didRead = it }) {
                        outputStream.write(buffer, 0, didRead)
                    }
                }
            }
        } catch (e: IOException) {
            if (outputFile.exists()) {
                outputFile.delete()
            }
            return false
        }
        return true
    }
}
