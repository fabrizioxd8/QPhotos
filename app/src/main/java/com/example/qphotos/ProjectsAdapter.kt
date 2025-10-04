package com.example.qphotos

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ProjectsAdapter(
    private var items: List<FileSystemItem>,
    private val onItemClicked: (FileSystemItem) -> Unit,
    private val onItemLongClicked: (FileSystemItem) -> Unit
) : RecyclerView.Adapter<ProjectsAdapter.ViewHolder>() {

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
        val item = items[position]
        holder.itemName.text = item.name

        val iconRes = when (item.type) {
            "month" -> R.drawable.ic_folder
            "project" -> R.drawable.ic_folder
            "day" -> R.drawable.ic_gallery
            else -> R.drawable.ic_folder
        }
        holder.itemIcon.setImageResource(iconRes)

        holder.itemView.setOnClickListener {
            onItemClicked(item)
        }

        if (item.type == "project") {
            holder.itemView.setOnLongClickListener {
                onItemLongClicked(item)
                true
            }
        } else {
            holder.itemView.setOnLongClickListener(null)
        }
    }

    override fun getItemCount() = items.size

    fun updateItems(newItems: List<FileSystemItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}
