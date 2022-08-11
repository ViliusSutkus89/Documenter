package com.viliussutkus89.documenter.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.preference.PreferenceFragmentCompat
import com.viliussutkus89.documenter.R


class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        (requireActivity() as MainActivity).setMainMenuVisibility(false)
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onPause() {
        (requireActivity() as MainActivity).setMainMenuVisibility(true)
        super.onPause()
    }
}
