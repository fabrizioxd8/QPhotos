package com.example.qphotos

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import coil.load

class GalleryAdapter(
    private var photos: MutableList<String>,
    private val onPhotoClick: (Int) -> Unit,
    private val onSelectionChange: (Boolean) -> Unit
) : RecyclerView.Adapter<GalleryAdapter.PhotoViewHolder>() {

    private val selectedItems = mutableSetOf<Int>()
    var isSelectionMode = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.gallery_item_layout, parent, false)
        return PhotoViewHolder(view)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        val photoUrl = photos[position]
        holder.bind(photoUrl, selectedItems.contains(position))

        holder.itemView.setOnClickListener {
            if (isSelectionMode) {
                toggleSelection(position)
            } else {
                onPhotoClick(position)
            }
        }

        holder.itemView.setOnLongClickListener {
            if (!isSelectionMode) {
                isSelectionMode = true
                onSelectionChange(true)
            }
            toggleSelection(position)
            true
        }
    }

    override fun getItemCount(): Int = photos.size

    fun getSelectedPhotos(): List<String> {
        return selectedItems.map { photos[it] }
    }

    fun removeDeletedPhotos(deletedPhotos: List<String>) {
        photos.removeAll(deletedPhotos)
        selectedItems.clear()
        isSelectionMode = false
        onSelectionChange(false)
        notifyDataSetChanged()
    }

    private fun toggleSelection(position: Int) {
        if (selectedItems.contains(position)) {
            selectedItems.remove(position)
        } else {
            selectedItems.add(position)
        }
        notifyItemChanged(position)

        if (selectedItems.isEmpty()) {
            isSelectionMode = false
            onSelectionChange(false)
        }
    }

    class PhotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.photoImageView)
        private val selectionOverlay: View = itemView.findViewById(R.id.selectionOverlay)

        fun bind(photoUrl: String, isSelected: Boolean) {
            val prefs = itemView.context.getSharedPreferences("AppPrefs", android.content.Context.MODE_PRIVATE)
            val ip = prefs.getString("server_ip", null)
            val fullUrl = "http://$ip:5000/thumbnail/$photoUrl"
            imageView.load(fullUrl)
            selectionOverlay.visibility = if (isSelected) View.VISIBLE else View.GONE
        }
    }
}