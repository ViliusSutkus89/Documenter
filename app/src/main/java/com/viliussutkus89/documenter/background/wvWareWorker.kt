/*
 * wvWareWorker.kt
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

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.preference.PreferenceManager
import androidx.work.Data
import androidx.work.WorkerParameters
import com.viliussutkus89.android.wvware.wvWare
import com.viliussutkus89.documenter.model.Document
import java.io.File
import java.io.IOException

class wvWareWorker(ctx: Context, params: WorkerParameters): ConverterWorkerCommon(ctx, params) {
    class RemoteWorkerService : androidx.work.multiprocess.RemoteWorkerService()

    companion object {
        private val TAG = "WorkerwvWare"
        private const val SETTING_KEY_NO_GRAPHICS = "setting_nographics"

        // https://filext.com/file-extension/DOC
        val SUPPORTED_MIME_TYPES = arrayOf(
            "application/msword",
            "application/doc",
            "appl/text",
            "application/vnd.msword",
            "application/vnd.ms-word",
            "application/winword",
            "application/word",
            "application/x-msw6",
            "application/x-msword"
        )

        fun generateConvertedFileName(inputFilename: String): String = inputFilename.removeSuffix(".doc") + ".html"

        fun buildInputData(document: Document, application: Application): Data {
            val preferences = PreferenceManager.getDefaultSharedPreferences(application)
            return commonDataBuilder(document, application)
                .putString(ARGUMENT_CLASS_NAME, RemoteWorkerService::class.java.name)
                .putBoolean(SETTING_KEY_NO_GRAPHICS, preferences.getBoolean("wvware_nographics", true))
                .build()
        }
    }

    override fun doWorkSync(inputFile: File): File? {
        return try {
            val converter = wvWare(applicationContext).setInputDOC(inputFile)
            if (inputData.getBoolean(SETTING_KEY_NO_GRAPHICS, false)) {
                converter.setNoGraphicsMode()
            }
            converter.convertToHTML()
        } catch (e: IOException) {
            Log.e(TAG, "Conversion failed")
            e.printStackTrace()
            null
        }
    }
}
