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
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.util.concurrent.TimeUnit

class UploadWorker(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    companion object {
        const val TAG = "UploadWorker"
        const val NOTIFICATION_ID = 1
    }

    private fun createForegroundInfo(): ForegroundInfo {
        val channelId = "upload_channel"
        val title = "Subiendo fotos..."

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Canal de Subida", NotificationManager.IMPORTANCE_DEFAULT)
            val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle(title)
            .setTicker(title)
            .setSmallIcon(R.drawable.ic_folder) // A more fitting icon
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
        val prefs = applicationContext.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val ip = prefs.getString("server_ip", null)

        if (ip.isNullOrBlank()) {
            Log.e(TAG, "Server IP is not configured. Work failed.")
            return Result.failure()
        }

        val baseUrl = "http://$ip:5000"
        val client = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()

        var allUploadsSuccessful = true

        while (true) {
            val task = dao.getNextTask() ?: break

            task.status = "Subiendo..."
            dao.update(task)
            Log.d(TAG, "Processing task ${task.id} for project ${task.projectName}. Status: Uploading")

            val success = uploadTask(task, client, baseUrl)

            if (success) {
                task.status = "Completo"
                dao.update(task)
                Log.i(TAG, "Successfully uploaded task ${task.id}. Deleting task and file.")
                delay(500) // Keep "Complete" status visible for a moment
                dao.delete(task)
                File(task.imagePath).delete()
            } else {
                task.status = "Fallido"
                dao.update(task)
                Log.e(TAG, "Failed to upload task ${task.id}.")
                allUploadsSuccessful = false
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

    private fun uploadTask(task: UploadTask, client: OkHttpClient, baseUrl: String): Boolean {
        return try {
            val file = File(task.imagePath)
            if (!file.exists()) return true

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("project_name", task.projectName)
                .addFormDataPart("uuid", task.uuid)
                .addFormDataPart("file", file.name, file.asRequestBody("image/jpeg".toMediaTypeOrNull()))
                .build()

            val request = Request.Builder().url("$baseUrl/upload").post(requestBody).build()
            val response = client.newCall(request).execute()

            Log.d(TAG, "Task ${task.id} - Server response: ${response.code}")
            response.isSuccessful

        } catch (e: Exception) {
            Log.e(TAG, "Exception during uploadTask for task ${task.id}: ${e.message}")
            e.printStackTrace()
            false
        }
    }
}