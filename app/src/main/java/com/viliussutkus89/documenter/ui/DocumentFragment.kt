/*
 * DocumentFragment.kt
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

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.viliussutkus89.documenter.DocumenterApplication
import com.viliussutkus89.documenter.databinding.FragmentDocumentBinding
import com.viliussutkus89.documenter.model.getConvertedHtmlFile
import com.viliussutkus89.documenter.utils.observeOnce
import com.viliussutkus89.documenter.viewmodel.DocumentViewModel

class DocumentFragment: Fragment() {
    companion object {
        private const val BUNDLE_KEY_DOCUMENT_VIEW = "BUNDLE_KEY_DOCUMENT_VIEW"
    }

    private var _binding: FragmentDocumentBinding? = null
    private val binding get() = _binding!!

    private val args: DocumentFragmentArgs by navArgs()

    private val viewModel: DocumentViewModel by viewModels {
        val app = requireActivity().application as DocumenterApplication
        DocumentViewModel.Factory(args.documentId, app.documentDatabase.documentDao())
    }

    private fun takeScreenshot() {
        val view = binding.documentWrapper
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        viewModel.saveBitmap(bitmap, requireContext().cacheDir)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDocumentBinding.inflate(inflater, container, false)

        binding.documentView.settings.apply {
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = true
            @SuppressLint("SetJavaScriptEnabled")
            javaScriptEnabled = true
            allowContentAccess = true
            blockNetworkLoads = true
            allowFileAccess = true
        }

        viewModel.document.observeOnce(viewLifecycleOwner) { document ->
            savedInstanceState?.getBundle(BUNDLE_KEY_DOCUMENT_VIEW)?.let {
                binding.documentView.restoreState(it)
            } ?: run {
                document.getConvertedHtmlFile(requireContext().filesDir)?.let { htmlFile ->
                    val convertedUri = FileProvider.getUriForFile(requireContext(), requireContext().packageName + ".provider", htmlFile)
                    binding.documentView.loadUrl(convertedUri.toString())
                }
            }
        }
        return binding.root
    }

    override fun onPause() {
        super.onPause()
        takeScreenshot()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        val documentViewBundle = Bundle()
        binding.documentView.saveState(documentViewBundle)
        outState.putBundle(BUNDLE_KEY_DOCUMENT_VIEW, documentViewBundle)
    }
}
