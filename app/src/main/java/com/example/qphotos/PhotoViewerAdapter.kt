package com.example.qphotos

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.github.chrisbanes.photoview.PhotoView

class PhotoViewerAdapter(private val photoUrls: List<String>, private val baseUrl: String) :
    RecyclerView.Adapter<PhotoViewerAdapter.PhotoViewHolder>() {

    class PhotoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val photoView: PhotoView = view.findViewById(R.id.photoView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_photo_viewer, parent, false)
        return PhotoViewHolder(view)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        val fullUrl = "$baseUrl/uploads/${photoUrls[position]}"

        // Reset scale on image bind to ensure no leftover zoom
        holder.photoView.setScale(1f, false)

        holder.photoView.load(fullUrl) {
            placeholder(R.drawable.ic_gallery)
            error(R.drawable.ic_folder)
        }
    }

    override fun getItemCount() = photoUrls.size
}