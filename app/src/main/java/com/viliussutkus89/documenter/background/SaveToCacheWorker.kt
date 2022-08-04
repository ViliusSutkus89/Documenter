/*
 * SaveToCacheWorker.kt
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

package com.viliussutkus89.documenter.background

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.work.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


class SaveToCacheWorker(context: Context, params: WorkerParameters): Worker(context, params) {
    companion object {
        private const val TAG = "WorkerSaveToCache"
        private const val workTag = "SaveToCacheWork"

        fun oneTimeWorkRequestBuilder(sourceUri: Uri, cachedFile: File): OneTimeWorkRequest.Builder {
            return OneTimeWorkRequestBuilder<SaveToCacheWorker>()
                .addTag(workTag)
                .setInputData(
                    workDataOf(
                        DATA_KEY_INPUT_URI to sourceUri.toString(),
                        DATA_KEY_CACHED_FILE to cachedFile.path
                    )
                )
        }
    }

    private val inputUri = Uri.parse(inputData.getString(DATA_KEY_INPUT_URI)
        ?: throw IllegalArgumentException("DATA_KEY_INPUT_URI is null"))

    private val cachedFile = File(inputData.getString(DATA_KEY_CACHED_FILE)
        ?: throw IllegalArgumentException("DATA_KEY_CACHED_FILE is null"))

    private val cr by lazy { applicationContext.contentResolver }

    override fun doWork(): Result {
        return if (copyFromUriToFile(inputUri, cachedFile)) { Result.success() } else { Result.failure() }
    }

    private fun copyFromUriToFile(inputUri: Uri, outputFile: File): Boolean {
        try {
            cr.openInputStream(inputUri).use { inputStream ->
                if (null == inputStream) {
                    Log.e(TAG, "Failed to open input Uri")
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
            Log.e(TAG, "Failed to cache document")
            e.printStackTrace()
            if (outputFile.exists()) {
                outputFile.delete()
            }
            return false
        }
        return true
    }
}
