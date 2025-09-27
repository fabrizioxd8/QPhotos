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
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody // NUEVO: Import para streaming
import java.io.File
import java.util.concurrent.TimeUnit

// CAMBIO: The constructor is simplified to use the default context.
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
            // CAMBIO: Use the built-in 'applicationContext'
            val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        // CAMBIO: Use the built-in 'applicationContext'
        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle(title)
            .setTicker(title)
            .setSmallIcon(android.R.drawable.ic_menu_upload)
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

        // CAMBIO: Use the built-in 'applicationContext' here as well
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

        // CAMBIO: We now track the overall success of the batch
        var allUploadsSuccessful = true

        while (true) {
            val task = dao.getNextTask() ?: break // Get next task or exit if queue is empty

            Log.d(TAG, "Processing task ${task.id} for project ${task.projectName}")
            val success = uploadTask(task, client, baseUrl)

            if (success) {
                Log.i(TAG, "Successfully uploaded task ${task.id}. Deleting task and file.")
                dao.delete(task)
                File(task.imagePath).delete()
            } else {
                Log.e(TAG, "Failed to upload task ${task.id}.")
                // CAMBIO: We mark the job as failed but continue the loop to the next photo
                allUploadsSuccessful = false
            }
        }

        // After attempting all photos, we return the final status
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

            // The file is streamed directly into the request body.
            // This uses almost no memory, no matter how big the photo is.
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