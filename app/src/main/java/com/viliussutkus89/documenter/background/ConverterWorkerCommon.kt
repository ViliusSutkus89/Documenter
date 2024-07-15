/*
 * ConverterWorkerCommon.kt
 *
 * Copyright (C) 2022 ViliusSutkus89.com
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

package com.viliussutkus89.documenter.background

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.core.app.NotificationCompat
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.multiprocess.RemoteListenableWorker
import androidx.work.workDataOf
import com.google.common.util.concurrent.ListenableFuture
import com.viliussutkus89.documenter.R
import java.io.File


abstract class ConverterWorkerCommon(ctx: Context, params: WorkerParameters): RemoteListenableWorker(ctx, params) {
    companion object {
        @RequiresApi(Build.VERSION_CODES.O)
        private fun createNotificationChannel(context: Context) {
            val id: String = context.getString(R.string.converter_worker_notification_channel_id)
            val name: CharSequence = context.getString(R.string.converter_worker_notification_channel_name)
            val description: String = context.getString(R.string.converter_worker_notification_channel_description)
            val channel = android.app.NotificationChannel(id, name, NotificationManager.IMPORTANCE_LOW)
            channel.description = description
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        fun createNotification(context: Context): Notification {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                createNotificationChannel(context)
            }

            val channelId = context.getString(R.string.converter_worker_notification_channel_id)
            val title = context.getString(R.string.converter_worker_notification_title)

            return NotificationCompat.Builder(context, channelId)
                .setContentTitle(title)
                .setTicker(title)
                .setSmallIcon(R.drawable.ic_photo_filter)
                .setOngoing(true)
                .build()
        }

        @JvmStatic
        protected fun commonDataBuilder(cachedSourceFile: File, convertedHtmlFile: File, packageName: String): Data.Builder {
            return Data.Builder()
                .putString(ARGUMENT_PACKAGE_NAME, packageName)
                .putString(DATA_KEY_CACHED_FILE, cachedSourceFile.path)
                .putString(DATA_KEY_CONVERTED_FILE, convertedHtmlFile.path)
        }
    }

    override fun getForegroundInfoAsync(): ListenableFuture<ForegroundInfo?> {
        return CallbackToFutureAdapter.getFuture {
            it.set(ForegroundInfo(id.hashCode(), createNotification(applicationContext),
                if (Build.VERSION.SDK_INT >= 29) FOREGROUND_SERVICE_TYPE_DATA_SYNC else 0
            ))
        }
    }

    private val cachedFile = File(inputData.getString(DATA_KEY_CACHED_FILE)
        ?: throw IllegalArgumentException("DATA_KEY_CACHED_FILE is null"))

    private val convertedFile = File(inputData.getString(DATA_KEY_CONVERTED_FILE)
        ?: throw IllegalArgumentException("DATA_KEY_CONVERTED_FILE is null"))

    abstract fun doWorkSync(inputFile: File): File?

    protected var copyProtected = false

    override fun startRemoteWork(): ListenableFuture<Result> {
        return CallbackToFutureAdapter.getFuture { completer: CallbackToFutureAdapter.Completer<Result> ->

            doWorkSync(cachedFile)?.run {
                renameTo(convertedFile)
            } ?: run {
                return@getFuture completer.set(Result.failure())
            }

            return@getFuture completer.set(Result.success(workDataOf(
                DATA_KEY_DOCUMENT_COPY_PROTECTED to copyProtected
            )))
        }
    }
}
