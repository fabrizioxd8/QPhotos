package com.example.qphotos

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File

class ProjectsActivity : AppCompatActivity() {

    private lateinit var projectsRecyclerView: RecyclerView
    private lateinit var breadcrumbTextView: TextView
    private lateinit var projectsAdapter: ProjectsAdapter
    private lateinit var repository: ProjectRepository
    private var currentPath = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_projects)
        title = "Browse Projects"

        projectsRecyclerView = findViewById(R.id.projectsRecyclerView)
        breadcrumbTextView = findViewById(R.id.breadcrumbTextView)
        repository = ProjectRepository(this)

        setupRecyclerView()
        loadContents(currentPath)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (currentPath.isNotEmpty()) {
                    currentPath = File(currentPath).parent ?: ""
                    loadContents(currentPath)
                } else {
                    finish()
                }
            }
        })
    }

    private fun setupRecyclerView() {
        projectsAdapter = ProjectsAdapter(emptyList()) { item ->
            onItemClicked(item)
        }
        projectsRecyclerView.adapter = projectsAdapter
        projectsRecyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun onItemClicked(item: FileSystemItem) {
        if (item.type == "folder") {
            // If the item is a folder, navigate into it
            currentPath = item.path
            loadContents(currentPath)
        } else {
            // This is a project folder, so open the gallery
            val pathParts = item.path.split("/")
            if (pathParts.size >= 2) {
                val intent = Intent(this, GalleryActivity::class.java).apply {
                    putExtra("MONTH_FOLDER", pathParts[0])
                    putExtra("PROJECT_NAME", pathParts[1])
                }
                startActivity(intent)
            } else {
                Toast.makeText(this, "Invalid project path", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadContents(path: String) {
        repository.getContents(path, object : ProjectRepository.FileSystemItemsCallback {
            override fun onSuccess(items: List<FileSystemItem>) {
                runOnUiThread {
                    projectsAdapter.updateItems(items)
                    updateBreadcrumb()
                }
            }

            override fun onError() {
                runOnUiThread {
                    Toast.makeText(applicationContext, "Failed to load contents", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    private fun updateBreadcrumb() {
        breadcrumbTextView.text = if (currentPath.isEmpty()) "Home" else "Home / $currentPath"
    }
}