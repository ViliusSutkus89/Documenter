/*
 * DocumentViewModel.kt
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

package com.viliussutkus89.documenter.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.viliussutkus89.documenter.DocumenterApplication
import androidx.work.workDataOf
import com.viliussutkus89.documenter.background.SaveWorker
import com.viliussutkus89.documenter.model.DocumentDao
import com.viliussutkus89.documenter.model.DocumentScoped_Filename_ConvertedFilename
import com.viliussutkus89.documenter.model.getConvertedHtmlFile
import com.viliussutkus89.documenter.model.getScreenshotFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.*


class DocumentViewModel(private val app: DocumenterApplication, private val documentId: Long) : AndroidViewModel(app) {
    class Factory(
        private val application: DocumenterApplication,
        private val documentId: Long
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(DocumentViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return DocumentViewModel(application, documentId) as T
            }
            throw IllegalArgumentException("Unable to construct DocumentViewModel")
        }
    }

    private val documentDao = app.documentDatabase.documentDao()
    val document = documentDao.getFilenameConvertedFilename(documentId).asLiveData()

    val htmlFile: LiveData<File> = Transformations.map(document) {
        getConvertedHtmlFile(appFilesDir = context.filesDir, it.id, it.convertedFilename!!)
    }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            documentDao.updateLastAccessed(documentId)
        }
    }

    fun saveThumbnail(bitmap: Bitmap, appCacheDir: File) {
        viewModelScope.launch(Dispatchers.IO) {
            val screenshot = document.value?.getScreenshotFile(appCacheDir)
            FileOutputStream(screenshot).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 50, out)
            }
            documentDao.updateLastAccessedAndSetThumbnailAvailable(documentId)
        }
    }

    fun saveDocument(destinationUri: Uri) {
        WorkManager.getInstance(app).beginUniqueWork(
            "saveDocument-${documentId}",
            ExistingWorkPolicy.REPLACE,
            SaveWorker.oneTimeWorkRequestBuilder(htmlFile.value!!.toUri(), destinationUri).build()
        ).enqueue()
    }
}
