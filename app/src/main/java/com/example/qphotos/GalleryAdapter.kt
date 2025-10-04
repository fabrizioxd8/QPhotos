package com.example.qphotos

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load

class GalleryAdapter(
    private val photoUrls: List<String>,
    private val onPhotoClick: (Int) -> Unit
) : RecyclerView.Adapter<GalleryAdapter.PhotoViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.gallery_item_layout, parent, false)
        return PhotoViewHolder(view)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        val photoUrl = photoUrls[position]
        holder.bind(photoUrl)
        holder.itemView.setOnClickListener {
            onPhotoClick(position)
        }
    }

    override fun getItemCount(): Int = photoUrls.size

    class PhotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.photoImageView)

        fun bind(photoUrl: String) {
            val prefs = itemView.context.getSharedPreferences("AppPrefs", android.content.Context.MODE_PRIVATE)
            val ip = prefs.getString("server_ip", null)
            val fullUrl = "http://$ip:5000/thumbnail/$photoUrl"
            imageView.load(fullUrl) {
                crossfade(true)
                placeholder(R.drawable.ic_placeholder) // Optional: add a placeholder drawable
            }
        }
    }
}