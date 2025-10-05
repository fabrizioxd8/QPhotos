package com.example.qphotos

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

class ProjectsAdapter(
    private val onItemClicked: (FileSystemItem) -> Unit,
    private val onItemLongClicked: (FileSystemItem) -> Unit
) : ListAdapter<FileSystemItem, ProjectsAdapter.ViewHolder>(ProjectDiffCallback()) {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val itemName: TextView = view.findViewById(R.id.tvProjectName)
        val itemIcon: ImageView = view.findViewById(R.id.itemIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.project_item_layout, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.itemName.text = item.name

        val iconRes = when (item.type) {
            "month" -> R.drawable.ic_calendar_month
            "project" -> R.drawable.ic_folder
            "day" -> R.drawable.ic_gallery
            else -> R.drawable.ic_folder // Default fallback icon
        }
        holder.itemIcon.setImageResource(iconRes)

        holder.itemView.setOnClickListener {
            onItemClicked(item)
        }

        // Enable long clicks only on folders that are not date-specific or top-level month folders
        if (item.type == "project") {
            holder.itemView.setOnLongClickListener {
                onItemLongClicked(item)
                true
            }
        } else {
            holder.itemView.isLongClickable = false
        }
    }
}

class ProjectDiffCallback : DiffUtil.ItemCallback<FileSystemItem>() {
    override fun areItemsTheSame(oldItem: FileSystemItem, newItem: FileSystemItem): Boolean {
        return oldItem.path == newItem.path
    }

    override fun areContentsTheSame(oldItem: FileSystemItem, newItem: FileSystemItem): Boolean {
        return oldItem == newItem
    }
}