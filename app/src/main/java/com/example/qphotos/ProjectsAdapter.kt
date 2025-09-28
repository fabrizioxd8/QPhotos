package com.example.qphotos

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ProjectsAdapter(
    private var projects: List<Project>,
    private val onItemClicked: (Project) -> Unit,
    private val onItemLongClicked: (Project) -> Unit
) : RecyclerView.Adapter<ProjectsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val projectName: TextView = view.findViewById(R.id.tvProjectName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.project_item_layout, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val project = projects[position]
        holder.projectName.text = project.name

        holder.itemView.setOnClickListener {
            onItemClicked(project)
        }
        holder.itemView.setOnLongClickListener {
            onItemLongClicked(project)
            true
        }
    }

    override fun getItemCount() = projects.size

    fun updateProjects(newProjects: List<Project>) {
        projects = newProjects
        notifyDataSetChanged()
    }
}