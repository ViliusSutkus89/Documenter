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
        return FragmentAboutBinding.inflate(inflater, container, false).apply {
            documenterVersion = BuildConfig.VERSION_NAME
            // @TODO: expose these strings in libraries
            pdf2htmlEXVersion = "0.18.18"
            wvWareVersion = "1.2.7"
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<Button>(R.id.show_licenses).setOnClickListener {
            findNavController().navigate(AboutFragmentDirections.actionAboutFragmentToAboutLibs())
        }
    }
}
