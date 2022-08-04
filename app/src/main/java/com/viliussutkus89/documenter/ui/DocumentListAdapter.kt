/*
 * DocumentListAdapter.kt
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

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.viliussutkus89.documenter.databinding.ListItemDocumentBinding
import com.viliussutkus89.documenter.model.Document
import java.io.File


class DocumentListAdapter(
    private val appCacheDir: File,
    private val openListener: (Document) -> Unit,
    private val removeListener: (Document) -> Unit
): ListAdapter<Document, DocumentListAdapter.DocumentViewHolder>(DiffCallback) {

    class DocumentViewHolder(internal val binding: ListItemDocumentBinding)
        : RecyclerView.ViewHolder(binding.root)

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

    override fun onBindViewHolder(holder: DocumentViewHolder, position: Int) {
        val doc = getItem(position)
        holder.binding.appCacheDir = appCacheDir
        holder.binding.apply {
            document = doc
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
