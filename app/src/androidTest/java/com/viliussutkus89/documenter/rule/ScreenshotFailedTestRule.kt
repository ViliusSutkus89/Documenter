/*
 * ScreenshotFailedTestRule.kt
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

package com.viliussutkus89.documenter.rule

import android.app.Activity
import android.graphics.Bitmap
import android.util.Log
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.screenshot.BasicScreenCaptureProcessor
import androidx.test.runner.screenshot.Screenshot
import com.viliussutkus89.documenter.ui.MainActivity
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import java.io.File
import java.io.IOException


class ScreenshotFailedTestRule(private val scenario: ActivityScenarioRule<MainActivity>): TestWatcher() {
    companion object {
        private const val TAG = "ScreenFailedTestRule"
    }

    private val processor = object: BasicScreenCaptureProcessor() {
        init { mDefaultScreenshotPath = getStorageDir() }
        private fun getStorageDir(): File? {
            val ctx = InstrumentationRegistry.getInstrumentation().targetContext
            listOfNotNull(
                File("/data/local/tmp"),
                ctx.cacheDir,
                ctx.externalCacheDir
            ).map { File(it, "TestScreenshots") }
            .forEach {
                it.mkdirs()
                if (it.isDirectory && it.canWrite()) {
                    Log.v(TAG, "setting mDefaultScreenshotPath to " + it.path)
                    return it
                }
            }
            return null
        }
    }

    // Activity required for API level <18
    private lateinit var activity: Activity

    override fun starting(description: Description?) {
        scenario.scenario.onActivity {
            activity = it
        }
    }

    override fun failed(e: Throwable, description: Description) {
        val capture = Screenshot.capture(activity).apply {
            name = description.testClass.simpleName + "-" + description.methodName
            format = Bitmap.CompressFormat.PNG
        }
        try {
            processor.process(capture)
        } catch (err: IOException) {
            err.printStackTrace()
        }
    }
}
