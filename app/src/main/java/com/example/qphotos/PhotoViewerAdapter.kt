package com.example.qphotos

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.request.ImageRequest

class PhotoViewerAdapter(private val photoUrls: List<String>, private val baseUrl: String) :
    RecyclerView.Adapter<PhotoViewerAdapter.PhotoViewHolder>() {

    class PhotoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ZoomableImageView = view.findViewById(R.id.imageView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_photo_viewer, parent, false)
        return PhotoViewHolder(view)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        val fullUrl = "$baseUrl/uploads/${photoUrls[position]}"
        holder.imageView.prepareForNewImage()
        holder.imageView.load(fullUrl) {
            placeholder(R.drawable.ic_gallery)
            error(R.drawable.ic_folder)
            listener(
                onStart = { _ -> holder.imageView.resetToInitialState() },
                onSuccess = { _, _ -> holder.imageView.resetToInitialState() },
                onError = { _, _ -> holder.imageView.resetToInitialState() }
            )
        }
    }

    override fun getItemCount() = photoUrls.size
}