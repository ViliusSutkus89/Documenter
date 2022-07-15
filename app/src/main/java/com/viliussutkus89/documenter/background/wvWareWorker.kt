/*
 * wvWareWorker.kt
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
import com.viliussutkus89.android.wvware.wvWare
import java.io.File
import java.io.IOException

class wvWareWorker(ctx: Context, params: WorkerParameters): RemoteListenableWorkerCommon(ctx, params) {
    class RemoteWorkerService : androidx.work.multiprocess.RemoteWorkerService()

    companion object {
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
        val INPUT_KEY_DOCUMENT_ID = RemoteListenableWorkerCommon.INPUT_KEY_DOCUMENT_ID
    }

    override fun doWorkSync(inputFile: File): File? {
        return try {
            val converter = wvWare(applicationContext).setInputDOC(inputFile)
            converter.convertToHTML()
        } catch (e: IOException) {
            e.printStackTrace()
            Log.e("wvWareWorker", "Conversion failed")
            null
        }
    }
}