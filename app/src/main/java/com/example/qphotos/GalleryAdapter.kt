package com.example.qphotos

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import coil.load

class GalleryAdapter() : RecyclerView.Adapter<GalleryAdapter.ViewHolder>() {

    private var photoUrls: List<String> = emptyList()
    private var baseUrl: String = ""
    private val TAG = "GalleryDebug" // Tag for our logs

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.ivPhoto)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.gallery_item_layout, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // THE FIX IS HERE: We add "/thumbnail/" to the start of the path
        val thumbnailUrl = "$baseUrl/thumbnail/${photoUrls[position]}"

        Log.d(TAG, "Binding view for position: $position, URL: $thumbnailUrl")

        holder.imageView.load(thumbnailUrl) {
            crossfade(true)
            placeholder(android.R.drawable.ic_menu_gallery)
            error(android.R.drawable.ic_delete)
            listener(
                onError = { _, result ->
                    Log.e(TAG, "Coil error loading image: ${result.throwable}")
                }
            )
        }
    }

    override fun getItemCount() = photoUrls.size

    fun updatePhotos(newUrls: List<String>, newBaseUrl: String) {
        Log.d(TAG, "GalleryAdapter is updating with ${newUrls.size} photos.")
        this.photoUrls = newUrls
        this.baseUrl = newBaseUrl
        notifyDataSetChanged()
    }
}