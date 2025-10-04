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

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val thumbnail: ImageView = view.findViewById(R.id.ivQueueThumbnail)
        val projectName: TextView = view.findViewById(R.id.tvQueueProjectName)
        val status: TextView = view.findViewById(R.id.tvQueueStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.queue_item_layout, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val task = tasks[position]
        holder.projectName.text = task.projectName

        val context = holder.itemView.context
        holder.status.text = when (task.status) {
            "Pending" -> context.getString(R.string.status_pending)
            "Uploading" -> context.getString(R.string.status_uploading)
            else -> task.status
        }

        val imageFile = File(task.imagePath)
        if (imageFile.exists()) {
            holder.thumbnail.load(imageFile) {
                crossfade(true)
                placeholder(R.drawable.ic_gallery)
                error(R.drawable.ic_folder)
            }
        } else {
            holder.thumbnail.setImageResource(R.drawable.ic_folder)
        }
    }

    override fun getItemCount() = tasks.size

    fun updateTasks(newTasks: List<UploadTask>) {
        this.tasks = newTasks
        notifyDataSetChanged()
    }
}