package com.example.qphotos

import android.os.Bundle
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.stfalcon.imageviewer.StfalconImageViewer
import com.stfalcon.imageviewer.loader.ImageLoader
import okhttp3.*
import org.json.JSONArray
import java.io.IOException

class GalleryActivity : AppCompatActivity(), ImageLoader<String> {

    private lateinit var photosRecyclerView: RecyclerView
    private lateinit var galleryAdapter: GalleryAdapter
    private val client = OkHttpClient()
    private var photoUrls = mutableListOf<String>()
    private var viewer: StfalconImageViewer<String>? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gallery)

        val projectName = intent.getStringExtra("PROJECT_NAME") ?: "Default"
        val monthFolder = intent.getStringExtra("MONTH_FOLDER") ?: "Default"
        title = projectName

        photosRecyclerView = findViewById(R.id.photosRecyclerView)

        setupRecyclerView()
        fetchPhotos(monthFolder, projectName)
    }

    override fun loadImage(imageView: ImageView, imageUrl: String) {
        val prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val ip = prefs.getString("server_ip", null)
        val fullUrl = "http://$ip:5000/uploads/$imageUrl"
        imageView.load(fullUrl)
    }

    private fun setupRecyclerView() {
        galleryAdapter = GalleryAdapter(photoUrls) { position ->
            showPhotoViewer(position)
        }
        photosRecyclerView.adapter = galleryAdapter
        photosRecyclerView.layoutManager = GridLayoutManager(this, 3)
    }

    private fun showPhotoViewer(startPosition: Int) {
        val overlayView = layoutInflater.inflate(R.layout.photo_overlay, null)
        val deleteButton = overlayView.findViewById<ImageButton>(R.id.deleteButton)

        // Set the initial listener for the first image shown
        deleteButton.setOnClickListener {
            showDeleteConfirmationDialog(photoUrls[startPosition])
        }

        viewer = StfalconImageViewer.Builder(this, photoUrls, this)
            .withStartPosition(startPosition)
            .withOverlayView(overlayView)
            .withImageChangeListener { position ->
                // Update the listener for subsequent images
                deleteButton.setOnClickListener {
                    showDeleteConfirmationDialog(photoUrls[position])
                }
            }
            .show()
    }

    private fun fetchPhotos(monthFolder: String, projectName: String) {
        val prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val ip = prefs.getString("server_ip", null) ?: return
        val url = "http://$ip:5000/photos/$monthFolder/$projectName"
        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { Toast.makeText(applicationContext, "Failed to fetch photos.", Toast.LENGTH_SHORT).show() }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    val jsonArray = JSONArray(responseBody)
                    val newPhotoUrls = mutableListOf<String>()
                    for (i in 0 until jsonArray.length()) {
                        newPhotoUrls.add(jsonArray.getString(i))
                    }
                    runOnUiThread {
                        photoUrls.clear()
                        photoUrls.addAll(newPhotoUrls)
                        galleryAdapter.notifyDataSetChanged()
                    }
                }
            }
        })
    }

    private fun showDeleteConfirmationDialog(photoUrl: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Photo")
            .setMessage("Are you sure you want to delete this photo? This action cannot be undone.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ ->
                deletePhoto(photoUrl)
            }
            .show()
    }


    private fun deletePhoto(photoUrl: String) {
        val prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val ip = prefs.getString("server_ip", null) ?: return
        val url = "http://$ip:5000/photo/$photoUrl"
        val request = Request.Builder().url(url).delete().build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(applicationContext, "Failed to delete photo. Please try again.", Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    if (response.isSuccessful) {
                        Toast.makeText(applicationContext, "Photo deleted.", Toast.LENGTH_SHORT).show()
                        galleryAdapter.removePhoto(photoUrl)
                        viewer?.dismiss()

                    } else {
                        Toast.makeText(applicationContext, "Error: ${response.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        })
    }
}