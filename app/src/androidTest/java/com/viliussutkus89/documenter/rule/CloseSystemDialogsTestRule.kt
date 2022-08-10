package com.viliussutkus89.documenter.rule

import android.content.Intent
import android.os.Build
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement


class CloseSystemDialogsTestRule: TestRule {
    override fun apply(base: Statement, description: Description?): Statement {
        // ACTION_CLOSE_SYSTEM_DIALOGS is perfectly fine in tests
        @Suppress("DEPRECATION")
        InstrumentationRegistry.getInstrumentation().context
            .sendBroadcast(Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS))

        // Watch for ANRs and click wait
        if (Build.VERSION.SDK_INT >= 18) {
            @OptIn(DelicateCoroutinesApi::class)
            GlobalScope.launch {
                val anrSelector = UiSelector()
                    .packageName("android")
                    .textContains("isn't responding")

                val waitButtonSelector = UiSelector()
                    .packageName("android")
                    .className("android.widget.Button")
                    .textContains("Wait")

                val uiDevice: UiDevice =
                    UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
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

        return base
    }
}