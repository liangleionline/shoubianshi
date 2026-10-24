package com.liangleionline.shoubianshi

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.liangleionline.shoubianshi.data.Task
import com.liangleionline.shoubianshi.databinding.ItemManageTaskBinding

/** 管理页适配器：展示全部事项，支持编辑、删除。 */
class ManageAdapter(
    private val onEdit: (Task) -> Unit,
    private val onDelete: (Task) -> Unit
) : RecyclerView.Adapter<ManageAdapter.VH>() {

    private val items = mutableListOf<Task>()

    fun submit(list: List<Task>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemManageTaskBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val task = items[position]
        holder.title.text = task.title
        holder.state.text = if (task.isPinned) {
            holder.state.context.getString(R.string.state_now)
        } else {
            holder.state.context.getString(R.string.state_pool)
        }
        holder.edit.setOnClickListener { onEdit(task) }
        holder.delete.setOnClickListener { onDelete(task) }
    }

    override fun getItemCount(): Int = items.size

    class VH(binding: ItemManageTaskBinding) : RecyclerView.ViewHolder(binding.root) {
        val title: TextView = binding.taskTitle
        val state: TextView = binding.taskState
        val edit: TextView = binding.btnEdit
        val delete: TextView = binding.btnDelete
    }
}
