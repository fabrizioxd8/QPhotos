package com.example.qphotos

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File

class ProjectsActivity : AppCompatActivity() {

    private lateinit var projectsRecyclerView: RecyclerView
    private lateinit var breadcrumbTextView: TextView
    private lateinit var projectsAdapter: ProjectsAdapter
    private var currentPath = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_projects)
        title = getString(R.string.browse_projects_title)

        projectsRecyclerView = findViewById(R.id.projectsRecyclerView)
        breadcrumbTextView = findViewById(R.id.breadcrumbTextView)

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
        projectsAdapter = ProjectsAdapter(
            emptyList(),
            onItemClicked = { item -> onItemClicked(item) },
            onItemLongClicked = { item -> onItemLongClicked(item) }
        )
        projectsRecyclerView.adapter = projectsAdapter
        projectsRecyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun onItemClicked(item: FileSystemItem) {
        if (item.type == "day") {
            val intent = Intent(this, GalleryActivity::class.java).apply {
                putExtra("DAY_PATH", item.path)
            }
            startActivity(intent)
        } else {
            currentPath = item.path
            loadContents(currentPath)
        }
    }

    private fun onItemLongClicked(item: FileSystemItem) {
        if (item.type != "project") return

        val options = arrayOf(getString(R.string.rename), getString(R.string.delete))
        AlertDialog.Builder(this)
            .setTitle(item.name)
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> showRenameDialog(item)
                    1 -> showDeleteDialog(item)
                }
                dialog.dismiss()
            }
            .show()
    }

    private fun showRenameDialog(item: FileSystemItem) {
        val editText = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT
            setText(item.name)
        }

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.rename_project))
            .setView(editText)
            .setPositiveButton(getString(R.string.rename)) { _, _ ->
                val newName = editText.text.toString().trim()
                if (newName.isNotEmpty() && newName != item.name) {
                    renameProject(item, newName)
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun showDeleteDialog(item: FileSystemItem) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.delete_project))
            .setMessage(getString(R.string.are_you_sure_you_want_to_delete, item.name))
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                deleteProject(item)
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun renameProject(item: FileSystemItem, newName: String) {
        ApiClient.renameProject(this, item, newName, object : ApiClient.SimpleCallback {
            override fun onSuccess() {
                runOnUiThread {
                    Toast.makeText(applicationContext, getString(R.string.project_renamed), Toast.LENGTH_SHORT).show()
                    loadContents(currentPath)
                }
            }

            override fun onError(message: String) {
                runOnUiThread {
                    Toast.makeText(applicationContext, getString(R.string.failed_to_rename_project), Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    private fun deleteProject(item: FileSystemItem) {
        ApiClient.deleteProject(this, item, object : ApiClient.SimpleCallback {
            override fun onSuccess() {
                runOnUiThread {
                    Toast.makeText(applicationContext, getString(R.string.project_deleted), Toast.LENGTH_SHORT).show()
                    loadContents(currentPath)
                }
            }

            override fun onError(message: String) {
                runOnUiThread {
                    Toast.makeText(applicationContext, getString(R.string.failed_to_delete_project), Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    private fun loadContents(path: String) {
        ApiClient.getContents(this, path, object : ApiClient.ApiCallback<List<FileSystemItem>> {
            override fun onSuccess(result: List<FileSystemItem>) {
                runOnUiThread {
                    projectsAdapter.updateItems(result)
                    updateBreadcrumb()
                }
            }

            override fun onError(message: String) {
                runOnUiThread {
                    Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    private fun updateBreadcrumb() {
        breadcrumbTextView.text = if (currentPath.isEmpty()) getString(R.string.home) else getString(R.string.home_breadcrumb, currentPath)
    }
}
