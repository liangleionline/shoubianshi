package com.liangleionline.shoubianshi

import android.content.ClipData
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.liangleionline.shoubianshi.data.Task
import com.liangleionline.shoubianshi.databinding.ItemTaskBinding

/**
 * 首页上下两区的统一适配器。
 * isNowZone = true 用于上区（接下来待办，显示“完成”按钮）；
 * isNowZone = false 用于下区（全部事项素材库）。
 */
class TaskAdapter(
    private val isNowZone: Boolean,
    private val onDragStart: (holder: RecyclerView.ViewHolder, task: Task) -> Unit,
    private val onComplete: (Task) -> Unit
) : RecyclerView.Adapter<TaskAdapter.VH>() {

    private val items = mutableListOf<Task>()

    fun submit(list: List<Task>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemTaskBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val task = items[position]
        holder.title.text = task.title

        // 拖拽源：长按卡片启动拖拽
        holder.itemView.setOnLongClickListener {
            onDragStart(holder, task)
            true
        }

        // “完成”按钮仅上区显示
        if (isNowZone) {
            holder.complete.visibility = View.VISIBLE
            holder.complete.setOnClickListener { onComplete(task) }
            // 长按按钮时不触发卡片拖拽
            holder.complete.setOnLongClickListener { true }
        } else {
            holder.complete.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = items.size

    class VH(binding: ItemTaskBinding) : RecyclerView.ViewHolder(binding.root) {
        val title: TextView = binding.taskTitle
        val complete: TextView = binding.btnComplete
    }
}
