package com.example.qphotos

import android.content.Context
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class ProjectRepository(private val context: Context) {

    private val client = OkHttpClient()

    interface ProjectsCallback {
        fun onSuccess(projects: List<Project>)
        fun onError()
    }

    interface FileSystemItemsCallback {
        fun onSuccess(items: List<FileSystemItem>)
        fun onError()
    }

    fun getProjects(callback: ProjectsCallback) {
        val prefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val ip = prefs.getString("server_ip", null) ?: return callback.onError()
        val url = "http://$ip:5000/projects"
        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback.onError()
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val projects = mutableListOf<Project>()
                    val jsonArray = JSONArray(response.body!!.string())
                    for (i in 0 until jsonArray.length()) {
                        val jsonObject = jsonArray.getJSONObject(i)
                        projects.add(Project(jsonObject.getString("month"), jsonObject.getString("name")))
                    }
                    callback.onSuccess(projects)
                } else {
                    callback.onError()
                }
            }
        })
    }

    fun getContents(path: String, callback: FileSystemItemsCallback) {
        val prefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val ip = prefs.getString("server_ip", null) ?: return callback.onError()
        val url = "http://$ip:5000/browse/$path"
        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback.onError()
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val items = mutableListOf<FileSystemItem>()
                    val jsonArray = JSONArray(response.body!!.string())
                    for (i in 0 until jsonArray.length()) {
                        val jsonObject = jsonArray.getJSONObject(i)
                        items.add(FileSystemItem(jsonObject.getString("name"), jsonObject.getString("type")))
                    }
                    callback.onSuccess(items)
                } else {
                    callback.onError()
                }
            }
        })
    }
}