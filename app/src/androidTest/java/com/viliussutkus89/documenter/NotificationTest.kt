package com.viliussutkus89.documenter

import androidx.core.app.NotificationManagerCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.viliussutkus89.documenter.background.ConverterWorkerCommon
import com.viliussutkus89.documenter.rule.CloseSystemDialogsTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*


@RunWith(AndroidJUnit4::class)
// Issue #8 workaround
@SdkSuppress(maxSdkVersion = 20)
class NotificationTest {
    @get:Rule
    val actionCloseSystemDialogsRule = CloseSystemDialogsTestRule()

    @Test
    fun showNotification() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val notification = ConverterWorkerCommon.createNotification(appContext)
        val notificationId = UUID.randomUUID().hashCode()
        NotificationManagerCompat.from(appContext).notify(notificationId, notification)
    }
}
