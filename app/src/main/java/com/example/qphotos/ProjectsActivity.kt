package com.example.qphotos

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

// (La data class 'Project' debe estar en su propio archivo Project.kt)

class ProjectsActivity : AppCompatActivity() {

    private val client = OkHttpClient()
    private var currentProjects = mutableListOf<Project>()
    private lateinit var projectsAdapter: ProjectsAdapter
    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_projects)
        title = "Proyectos Subidos"
        setupRecyclerView()
        fetchProjects()
    }

    // CAMBIO: Esta función ya no es necesaria, la lógica está en el adapter
    // override fun onContextItemSelected(...) { ... }

    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.projectsRecyclerView)
        recyclerView.layoutManager = androidx.recyclerview.widget.GridLayoutManager(this, 2) // 2 columns for a grid
        projectsAdapter = ProjectsAdapter(currentProjects)
        recyclerView.adapter = projectsAdapter
    }

    private fun fetchProjects() {
        val prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val ip = prefs.getString("server_ip", null)
        if (ip.isNullOrBlank()) {
            Toast.makeText(this, "IP del servidor no configurada.", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        val baseUrl = "http://$ip:5000"
        val url = "$baseUrl/projects"
        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { Toast.makeText(applicationContext, "Fallo al obtener proyectos", Toast.LENGTH_SHORT).show() }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseBodyString = response.body?.string()
                    if (responseBodyString != null) {
                        val projects = mutableListOf<Project>()
                        val jsonArray = JSONArray(responseBodyString)
                        for (i in 0 until jsonArray.length()) {
                            val jsonObject = jsonArray.getJSONObject(i)
                            val thumbnailUrl = jsonObject.optString("thumbnail", "") // Get the new field
                            projects.add(
                                Project(
                                    month = jsonObject.getString("month"),
                                    name = jsonObject.getString("name"),
                                    thumbnailUrl = if (thumbnailUrl.isNotBlank()) "$baseUrl/uploads/$thumbnailUrl" else ""
                                )
                            )
                        }
                        runOnUiThread {
                            projectsAdapter.updateProjects(projects)
                        }
                    }
                }
            }
        })
    }
}

