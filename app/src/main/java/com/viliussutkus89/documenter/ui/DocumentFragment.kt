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
import android.os.Build
import android.os.Bundle
import android.view.*
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
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
import java.io.File

class DocumentFragment: Fragment() {
    companion object {
        private const val BUNDLE_KEY_DOCUMENT_VIEW = "BUNDLE_KEY_DOCUMENT_VIEW"
    }

    private var _binding: FragmentDocumentBinding? = null
    private val binding get() = _binding!!

    private val args: DocumentFragmentArgs by navArgs()

    private val viewModel: DocumentViewModel by viewModels {
        val app = requireActivity().application as DocumenterApplication
        DocumentViewModel.Factory(app, args.documentId)
    }

    private fun getFileUri(file: File): Uri {
        val ctx = requireContext()
        return FileProvider.getUriForFile(ctx, ctx.packageName + ".provider", file)
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            requireActivity().addMenuProvider(saveMenu, viewLifecycleOwner, Lifecycle.State.RESUMED)
        }
        requireActivity().addMenuProvider(documentMenu, viewLifecycleOwner, Lifecycle.State.RESUMED)
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

    private val documentMenu = object: MenuProvider {
        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            menuInflater.inflate(R.menu.document_menu, menu)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
                menu.findItem(R.id.save).isVisible = false
            }

            // Workaround for Issue #7
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
                menu.findItem(R.id.open_with).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
                menu.findItem(R.id.share).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
            }
        }

        private fun showSnackBar(@StringRes resId: Int) {
            Snackbar.make(binding.root, resId, Snackbar.LENGTH_LONG).show()
        }

        @RequiresApi(Build.VERSION_CODES.KITKAT)
        private fun save() {
            documentViewModel.document.observeOnce(viewLifecycleOwner) { document ->
                registerForActivityResult<String, Uri>(ActivityResultContracts.CreateDocument("text/html")) {
                    it?.let {
                        documentViewModel.saveDocument(it)
                    }
                }.launch(document.convertedFilename)
            }
        }

        private fun openWith() {
            viewModel.document.observeOnce(viewLifecycleOwner) { document ->
                document.getConvertedHtmlFile(requireContext().filesDir)?.let { htmlFile ->
                    try {
                        startActivity(Intent(Intent.ACTION_VIEW, getFileUri(htmlFile))
                            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
                    } catch (e: ActivityNotFoundException) {
                        e.printStackTrace()
                        showSnackBar(R.string.error_open_with_failed)
                    }
                }
            }
        }

        private fun share() {
            documentViewModel.htmlFile.observeOnce(viewLifecycleOwner) { htmlFile ->
                try {
                    startActivity(Intent(Intent.ACTION_SEND).apply {
                        type = "text/html"
                        putExtra(Intent.EXTRA_STREAM, getFileUri(htmlFile))
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    })
                } catch (e: ActivityNotFoundException) {
                    e.printStackTrace()
                    showSnackBar(R.string.error_share_failed)
                }
            }


            viewModel.document.observeOnce(viewLifecycleOwner) { document ->
                document.getConvertedHtmlFile(requireContext().filesDir)?.let { htmlFile ->
                    try {
                        startActivity(Intent(Intent.ACTION_SEND).apply {
                            type = "text/html"
                            putExtra(Intent.EXTRA_STREAM, getFileUri(htmlFile))
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        })
                    } catch (e: ActivityNotFoundException) {
                        e.printStackTrace()
                        showSnackBar(R.string.error_share_failed)
                    }
                }
            }
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
            return when(menuItem.itemId) {
                R.id.save -> {
                    // Pre KitKat will not reach this code, because menu item is hidden for pre KitKat
                    // Added the check anyways, to reassure linter
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        save()
                    }
                    true
                }
                R.id.open_with -> {
                    openWith()
                    true
                }
                R.id.share -> {
                    share()
                    true
                }
                else -> false
            }
        }
    }
}
