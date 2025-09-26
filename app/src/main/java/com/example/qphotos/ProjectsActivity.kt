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
        recyclerView.layoutManager = LinearLayoutManager(this)

        projectsAdapter = ProjectsAdapter(
            currentProjects,
            onItemClicked = { project -> onProjectClicked(project) },
            onItemLongClicked = { project -> showOptionsDialog(project) } // Long press shows our dialog
        )
        recyclerView.adapter = projectsAdapter
        // CAMBIO: No more registerForContextMenu
    }

    // NUEVO: This function creates and shows our custom icon menu
    private fun showOptionsDialog(project: Project) {
        val dialogView = layoutInflater.inflate(R.layout.project_options_menu, null)
        val dialog = AlertDialog.Builder(this)
            .setTitle("Opciones para '${project.name}'")
            .setView(dialogView)
            .setNegativeButton("Cancelar", null)
            .create()

        val btnEdit = dialogView.findViewById<TextView>(R.id.option_edit)
        val btnDelete = dialogView.findViewById<TextView>(R.id.option_delete)

        btnEdit.setOnClickListener {
            dialog.dismiss()
            showRenameDialog(project)
        }
        btnDelete.setOnClickListener {
            dialog.dismiss()
            showDeleteConfirmationDialog(project)
        }

        dialog.show()
    }


    fun onProjectClicked(project: Project) {
        val intent = Intent(this, GalleryActivity::class.java).apply {
            putExtra("PROJECT_NAME", project.name)
            putExtra("MONTH_FOLDER", project.month)
        }
        startActivity(intent)
    }

    private fun fetchProjects() {
        val prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val ip = prefs.getString("server_ip", null)
        if (ip.isNullOrBlank()) {
            Toast.makeText(this, "IP del servidor no configurada.", Toast.LENGTH_LONG).show()
            finish(); return
        }
        val url = "http://$ip:5000/projects"
        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { Toast.makeText(applicationContext, "Fallo al obtener proyectos", Toast.LENGTH_SHORT).show() }
            }
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    response.body.let {
                        val responseBodyString = it.string()
                        val projects = mutableListOf<Project>()
                        val jsonArray = JSONArray(responseBodyString)
                        for (i in 0 until jsonArray.length()) {
                            val jsonObject = jsonArray.getJSONObject(i)
                            projects.add(Project(jsonObject.getString("month"), jsonObject.getString("name")))
                        }
                        runOnUiThread {
                            currentProjects.clear()
                            currentProjects.addAll(projects)
                            projectsAdapter.updateProjects(projects)
                        }
                    }
                }
            }
        })
    }

    private fun showRenameDialog(project: Project) {
                val editText = EditText(this).apply { setText(project.name) }
                AlertDialog.Builder(this)
                    .setTitle("Renombrar Proyecto")
                    .setView(editText)
                    .setPositiveButton("Guardar") { _, _ ->
                        val newName = editText.text.toString().trim()
                        if (newName.isNotEmpty() && newName != project.name) {
                            renameProject(project, newName)
                        }
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }

            private fun renameProject(project: Project, newName: String) {
                val prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
                val ip = prefs.getString("server_ip", null) ?: return
                val url = "http://$ip:5000/project/${project.month}/${project.name}"

                val json = JSONObject().apply { put("new_name", newName) }
                val body = json.toString()
                    .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

                val request = Request.Builder().url(url).put(body).build()

                client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        runOnUiThread {
                            Toast.makeText(
                                applicationContext,
                                "Fallo al renombrar",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                    override fun onResponse(call: Call, response: Response) {
                        if (response.isSuccessful) {
                            runOnUiThread {
                                Toast.makeText(
                                    applicationContext,
                                    "Proyecto renombrado",
                                    Toast.LENGTH_SHORT
                                ).show()
                                fetchProjects() // Refresh the list
                            }
                        } else {
                            runOnUiThread {
                                Toast.makeText(
                                    applicationContext,
                                    "Error del servidor al renombrar",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                })
            }

            // --- COMPLETE DELETE FUNCTIONS ---
            private fun showDeleteConfirmationDialog(project: Project) {
                AlertDialog.Builder(this)
                    .setTitle("Borrar Proyecto")
                    .setMessage("¿Estás seguro de que quieres borrar el proyecto '${project.name}' y todas sus fotos? Esta acción no se puede deshacer.")
                    .setPositiveButton("Borrar") { _, _ ->
                        deleteProject(project)
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }

            private fun deleteProject(project: Project) {
                val prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
                val ip = prefs.getString("server_ip", null) ?: return
                val url = "http://$ip:5000/project/${project.month}/${project.name}"

                val request = Request.Builder().url(url).delete().build()

                client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        runOnUiThread {
                            Toast.makeText(
                                applicationContext,
                                "Fallo al borrar el proyecto",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                    override fun onResponse(call: Call, response: Response) {
                        runOnUiThread {
                            if (response.isSuccessful) {
                                Toast.makeText(
                                    applicationContext,
                                    "Proyecto borrado",
                                    Toast.LENGTH_SHORT
                                ).show()
                                fetchProjects() // Refresh the list to reflect the change
                            } else {
                                Toast.makeText(
                                    applicationContext,
                                    "Error del servidor al borrar",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                })
            }
        }

