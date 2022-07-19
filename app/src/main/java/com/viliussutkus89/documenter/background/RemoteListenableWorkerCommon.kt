/*
 * RemoteListenableWorkerCommon.kt
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
import com.viliussutkus89.documenter.model.DocumentDatabase
import com.viliussutkus89.documenter.model.State
import com.viliussutkus89.documenter.model.getCachedSourceFile
import com.viliussutkus89.documenter.model.getConvertedHtmlFile
import java.io.File

abstract class RemoteListenableWorkerCommon(ctx: Context, params: WorkerParameters): RemoteListenableWorker(ctx, params) {
    companion object {
        const val INPUT_KEY_DOCUMENT_ID = "key_id"
    }

    private val documentId = inputData.getLong(INPUT_KEY_DOCUMENT_ID, -1)
    private val documentDao by lazy { DocumentDatabase.getDatabase(applicationContext).documentDao() }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val ctx = applicationContext
        val id: String = ctx.getString(R.string.converter_worker_notification_channel_id)
        val name: CharSequence = ctx.getString(R.string.converter_worker_notification_channel_name)
        val description: String = ctx.getString(R.string.converter_worker_notification_channel_description)
        val channel = android.app.NotificationChannel(id, name, NotificationManager.IMPORTANCE_LOW)
        channel.description = description
        ctx.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    override fun getForegroundInfoAsync(): ListenableFuture<ForegroundInfo?> {
        return CallbackToFutureAdapter.getFuture { completer: CallbackToFutureAdapter.Completer<ForegroundInfo?> ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                createNotificationChannel()
            }

            val channelId = applicationContext.getString(R.string.converter_worker_notification_channel_id)
            val title = applicationContext.getString(R.string.converter_worker_notification_title)

            val notification: Notification = NotificationCompat.Builder(applicationContext, channelId)
                .setContentTitle(title)
                .setTicker(title)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setOngoing(true)
                .build()

            // Use WorkRequest ID to generate Notification ID.
            // Each Notification ID must be unique to create a new notification for each work request.
            val notificationId = id.hashCode()
            completer.set(ForegroundInfo(notificationId, notification))
        }
    }

    abstract fun doWorkSync(inputFile: File): File?

    override fun startRemoteWork(): ListenableFuture<Result> {
        return CallbackToFutureAdapter.getFuture { completer: CallbackToFutureAdapter.Completer<Result> ->
            if (documentId == (-1).toLong()) {
                return@getFuture completer.set(Result.failure())
            }
            var document = documentDao.getDocument(documentId)
            documentDao.updateState(documentId, State.Converting)

            val cachedFile = document.getCachedSourceFile(appCacheDir = applicationContext.cacheDir)

            val convertedFile = doWorkSync(cachedFile) ?: return@getFuture completer.set(Result.failure())

            document = document.copy(convertedFilename = convertedFile.name, state = State.Converted)
            convertedFile.renameTo(document.getConvertedHtmlFile(appFilesDir = applicationContext.filesDir)!!)
            documentDao.update(document)

            return@getFuture completer.set(Result.success())
        }
    }
}
