/*
 * CloseSystemDialogsTestRule.kt
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

import android.content.Intent
import android.os.Build
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.*
import org.junit.rules.ExternalResource


class CloseSystemDialogsTestRule: ExternalResource() {
    var dialogClicker: Job? = null

    override fun before() {
        // ACTION_CLOSE_SYSTEM_DIALOGS is perfectly fine in tests
        @Suppress("DEPRECATION")
        InstrumentationRegistry.getInstrumentation().context
            .sendBroadcast(Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS))

        // Watch for ANRs and click wait
        if (Build.VERSION.SDK_INT >= 18) {
            dialogClicker = CoroutineScope(Dispatchers.Default).launch {
                val anrSelector = androidx.test.uiautomator.UiSelector()
                    .packageName("android")
                    .textContains("isn't responding")

                val waitButtonSelector = androidx.test.uiautomator.UiSelector()
                    .packageName("android")
                    .className("android.widget.Button")
                    .textContains("Wait")

                val uiDevice: androidx.test.uiautomator.UiDevice =
                    androidx.test.uiautomator.UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
                while (true) {
                    yield()
                    if (uiDevice.findObject(anrSelector).exists()) {
                        try {
                            uiDevice.findObject(waitButtonSelector).click()
                        } catch (_: Exception) {
                        }
                    }
                }
            }
        }
    }

    override fun after() {
        dialogClicker?.cancel()
    }
}
