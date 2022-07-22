package com.viliussutkus89.documenter

import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.viliussutkus89.documenter.ui.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// https://github.com/ViliusSutkus89/Documenter/issues/5

@RunWith(AndroidJUnit4::class)
class AboutMenuItemVisibilityTest {
    @get:Rule
    val rule = activityScenarioRule<MainActivity>()

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
