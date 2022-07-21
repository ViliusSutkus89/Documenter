/*
 * ConverterWorkerCommon.kt
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

package com.viliussutkus89.documenter.background

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.core.app.NotificationCompat
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.multiprocess.RemoteListenableWorker
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
    }

    override fun getForegroundInfoAsync(): ListenableFuture<ForegroundInfo?> {
        return CallbackToFutureAdapter.getFuture { completer: CallbackToFutureAdapter.Completer<ForegroundInfo?> ->
            completer.set(ForegroundInfo(id.hashCode(), createNotification(applicationContext)))
        }
    }

    private val cachedFile = File(inputData.getString(DATA_KEY_CACHED_FILE)
        ?: throw IllegalArgumentException("DATA_KEY_CACHED_FILE is null"))

    private val convertedFile = File(inputData.getString(DATA_KEY_CONVERTED_FILE)
        ?: throw IllegalArgumentException("DATA_KEY_CONVERTED_FILE is null"))

    abstract fun doWorkSync(inputFile: File): File?

    override fun startRemoteWork(): ListenableFuture<Result> {
        return CallbackToFutureAdapter.getFuture { completer: CallbackToFutureAdapter.Completer<Result> ->

            doWorkSync(cachedFile)?.run {
                renameTo(convertedFile)
            } ?: run {
                return@getFuture completer.set(Result.failure())
            }

            return@getFuture completer.set(Result.success())
        }
    }
}
