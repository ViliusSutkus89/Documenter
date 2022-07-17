/*
 * DocumentViewModel.kt
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

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.viliussutkus89.documenter.model.DocumentDao
import com.viliussutkus89.documenter.model.getScreenshotFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.*

class DocumentViewModel(private val documentId: Long, private val documentDao: DocumentDao) : ViewModel() {
    class Factory(private val documentId: Long, private val documentDao: DocumentDao): ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(DocumentViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return DocumentViewModel(documentId, documentDao) as T
            }
            throw IllegalArgumentException("Unable to construct DocumentViewModel")
        }
    }

    val document = documentDao.getFilenameConvertedFilename(documentId).asLiveData()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            documentDao.updateLastAccessed(documentId, Date().time)
        }
    }

    fun saveBitmap(bitmap: Bitmap, appCacheDir: File) {
        viewModelScope.launch(Dispatchers.IO) {
            val screenshot = document.value?.getScreenshotFile(appCacheDir)
            FileOutputStream(screenshot).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 50, out)
            }
            documentDao.updateLastAccessed(documentId, Date().time)
        }
    }
}
