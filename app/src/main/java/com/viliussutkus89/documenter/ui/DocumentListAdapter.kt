/*
 * DocumentListAdapter.kt
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

package com.viliussutkus89.documenter.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.viliussutkus89.documenter.R
import com.viliussutkus89.documenter.databinding.ListItemDocumentBinding
import com.viliussutkus89.documenter.model.Document
import com.viliussutkus89.documenter.model.State
import com.viliussutkus89.documenter.model.getScreenshotFile
import java.io.File

class DocumentListAdapter(
    private val appCacheDir: File,
    private val openListener: (Document) -> Unit,
    private val removeListener: (Document) -> Unit
): ListAdapter<Document, DocumentListAdapter.DocumentViewHolder>(DiffCallback) {

    class DocumentViewHolder(private val binding: ListItemDocumentBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(doc: Document, appCacheDir: File, openListener: (Document) -> Unit, removeListener: (Document) -> Unit) {
            binding.apply {
                document = doc
                if (State.Converted == document.state) {
                    thumbnail.setImageURI(doc.getScreenshotFile(appCacheDir).toUri())
                } else {
                    thumbnail.setImageResource(R.drawable.loading_animation)
                }
                thumbnail.setOnClickListener {
                    openListener(doc)
                }
                filename.setOnClickListener {
                    openListener(doc)
                }
                removeButton.setOnClickListener {
                    removeListener(doc)
                }
                executePendingBindings()
            }
        }
    }

    companion object DiffCallback: DiffUtil.ItemCallback<Document>() {
        override fun areItemsTheSame(oldItem: Document, newItem: Document): Boolean {
            return oldItem.id == newItem.id
        }
        override fun areContentsTheSame(oldItem: Document, newItem: Document): Boolean {
            return oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DocumentViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val listItemBinding = ListItemDocumentBinding.inflate(layoutInflater, parent, false)
        return DocumentViewHolder(listItemBinding)
    }

    override fun onBindViewHolder(holder: DocumentViewHolder, position: Int) =
        holder.bind(getItem(position), appCacheDir, openListener, removeListener)
}
