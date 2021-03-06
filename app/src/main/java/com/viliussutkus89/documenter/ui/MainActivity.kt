/*
 * MainActivity.kt
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
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuProvider
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.test.espresso.idling.CountingIdlingResource
import com.viliussutkus89.documenter.DocumenterApplication
import com.viliussutkus89.documenter.R
import com.viliussutkus89.documenter.viewmodel.ConverterViewModel

class MainActivity : AppCompatActivity(R.layout.activity_main) {
    private val navController: NavController by lazy {
        (supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment)
            .navController
    }

    private val converterViewModel: ConverterViewModel by viewModels {
        val app = application as DocumenterApplication
        ConverterViewModel.Factory(app, app.documentDatabase.documentDao())
    }

    private val aboutMenuButtonProvider = object: MenuProvider {
        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            menuInflater.inflate(R.menu.main_menu, menu)
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
            return if (R.id.about == menuItem.itemId) {
                navController.navigate(AboutFragmentDirections.actionGlobalAboutFragment())
                true
            } else false
        }
    }

    internal fun setAboutButtonVisibility(value: Boolean) {
        if (value) {
            addMenuProvider(aboutMenuButtonProvider)
        } else {
            removeMenuProvider(aboutMenuButtonProvider)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupActionBarWithNavController(navController)

        addMenuProvider(aboutMenuButtonProvider)

        converterViewModel.workWatcher.observe(this) {}
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    private val idlingResourceDelegate = lazy {
        CountingIdlingResource("${javaClass.name}.idlingResource")
    }

    @VisibleForTesting
    val idlingResource by idlingResourceDelegate

    internal fun isIdlingResourceInitialized() = idlingResourceDelegate.isInitialized()

    internal fun incrementIdlingResource() {
        if (idlingResourceDelegate.isInitialized()) {
            idlingResource.increment()
        }
    }

    internal fun decrementIdlingResource() {
        if (idlingResourceDelegate.isInitialized()) {
            idlingResource.decrement()
        }
    }
}
