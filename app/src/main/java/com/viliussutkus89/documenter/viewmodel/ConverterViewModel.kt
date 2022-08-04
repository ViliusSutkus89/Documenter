/*
 * ConverterViewModel.kt
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

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.*
import androidx.work.*
import com.viliussutkus89.documenter.background.*
import com.viliussutkus89.documenter.model.*
import com.viliussutkus89.documenter.utils.getFilename
import com.viliussutkus89.documenter.utils.getMimeType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class ConverterViewModel(private val app: DocumenterApplication) : AndroidViewModel(app) {
    companion object {
        val supportedMimeTypes = pdf2htmlEXWorker.SUPPORTED_MIME_TYPES + wvWareWorker.SUPPORTED_MIME_TYPES
        private const val TAG = "ConverterViewModel"
    }

    class Factory(private val application: DocumenterApplication): ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ConverterViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return ConverterViewModel(application) as T
            }
            throw IllegalArgumentException("Unable to construct ConverterViewModel")
        }
    }

    private val documentDao = app.documentDatabase.documentDao()
    private val workManager get() = WorkManager.getInstance(app)

    private val WorkInfo.documentId: Long get() = getDocumentIdFun()
    private fun WorkInfo.getDocumentIdFun(): Long {
        tags.forEach {
            if (it.startsWith("DocumentWork-")) {
                return it.removePrefix("DocumentWork-").toLong()
            }
        }
        return -1L
    }

    private val documentWorkInfoList: LiveData<List<WorkInfo>> = workManager.getWorkInfosByTagLiveData("DocumentWork")
    val workWatcher: LiveData<Unit> = Transformations.map(documentWorkInfoList) { workInfoListUnfiltered ->
        viewModelScope.launch(Dispatchers.IO) {
            workInfoListUnfiltered.filter { workInfo ->
                workInfo.documentId != -1L
            }.forEach { workInfo ->
                if (workInfo.state == WorkInfo.State.FAILED) {
                    documentDao.errorState(workInfo.documentId)
                }
                else if (workInfo.state == WorkInfo.State.RUNNING) {
                    if (workInfo.tags.contains("SaveToCacheWork")) {
                        documentDao.progressState(workInfo.documentId, State.Caching)
                    }
                    else if (workInfo.tags.contains("ConvertWork")) {
                        documentDao.progressState(workInfo.documentId, State.Converting)
                    }
                }
                else if (workInfo.state == WorkInfo.State.SUCCEEDED) {
                    if (workInfo.tags.contains("SaveToCacheWork")) {
                        documentDao.progressState(workInfo.documentId, State.Cached)
                    }
                    else if (workInfo.tags.contains("ConvertWork")) {
                        documentDao.progressState(workInfo.documentId, State.Converted)
                    }
                }
            }
        }
    }

    fun convertDocument(uri: Uri): LiveData<Document> {
        val result = MutableLiveData<Document>()
        viewModelScope.launch(Dispatchers.IO) {
            val type = uri.getMimeType(app.contentResolver)
            val filename = uri.getFilename(app.contentResolver) ?: "Unknown file"
            val convertedFilename = if (pdf2htmlEXWorker.SUPPORTED_MIME_TYPES.contains(type)) {
                pdf2htmlEXWorker.generateConvertedFileName(filename)
            } else if (wvWareWorker.SUPPORTED_MIME_TYPES.contains(type)) {
                wvWareWorker.generateConvertedFileName(filename)
            } else {
                Log.e(TAG, "Failed to get converted filename. MIME Type='%s', Uri='%s'".format(type, uri))
                filename
            }
            val documentId = documentDao.insert(Document(
                sourceUri = uri,
                filename = filename,
                convertedFilename = convertedFilename
            ))
            var document = documentDao.getDocument(documentId)
            result.postValue(document)

            document.getCachedDir(appCacheDir = app.cacheDir).mkdirs()
            document.getFilesDir(appFilesDir = app.filesDir).mkdirs()

            val cachedSourceFile = getCachedSourceFile(appCacheDir = app.cacheDir, document.id, document.filename)
            val saveToCacheWorkRequest = SaveToCacheWorker.oneTimeWorkRequestBuilder(document.sourceUri, cachedSourceFile)
                .addTag("DocumentWork-${document.id}")
                .addTag("SaveToCacheWork")
                .build()

            var continuation = workManager.beginUniqueWork("document-${documentId}", ExistingWorkPolicy.REPLACE, saveToCacheWorkRequest)

            val type = uri.getMimeType(app.contentResolver)

            val convertedFile = getConvertedHtmlFile(appFilesDir = app.filesDir, documentId, convertedFilename)
            val converterWorkRequestBuilder = if (pdf2htmlEXWorker.SUPPORTED_MIME_TYPES.contains(type)) {
                pdf2htmlEXWorker.oneTimeWorkRequestBuilder(cachedSourceFile, convertedFile, app)
            } else if (wvWareWorker.SUPPORTED_MIME_TYPES.contains(type)) {
                wvWareWorker.oneTimeWorkRequestBuilder(cachedSourceFile, convertedFile, app)
            } else {
                Log.e(TAG, "Failed to find appropriate worker. MIME Type='%s', Uri='%s'".format(type, uri))
                documentDao.errorState(id = documentId)
                return@launch
            }

            val converterWorkRequest = converterWorkRequestBuilder
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .addTag("DocumentWork")
                .addTag("DocumentWork-${document.id}")
                .addTag("ConvertWork")
                .build()

            continuation = continuation.then(converterWorkRequest)

            continuation = continuation.then(
                CleanupCachedDocumentWorker.oneTimeWorkRequestBuilder(cachedSourceFile).build()
            )

            continuation.enqueue()
        }
        return result
    }
}
