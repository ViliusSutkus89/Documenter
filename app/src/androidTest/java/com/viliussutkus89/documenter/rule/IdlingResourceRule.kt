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
