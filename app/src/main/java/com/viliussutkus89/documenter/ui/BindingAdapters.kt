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
