/*
 * HomeFragment.kt
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

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.doOnLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.viliussutkus89.documenter.DocumenterApplication
import com.viliussutkus89.documenter.PreKitKatFragmentDirections
import com.viliussutkus89.documenter.R
import com.viliussutkus89.documenter.databinding.FragmentHomeBinding
import com.viliussutkus89.documenter.viewmodel.ConverterViewModel
import com.viliussutkus89.documenter.viewmodel.HomeViewModel

class HomeFragment: Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val app get() = requireActivity().application as DocumenterApplication
    private val homeViewModel: HomeViewModel by viewModels {
        HomeViewModel.Factory(app.documentDatabase.documentDao())
    }
    private val converterViewModel: ConverterViewModel by activityViewModels {
        ConverterViewModel.Factory(app)
    }

    private val openDocument = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
        registerForActivityResult(ActivityResultContracts.OpenDocument()) {
            it?.let { openUri(it) }
        }
    } else null

    private fun openUri(uri: Uri) {
        when (uri.scheme) {
            ContentResolver.SCHEME_CONTENT -> {
            (requireActivity() as MainActivity).incrementIdlingResource()
            converterViewModel.convert(uri).observe(viewLifecycleOwner) { document ->
                findNavController().navigate(
                    HomeFragmentDirections.actionHomeFragmentToDocumentFragment(
                        document.id, document.filename
                    )
                )
            }
            }
            else -> binding.root.doOnLayout {
                Snackbar.make(it, R.string.error_file_scheme, Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun handleIntent(intent: Intent) {
        when (intent.action) {
            // ACTION_VIEW sends URI in data field
            Intent.ACTION_VIEW -> intent.data
            // ACTION_SEND sends URI in parcelable extra
            Intent.ACTION_SEND -> intent.getParcelableExtra(Intent.EXTRA_STREAM)
            else -> null
        }?.let {
            if (!homeViewModel.intentUriHandlerGate()) {
                openUri(it)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        handleIntent(requireActivity().intent)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (!WebViewInstallFragment.isWebViewAvailable(requireContext())) {
            val action = HomeFragmentDirections.actionHomeFragmentToWebViewInstallFragment()
            findNavController().navigate(action)
        }
        val adapter = DocumentListAdapter(
            appCacheDir = requireContext().cacheDir,
            openListener = {
                (requireActivity() as MainActivity).incrementIdlingResource()
                findNavController().navigate(HomeFragmentDirections.actionHomeFragmentToDocumentFragment(it.id, it.filename))
            },
            removeListener = { homeViewModel.removeDocument(it, requireContext()) }
        )
        binding.recyclerView.adapter = adapter
        homeViewModel.documents.observe(viewLifecycleOwner) { documentList ->
            adapter.submitList(documentList)
        }

        binding.openButton.apply {
            this.setOnClickListener {
                openDocument?.launch(ConverterViewModel.supportedMimeTypes) ?: run {
                    findNavController().navigate(PreKitKatFragmentDirections.actionGlobalPreKitKatFragment())
                }
            }
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
                binding.openButton.setImageResource(R.drawable.ic_baseline_clear_24)
            }
        }
    }
}
