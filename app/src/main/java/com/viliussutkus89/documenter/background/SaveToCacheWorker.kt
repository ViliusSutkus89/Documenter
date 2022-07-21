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

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class SaveToCacheWorker(context: Context, params: WorkerParameters): Worker(context, params) {
    companion object {
        private const val TAG = "WorkerSaveToCache"
    }

    private val inputUri = Uri.parse(inputData.getString(DATA_KEY_INPUT_URI)
        ?: throw IllegalArgumentException("DATA_KEY_INPUT_URI is null"))

    private val cachedFile = File(inputData.getString(DATA_KEY_CACHED_FILE)
        ?: throw IllegalArgumentException("DATA_KEY_CACHED_FILE is null"))

    private val cr by lazy { applicationContext.contentResolver }

    override fun doWork(): Result {
        val permissionTook = takePermission(inputUri)
        val copyResult = copyFromUriToFile(inputUri, cachedFile)
        if (permissionTook) { releasePermission(inputUri) }
        return if (copyResult) { Result.success() } else { Result.failure() }
    }

    private fun takePermission(uri: Uri): Boolean {
        if (uri.scheme == "content") {
            if (applicationContext.checkUriPermission(uri, android.os.Process.myPid(), android.os.Process.myUid(), Intent.FLAG_GRANT_READ_URI_PERMISSION) == PackageManager.PERMISSION_GRANTED) {
                try {
                    cr.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    return true
                } catch (se: SecurityException) {
                }
            }
        }
        return false
    }

    private fun releasePermission(uri: Uri) {
        if (uri.scheme == "content") {
            cr.releasePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
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
