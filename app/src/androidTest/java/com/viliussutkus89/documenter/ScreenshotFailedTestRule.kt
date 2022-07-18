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

import android.app.Instrumentation
import android.os.Environment
import androidx.test.uiautomator.UiDevice
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import java.io.File
import java.io.IOException

class ScreenshotFailedTestRule(private val instrumentation: Instrumentation): TestWatcher() {
    override fun failed(e: Throwable, description: Description) {
        val folder = instrumentation.targetContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val screenshotFile = File(folder, description.testClass.simpleName + "-" + description.methodName + ".png")
        val uiDevice = UiDevice.getInstance(instrumentation)
        try {
            uiDevice.takeScreenshot(screenshotFile)
        } catch (err: IOException) {
            err.printStackTrace()
        }
    }
}
