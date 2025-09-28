package com.example.qphotos

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import coil.load

class PhotoViewerAdapter(private val photoUrls: List<String>, private val baseUrl: String) :
    RecyclerView.Adapter<PhotoViewerAdapter.PhotoViewHolder>() {

    class PhotoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.imageView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_photo_viewer, parent, false)
        return PhotoViewHolder(view)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        val fullUrl = "$baseUrl/uploads/${photoUrls[position]}"
        holder.imageView.load(fullUrl) {
            placeholder(R.drawable.ic_gallery) // Optional: show a placeholder while loading
            error(R.drawable.ic_folder) // Optional: show an error image if loading fails
        }
    }

    override fun getItemCount() = photoUrls.size
}