/*
 * CleanupCachedDocumentWorker.kt
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
import android.util.Log
import androidx.work.*
import java.io.File
import java.io.IOException

class CleanupCachedDocumentWorker(context: Context, params: WorkerParameters): Worker(context, params) {
    companion object {
        private const val TAG = "WorkerCleanup"

        fun oneTimeWorkRequestBuilder(cachedFile: File): OneTimeWorkRequest.Builder {
            return OneTimeWorkRequestBuilder<CleanupCachedDocumentWorker>()
                .setInputData(workDataOf(DATA_KEY_CACHED_FILE to cachedFile.path))
        }
    }

    private val cachedFile = File(inputData.getString(DATA_KEY_CACHED_FILE)
        ?: throw IllegalArgumentException("DATA_KEY_CACHED_FILE is null"))

    override fun doWork(): Result {
        return try {
            cachedFile.delete()
            Result.success()
        } catch (e: IOException) {
            Log.e(TAG, "Failed to cleanup cached document")
            e.printStackTrace()
            Result.failure()
        }
    }
}
