/*
 * DocumentFragment.kt
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

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.navArgs
import com.google.android.material.snackbar.Snackbar
import com.viliussutkus89.documenter.DocumenterApplication
import com.viliussutkus89.documenter.R
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDocumentBinding.inflate(inflater, container, false)

        binding.documentView.settings.apply {
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            useWideViewPort = true
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

            if ((requireActivity() as MainActivity).isIdlingResourceInitialized()) {
                binding.documentView.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        (requireActivity() as MainActivity).decrementIdlingResource()
                    }
                }
            }
        }
        return binding.root
    }

    override fun onPause() {
        super.onPause()

        val view = binding.documentWrapper
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        viewModel.saveThumbnail(bitmap, requireContext().cacheDir)
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        requireActivity().addMenuProvider(object: MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.document_menu, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when(menuItem.itemId) {
                    R.id.save -> {
                        viewModel.document.observeOnce(viewLifecycleOwner) { document ->
                            registerForActivityResult<String, Uri>(ActivityResultContracts.CreateDocument("text/html")) {
                                it?.let {
                                    viewModel.saveDocument(it, requireContext())
                                }
                            }.launch(document.convertedFilename)
                        }
                        true
                    }
                    R.id.open_with, R.id.share -> {
                        viewModel.document.observeOnce(viewLifecycleOwner) { document ->
                            document.getConvertedHtmlFile(requireContext().filesDir)?.let { htmlFile ->
                                val convertedUri = FileProvider.getUriForFile(requireContext(), requireContext().packageName + ".provider", htmlFile)
                                binding.documentView.loadUrl(convertedUri.toString())

                                when(menuItem.itemId) {
                                    R.id.open_with -> {
                                        try {
                                            startActivity(Intent(Intent.ACTION_VIEW, convertedUri)
                                                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
                                        } catch (e: ActivityNotFoundException) {
                                            e.printStackTrace()
                                            Snackbar.make(view, R.string.error_open_with_failed, Snackbar.LENGTH_LONG).show()
                                        }
                                    }
                                    R.id.share -> {
                                        try {
                                            startActivity(Intent(Intent.ACTION_SEND).apply {
                                                type = "text/html"
                                                putExtra(Intent.EXTRA_STREAM, convertedUri)
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            })
                                        } catch (e: ActivityNotFoundException) {
                                            e.printStackTrace()
                                            Snackbar.make(view, R.string.error_share_failed, Snackbar.LENGTH_LONG).show()
                                        }
                                    }
                                }
                            }
                        }
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }
}
