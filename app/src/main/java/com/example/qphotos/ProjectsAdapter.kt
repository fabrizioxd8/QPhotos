package com.example.qphotos

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ProjectsAdapter(
    private var items: List<FileSystemItem>,
    private val onItemClicked: (FileSystemItem) -> Unit
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

        if (item.type == "folder") {
            holder.itemIcon.setImageResource(R.drawable.ic_folder)
        } else {
            holder.itemIcon.setImageResource(R.drawable.ic_gallery) // Using gallery icon for photo collections
        }

        holder.itemView.setOnClickListener {
            onItemClicked(item)
        }
    }

    override fun getItemCount() = items.size

    fun updateItems(newItems: List<FileSystemItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}