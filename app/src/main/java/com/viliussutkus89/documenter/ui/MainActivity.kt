/*
 * MainActivity.kt
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

import android.os.Build
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
        ConverterViewModel.Factory(application as DocumenterApplication)
    }

    private val mainMenuProvider = object: MenuProvider {
        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            menuInflater.inflate(R.menu.main_menu, menu)

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
                // Workaround for Issue #7
                menu.findItem(R.id.about).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
                menu.findItem(R.id.settings).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
            }
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
            return when (menuItem.itemId) {
                R.id.about -> {
                    navController.navigate(AboutFragmentDirections.actionGlobalAboutFragment())
                    true
                }
                R.id.settings -> {
                    navController.navigate(SettingsFragmentDirections.actionGlobalSettingsFragment())
                    true
                }
                else -> false
            }
        }
    }

    internal fun setMainMenuVisibility(value: Boolean) {
        if (value) {
            addMenuProvider(mainMenuProvider)
        } else {
            removeMenuProvider(mainMenuProvider)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupActionBarWithNavController(navController)

        addMenuProvider(mainMenuProvider)

        converterViewModel.workWatcher.observe(this) {}
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    companion object {
        val isRunningTest: Boolean get() = try {
            Class.forName("androidx.test.espresso.Espresso")
            true
        } catch (e: ClassNotFoundException) {
            false
        }
    }

    @VisibleForTesting
    internal val idlingResource by lazy {
        CountingIdlingResource("${javaClass.name}.idlingResource")
    }

    internal fun incrementIdlingResource() {
        if (isRunningTest) {
            idlingResource.increment()
        }
    }

    internal fun decrementIdlingResource() {
        if (isRunningTest) {
            idlingResource.decrement()
        }
    }
}
