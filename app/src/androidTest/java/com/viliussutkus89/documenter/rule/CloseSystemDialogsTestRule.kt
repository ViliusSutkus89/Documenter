package com.viliussutkus89.documenter.rule

import android.content.Intent
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement


class CloseSystemDialogsTestRule: TestRule {
    override fun apply(base: Statement, description: Description?): Statement {
        // ACTION_CLOSE_SYSTEM_DIALOGS is perfectly fine in tests
        @Suppress("DEPRECATION")
        InstrumentationRegistry.getInstrumentation().context
            .sendBroadcast(Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS))
        return base
    }
}