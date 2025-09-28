package com.example.qphotos

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import java.io.File

class QueueAdapter(private var tasks: List<UploadTask>) : RecyclerView.Adapter<QueueAdapter.ViewHolder>() {

    // The ViewHolder now holds references to the new UI elements in queue_item_layout.xml
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val thumbnail: ImageView = view.findViewById(R.id.ivQueueThumbnail)
        val projectName: TextView = view.findViewById(R.id.tvQueueProjectName)
        val status: TextView = view.findViewById(R.id.tvQueueStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // We inflate the new layout file
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.queue_item_layout, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val task = tasks[position]
        holder.projectName.text = task.projectName
        holder.status.text = "Pendiente" // For now, the status is always pending

        // Use Coil to load the local image file into the thumbnail ImageView
        val imageFile = File(task.imagePath)
        if (imageFile.exists()) {
            holder.thumbnail.load(imageFile) {
                crossfade(true)
                placeholder(R.drawable.ic_gallery) // Placeholder while loading
                error(R.drawable.ic_folder)       // Image to show if loading fails
            }
        } else {
            holder.thumbnail.setImageResource(R.drawable.ic_folder) // Show error icon if file doesn't exist
        }
    }

    override fun getItemCount() = tasks.size

    fun updateTasks(newTasks: List<UploadTask>) {
        this.tasks = newTasks
        notifyDataSetChanged()
    }
}