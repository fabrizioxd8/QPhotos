package com.example.qphotos

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load

class GalleryAdapter(
    private var galleryItems: MutableList<GalleryItem>,
    private val onPhotoClick: (List<String>, Int) -> Unit,
    private val onSelectionChange: (Boolean) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val selectedItems = mutableSetOf<Int>()
    var isSelectionMode = false

    companion object {
        private const val TYPE_DATE_HEADER = 0
        private const val TYPE_PHOTO_ITEM = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (galleryItems[position]) {
            is GalleryItem.DateHeader -> TYPE_DATE_HEADER
            is GalleryItem.PhotoItem -> TYPE_PHOTO_ITEM
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_DATE_HEADER -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.gallery_date_header_layout, parent, false)
                DateHeaderViewHolder(view)
            }
            TYPE_PHOTO_ITEM -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.gallery_item_layout, parent, false)
                PhotoViewHolder(view)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = galleryItems[position]) {
            is GalleryItem.DateHeader -> (holder as DateHeaderViewHolder).bind(item)
            is GalleryItem.PhotoItem -> {
                (holder as PhotoViewHolder).bind(item.photoUrl, selectedItems.contains(position))

                holder.itemView.setOnClickListener {
                    if (isSelectionMode) {
                        toggleSelection(position)
                    } else {
                        val photoUrls = galleryItems.filterIsInstance<GalleryItem.PhotoItem>().map { it.photoUrl }
                        val photoIndex = photoUrls.indexOf(item.photoUrl)
                        onPhotoClick(photoUrls, photoIndex)
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
        }
    }

    override fun getItemCount(): Int = galleryItems.size

    fun getSelectedPhotos(): List<String> {
        return selectedItems.mapNotNull { position ->
            (galleryItems.getOrNull(position) as? GalleryItem.PhotoItem)?.photoUrl
        }
    }

    fun removeDeletedPhotos(deletedPhotos: List<String>) {
        galleryItems.removeAll { it is GalleryItem.PhotoItem && deletedPhotos.contains(it.photoUrl) }
        // Optional: Clean up empty date headers
        val iterator = galleryItems.iterator()
        var lastItem: GalleryItem? = null
        while(iterator.hasNext()) {
            val currentItem = iterator.next()
            if (lastItem is GalleryItem.DateHeader && currentItem is GalleryItem.DateHeader) {
                iterator.remove()
            }
            lastItem = currentI
        }
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

    class DateHeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val dateTextView: TextView = itemView.findViewById(R.id.dateTextView)
        fun bind(header: GalleryItem.DateHeader) {
            dateTextView.text = header.date
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