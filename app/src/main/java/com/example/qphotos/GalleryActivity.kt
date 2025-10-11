package com.example.qphotos

import android.os.Bundle
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.request.CachePolicy
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.stfalcon.imageviewer.StfalconImageViewer
import com.stfalcon.imageviewer.loader.ImageLoader as StfalconImageLoader

class GalleryActivity : AppCompatActivity(), StfalconImageLoader<String> {

    private lateinit var photosRecyclerView: RecyclerView
    private lateinit var galleryAdapter: GalleryAdapter
    private lateinit var emptyFolderMessage: TextView
    private var viewer: StfalconImageViewer<String>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gallery)

        val dayPath = intent.getStringExtra("DAY_PATH")
        if (dayPath == null) {
            Toast.makeText(this, getString(R.string.error_missing_day_path), Toast.LENGTH_LONG).show()
            finish()
            return
        }
        title = dayPath.split("/").lastOrNull() ?: getString(R.string.gallery_title)

        photosRecyclerView = findViewById(R.id.photosRecyclerView)
        emptyFolderMessage = findViewById(R.id.emptyFolderMessage)

        setupRecyclerView()
        fetchPhotos(dayPath)
    }

    override fun loadImage(imageView: ImageView, imageUrl: String) {
        val fullUrl = ApiClient.getUploadUrl(this, imageUrl)
        imageView.load(fullUrl) {
            crossfade(300)
            memoryCachePolicy(coil.request.CachePolicy.ENABLED)
            diskCachePolicy(coil.request.CachePolicy.ENABLED)
            networkCachePolicy(coil.request.CachePolicy.ENABLED)
            // Use a reasonable max size to avoid loading huge images
            size(1920, 1920)
        }
    }

    private fun setupRecyclerView() {
        galleryAdapter = GalleryAdapter { photoUrl ->
            val startPosition = galleryAdapter.currentList.indexOf(photoUrl)
            if (startPosition != -1) {
                showPhotoViewer(startPosition)
            }
        }
        photosRecyclerView.adapter = galleryAdapter
        
        val layoutManager = GridLayoutManager(this, 3)
        photosRecyclerView.layoutManager = layoutManager
        
        // Enable item prefetching for smoother scrolling
        layoutManager.isItemPrefetchEnabled = true
        layoutManager.initialPrefetchItemCount = 6
        
        // Optimize RecyclerView performance
        photosRecyclerView.setHasFixedSize(true)
        photosRecyclerView.setItemViewCacheSize(20)
    }

    private fun showPhotoViewer(startPosition: Int) {
        val overlayView = layoutInflater.inflate(R.layout.photo_overlay, photosRecyclerView, false)
        val deleteButtonInOverlay = overlayView.findViewById<ImageButton>(R.id.deleteButton)
        val currentPhotoList = galleryAdapter.currentList

        viewer = StfalconImageViewer.Builder(this, currentPhotoList, this)
            .withStartPosition(startPosition)
            .withOverlayView(overlayView)
            .withImageChangeListener { position ->
                deleteButtonInOverlay.setOnClickListener {
                    showDeleteConfirmationDialog(currentPhotoList[position])
                }
            }
            .show()

        deleteButtonInOverlay.setOnClickListener {
            showDeleteConfirmationDialog(currentPhotoList[startPosition])
        }
    }

    private fun fetchPhotos(dayPath: String) {
        ApiClient.getContents(this, dayPath, object : ApiClient.ApiCallback<List<FileSystemItem>> {
            override fun onSuccess(result: List<FileSystemItem>) {
                val newPhotoUrls = result.filter { it.type == "photo" }.map { it.path }
                runOnUiThread {
                    updateUiWithPhotos(newPhotoUrls)
                }
            }

            override fun onError(message: String) {
                runOnUiThread {
                    Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
                    updateUiWithPhotos(emptyList())
                }
            }
        })
    }

    private fun updateUiWithPhotos(newPhotoUrls: List<String>) {
        galleryAdapter.submitList(newPhotoUrls)

        photosRecyclerView.isVisible = newPhotoUrls.isNotEmpty()
        emptyFolderMessage.isVisible = newPhotoUrls.isEmpty()
    }

    private fun showDeleteConfirmationDialog(photoUrl: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.delete_photo_title))
            .setMessage(getString(R.string.delete_photo_message))
            .setNegativeButton(getString(R.string.cancel), null)
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                deletePhoto(photoUrl)
            }
            .show()
    }

    private fun deletePhoto(photoUrlToDelete: String) {
        ApiClient.deletePhoto(this, photoUrlToDelete, object : ApiClient.SimpleCallback {
            override fun onSuccess() {
                runOnUiThread {
                    Toast.makeText(applicationContext, getString(R.string.photo_deleted), Toast.LENGTH_SHORT).show()
                    viewer?.dismiss()

                    // Create a new list without the deleted item and submit it.
                    val newList = galleryAdapter.currentList.toMutableList().apply {
                        remove(photoUrlToDelete)
                    }
                    updateUiWithPhotos(newList)
                }
            }

            override fun onError(message: String) {
                runOnUiThread { Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show() }
            }
        })
    }
}