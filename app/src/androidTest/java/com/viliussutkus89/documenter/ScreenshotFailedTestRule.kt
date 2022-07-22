/*
 * ScreenshotFailedTestRule.kt
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
import android.graphics.Bitmap
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.screenshot.BasicScreenCaptureProcessor
import androidx.test.runner.screenshot.Screenshot
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import java.io.File
import java.io.IOException

class ScreenshotFailedTestRule: TestWatcher() {
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
    lateinit var activity: Activity

    override fun failed(e: Throwable, description: Description) {
        val capture = if (::activity.isInitialized) {
            Screenshot.capture(activity)
        } else {
            Screenshot.capture()
        }
        capture.apply {
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
