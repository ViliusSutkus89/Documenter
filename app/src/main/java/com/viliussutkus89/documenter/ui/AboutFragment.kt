/*
 * AboutFragment.kt
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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.viliussutkus89.documenter.BuildConfig
import com.viliussutkus89.documenter.R
import com.viliussutkus89.documenter.databinding.FragmentAboutBinding

class AboutFragment: Fragment(R.layout.fragment_about) {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        (requireActivity() as MainActivity).setMainMenuVisibility(false)
        return FragmentAboutBinding.inflate(inflater, container, false).apply {
            documenterVersion = BuildConfig.VERSION_NAME
            pdf2htmlEXVersion = app.opendocument.android.pdf2htmlex.BuildConfig.VERSION_NAME
            wvWareVersion = com.viliussutkus89.android.wvware.BuildConfig.VERSION_NAME
        }.root
    }

    private var navigatingToAboutLibs = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<Button>(R.id.show_licenses).setOnClickListener {
            navigatingToAboutLibs = true
            findNavController().navigate(AboutFragmentDirections.actionAboutFragmentToAboutLibs())
        }
    }

    override fun onResume() {
        super.onResume()
        navigatingToAboutLibs = false
    }

    override fun onPause() {
        if (!navigatingToAboutLibs) {
            (requireActivity() as MainActivity).setMainMenuVisibility(true)
        }
        super.onPause()
    }
}
