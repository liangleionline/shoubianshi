package com.liangleionline.shoubianshi

import android.content.ClipData
import android.content.Intent
import android.os.Bundle
import android.view.DragEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.liangleionline.shoubianshi.data.Task
import com.liangleionline.shoubianshi.databinding.ActivityMainBinding
import kotlinx.coroutines.launch
import kotlin.math.abs

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnManage.setOnClickListener {
            startActivity(Intent(this, ManageActivity::class.java))
        }
        setupDropTargets()
        observe()
    }

    // ---------------- 标签填充（椭圆小图标，多行排布） ----------------

    private fun fillZone(flow: FlowLayout, empty: View, tasks: List<Task>, fromNow: Boolean) {
        flow.removeAllViews()
        for (task in tasks) {
            val tag = layoutInflater.inflate(R.layout.item_task, flow, false) as TextView
            tag.text = task.title
            tag.isClickable = true
            tag.setOnTouchListener { v, e -> onTagTouch(v, e, task.id, fromNow) }
            flow.addView(tag)
        }
        empty.visibility = if (tasks.isEmpty()) View.VISIBLE else View.GONE
    }

    // ---------------- 直接拖拽（手指移动即启动，无需长按） ----------------

    private fun onTagTouch(v: View, e: MotionEvent, id: Long, fromNow: Boolean): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                // 认领手势，保证后续 MOVE 稳定到达、不被父容器拦截
                v.parent?.requestDisallowInterceptTouchEvent(true)
                v.setTag(R.id.drag_state, 0)
                v.setTag(R.id.drag_down, Pair(e.rawX, e.rawY))
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (v.getTag(R.id.drag_state) as? Int == 1) return true
                val down = v.getTag(R.id.drag_down) as? Pair<*, *> ?: return true
                val slop = ViewConfiguration.get(this).scaledTouchSlop
                val moved =
                    abs(e.rawX - (down.first as Float)) > slop ||
                        abs(e.rawY - (down.second as Float)) > slop
                if (moved) {
                    v.setTag(R.id.drag_state, 1)
                    startDrag(v, id, fromNow)
                }
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                v.setTag(R.id.drag_state, 0)
        }
        return true
    }

    private fun startDrag(v: View, id: Long, fromNow: Boolean) {
        val clip = ClipData.newPlainText("task_id", id.toString())
        val shadow = View.DragShadowBuilder(v)
        // localState 携带源区信息：true=来自上区(待办)，false=来自下区(素材库)
        v.startDragAndDrop(clip, shadow, fromNow, 0)
    }

    // ---------------- 拖放目标：上区 / 下区 / 完成区 ----------------

    private fun setupDropTargets() {
        binding.zoneNow.setOnDragListener { _, e -> onDrag(e, targetZone = "now") }
        binding.zonePool.setOnDragListener { _, e -> onDrag(e, targetZone = "pool") }
        binding.dropZone.setOnDragListener { _, e -> onDrag(e, targetZone = "drop") }
    }

    private fun onDrag(event: DragEvent, targetZone: String): Boolean {
        val fromNow = event.localState as? Boolean ?: false
        return when (event.action) {
            DragEvent.ACTION_DRAG_STARTED -> {
                // 仅从上区拖出时才在底部浮现完成区
                showDropZone(fromNow)
                true
            }

            DragEvent.ACTION_DRAG_ENTERED -> {
                if (targetZone == "drop") binding.dropZone.alpha = 0.6f
                true
            }

            DragEvent.ACTION_DRAG_EXITED -> {
                if (targetZone == "drop") binding.dropZone.alpha = 1f
                true
            }

            DragEvent.ACTION_DROP -> {
                binding.dropZone.alpha = 1f
                event.clipData?.getItemAt(0)?.text?.toString()?.toLongOrNull()?.let { id ->
                    when (targetZone) {
                        "now" -> if (!fromNow) vm.moveToNow(id)
                        "pool" -> if (fromNow) vm.moveToPool(id)
                        "drop" -> if (fromNow) vm.deleteById(id)
                    }
                }
                true
            }

            DragEvent.ACTION_DRAG_ENDED -> {
                hideDropZone()
                true
            }

            else -> true
        }
    }

    // ---------------- 完成区显隐与动效 ----------------

    private fun showDropZone(show: Boolean) {
        val dz = binding.dropZone
        if (show && dz.visibility != View.VISIBLE) {
            dz.visibility = View.VISIBLE
            dz.alpha = 0f
            dz.animate().alpha(1f).setDuration(150).start()
        } else if (!show) {
            hideDropZone()
        }
    }

    private fun hideDropZone() {
        val dz = binding.dropZone
        dz.animate().cancel()
        dz.visibility = View.GONE
        dz.alpha = 1f
    }

    // ---------------- 数据观察 ----------------

    private fun observe() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    vm.nowTasks.collect { list ->
                        fillZone(binding.nowFlow, binding.nowEmpty, list, fromNow = true)
                    }
                }
                launch {
                    vm.poolTasks.collect { list ->
                        fillZone(binding.poolFlow, binding.poolEmpty, list, fromNow = false)
                    }
                }
            }
        }
    }
}
