/*
 * IdlingResourceRule.kt
 *
 * Copyright (C) 2022 https://www.ViliusSutkus89.com/documenter/
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

package com.viliussutkus89.documenter.rule

import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.IdlingResource
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.viliussutkus89.documenter.ui.MainActivity
import org.junit.rules.ExternalResource


class IdlingResourceRule(private val scenarioRule: ActivityScenarioRule<MainActivity>): ExternalResource() {
    private lateinit var idlingResource: IdlingResource

    override fun before() {
        scenarioRule.scenario.onActivity {
            idlingResource = it.idlingResource
            IdlingRegistry.getInstance().register(idlingResource)
        }
    }

    override fun after() {
        IdlingRegistry.getInstance().unregister(idlingResource)
    }
}
