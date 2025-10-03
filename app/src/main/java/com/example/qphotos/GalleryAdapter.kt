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
    private val onPhotoClick: (List<String>, Int) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

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
                (holder as PhotoViewHolder).bind(item.photoUrl)
                holder.itemView.setOnClickListener {
                    val photoUrls = galleryItems.filterIsInstance<GalleryItem.PhotoItem>().map { it.photoUrl }
                    val photoIndex = photoUrls.indexOf(item.photoUrl)
                    onPhotoClick(photoUrls, photoIndex)
                }
            }
        }
    }

    override fun getItemCount(): Int = galleryItems.size

    fun updateItems(newItems: List<GalleryItem>) {
        galleryItems.clear()
        galleryItems.addAll(newItems)
        notifyDataSetChanged()
    }

    fun removePhoto(photoUrl: String) {
        val index = galleryItems.indexOfFirst { it is GalleryItem.PhotoItem && it.photoUrl == photoUrl }
        if (index != -1) {
            galleryItems.removeAt(index)
            notifyItemRemoved(index)
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

        fun bind(photoUrl: String) {
            val prefs = itemView.context.getSharedPreferences("AppPrefs", android.content.Context.MODE_PRIVATE)
            val ip = prefs.getString("server_ip", null)
            val fullUrl = "http://$ip:5000/thumbnail/$photoUrl"
            imageView.load(fullUrl)
        }
    }
}