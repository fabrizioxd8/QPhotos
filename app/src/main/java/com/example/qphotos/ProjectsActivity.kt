package com.example.qphotos

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.EditText
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

class ProjectsActivity : AppCompatActivity() {

    private val client = OkHttpClient()
    private lateinit var projectsAdapter: ProjectsAdapter
    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_projects)
        title = "Proyectos Subidos"
        setupRecyclerView()
        fetchProjects()
    }

    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.projectsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        projectsAdapter = ProjectsAdapter(
            emptyList(),
            onItemClicked = { project -> onProjectClicked(project) },
            onItemLongClicked = { project -> showOptionsDialog(project) }
        )
        recyclerView.adapter = projectsAdapter
    }

    private fun onProjectClicked(project: Project) {
        val intent = Intent(this, GalleryActivity::class.java).apply {
            putExtra("PROJECT_NAME", project.name)
            putExtra("MONTH_FOLDER", project.month)
        }
        startActivity(intent)
    }

    private fun showOptionsDialog(project: Project) {
        val options = arrayOf("Renombrar", "Borrar")
        AlertDialog.Builder(this)
            .setTitle("Opciones para '${project.name}'")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showRenameDialog(project)
                    1 -> showDeleteConfirmationDialog(project)
                }
            }
            .show()
    }

    private fun fetchProjects() {
        val prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val ip = prefs.getString("server_ip", null)
        if (ip.isNullOrBlank()) {
            Toast.makeText(this, "IP del servidor no configurada.", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        val url = "http://$ip:5000/projects"
        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { Toast.makeText(applicationContext, "Fallo al obtener proyectos", Toast.LENGTH_SHORT).show() }
            }
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    response.body?.let {
                        val responseBodyString = it.string()
                        val projects = mutableListOf<Project>()
                        val jsonArray = JSONArray(responseBodyString)
                        for (i in 0 until jsonArray.length()) {
                            val jsonObject = jsonArray.getJSONObject(i)
                            projects.add(Project(jsonObject.getString("month"), jsonObject.getString("name")))
                        }
                        runOnUiThread {
                            projectsAdapter.updateProjects(projects)
                        }
                    }
                }
            }
        })
    }

    private fun showRenameDialog(project: Project) {
        val editText = EditText(this).apply { setText(project.name) }

        val titleView = TextView(this).apply {
            text = "Renombrar Proyecto"
            setPadding(60, 30, 60, 30)
            textSize = 20f
            setTextColor(Color.parseColor("#03CFB5"))
        }

        val dialog = AlertDialog.Builder(this)
            .setCustomTitle(titleView)
            .setView(editText)
            .setPositiveButton("Guardar") { _, _ ->
                val newName = editText.text.toString().trim()
                if (newName.isNotEmpty() && newName != project.name) {
                    renameProject(project, newName)
                }
            }
            .setNegativeButton("Cancelar", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(Color.parseColor("#03CFB5"))
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(Color.parseColor("#03CFB5"))
        }
        dialog.show()
    }

    private fun renameProject(project: Project, newName: String) {
        val prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val ip = prefs.getString("server_ip", null) ?: return
        val url = "http://$ip:5000/project/${project.month}/${project.name}"

        val json = JSONObject().apply { put("new_name", newName) }
        val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        val request = Request.Builder().url(url).put(body).build()

        val dialog = AlertDialog.Builder(this)
            .setTitle("Renombrar Proyecto")
            .setView(editText)
            .setPositiveButton("Guardar") { _, _ ->
                val newName = editText.text.toString().trim()
                if (newName.isNotEmpty() && newName != project.name) {
                    renameProject(project, newName)
                }
            }
            .setNegativeButton("Cancelar", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(Color.parseColor("#03CFB5"))
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(Color.parseColor("#03CFB5"))
        }
        dialog.show()
    }

    private fun renameProject(project: Project, newName: String) {
        val prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val ip = prefs.getString("server_ip", null) ?: return
        val url = "http://$ip:5000/project/${project.month}/${project.name}"

        val json = JSONObject().apply { put("new_name", newName) }
        val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        val request = Request.Builder().url(url).put(body).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { Toast.makeText(applicationContext, "Fallo al renombrar", Toast.LENGTH_SHORT).show() }
            }
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    runOnUiThread {
                        Toast.makeText(applicationContext, "Proyecto renombrado", Toast.LENGTH_SHORT).show()
                        fetchProjects()
                    }
                } else {
                    runOnUiThread { Toast.makeText(applicationContext, "Error del servidor al renombrar", Toast.LENGTH_SHORT).show() }
                }
            }
        })
    }

    private fun showDeleteConfirmationDialog(project: Project) {

        val titleView = TextView(this).apply {
            text = "Borrar Proyecto"
            setPadding(60, 30, 60, 30)
            textSize = 20f
            setTextColor(Color.parseColor("#03CFB5"))
        }

        val dialog = AlertDialog.Builder(this)
            .setCustomTitle(titleView)
            .setMessage("¿Estás seguro de que quieres borrar el proyecto '${project.name}' y todas sus fotos? Esta acción no se puede deshacer.")
            .setPositiveButton("Borrar") { _, _ -> deleteProject(project) }
            .setNegativeButton("Cancelar", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(Color.parseColor("#03CFB5"))
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(Color.parseColor("#03CFB5"))
        }
        dialog.show()
    }

    private fun deleteProject(project: Project) {
        val prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val ip = prefs.getString("server_ip", null) ?: return
        val url = "http://$ip:5000/project/${project.month}/${project.name}"
        val request = Request.Builder().url(url).delete().build()


        val dialog = AlertDialog.Builder(this)
            .setTitle("Borrar Proyecto")
            .setMessage("¿Estás seguro de que quieres borrar el proyecto '${project.name}' y todas sus fotos? Esta acción no se puede deshacer.")
            .setPositiveButton("Borrar") { _, _ -> deleteProject(project) }
            .setNegativeButton("Cancelar", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(Color.parseColor("#03CFB5"))
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(Color.parseColor("#03CFB5"))
        }
        dialog.show()
    }

    private fun deleteProject(project: Project) {
        val prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val ip = prefs.getString("server_ip", null) ?: return
        val url = "http://$ip:5000/project/${project.month}/${project.name}"
        val request = Request.Builder().url(url).delete().build()


        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { Toast.makeText(applicationContext, "Fallo al borrar el proyecto", Toast.LENGTH_SHORT).show() }
            }
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    runOnUiThread {
                        Toast.makeText(applicationContext, "Proyecto borrado", Toast.LENGTH_SHORT).show()
                        fetchProjects()
                    }
                } else {
                    runOnUiThread { Toast.makeText(applicationContext, "Error del servidor al borrar", Toast.LENGTH_SHORT).show() }
                }
            }
        })
    }
}