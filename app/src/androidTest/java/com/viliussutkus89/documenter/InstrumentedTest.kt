/*
 * InstrumentedTest.kt
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

package com.viliussutkus89.documenter

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.os.Build
import androidx.core.content.FileProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingPolicies
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.IdlingResource
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.pressBack
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.viliussutkus89.android.assetextractor.AssetExtractor
import com.viliussutkus89.documenter.ui.MainActivity
import org.junit.*
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.model.Statement
import java.io.File
import java.util.concurrent.TimeUnit

@LargeTest
@RunWith(Parameterized::class)
class InstrumentedTest {
    @Parameterized.Parameter
    lateinit var pdfFile: File

    private lateinit var idlingResource: IdlingResource

    companion object {
        @BeforeClass @JvmStatic
        fun setIdlingTimeout() {
            IdlingPolicies.setMasterPolicyTimeout(5, TimeUnit.MINUTES)
            IdlingPolicies.setIdlingResourceTimeout(5, TimeUnit.MINUTES)
        }

        @BeforeClass @JvmStatic
        fun extractPDFs() {
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            AssetExtractor(instrumentation.context.assets)
                .setNoOverwrite()
                .extract(instrumentation.targetContext.cacheDir, "testFiles")
        }

        @Parameterized.Parameters @JvmStatic
        fun listPDFs(): List<File> {
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            val extractedToDir = File(instrumentation.targetContext.cacheDir, "testFiles")
            return instrumentation.context.assets.list("testFiles")!!.map {
                File(extractedToDir, it)
            }
        }
    }

    @get:Rule
    val screenshotFailedTestRule = ScreenshotFailedTestRule(InstrumentationRegistry.getInstrumentation())

    // https://github.com/android/android-test/issues/1433
    private val activityScenarioTestRuleWorkaroundNeeded = Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP

    @get:Rule
    val activityTestRule: TestRule = if (activityScenarioTestRuleWorkaroundNeeded) {
        @Suppress("DEPRECATION")
        androidx.test.rule.ActivityTestRule(MainActivity::class.java)
    } else {
        // Empty TestRule
        TestRule { _, _ ->
            object: Statement() {
                override fun evaluate() { }
            }
        }
    }

    @Before
    fun setUp() {
        Intents.init()
        if (activityScenarioTestRuleWorkaroundNeeded) {
            @Suppress("DEPRECATION", "UNCHECKED_CAST")
            val activity = (activityTestRule as androidx.test.rule.ActivityTestRule<MainActivity>).activity
            idlingResource = activity.idlingResource
            IdlingRegistry.getInstance().register(idlingResource)
        } else {
            ActivityScenario.launch(MainActivity::class.java).onActivity { activity ->
                idlingResource = activity.idlingResource
                IdlingRegistry.getInstance().register(idlingResource)
            }
        }
    }

    @After
    fun tearDown() {
        Intents.release()
        IdlingRegistry.getInstance().unregister(idlingResource)
    }

    @Test
    fun testAllSuppliedPDFs() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val authority = appContext.packageName + ".instrumentedTestsProvider"
        val uri = FileProvider.getUriForFile(appContext, authority, pdfFile)

        Intents.intending(IntentMatchers.hasAction(Intent.ACTION_OPEN_DOCUMENT)).respondWith(
            Instrumentation.ActivityResult(
                Activity.RESULT_OK,
                Intent().setData(uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            )
        )

        onView(withId(R.id.open_button)).perform(click())
        onView(withId(R.id.documentView)).perform(pressBack())
    }
}
