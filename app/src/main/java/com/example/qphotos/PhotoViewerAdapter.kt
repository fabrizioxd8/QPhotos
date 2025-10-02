package com.example.qphotos

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load

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


        // Reset zoom before loading a new image to handle view recycling correctly.
        holder.imageView.resetZoom()

        holder.imageView.load(fullUrl) {
            placeholder(R.drawable.ic_gallery)
            error(R.drawable.ic_folder)
            // It's good practice to also reset on success in case of complex lifecycle events,
            // though the initial reset handles most cases.
            listener(
                onSuccess = { _, _ -> holder.imageView.resetZoom() }
            )

        }
    }

    override fun getItemCount() = photoUrls.size
}