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
import com.github.stfalcon.stfalconimageviewer.StfalconImageViewer
import com.github.stfalcon.stfalconimageviewer.loader.ImageLoader as StfalconImageLoader
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

class GalleryActivity : AppCompatActivity(), StfalconImageLoader<String> {

    private lateinit var photosRecyclerView: RecyclerView
    private lateinit var galleryAdapter: GalleryAdapter
    private val client = OkHttpClient()
    private var photoUrls = mutableListOf<String>()
    private var viewer: StfalconImageViewer<String>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gallery)

        val dayPath = intent.getStringExtra("DAY_PATH")
        if (dayPath == null) {
            Toast.makeText(this, "Error: Missing day path", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        title = dayPath.split("/").lastOrNull() ?: "Gallery"

        photosRecyclerView = findViewById(R.id.photosRecyclerView)

        setupRecyclerView()
        fetchPhotos(dayPath)
    }

    override fun loadImage(imageView: ImageView, imageUrl: String) {
        val prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val ip = prefs.getString("server_ip", null)
        val fullUrl = "http://$ip:5000/uploads/$imageUrl"
        imageView.load(fullUrl)
    }

    private fun setupRecyclerView() {
        galleryAdapter = GalleryAdapter(photoUrls) { startPosition ->
            showPhotoViewer(startPosition)
        }
        photosRecyclerView.adapter = galleryAdapter
        photosRecyclerView.layoutManager = GridLayoutManager(this, 3) // Simple grid
    }

    private fun showPhotoViewer(startPosition: Int) {
        val overlayView = layoutInflater.inflate(R.layout.photo_overlay, null)
        val deleteButtonInOverlay = overlayView.findViewById<ImageButton>(R.id.deleteButton)

        viewer = com.github.stfalcon.stfalconimageviewer.StfalconImageViewer.Builder(this, photoUrls, this)
            .withStartPosition(startPosition)
            .withOverlayView(overlayView)
            .withImageChangeListener { position ->
                // Update the delete button listener when the image changes
                deleteButtonInOverlay.setOnClickListener {
                    showDeleteConfirmationDialog(photoUrls[position])
                }
            }
            .show()

        // Initial setup for the delete button
        deleteButtonInOverlay.setOnClickListener {
            showDeleteConfirmationDialog(photoUrls[startPosition])
        }
    }

    private fun fetchPhotos(dayPath: String) {
        val prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val ip = prefs.getString("server_ip", null) ?: return
        val url = "http://$ip:5000/list_photos_in_day/$dayPath"
        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { Toast.makeText(applicationContext, "Failed to fetch photos.", Toast.LENGTH_SHORT).show() }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseBody = response.body?.string() ?: return
                    val newPhotoUrls = mutableListOf<String>()
                    val jsonArray = JSONArray(responseBody)
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
            .setMessage("Are you sure you want to delete this photo?")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ ->
                deletePhoto(photoUrl)
            }
            .show()
    }


    private fun deletePhoto(photoUrlToDelete: String) {
        val prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val ip = prefs.getString("server_ip", null) ?: return
        val url = "http://$ip:5000/photo/$photoUrlToDelete"
        val request = Request.Builder().url(url).delete().build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { Toast.makeText(applicationContext, "Failed to delete photo.", Toast.LENGTH_LONG).show() }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    if (response.isSuccessful) {
                        Toast.makeText(applicationContext, "Photo deleted.", Toast.LENGTH_SHORT).show()
                        val indexToRemove = photoUrls.indexOf(photoUrlToDelete)
                        if (indexToRemove != -1) {
                            photoUrls.removeAt(indexToRemove)
                            galleryAdapter.notifyItemRemoved(indexToRemove)
                            galleryAdapter.notifyItemRangeChanged(indexToRemove, photoUrls.size)
                        }
                        viewer?.dismiss()
                    } else {
                        Toast.makeText(applicationContext, "Error: ${response.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        })
    }
}