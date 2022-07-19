package com.viliussutkus89.documenter

import androidx.core.app.NotificationManagerCompat
import androidx.test.platform.app.InstrumentationRegistry
import com.viliussutkus89.documenter.background.RemoteListenableWorkerCommon
import org.junit.Test
import java.util.*

class NotificationTest {
    @Test
    fun showNotification() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val notification = RemoteListenableWorkerCommon.createNotification(appContext)
        val notificationId = UUID.randomUUID().hashCode()
        NotificationManagerCompat.from(appContext).notify(notificationId, notification)
    }
}
