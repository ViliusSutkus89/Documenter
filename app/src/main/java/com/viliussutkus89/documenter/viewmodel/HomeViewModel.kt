/*
 * HomeViewModel.kt
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

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.viliussutkus89.documenter.model.Document
import com.viliussutkus89.documenter.model.DocumentDao
import com.viliussutkus89.documenter.model.getDocumentCacheDir
import com.viliussutkus89.documenter.model.getDocumentFilesDir
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HomeViewModel(private val documentDao: DocumentDao) : ViewModel() {
    class Factory(private val documentDao: DocumentDao): ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return HomeViewModel(documentDao) as T
            }
            throw IllegalArgumentException("Unable to construct HomeViewModel")
        }
    }

    val documents = documentDao.getDocuments().asLiveData()

    // handle uri incoming through Intent only once
    private var intentUriHandled = false
    fun intentUriHandlerGate(): Boolean {
        val prev = intentUriHandled
        intentUriHandled = true
        return prev
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    private fun releasePermission(uri: Uri, contentResolver: ContentResolver) {
        contentResolver.releasePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    fun removeDocument(document: Document, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            WorkManager.getInstance(context).cancelUniqueWork("document-${document.id}")
            getDocumentCacheDir(appCacheDir = context.cacheDir, document.id).deleteRecursively()
            getDocumentFilesDir(appFilesDir = context.filesDir, document.id).deleteRecursively()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                if (document.sourceUri.toString().isNotEmpty() && documentDao.isUriUnique(document.sourceUri)) {
                    releasePermission(document.sourceUri, context.contentResolver)
                }
            }
            documentDao.delete(document)
        }
    }
}
