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

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.*
import androidx.work.ExistingWorkPolicy
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.viliussutkus89.documenter.DocumenterApplication
import com.viliussutkus89.documenter.background.*
import com.viliussutkus89.documenter.model.*
import com.viliussutkus89.documenter.ui.DocumentFragment
import com.viliussutkus89.documenter.utils.getFilename
import com.viliussutkus89.documenter.utils.getMimeType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class ConverterViewModel(private val app: DocumenterApplication) : AndroidViewModel(app) {
    companion object {
        private const val TAG = "ConverterViewModel"
        val supportedMimeTypes = pdf2htmlEXWorker.SUPPORTED_MIME_TYPES + wvWareWorker.SUPPORTED_MIME_TYPES
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

    private val WorkInfo.documentId: Long
        get() = tags.find { it.startsWith("DocumentWork-") }?.removePrefix("DocumentWork-")
            ?.toLong() ?: -1L

    private val documentWorkInfoList: LiveData<List<WorkInfo>> = workManager.getWorkInfosByTagLiveData("DocumentWork")
    val workWatcher: LiveData<Unit> = Transformations.map(documentWorkInfoList) { workInfoListUnfiltered ->
        viewModelScope.launch(Dispatchers.IO) {
            workInfoListUnfiltered.filter { workInfo ->
                workInfo.documentId != -1L
            }.forEach { workInfo ->
                if (workInfo.state == WorkInfo.State.FAILED) {
                    Log.d(TAG, "errorState(${workInfo.documentId})")
                    documentDao.errorState(workInfo.documentId)
                }
                else if (workInfo.state == WorkInfo.State.RUNNING) {
                    if (workInfo.tags.contains("SaveToCacheWork")) {
                        Log.d(TAG, "progressState(${workInfo.documentId}, Caching)")
                        documentDao.progressState(workInfo.documentId, State.Caching)
                    }
                    else if (workInfo.tags.contains("ConvertWork")) {
                        Log.d(TAG, "progressState(${workInfo.documentId}, Converting)")
                        documentDao.progressState(workInfo.documentId, State.Converting)
                    }
                }
                else if (workInfo.state == WorkInfo.State.SUCCEEDED) {
                    if (workInfo.tags.contains("SaveToCacheWork")) {
                        Log.d(TAG, "progressState(${workInfo.documentId}, Cached)")
                        documentDao.progressState(workInfo.documentId, State.Cached)
                    }
                    else if (workInfo.tags.contains("ConvertWork")) {
                        if (workInfo.outputData.getBoolean(DATA_KEY_DOCUMENT_COPY_PROTECTED, false)) {
                            Log.d(TAG, "markDocumentAsCopyProtected(${workInfo.documentId})")
                            documentDao.markDocumentAsCopyProtected(workInfo.documentId)
                        }
                        Log.d(TAG, "progressState(${workInfo.documentId}, Converted)")
                        documentDao.progressState(workInfo.documentId, State.Converted)
                    }
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    private fun takePermission(uri: Uri) {
        if (app.checkUriPermission(uri, android.os.Process.myPid(), android.os.Process.myUid(), Intent.FLAG_GRANT_READ_URI_PERMISSION) == PackageManager.PERMISSION_GRANTED) {
            try {
                Log.d(TAG, "takePersistableUriPermission($uri)")
                app.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (se: SecurityException) {
            }
        }
    }

    fun convert(uri: Uri): LiveData<Document> {
        val result = MutableLiveData<Document>()
        viewModelScope.launch(Dispatchers.IO) {
            Log.d(TAG, "convert($uri)")
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                takePermission(uri)
            }
            val documentId = documentDao.insert(Document(
                sourceUri = uri,
                filename = filename,
                convertedFilename = convertedFilename
            ))
            documentDao.getDocument(documentId).let {
                result.postValue(it)
                convert(it)
            }
        }
        return result
    }

    fun reload(documentId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            Log.d(TAG, "reload($documentId)")
            getDocumentCacheDir(appCacheDir = app.cacheDir, documentId).deleteRecursively()
            getDocumentFilesDir(appFilesDir = app.filesDir, documentId).deleteRecursively()
            documentDao.reloadDocument(documentId)
            convert(documentDao.getDocument(documentId))
        }
    }

    private fun convert(document: Document) {
        Log.d(TAG, "convert(${document.id})")
        getDocumentCacheDir(appCacheDir = app.cacheDir, document.id).mkdirs()
        getDocumentFilesDir(appFilesDir = app.filesDir, document.id).mkdirs()

        val cachedSourceFile = getCachedSourceFile(appCacheDir = app.cacheDir, document.id, document.filename)
        val saveToCacheWorkRequest = SaveToCacheWorker.oneTimeWorkRequestBuilder(document.sourceUri, cachedSourceFile)
            .addTag("DocumentWork-${document.id}")
            .addTag("SaveToCacheWork")
            .build()

        var continuation = workManager.beginUniqueWork("document-${document.id}", ExistingWorkPolicy.REPLACE, saveToCacheWorkRequest)

        val type = document.sourceUri.getMimeType(app.contentResolver)

        val convertedFile = getConvertedHtmlFile(appFilesDir = app.filesDir, document.id, document.convertedFilename)
        val converterWorkRequestBuilder = if (pdf2htmlEXWorker.SUPPORTED_MIME_TYPES.contains(type)) {
            pdf2htmlEXWorker.oneTimeWorkRequestBuilder(cachedSourceFile, convertedFile, app)
        } else if (wvWareWorker.SUPPORTED_MIME_TYPES.contains(type)) {
            wvWareWorker.oneTimeWorkRequestBuilder(cachedSourceFile, convertedFile, app)
        } else {
            Log.e(TAG, "Failed to find appropriate worker. MIME Type='%s', Uri='%s'".format(type, document.sourceUri))
            documentDao.errorState(id = document.id)
            return
        }
        val converterWorkRequest = converterWorkRequestBuilder
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .addTag("DocumentWork")
            .addTag("DocumentWork-${document.id}")
            .build()

        continuation = continuation.then(converterWorkRequest)

        continuation = continuation.then(
            CleanupCachedDocumentWorker.oneTimeWorkRequestBuilder(cachedSourceFile).build()
        )

        continuation.enqueue()
    }
}
