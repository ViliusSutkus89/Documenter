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
import androidx.work.*
import java.io.IOException

class SaveWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {
    companion object {
        private const val INPUT_KEY_INPUT_URI = "INPUT_KEY_INPUT_URI"
        private const val INPUT_KEY_OUTPUT_URI = "INPUT_KEY_OUTPUT_URI"

        fun oneTimeWorkRequestBuilder(input: Uri, output: Uri): OneTimeWorkRequest.Builder {
            return OneTimeWorkRequestBuilder<SaveWorker>()
                .setInputData(
                    workDataOf(
                        INPUT_KEY_INPUT_URI to input.toString(),
                        INPUT_KEY_OUTPUT_URI to output.toString()
                    )
                )
        }
    }

    private val input = Uri.parse(inputData.getString(INPUT_KEY_INPUT_URI)
        ?: throw IllegalArgumentException("INPUT_KEY_INPUT_URI is null"))

    private val output = Uri.parse(inputData.getString(INPUT_KEY_OUTPUT_URI)
        ?: throw IllegalArgumentException("INPUT_KEY_OUTPUT_URI is null"))

    override fun doWork(): Result {
        val contentResolver = applicationContext.contentResolver
        try {
            contentResolver.openInputStream(input).use { inputStream ->
                contentResolver.openOutputStream(output).use { outputStream ->
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
