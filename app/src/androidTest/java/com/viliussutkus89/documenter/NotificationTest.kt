package com.viliussutkus89.documenter

import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.viliussutkus89.documenter.background.ConverterWorkerCommon
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*

@RunWith(AndroidJUnit4::class)
class NotificationTest {
    @Before
    fun setUp() {
        @Suppress("Deprecated") // ACTION_CLOSE_SYSTEM_DIALOGS is perfectly fine in tests
        InstrumentationRegistry.getInstrumentation().context.sendBroadcast(Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS))
    }

    @Test
    fun showNotification() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val notification = ConverterWorkerCommon.createNotification(appContext)
        val notificationId = UUID.randomUUID().hashCode()
        NotificationManagerCompat.from(appContext).notify(notificationId, notification)
    }
}
