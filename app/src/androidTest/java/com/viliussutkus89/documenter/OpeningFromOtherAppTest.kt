/*
 * OpeningFromOtherAppTest.kt
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

package com.viliussutkus89.documenter

import android.content.Intent
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.launchActivity
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingPolicies
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.IdlingResource
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.pressBack
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.viliussutkus89.android.assetextractor.AssetExtractor
import com.viliussutkus89.documenter.ui.MainActivity
import org.junit.*
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File
import java.util.concurrent.TimeUnit

@LargeTest
@RunWith(Parameterized::class)
class OpeningFromOtherAppTest {
    @Parameterized.Parameter
    lateinit var testFile: File

    private lateinit var idlingResource: IdlingResource

    companion object {
        @BeforeClass @JvmStatic
        fun setIdlingTimeout() {
            IdlingPolicies.setMasterPolicyTimeout(10, TimeUnit.MINUTES)
            IdlingPolicies.setIdlingResourceTimeout(10, TimeUnit.MINUTES)
        }

        @BeforeClass @JvmStatic
        fun extractDocuments() {
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
    val screenshotFailedTestRule = ScreenshotFailedTestRule()

    @Before
    fun setUp() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val authority = appContext.packageName + ".instrumentedTestsProvider"
        val uri = FileProvider.getUriForFile(appContext, authority, testFile)
        val viewIntent = Intent(Intent.ACTION_VIEW, uri)
            .putExtra(MainActivity.FORCE_INIT_IDLING_RESOURCE, true)

        launchActivity<MainActivity>(viewIntent).onActivity { activity ->
            screenshotFailedTestRule.activity = activity

            @Suppress("DEPRECATION") // ACTION_CLOSE_SYSTEM_DIALOGS is perfectly fine in tests
            activity.sendBroadcast(Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS))
            idlingResource = activity.idlingResource
            IdlingRegistry.getInstance().register(idlingResource)
        }
    }

    @After
    fun tearDown() {
        IdlingRegistry.getInstance().unregister(idlingResource)
    }

    @Test
    fun testParameterizedFile() {
        onView(withId(R.id.documentView)).perform(pressBack())

        // Open previously converted document
        onView(withId(R.id.recycler_view)).perform(
            RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click())
        )
        onView(withId(R.id.documentView)).perform(pressBack())
    }
}
