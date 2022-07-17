/*
 * LoadingFragment.kt
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

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.viliussutkus89.documenter.DocumenterApplication
import com.viliussutkus89.documenter.R
import com.viliussutkus89.documenter.model.State
import com.viliussutkus89.documenter.viewmodel.LoadingViewModel

class LoadingFragment: Fragment(R.layout.fragment_loading) {
    private val args: DocumentFragmentArgs by navArgs()

    private val viewModel: LoadingViewModel by viewModels {
        val app = requireActivity().application as DocumenterApplication
        LoadingViewModel.Factory(args.documentId, app.documentDatabase.documentDao())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel.document.observe(viewLifecycleOwner) { document ->
            if (State.Converted == document.state) {
                val action = LoadingFragmentDirections.actionLoadingFragmentToDocumentFragment(args.documentId)
                findNavController().navigate(action)
            }

            view.findViewById<TextView>(R.id.message).text = resources.getString(
                when (document.state) {
                    State.Init -> R.string.state_init
                    State.Caching -> R.string.state_caching
                    State.Cached -> R.string.state_cached
                    State.Converting -> R.string.state_converting
                    State.Converted -> R.string.state_converted
                    else -> R.string.state_error
                }
            )
        }
    }
}
