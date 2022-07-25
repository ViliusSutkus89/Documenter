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

import android.content.Context
import androidx.lifecycle.*
import androidx.work.WorkManager
import com.viliussutkus89.documenter.model.Document
import com.viliussutkus89.documenter.model.DocumentDao
import com.viliussutkus89.documenter.model.getCachedDir
import com.viliussutkus89.documenter.model.getFilesDir
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HomeViewModel(private val documentDao: DocumentDao) : ViewModel() {
    class Factory(private val documentDao: DocumentDao): ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return HomeViewModel(documentDao) as T
            }
            throw IllegalArgumentException("Unable to construct ViewModel")
        }
    }

    val documents: LiveData<List<Document>> = documentDao.getDocuments().asLiveData()

    // handle uri incoming through Intent only once
    private var intentUriHandled = false
    fun intentUriHandlerGate(): Boolean {
        val prev = intentUriHandled
        intentUriHandled = true
        return prev
    }

    fun removeDocument(document: Document, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            WorkManager.getInstance(context).cancelUniqueWork("document-${document.id}")
            document.getCachedDir(appCacheDir = context.cacheDir).deleteRecursively()
            document.getFilesDir(appFilesDir = context.filesDir).deleteRecursively()
            documentDao.delete(document)
        }
    }
}
