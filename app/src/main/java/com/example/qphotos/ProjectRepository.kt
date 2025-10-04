package com.example.qphotos

import android.content.Context
import android.content.Context.MODE_PRIVATE
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.IOException

data class FileSystemItem(
    val name: String,
    val path: String,
    val type: String // "month", "project", or "day"
)

class ProjectRepository(private val context: Context) {

    private val client = OkHttpClient()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    interface FileSystemItemsCallback {
        fun onSuccess(items: List<FileSystemItem>)
        fun onError()
    }

    interface Callback {
        fun onSuccess()
        fun onError()
    }

    fun getContents(path: String, callback: FileSystemItemsCallback) {
        val prefs = context.getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val ip = prefs.getString("server_ip", null)
        if (ip == null) {
            callback.onError()
            return
        }
        val url = "http://$ip:5000/browse/$path"
        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback.onError()
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    if (responseBody != null) {
                        try {
                            val jsonArray = JSONArray(responseBody)
                            val items = mutableListOf<FileSystemItem>()
                            for (i in 0 until jsonArray.length()) {
                                val jsonObject = jsonArray.getJSONObject(i)
                                val item = FileSystemItem(
                                    name = jsonObject.getString("name"),
                                    path = jsonObject.getString("path"),
                                    type = jsonObject.getString("type")
                                )
                                items.add(item)
                            }
                            callback.onSuccess(items)
                        } catch (e: JSONException) {
                            callback.onError()
                        }
                    } else {
                        callback.onError()
                    }
                } else {
                    callback.onError()
                }
            }
        })
    }

    fun renameProject(item: FileSystemItem, newName: String, callback: Callback) {
        val month = File(item.path).parent ?: return
        val name = File(item.path).name

        val prefs = context.getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val ip = prefs.getString("server_ip", null) ?: return
        val url = "http://$ip:5000/project/$month/$name"

        val jsonObject = JSONObject()
        jsonObject.put("new_name", newName)
        val body = jsonObject.toString().toRequestBody(jsonMediaType)

        val request = Request.Builder()
            .url(url)
            .put(body)
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback.onError()
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    callback.onSuccess()
                } else {
                    callback.onError()
                }
            }
        })
    }

    fun deleteProject(item: FileSystemItem, callback: Callback) {
        val month = File(item.path).parent ?: return
        val name = File(item.path).name

        val prefs = context.getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val ip = prefs.getString("server_ip", null) ?: return
        val url = "http://$ip:5000/project/$month/$name"

        val request = Request.Builder()
            .url(url)
            .delete()
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback.onError()
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    callback.onSuccess()
                } else {
                    callback.onError()
                }
            }
        })
    }
}
