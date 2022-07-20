/*
 * HomeFragment.kt
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

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.viliussutkus89.documenter.DocumenterApplication
import com.viliussutkus89.documenter.databinding.FragmentHomeBinding
import com.viliussutkus89.documenter.viewmodel.HomeViewModel

class HomeFragment: Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val homeViewModel: HomeViewModel by viewModels {
        val app = requireActivity().application as DocumenterApplication
        HomeViewModel.Factory(app, app.documentDatabase.documentDao())
    }

    private val openDocument = registerForActivityResult(ActivityResultContracts.OpenDocument()) {
        it?.let { openUri(it) }
    }

    private fun openUri(uri: Uri) {
        (requireActivity() as MainActivity).incrementIdlingResource()
        homeViewModel.openDocument(uri).observe(viewLifecycleOwner) { document ->
            findNavController().navigate(HomeFragmentDirections.actionHomeFragmentToLoadingFragment(document.id, document.filename))
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        requireActivity().intent.let { intent ->
            when (requireActivity().intent.action) {
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
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
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
                findNavController().navigate(HomeFragmentDirections.actionHomeFragmentToLoadingFragment(it.id, it.filename))
            },
            removeListener = { homeViewModel.removeDocument(it) }
        )
        binding.recyclerView.adapter = adapter
        homeViewModel.documents.observe(viewLifecycleOwner) { documentList ->
            adapter.submitList(documentList)
        }

        binding.openButton.setOnClickListener {
            openDocument.launch(homeViewModel.supportedMimeTypes)
        }
    }
}
