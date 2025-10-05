package com.example.qphotos

import android.content.Context
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.IOException

object ApiClient {

    private val client = OkHttpClient()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private fun getBaseUrl(context: Context): String? {
        val prefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val ip = prefs.getString("server_ip", null)
        return if (ip.isNullOrBlank()) null else "http://$ip:5000"
    }

    fun getThumbnailUrl(context: Context, path: String): String? {
        val baseUrl = getBaseUrl(context) ?: return null
        return "$baseUrl/thumbnail/$path"
    }

    fun getUploadUrl(context: Context, path: String): String? {
        val baseUrl = getBaseUrl(context) ?: return null
        return "$baseUrl/uploads/$path"
    }

    // Generic callback interfaces
    interface ApiCallback<T> {
        fun onSuccess(result: T)
        fun onError(message: String)
    }

    interface SimpleCallback {
        fun onSuccess()
        fun onError(message: String)
    }

    // Synchronous function for use in workers
    fun uploadPhoto(context: Context, task: UploadTask): Result<Unit> {
        val baseUrl = getBaseUrl(context) ?: return Result.failure(IOException("Server IP not configured"))
        val url = "$baseUrl/upload"

        val file = File(task.imagePath)
        if (!file.exists()) {
            return Result.failure(IOException("File not found"))
        }

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", file.name, file.asRequestBody("image/jpeg".toMediaTypeOrNull()))
            .addFormDataPart("project_name", task.projectName)
            .addFormDataPart("uuid", task.uuid)
            .build()

        val request = Request.Builder().url(url).post(requestBody).build()

        return try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(IOException("Server error: ${response.code}"))
            }
        } catch (e: IOException) {
            Result.failure(e)
        }
    }


    fun getContents(context: Context, path: String, callback: ApiCallback<List<FileSystemItem>>) {
        val baseUrl = getBaseUrl(context) ?: return callback.onError("Server IP not configured")
        val url = "$baseUrl/browse/$path"
        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback.onError(e.message ?: "Network error")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    try {
                        val items = JSONArray(response.body.string()).let { array ->
                            List(array.length()) { i -> array.getJSONObject(i) }.map { obj ->
                                FileSystemItem(
                                    name = obj.getString("name"),
                                    path = obj.getString("path"),
                                    type = obj.getString("type")
                                )
                            }
                        }
                        callback.onSuccess(items)
                    } catch (_: JSONException) {
                        callback.onError("Error parsing server response")
                    }
                } else {
                    callback.onError("Server error: ${response.code}")
                }
            }
        })
    }

    fun renameProject(context: Context, item: FileSystemItem, newName: String, callback: SimpleCallback) {
        val baseUrl = getBaseUrl(context) ?: return callback.onError("Server IP not configured")
        val month = File(item.path).parent ?: return
        val name = File(item.path).name
        val url = "$baseUrl/project/$month/$name"

        val jsonObject = JSONObject().put("new_name", newName)
        val body = jsonObject.toString().toRequestBody(jsonMediaType)
        val request = Request.Builder().url(url).put(body).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback.onError(e.message ?: "Network error")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) callback.onSuccess() else callback.onError("Server error: ${response.code}")
            }
        })
    }

    fun deleteProject(context: Context, item: FileSystemItem, callback: SimpleCallback) {
        val baseUrl = getBaseUrl(context) ?: return callback.onError("Server IP not configured")
        val month = File(item.path).parent ?: return
        val name = File(item.path).name
        val url = "$baseUrl/project/$month/$name"

        val request = Request.Builder().url(url).delete().build()

        client.newCall(request).enqueue(object : Callback {
             override fun onFailure(call: Call, e: IOException) {
                callback.onError(e.message ?: "Network error")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) callback.onSuccess() else callback.onError("Server error: ${response.code}")
            }
        })
    }

    fun fetchLastProject(context: Context, callback: ApiCallback<String>) {
        val baseUrl = getBaseUrl(context) ?: return callback.onError("Server IP not configured")
        val request = Request.Builder().url("$baseUrl/last-project").build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                 callback.onError(e.message ?: "Network error")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val lastProject = JSONObject(response.body.string()).optString("last_project", "")
                    callback.onSuccess(lastProject)
                } else {
                    callback.onError("Server error: ${response.code}")
                }
            }
        })
    }

    fun fetchProjectList(context: Context, callback: ApiCallback<List<String>>) {
        val baseUrl = getBaseUrl(context) ?: return callback.onError("Server IP not configured")
        val request = Request.Builder().url("$baseUrl/projects_current_month").build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback.onError(e.message ?: "Network error")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val projects = JSONArray(response.body.string()).let { array ->
                        List(array.length()) { i -> array.getString(i) }
                    }
                    callback.onSuccess(projects)
                } else {
                    callback.onError("Server error: ${response.code}")
                }
            }
        })
    }

    fun deletePhoto(context: Context, photoUrl: String, callback: SimpleCallback) {
        val baseUrl = getBaseUrl(context) ?: return callback.onError("Server IP not configured")
        val request = Request.Builder().url("$baseUrl/photo/$photoUrl").delete().build()

        client.newCall(request).enqueue(object : Callback {
             override fun onFailure(call: Call, e: IOException) {
                callback.onError(e.message ?: "Network error")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) callback.onSuccess() else callback.onError("Server error: ${response.code}")
            }
        })
    }
}