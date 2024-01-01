/*
 * AboutMenuItemVisibilityTest.kt
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

package com.viliussutkus89.documenter

import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.viliussutkus89.documenter.rule.CloseSystemDialogsTestRule
import com.viliussutkus89.documenter.rule.ScreenshotFailedTestRule
import com.viliussutkus89.documenter.ui.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith


// https://github.com/ViliusSutkus89/Documenter/issues/5
@RunWith(AndroidJUnit4::class)
class AboutMenuItemVisibilityTest {
    private val scenarioRule: ActivityScenarioRule<MainActivity> = activityScenarioRule()

    @get:Rule
    val ruleChain: RuleChain = RuleChain
        .outerRule(scenarioRule)
        .around(CloseSystemDialogsTestRule())
        .around(ScreenshotFailedTestRule())

    @Test
    fun shownInHome_Test() {
        onView(withId(R.id.about)).check(matches(isDisplayed()))
    }

    @Test
    fun notShownInAbout_Test() {
        onView(withId(R.id.about)).perform(click())
        onView(withId(R.id.about)).check(doesNotExist())
    }

    @Test
    fun notShownInAboutLibraries_Test() {
        onView(withId(R.id.about)).perform(click())
        onView(withId(R.id.show_licenses)).perform(click())
        onView(withId(R.id.about)).check(doesNotExist())
    }

    @Test
    fun notShownAfterReturnFromAboutLibraries_Test() {
        onView(withId(R.id.about)).perform(click())
        onView(withId(R.id.show_licenses)).perform(click())
        Espresso.pressBack()
        onView(withId(R.id.about)).check(doesNotExist())
    }

    @Test
    fun shownInHomeAfterAfterReturnFromAboutLibraries_Test() {
        onView(withId(R.id.about)).perform(click())
        onView(withId(R.id.show_licenses)).perform(click())
        Espresso.pressBack()
        Espresso.pressBack()
        onView(withId(R.id.about)).check(matches(isDisplayed()))
    }
}
