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
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.navArgs
import com.google.android.material.snackbar.Snackbar
import com.viliussutkus89.documenter.DocumenterApplication
import com.viliussutkus89.documenter.R
import com.viliussutkus89.documenter.databinding.FragmentDocumentBinding
import com.viliussutkus89.documenter.model.State
import com.viliussutkus89.documenter.model.stringRes
import com.viliussutkus89.documenter.utils.observeOnce
import com.viliussutkus89.documenter.viewmodel.ConverterViewModel
import com.viliussutkus89.documenter.viewmodel.DocumentViewModel
import java.io.File


class DocumentFragment: Fragment() {
    companion object {
        private const val BUNDLE_KEY_DOCUMENT_VIEW = "BUNDLE_KEY_DOCUMENT_VIEW"
    }

    private var _binding: FragmentDocumentBinding? = null
    private val binding get() = _binding!!

    private val args: DocumentFragmentArgs by navArgs()

    private val app get() = requireActivity().application as DocumenterApplication
    private val documentViewModel: DocumentViewModel by viewModels {
        DocumentViewModel.Factory(app, args.documentId)
    }
    private val converterViewModel: ConverterViewModel by activityViewModels {
        ConverterViewModel.Factory(app)
    }

    private var savedWebViewBundle: Bundle? = null

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

        savedWebViewBundle = savedInstanceState?.getBundle(BUNDLE_KEY_DOCUMENT_VIEW)
        documentViewModel.stateAndHtmlFile.observe(viewLifecycleOwner) { doc ->
            if (State.Converted == doc.state) {
                // @TODO: this is executed twice
                binding.loading.visibility = View.GONE
                binding.documentWrapper.visibility = View.VISIBLE
                savedWebViewBundle?.let {
                    binding.documentView.restoreState(it)
                } ?: let {
                    binding.documentView.loadUrl(getFileUri(doc.htmlFile).toString())
                }
            } else {
                binding.documentWrapper.visibility = View.GONE
                binding.loading.visibility = View.VISIBLE
                binding.loadingMessage.text = resources.getString(doc.state.stringRes)
                binding.progressBar.visibility = if (State.Error == doc.state) View.INVISIBLE else View.VISIBLE
            }
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        documentViewModel.state.observe(viewLifecycleOwner) {
            requireActivity().run {
                removeMenuProvider(documentMenu)
                if (State.Converted == it) {
                    addMenuProvider(documentMenu, viewLifecycleOwner, Lifecycle.State.RESUMED)
                }
            }
        }
        binding.documentView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                (requireActivity() as MainActivity).decrementIdlingResource()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if (View.VISIBLE == binding.documentWrapper.visibility) {
            val view = binding.documentWrapper
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap))
            documentViewModel.saveThumbnail(bitmap)
        }
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
                menu.findItem(R.id.reload).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
            }

            documentViewModel.canReload.observeOnce(viewLifecycleOwner) {
                menu.findItem(R.id.reload).isVisible = it
            }
        }

        private fun showSnackBar(@StringRes resId: Int) {
            Snackbar.make(binding.root, resId, Snackbar.LENGTH_LONG).show()
        }

        @RequiresApi(Build.VERSION_CODES.KITKAT)
        private fun save() {
            documentViewModel.document.observeOnce(viewLifecycleOwner) { document ->
                if (document.copyProtected) {
                    showSnackBar(R.string.error_cannot_save_copy_protected_document)
                } else {
                    registerForActivityResult<String, Uri>(ActivityResultContracts.CreateDocument("text/html")) {
                        it?.let {
                            documentViewModel.saveDocument(it)
                        }
                    }.launch(document.convertedFilename)
                }
            }
        }

        private fun openWith() {
            documentViewModel.htmlFile.observeOnce(viewLifecycleOwner) { htmlFile ->
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, getFileUri(htmlFile))
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
                } catch (e: ActivityNotFoundException) {
                    e.printStackTrace()
                    showSnackBar(R.string.error_open_with_failed)
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
        }

        private fun reload() {
            savedWebViewBundle = null
            documentViewModel.canReload.observeOnce(viewLifecycleOwner) {
                if (it) {
                    (requireActivity() as MainActivity).incrementIdlingResource()
                    converterViewModel.reload(args.documentId)
                } else {
                    showSnackBar(R.string.error_cannot_reload_because_old_version)
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
                R.id.reload -> {
                    reload()
                    true
                }
                else -> false
            }
        }
    }
}
