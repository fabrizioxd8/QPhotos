package com.example.qphotos

import android.content.Context
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import kotlinx.coroutines.delay
import java.io.File
import java.io.IOException

class UploadWorker(private val context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    companion object {
        const val TAG = "UploadWorker"
        const val NOTIFICATION_ID = 1
    }

    private fun createForegroundInfo(): ForegroundInfo {
        val channelId = "upload_channel"
        val title = context.getString(R.string.notification_title_uploading)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = context.getString(R.string.notification_channel_name)
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT)
            val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle(title)
            .setTicker(title)
            .setSmallIcon(R.drawable.ic_folder)
            .setOngoing(true)
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "Work has started.")

        try {
            setForeground(createForegroundInfo())
        } catch (e: Exception) {
            Log.e(TAG, "Error setting foreground service. Is POST_NOTIFICATIONS permission missing?", e)
        }

        val dao = AppDatabase.getDatabase(applicationContext).uploadTaskDao()

        var allUploadsSuccessful = true

        while (true) {
            val task = dao.getNextTask() ?: break

            task.status = context.getString(R.string.status_uploading)
            dao.update(task)
            Log.d(TAG, "Processing task ${task.id} for project ${task.projectName}. Status: Uploading")

            val result = ApiClient.uploadPhoto(applicationContext, task)

            if (result.isSuccess) {
                Log.i(TAG, "Successfully uploaded task ${task.id}. Deleting task and file.")
                dao.delete(task)
                delay(500) 
                File(task.imagePath).delete()
            } else {
                val exception = result.exceptionOrNull()
                if (exception is IOException && exception.message == "File not found") {
                    Log.e(TAG, "File for task ${task.id} not found. Deleting task.")
                    dao.delete(task)
                } else {
                    task.status = context.getString(R.string.status_failed)
                    dao.update(task)
                    Log.e(TAG, "Failed to upload task ${task.id}: ${exception?.message}")
                    allUploadsSuccessful = false
                }
            }
        }

        return if (allUploadsSuccessful) {
            Log.i(TAG, "All tasks completed successfully.")
            Result.success()
        } else {
            Log.w(TAG, "One or more tasks failed. Will retry later.")
            Result.retry()
        }
    }
}