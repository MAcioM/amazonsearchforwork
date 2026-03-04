package com.dynamicisland.settings

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AppFilterAdapter(
    private val apps: List<AppInfo>,
    private val selectedApps: MutableSet<String>,
    private val onSelectionChanged: (Set<String>) -> Unit
) : RecyclerView.Adapter<AppFilterAdapter.ViewHolder>() {

    data class AppInfo(
        val packageName: String,
        val label: String,
        val icon: Drawable
    )

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(android.R.id.icon)
        val label: TextView = view.findViewById(android.R.id.text1)
        val checkbox: CheckBox = view.findViewById(android.R.id.checkbox)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.activity_list_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val app = apps[position]
        holder.icon.setImageDrawable(app.icon)
        holder.label.text = app.label
        holder.label.setTextColor(0xFFFFFFFF.toInt())
        holder.checkbox.isChecked = app.packageName in selectedApps

        val clickListener = View.OnClickListener {
            if (app.packageName in selectedApps) {
                selectedApps.remove(app.packageName)
            } else {
                selectedApps.add(app.packageName)
            }
            holder.checkbox.isChecked = app.packageName in selectedApps
            onSelectionChanged(selectedApps.toSet())
        }

        holder.itemView.setOnClickListener(clickListener)
        holder.checkbox.setOnClickListener(clickListener)
    }

    override fun getItemCount(): Int = apps.size
}
