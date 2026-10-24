package com.liangleionline.shoubianshi

import android.content.Intent
import android.os.Bundle
import android.view.DragEvent
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.liangleionline.shoubianshi.data.Task
import com.liangleionline.shoubianshi.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val vm: MainViewModel by viewModels()

    private lateinit var nowAdapter: TaskAdapter
    private lateinit var poolAdapter: TaskAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupLists()
        setupDropTargets()
        observe()
    }

    private fun setupLists() {
        nowAdapter = TaskAdapter(
            isNowZone = true,
            onDragStart = ::startDrag,
            onComplete = vm::delete
        )
        poolAdapter = TaskAdapter(
            isNowZone = false,
            onDragStart = ::startDrag,
            onComplete = vm::delete
        )

        binding.nowRv.layoutManager = LinearLayoutManager(this)
        binding.nowRv.adapter = nowAdapter
        binding.poolRv.layoutManager = LinearLayoutManager(this)
        binding.poolRv.adapter = poolAdapter

        binding.btnManage.setOnClickListener {
            startActivity(Intent(this, ManageActivity::class.java))
        }
    }

    /** 长按卡片启动拖拽，卡片跟随悬浮 */
    private fun startDrag(holder: RecyclerView.ViewHolder, task: Task) {
        val data = android.content.ClipData.newPlainText("task_id", task.id.toString())
        val shadow = View.DragShadowBuilder(holder.itemView)
        holder.itemView.startDragAndDrop(data, shadow, null, 0)
    }

    /** 两个区域都作为拖放目标：上区=加入待办，下区=撤回素材库 */
    private fun setupDropTargets() {
        binding.zoneNow.setOnDragListener { _, e -> onZoneDrag(binding.zoneNow, e, targetNow = true) }
        binding.zonePool.setOnDragListener { _, e -> onZoneDrag(binding.zonePool, e, targetNow = false) }
    }

    private fun onZoneDrag(zone: View, event: DragEvent, targetNow: Boolean): Boolean {
        return when (event.action) {
            DragEvent.ACTION_DRAG_STARTED -> true

            DragEvent.ACTION_DRAG_ENTERED -> {
                setZoneHighlight(zone, true); true
            }

            DragEvent.ACTION_DRAG_EXITED -> {
                setZoneHighlight(zone, false); true
            }

            DragEvent.ACTION_DROP -> {
                setZoneHighlight(zone, false)
                event.clipData?.getItemAt(0)?.text?.toString()?.toLongOrNull()?.let { id ->
                    if (targetNow) vm.moveToNow(id) else vm.moveToPool(id)
                }
                true
            }

            DragEvent.ACTION_DRAG_ENDED -> {
                setZoneHighlight(zone, false); true
            }

            else -> false
        }
    }

    /** 拖拽悬停高亮 */
    private fun setZoneHighlight(zone: View, on: Boolean) {
        zone.alpha = if (on) 0.88f else 1f
    }

    private fun observe() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    vm.nowTasks.collect { list ->
                        nowAdapter.submit(list)
                        binding.nowEmpty.visibility =
                            if (list.isEmpty()) View.VISIBLE else View.GONE
                    }
                }
                launch {
                    vm.poolTasks.collect { list ->
                        poolAdapter.submit(list)
                        binding.poolEmpty.visibility =
                            if (list.isEmpty()) View.VISIBLE else View.GONE
                    }
                }
            }
        }
    }
}
