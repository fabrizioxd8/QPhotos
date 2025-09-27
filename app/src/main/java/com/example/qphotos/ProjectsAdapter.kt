package com.example.qphotos

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load

class ProjectsAdapter(private var projects: List<Project>) :
    RecyclerView.Adapter<ProjectsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val projectName: TextView = view.findViewById(R.id.tvProjectName)
        val projectThumbnail: ImageView = view.findViewById(R.id.ivProjectThumbnail)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.project_item_layout, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val project = projects[position]
        holder.projectName.text = project.name

        // Load the thumbnail
        if (project.thumbnailUrl.isNotBlank()) {
            holder.projectThumbnail.load(project.thumbnailUrl) {
                crossfade(true)
                placeholder(R.drawable.ic_folder)
                error(R.drawable.ic_gallery)
            }
        } else {
            holder.projectThumbnail.setImageResource(R.drawable.ic_folder) // Default image
        }


        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, GalleryActivity::class.java).apply {
                putExtra("PROJECT_NAME", project.name)
                putExtra("PROJECT_MONTH", project.month)
            }
            context.startActivity(intent)
        }
    }

    override fun getItemCount() = projects.size

    fun updateProjects(newProjects: List<Project>) {
        projects = newProjects
        notifyDataSetChanged()
    }
}