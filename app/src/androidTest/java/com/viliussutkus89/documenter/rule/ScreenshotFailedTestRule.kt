/*
 * ScreenshotFailedTestRule.kt
 *
 * Copyright (C) 2022, 2024 https://www.ViliusSutkus89.com/documenter/
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

import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import androidx.test.core.app.takeScreenshotNoSync
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.screenshot.captureToBitmap
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import java.io.File
import java.io.FileOutputStream


class ScreenshotFailedTestRule: TestWatcher() {

    companion object {
        private const val TAG = "ScreenFailedTestRule"
    }

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
                    Log.v(TAG, "setting screenshot path to " + it.path)
                    return it
                }
            }
        return null
    }

    override fun failed(e: Throwable, description: Description) {
        getStorageDir()?.let { storageDir ->
            val fileName = (description.testClass.simpleName + "-" + description.methodName).filter {
                it.isLetterOrDigit() || listOf('-', '_', '.').contains(it)
            }
            val outputFile = storageDir.resolve("$fileName.png")
            FileOutputStream(outputFile).use { outputFileStream ->
                val bitmap = if (Build.VERSION.SDK_INT >= 18) {
                    takeScreenshotNoSync()
                } else {
                    onView(isRoot()).captureToBitmap()
                }
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputFileStream)
            }
        }
    }
}
