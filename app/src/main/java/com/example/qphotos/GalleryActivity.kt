package com.example.qphotos

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.stfalcon.imageviewer.StfalconImageViewer
import com.stfalcon.imageviewer.loader.ImageLoader
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import android.widget.ImageView
import coil.load

class GalleryActivity : AppCompatActivity(), ImageLoader<String> {

    private lateinit var photosRecyclerView: RecyclerView
    private lateinit var galleryAdapter: GalleryAdapter
    private lateinit var deleteButtonContainer: LinearLayout
    private val client = OkHttpClient()
    private var galleryItems = mutableListOf<GalleryItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gallery)

        val projectName = intent.getStringExtra("PROJECT_NAME") ?: "Default"
        val monthFolder = intent.getStringExtra("MONTH_FOLDER") ?: "Default"
        title = projectName

        photosRecyclerView = findViewById(R.id.photosRecyclerView)
        deleteButtonContainer = findViewById(R.id.deleteButtonContainer)
        val deleteButton: Button = findViewById(R.id.deleteButton)

        setupRecyclerView()
        fetchPhotos(monthFolder, projectName)

        deleteButton.setOnClickListener {
            deleteSelectedPhotos()
        }
    }

    override fun loadImage(imageView: ImageView, imageUrl: String) {
        val prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val ip = prefs.getString("server_ip", null)
        val fullUrl = "http://$ip:5000/uploads/$imageUrl"
        imageView.load(fullUrl)
    }

    private fun setupRecyclerView() {
        galleryAdapter = GalleryAdapter(galleryItems,
            onPhotoClick = { photoUrls, startPosition ->
                StfalconImageViewer.Builder(this, photoUrls, this)
                    .withStartPosition(startPosition)
                    .show()
            },
            onSelectionChange = { isInSelectionMode ->
                deleteButtonContainer.visibility = if (isInSelectionMode) View.VISIBLE else View.GONE
            }
        )
        photosRecyclerView.adapter = galleryAdapter
        val layoutManager = GridLayoutManager(this, 3)
        layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return when (galleryAdapter.getItemViewType(position)) {
                    0 -> 3 // Date Header
                    1 -> 1 // Photo Item
                    else -> 1
                }
            }
        }
        photosRecyclerView.layoutManager = layoutManager
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
                    val jsonObject = JSONObject(responseBody)
                    val newGalleryItems = mutableListOf<GalleryItem>()
                    jsonObject.keys().forEach { date ->
                        newGalleryItems.add(GalleryItem.DateHeader(date))
                        val photoArray = jsonObject.getJSONArray(date)
                        for (i in 0 until photoArray.length()) {
                            newGalleryItems.add(GalleryItem.PhotoItem(photoArray.getString(i)))
                        }
                    }
                    runOnUiThread {
                        galleryItems.clear()
                        galleryItems.addAll(newGalleryItems)
                        galleryAdapter.notifyDataSetChanged()
                    }
                }
            }
        })
    }

    private fun deleteSelectedPhotos() {
        val selectedPhotos = galleryAdapter.getSelectedPhotos()
        if (selectedPhotos.isEmpty()) {
            Toast.makeText(this, "No photos selected.", Toast.LENGTH_SHORT).show()
            return
        }

        val prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val ip = prefs.getString("server_ip", null) ?: return

        selectedPhotos.forEach { photoUrl ->
            val url = "http://$ip:5000/photo/$photoUrl"
            val request = Request.Builder().url(url).delete().build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    // Optionally handle failure for individual photo deletion
                }

                override fun onResponse(call: Call, response: Response) {
                    // Optionally handle successful deletion
                }
            })
        }
        galleryAdapter.removeDeletedPhotos(selectedPhotos)
        Toast.makeText(this, "${selectedPhotos.size} photos deleted.", Toast.LENGTH_SHORT).show()
    }
}