package com.example.qphotos

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class QueueAdapter(private var tasks: List<UploadTask>) : RecyclerView.Adapter<QueueAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val projectName: TextView = view.findViewById(R.id.tvItemProjectName)
        val imagePath: TextView = view.findViewById(R.id.tvItemImagePath)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.queue_item_layout, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val task = tasks[position]
        holder.projectName.text = task.projectName
        holder.imagePath.text = task.imagePath
    }

    override fun getItemCount() = tasks.size

    // --- THE FIX IS HERE ---
    // The function must accept a List of UploadTask, not an Int
    fun updateTasks(newTasks: List<UploadTask>) {
        this.tasks = newTasks
        notifyDataSetChanged() // This tells the RecyclerView to refresh itself
    }
}