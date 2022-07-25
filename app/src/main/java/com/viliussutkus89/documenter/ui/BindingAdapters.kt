/*
 * BindingAdapters.kt
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

package com.viliussutkus89.documenter.ui

import android.widget.ImageView
import androidx.core.net.toUri
import androidx.databinding.BindingAdapter
import com.viliussutkus89.documenter.R
import com.viliussutkus89.documenter.model.Document
import com.viliussutkus89.documenter.model.getThumbnailFile
import java.io.File

@BindingAdapter("documentThumbnail", "cacheDir")
fun setDocumentThumbnail(imageView: ImageView, document: Document, appCacheDir: File) {
    document.getThumbnailFile(appCacheDir)?.let {
        imageView.setImageURI(it.toUri())
    } ?: run {
        imageView.setImageResource(R.drawable.loading_img)
    }
}
