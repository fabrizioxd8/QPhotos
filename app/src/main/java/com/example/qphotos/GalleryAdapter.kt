package com.example.qphotos

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load

class GalleryAdapter(
    private val onPhotoClick: (String) -> Unit
) : ListAdapter<String, GalleryAdapter.PhotoViewHolder>(PhotoDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.gallery_item_layout, parent, false)
        return PhotoViewHolder(view)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        val photoUrl = getItem(position)
        holder.bind(photoUrl)
        holder.itemView.setOnClickListener {
            onPhotoClick(photoUrl)
        }
    }

    class PhotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.photoImageView)

        fun bind(photoUrl: String) {
            val fullUrl = ApiClient.getThumbnailUrl(itemView.context, photoUrl)
            imageView.load(fullUrl) {
                crossfade(true)
                placeholder(R.drawable.ic_folder)
            }
        }
    }
}

class PhotoDiffCallback : DiffUtil.ItemCallback<String>() {
    override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
        return oldItem == newItem
    }

    override fun areContentsTheSame(oldItem: String, newItem: String): Boolean {
        return oldItem == newItem
    }
}