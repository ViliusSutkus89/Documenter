/*
 * SaveWorker.java
 *
 * Copyright (C) 2021 - 2022 Vilius Sutkus'89 - ViliusSutkus89.com
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
 *
 * Migrated from pdf2htmlEX-Android sample app
 * https://github.com/ViliusSutkus89/pdf2htmlEX-Android/blob/2bd1b17004e9101087a8be564572d43bb402f878/application/app/src/main/java/com/viliussutkus89/android/pdf2htmlex/application/SaveWorker.java
 */

package com.viliussutkus89.documenter.background;

import android.content.Context
import android.net.Uri
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.io.IOException

class SaveWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {
    companion object {
        const val INPUT_KEY_INPUT_URI = "INPUT_KEY_INPUT_URI"
        const val INPUT_KEY_OUTPUT_URI = "INPUT_KEY_OUTPUT_URI"
    }

    override fun doWork(): Result {
        val inputUriStr = inputData.getString(INPUT_KEY_INPUT_URI)
        val outputUriStr = inputData.getString(INPUT_KEY_OUTPUT_URI)
        if (null == inputUriStr || null == outputUriStr) {
            return Result.failure()
        }

        val inputUri = Uri.parse(inputUriStr)
        val outputUri = Uri.parse(outputUriStr)

        val contentResolver = applicationContext.contentResolver
        try {
            contentResolver.openInputStream(inputUri).use { inputStream ->
                contentResolver.openOutputStream(outputUri).use { outputStream ->
                    val readBufferSize = 2048
                    val buffer = ByteArray(readBufferSize)
                    var didRead: Int
                    while (-1 != inputStream!!.read(buffer, 0, readBufferSize).also { didRead = it }) {
                        outputStream!!.write(buffer, 0, didRead)
                    }
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
            return Result.failure()
        }
        return Result.success()
    }
}
