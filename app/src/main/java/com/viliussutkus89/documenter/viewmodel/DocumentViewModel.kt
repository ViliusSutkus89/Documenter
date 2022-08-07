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

import android.graphics.Bitmap
import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.*
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import com.viliussutkus89.documenter.DocumenterApplication
import com.viliussutkus89.documenter.background.SaveWorker
import com.viliussutkus89.documenter.model.State
import com.viliussutkus89.documenter.model.getConvertedHtmlFile
import com.viliussutkus89.documenter.model.getThumbnail
import com.viliussutkus89.documenter.utils.observeOnce
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream


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

    init {
        viewModelScope.launch(Dispatchers.IO) {
            documentDao.updateLastAccessed(documentId)
        }
    }

    var document = documentDao.getFilenameSourceUriConvertedFilenameStateCopyProtected(documentId).asLiveData()

    data class StateAndHtmlFile(
        val state: State,
        val htmlFile: File
    )
    val stateAndHtmlFile: LiveData<StateAndHtmlFile> = Transformations.distinctUntilChanged(
        Transformations.map(document) {
            StateAndHtmlFile(
                state = it.state,
                htmlFile = getConvertedHtmlFile(appFilesDir = app.filesDir, documentId, it.convertedFilename)
            )
        }
    )

    val state = Transformations.map(document) {
        it.state
    }

    val canReload: LiveData<Boolean> = Transformations.map(document) {
        if (!Uri.EMPTY.equals(it.sourceUri)) {
            try {
                app.contentResolver.openInputStream(it.sourceUri)
                true
            } catch (e: Exception) {
                false
            }
        } else {
            false
        }
    }

    val htmlFile: LiveData<File> = Transformations.map(document) {
        getConvertedHtmlFile(appFilesDir = app.filesDir, documentId, it.convertedFilename)
    }

    fun saveThumbnail(bitmap: Bitmap) {
        viewModelScope.launch(Dispatchers.IO) {
            FileOutputStream(getThumbnail(appCacheDir = app.cacheDir, documentId)).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 50, out)
            }
            documentDao.updateLastAccessedAndSetThumbnailAvailable(documentId)
        }
    }

    fun saveDocument(destinationUri: Uri) {
        document.observeOnce {
            val html = getConvertedHtmlFile(appFilesDir = app.filesDir, documentId, it.convertedFilename)
            WorkManager.getInstance(app).beginUniqueWork(
                "saveDocument-${documentId}",
                ExistingWorkPolicy.REPLACE,
                SaveWorker.oneTimeWorkRequestBuilder(html.toUri(), destinationUri).build()
            ).enqueue()
        }
    }
}
