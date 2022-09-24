/*
 * ScreenshotFailedTestRule.kt
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

import android.app.Activity
import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.screenshot.BasicScreenCaptureProcessor
import androidx.test.runner.screenshot.ScreenCapture
import androidx.test.runner.screenshot.Screenshot
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import java.io.File
import java.io.IOException


class ScreenshotFailedTestRule<T>(private val scenario: ActivityScenarioRule<T>): TestWatcher() where T: Activity {

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

    private fun makeScreenshot(screenCapture: ScreenCapture, description: Description) {
        screenCapture.apply {
            name = description.testClass.simpleName + "-" + description.methodName
            format = Bitmap.CompressFormat.PNG
        }
        try {
            processor.process(screenCapture)
        } catch (err: IOException) {
            err.printStackTrace()
        }
    }

    override fun failed(e: Throwable, description: Description) {
        if (Build.VERSION.SDK_INT >= 18) {
            makeScreenshot(Screenshot.capture(), description)
        } else {
            scenario.scenario.onActivity { activity ->
                makeScreenshot(Screenshot.capture(activity), description)
            }
        }
    }
}
