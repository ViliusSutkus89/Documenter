/*
 * pdf2htmlEXWorker.kt
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
import androidx.preference.PreferenceManager
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkerParameters
import com.viliussutkus89.android.pdf2htmlex.pdf2htmlEX
import java.io.File
import java.io.IOException

class pdf2htmlEXWorker(ctx: Context, params: WorkerParameters): ConverterWorkerCommon(ctx, params) {
    class RemoteWorkerService : androidx.work.multiprocess.RemoteWorkerService()

    companion object {
        private const val TAG = "Workerpdf2htmlEX"
        private const val SETTING_KEY_OUTLINE = "setting_outline"
        private const val SETTING_KEY_DRM = "setting_drm"
        private const val SETTING_KEY_ANNOTATIONS = "setting_annotation"

        // https://filext.com/file-extension/PDF
        val SUPPORTED_MIME_TYPES = arrayOf(
            "application/pdf",
            "application/x-pdf",
            "application/acrobat",
            "application/vnd.pdf",
            "text/pdf",
            "text/x-pdf"
        )

        fun generateConvertedFileName(inputFilename: String): String = inputFilename.removeSuffix(".pdf") + ".html"

        fun oneTimeWorkRequestBuilder(cachedSourceFile: File, convertedHtmlFile: File, context: Context): OneTimeWorkRequest.Builder {
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            return OneTimeWorkRequestBuilder<pdf2htmlEXWorker>()
                .setInputData(
                    commonDataBuilder(cachedSourceFile, convertedHtmlFile, context.packageName)
                        .putString(ARGUMENT_CLASS_NAME, RemoteWorkerService::class.java.name)
                        .putBoolean(SETTING_KEY_OUTLINE, preferences.getBoolean("pdf2htmlex_outline", true))
                        .putBoolean(SETTING_KEY_DRM, preferences.getBoolean("pdf2htmlex_drm", true))
                        .putBoolean(SETTING_KEY_ANNOTATIONS, preferences.getBoolean("pdf2htmlex_annotations", true))
                        .build()
                )
                .addTag("ConvertWork")
        }
    }

    override fun doWorkSync(inputFile: File): File? {
        return try {
            val converter = pdf2htmlEX(applicationContext).setInputPDF(inputFile)
            converter.setOutline(inputData.getBoolean(SETTING_KEY_OUTLINE, false))
            converter.setDRM(inputData.getBoolean(SETTING_KEY_DRM, false))
            converter.setProcessAnnotation(inputData.getBoolean(SETTING_KEY_ANNOTATIONS, false))
            converter.convert()
        } catch (e: IOException) {
            Log.e(TAG, "Conversion failed")
            e.printStackTrace()
            null
        }
    }
}
