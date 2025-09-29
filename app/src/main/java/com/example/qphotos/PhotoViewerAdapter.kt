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
        val request = ImageRequest.Builder(holder.imageView.context)
            .data(fullUrl)
            .placeholder(R.drawable.ic_gallery)
            .error(R.drawable.ic_folder)
            .target(
                onStart = { placeholder ->
                    holder.imageView.setImageDrawable(placeholder)
                    holder.imageView.resetZoom()
                },
                onSuccess = { result ->
                    holder.imageView.setImageDrawable(result)
                    holder.imageView.resetZoom()
                },
                onError = { error ->
                    holder.imageView.setImageDrawable(error)
                    holder.imageView.resetZoom()
                }
            )
            .build()
        holder.imageView.load(request)
    }

    override fun getItemCount() = photoUrls.size
}