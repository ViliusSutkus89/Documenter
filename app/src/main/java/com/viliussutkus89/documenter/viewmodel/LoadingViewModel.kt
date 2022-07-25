/*
 * LoadingViewModel.kt
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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import com.viliussutkus89.documenter.model.DocumentDao

class LoadingViewModel(documentId: Long, documentDao: DocumentDao) : ViewModel() {
    class Factory(private val documentId: Long, private val documentDao: DocumentDao): ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(LoadingViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return LoadingViewModel(documentId, documentDao) as T
            }
            throw IllegalArgumentException("Unable to construct LoadingViewModel")
        }
    }

    val document = documentDao.getFilenameAndState(documentId).asLiveData()
}
