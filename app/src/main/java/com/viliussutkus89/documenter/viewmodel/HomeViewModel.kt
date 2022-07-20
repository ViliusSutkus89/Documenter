/*
 * HomeViewModel.kt
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

package com.viliussutkus89.documenter.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.lifecycle.*
import androidx.work.*
import androidx.work.multiprocess.RemoteListenableWorker.ARGUMENT_CLASS_NAME
import androidx.work.multiprocess.RemoteListenableWorker.ARGUMENT_PACKAGE_NAME
import com.viliussutkus89.documenter.background.CleanupCachedDocumentWorker
import com.viliussutkus89.documenter.background.SaveToCacheWorker
import com.viliussutkus89.documenter.background.pdf2htmlEXWorker
import com.viliussutkus89.documenter.background.wvWareWorker
import com.viliussutkus89.documenter.model.*
import com.viliussutkus89.documenter.utils.getFilename
import com.viliussutkus89.documenter.utils.getMimeType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HomeViewModel(application: Application, private val documentDao: DocumentDao) : AndroidViewModel(application) {
    class Factory(private val application: Application, private val documentDao: DocumentDao): ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return HomeViewModel(application, documentDao) as T
            }
            throw IllegalArgumentException("Unable to construct ViewModel")
        }
    }

    // app getter shortcut
    private val app get() = getApplication<Application>()

    val supportedMimeTypes = pdf2htmlEXWorker.SUPPORTED_MIME_TYPES + wvWareWorker.SUPPORTED_MIME_TYPES
    val documents: LiveData<List<Document>> = documentDao.getDocuments().asLiveData()

    // handle uri incoming through Intent only once
    private var intentUriHandled = false
    fun intentUriHandlerGate(): Boolean {
        val prev = intentUriHandled
        intentUriHandled = true
        return prev
    }

    fun removeDocument(document: Document) {
        viewModelScope.launch(Dispatchers.IO) {
            WorkManager.getInstance(app).cancelUniqueWork("document-${document.id}")
            document.getCachedDir(appCacheDir = app.cacheDir).deleteRecursively()
            document.getFilesDir(appFilesDir = app.filesDir).deleteRecursively()
            documentDao.delete(document)
        }
    }

    fun openDocument(uri: Uri): LiveData<Document> {
        val result = MutableLiveData<Document>()
        viewModelScope.launch(Dispatchers.IO) {
            val documentId = documentDao.insert(Document(
                filename = uri.getFilename(app.contentResolver) ?: "Unknown file",
                sourceUri = uri
            ))
            val document = documentDao.getDocument(documentId)
            result.postValue(document)

            document.getCachedDir(appCacheDir = app.cacheDir).mkdirs()
            document.getFilesDir(appFilesDir = app.filesDir).mkdirs()

            val saveToCacheWorkRequest = OneTimeWorkRequestBuilder<SaveToCacheWorker>()
                .setInputData(workDataOf(SaveToCacheWorker.INPUT_KEY_DOCUMENT_ID to documentId))
                .build()
            var continuation = WorkManager.getInstance(app)
                .beginUniqueWork("document-${documentId}", ExistingWorkPolicy.REPLACE, saveToCacheWorkRequest)

            val type = uri.getMimeType(app.contentResolver)
            if (pdf2htmlEXWorker.SUPPORTED_MIME_TYPES.contains(type)) {
                continuation = continuation
                    .then(OneTimeWorkRequestBuilder<pdf2htmlEXWorker>()
                        .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                        .setInputData(workDataOf(
                            ARGUMENT_PACKAGE_NAME to app.packageName,
                            ARGUMENT_CLASS_NAME to pdf2htmlEXWorker.RemoteWorkerService::class.java.name,
                            pdf2htmlEXWorker.INPUT_KEY_DOCUMENT_ID to documentId
                        ))
                        .build())
            } else if (wvWareWorker.SUPPORTED_MIME_TYPES.contains(type)) {
                continuation = continuation
                    .then(OneTimeWorkRequestBuilder<wvWareWorker>()
                        .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                        .setInputData(workDataOf(
                            ARGUMENT_PACKAGE_NAME to app.packageName,
                            ARGUMENT_CLASS_NAME to wvWareWorker.RemoteWorkerService::class.java.name,
                            wvWareWorker.INPUT_KEY_DOCUMENT_ID to documentId
                        ))
                        .build())
            } else {
                Log.e("HomeViewModel", "Failed to find appropriate worker. MIME Type='%s', URI='%s'".format(type, uri))
                documentDao.update(documentDao.getDocument(documentId).copy(state = State.Error))
                return@launch
            }

            continuation = continuation.then(OneTimeWorkRequestBuilder<SaveToCacheWorker>()
                .setInputData(workDataOf(CleanupCachedDocumentWorker.INPUT_KEY_DOCUMENT_ID to documentId))
                .build())

            continuation.enqueue()
        }
        return result
    }
}
