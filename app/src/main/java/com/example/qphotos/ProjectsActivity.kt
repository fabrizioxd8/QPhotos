package com.example.qphotos

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import com.stfalcon.imageviewer.StfalconImageViewer
import com.stfalcon.imageviewer.loader.ImageLoader
import android.widget.ImageView
import coil.load
import com.google.android.material.dialog.MaterialAlertDialogBuilder


class ProjectsActivity : AppCompatActivity(), ImageLoader<String> {

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

    override fun loadImage(imageView: ImageView, imageUrl: String) {
        val prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val ip = prefs.getString("server_ip", null)
        val fullUrl = "http://$ip:5000/uploads/$imageUrl"
        imageView.load(fullUrl)
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
        fetchPhotosForProject(project)
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
            setTextColor(ContextCompat.getColor(this@ProjectsActivity, R.color.brand_teal))
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
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(ContextCompat.getColor(this, R.color.brand_teal))
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(ContextCompat.getColor(this, R.color.brand_teal))
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
            setTextColor(ContextCompat.getColor(this@ProjectsActivity, R.color.brand_teal))
        }

        val dialog = AlertDialog.Builder(this)
            .setCustomTitle(titleView)
            .setMessage("¿Estás seguro de que quieres borrar el proyecto '${project.name}' y todas sus fotos? Esta acción no se puede deshacer.")
            .setPositiveButton("Borrar") { _, _ -> deleteProject(project) }
            .setNegativeButton("Cancelar", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(ContextCompat.getColor(this, R.color.brand_teal))
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(ContextCompat.getColor(this, R.color.brand_teal))
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

    private fun fetchPhotosForProject(project: Project) {
        val prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val ip = prefs.getString("server_ip", null) ?: return
        val url = "http://$ip:5000/photos/${project.month}/${project.name}"
        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { Toast.makeText(applicationContext, "Failed to fetch photos.", Toast.LENGTH_SHORT).show() }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val photoUrls = mutableListOf<String>()
                    val jsonArray = JSONArray(response.body!!.string())
                    for (i in 0 until jsonArray.length()) {
                        photoUrls.add(jsonArray.getString(i))
                    }
                    runOnUiThread {
                        if (photoUrls.isNotEmpty()) {
                            showPhotoViewer(photoUrls)
                        } else {
                            Toast.makeText(applicationContext, "No photos in this project.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        })
    }

    private fun showPhotoViewer(photoUrls: MutableList<String>) {
        val overlayView = layoutInflater.inflate(R.layout.photo_overlay, null)
        val viewer = StfalconImageViewer.Builder(this, photoUrls, this)
            .withOverlayView(overlayView)
            .withImageChangeListener { position ->
                overlayView.findViewById<ImageButton>(R.id.deleteButton).setOnClickListener {
                    showDeleteConfirmationDialog(photoUrls[position]) {
                        // This block will be executed if the user confirms deletion.
                        // viewer.dismiss() is not available here, so we need a different approach.
                        // For now, we'll just delete the photo and the user can manually close.
                    }
                }
            }
            .show()
    }

    private fun showDeleteConfirmationDialog(photoUrl: String, onConfirmed: () -> Unit) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Photo")
            .setMessage("Are you sure you want to delete this photo? This action cannot be undone.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ ->
                deletePhoto(photoUrl, onConfirmed)
            }
            .show()
    }

    private fun deletePhoto(photoUrl: String, onConfirmed: () -> Unit) {
        val prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val ip = prefs.getString("server_ip", null) ?: return
        // The photoUrl is the filepath, e.g., "01 ENERO/Project Alpha/2023-10-27/uuid.jpg"
        val url = "http://$ip:5000/photo/$photoUrl"
        val request = Request.Builder().url(url).delete().build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { Toast.makeText(applicationContext, "Failed to delete photo.", Toast.LENGTH_SHORT).show() }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    runOnUiThread {
                        Toast.makeText(applicationContext, "Photo deleted.", Toast.LENGTH_SHORT).show()
                        onConfirmed() // This might be used to refresh the view
                    }
                } else {
                    runOnUiThread { Toast.makeText(applicationContext, "Server error while deleting photo.", Toast.LENGTH_SHORT).show() }
                }
            }
        })
    }
}