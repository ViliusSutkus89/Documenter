/*
 * pdf2htmlEXWorker.kt
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
import android.util.Log
import androidx.work.WorkerParameters
import com.viliussutkus89.android.pdf2htmlex.pdf2htmlEX
import java.io.File
import java.io.IOException

class pdf2htmlEXWorker(ctx: Context, params: WorkerParameters): ConverterWorkerCommon(ctx, params) {
    class RemoteWorkerService : androidx.work.multiprocess.RemoteWorkerService()

    companion object {
        private const val TAG = "Workerpdf2htmlEX"

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
    }

    override fun doWorkSync(inputFile: File): File? {
        return try {
            val converter = pdf2htmlEX(applicationContext).setInputPDF(inputFile)
            converter.convert()
        } catch (e: IOException) {
            Log.e(TAG, "Conversion failed")
            e.printStackTrace()
            null
        }
    }
}
