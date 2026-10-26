package com.liangleionline.shoubianshi

import android.content.Intent
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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

    // ---------------- 手写拖拽状态 ----------------
    private var dragging = false
    private var dragGhost: TextView? = null
    private var dragSource: View? = null
    private var dragId = 0L
    private var dragFromNow = false

    // ---------------- 单击飞行状态 ----------------
    private var flySrcX = 0f
    private var flySrcY = 0f
    private var flyGhost: TextView? = null
    private var flyTargetId = 0L

    /** 胶囊配色（浅色背景 / 同色系深色文字），按事项 id 稳定分配 */
    private val capsulePalette = listOf(
        R.color.capsule_blue_bg to R.color.capsule_blue_text,
        R.color.capsule_green_bg to R.color.capsule_green_text,
        R.color.capsule_orange_bg to R.color.capsule_orange_text,
        R.color.capsule_purple_bg to R.color.capsule_purple_text,
        R.color.capsule_pink_bg to R.color.capsule_pink_text,
        R.color.capsule_teal_bg to R.color.capsule_teal_text,
        R.color.capsule_red_bg to R.color.capsule_red_text,
        R.color.capsule_indigo_bg to R.color.capsule_indigo_text,
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnManage.setOnClickListener {
            startActivity(Intent(this, ManageActivity::class.java))
        }
        observe()
    }

    // ---------------- 标签填充（椭圆小图标，多行排布） ----------------

    private fun fillZone(flow: FlowLayout, empty: View, tasks: List<Task>, fromNow: Boolean) {
        flow.removeAllViews()
        for (task in tasks) {
            val tag = layoutInflater.inflate(R.layout.item_task, flow, false) as TextView
            tag.text = task.title
            tag.isClickable = true
            // 上色：浅色胶囊底 + 同色系深色文字
            val (bgRes, textRes) = capsulePalette[(task.id % capsulePalette.size).toInt()]
            tag.setTextColor(ContextCompat.getColor(this, textRes))
            val radius = resources.getDimension(R.dimen.tag_corner)
            tag.background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = radius
                setColor(ContextCompat.getColor(this@MainActivity, bgRes))
            }
            // 胶囊之间留出水平与垂直间距
            val density = resources.displayMetrics.density
            val lp = tag.layoutParams as ViewGroup.MarginLayoutParams
            lp.rightMargin = (8 * density).toInt()
            lp.bottomMargin = (8 * density).toInt()
            tag.setOnTouchListener { v, e -> onTagTouch(v, e, task.id, fromNow) }
            flow.addView(tag)
            // 若是单击飞行移动过来的胶囊：先隐藏真实胶囊，让幽灵飞向它的真实落点，到达后再显示
            if (task.id == flyTargetId && flyTargetId != 0L) {
                tag.visibility = View.INVISIBLE
                startFlyTo(tag)
            }
        }
        empty.visibility = if (tasks.isEmpty()) View.VISIBLE else View.GONE
    }

    // ---------------- 手指直接拖拽（手写实现，不依赖系统拖拽框架） ----------------

    private fun onTagTouch(v: View, e: MotionEvent, id: Long, fromNow: Boolean): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                v.parent?.requestDisallowInterceptTouchEvent(true)
                v.setTag(R.id.drag_down, Pair(e.rawX, e.rawY))
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (!dragging) {
                    val down = v.getTag(R.id.drag_down) as? Pair<*, *> ?: return true
                    val slop = ViewConfiguration.get(this).scaledTouchSlop
                    val moved =
                        abs(e.rawX - (down.first as Float)) > slop ||
                            abs(e.rawY - (down.second as Float)) > slop
                    if (moved) beginDrag(v, id, fromNow)
                }
                if (dragging) {
                    moveGhost(e.rawX, e.rawY)
                    highlightTarget(e.rawX, e.rawY)
                }
                return true
            }

            MotionEvent.ACTION_UP -> {
                if (dragging) endDrag(e.rawX, e.rawY) else onTagTap(v, id, fromNow)
                dragging = false
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                if (dragging) cancelDrag()
                dragging = false
                return true
            }
        }
        return true
    }

    /** 手指移动超过阈值：生成悬浮幽灵胶囊，原标签隐藏 */
    private fun beginDrag(v: View, id: Long, fromNow: Boolean) {
        dragging = true
        dragId = id
        dragFromNow = fromNow
        dragSource = v

        val ghost = layoutInflater.inflate(R.layout.item_task, binding.root, false) as TextView
        val src = v as TextView
        ghost.text = src.text
        ghost.background = src.background
        ghost.setTextColor(src.currentTextColor)
        ghost.alpha = 0.92f
        ghost.elevation = 12f
        ghost.isClickable = false
        binding.root.addView(ghost)

        // 同步测量布局，确保 width/height 立即可用，避免首帧用 0 尺寸定位导致瞬移闪烁
        val spec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        ghost.measure(spec, spec)
        ghost.layout(0, 0, ghost.measuredWidth, ghost.measuredHeight)
        dragGhost = ghost

        v.visibility = View.INVISIBLE
        // 幽灵定位到与源胶囊完全重合，做到无缝衔接
        val srcPos = IntArray(2)
        v.getLocationOnScreen(srcPos)
        positionGhost(ghost, srcPos[0] + v.width / 2f, srcPos[1] + v.height / 2f)
    }

    /** 幽灵跟随手指（rawX/rawY 为屏幕坐标，换算到根布局坐标） */
    private fun positionGhost(ghost: TextView, rawX: Float, rawY: Float) {
        val offset = IntArray(2)
        binding.root.getLocationOnScreen(offset)
        ghost.x = rawX - offset[0] - ghost.width / 2f
        ghost.y = rawY - offset[1] - ghost.height / 2f
    }

    private fun moveGhost(rawX: Float, rawY: Float) {
        dragGhost?.let { positionGhost(it, rawX, rawY) }
    }

    /** 松手：按落点判定进入哪个区域并执行 */
    private fun endDrag(rawX: Float, rawY: Float) {
        when (findTarget(rawX, rawY)) {
            "now" -> if (!dragFromNow) vm.moveToNow(dragId)
            "pool" -> if (dragFromNow) vm.moveToPool(dragId)
        }
        cleanupDrag()
    }

    private fun cancelDrag() = cleanupDrag()

    private fun cleanupDrag() {
        resetHighlight()
        dragSource?.visibility = View.VISIBLE
        dragGhost?.let { binding.root.removeView(it) }
        dragGhost = null
        dragSource = null
        dragging = false
    }

    /** 落点区域：上区 / 下区 */
    private fun findTarget(rawX: Float, rawY: Float): String? = when {
        contains(binding.zoneNow, rawX, rawY) -> "now"
        contains(binding.zonePool, rawX, rawY) -> "pool"
        else -> null
    }

    private fun contains(view: View, rawX: Float, rawY: Float): Boolean {
        val rect = Rect()
        view.getGlobalVisibleRect(rect)
        return rect.contains(rawX.toInt(), rawY.toInt())
    }

    /** 拖拽悬停高亮 */
    private fun highlightTarget(rawX: Float, rawY: Float) {
        binding.zoneNow.alpha = if (contains(binding.zoneNow, rawX, rawY)) 0.86f else 1f
        binding.zonePool.alpha = if (contains(binding.zonePool, rawX, rawY)) 0.86f else 1f
    }

    private fun resetHighlight() {
        binding.zoneNow.alpha = 1f
        binding.zonePool.alpha = 1f
    }

    // ---------------- 单击：胶囊飞向目标区域 ----------------

    private fun onTagTap(v: View, id: Long, fromNow: Boolean) {
        // 记录源胶囊中心（屏幕坐标）
        val srcPos = IntArray(2)
        v.getLocationOnScreen(srcPos)
        flySrcX = srcPos[0] + v.width / 2f
        flySrcY = srcPos[1] + v.height / 2f
        flyTargetId = id

        // 幽灵副本
        val ghost = layoutInflater.inflate(R.layout.item_task, binding.root, false) as TextView
        val src = v as TextView
        ghost.text = src.text
        ghost.background = src.background
        ghost.setTextColor(src.currentTextColor)
        ghost.alpha = 1f
        ghost.elevation = 12f
        ghost.isClickable = false
        val spec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        ghost.measure(spec, spec)
        ghost.layout(0, 0, ghost.measuredWidth, ghost.measuredHeight)
        binding.root.addView(ghost)

        // addView 完成后再设位移：幽灵中心与源胶囊中心重合（精确从原位置起飞）
        val rootOffset = IntArray(2)
        binding.root.getLocationOnScreen(rootOffset)
        ghost.translationX = flySrcX - rootOffset[0] - ghost.width / 2f
        ghost.translationY = flySrcY - rootOffset[1] - ghost.height / 2f
        flyGhost = ghost

        v.visibility = View.INVISIBLE

        // 先移动数据，触发目标区重渲染；fillZone 会在渲染出该胶囊后接管飞行终点
        if (fromNow) vm.moveToPool(id) else vm.moveToNow(id)
    }

    /** 目标区渲染出飞行胶囊后：等布局完成拿到真实落点，驱动幽灵从源位置直接飞过去 */
    private fun startFlyTo(tag: View) {
        val ghost = flyGhost ?: return
        tag.post {
            val rootOffset = IntArray(2)
            binding.root.getLocationOnScreen(rootOffset)
            // 重新断言幽灵起点 = 源胶囊中心（此时根已布局完，ghost.width 为真实宽度；
            // 防止移动数据期间素材库重排触发根布局、把位移重置/算偏，导致起点偏移）
            ghost.translationX = flySrcX - rootOffset[0] - ghost.width / 2f
            ghost.translationY = flySrcY - rootOffset[1] - ghost.height / 2f
            ghost.alpha = 1f
            ghost.scaleX = 1f
            ghost.scaleY = 1f
            val pos = IntArray(2)
            tag.getLocationOnScreen(pos)
            val dx = (pos[0] + tag.width / 2f) - flySrcX
            val dy = (pos[1] + tag.height / 2f) - flySrcY
            ghost.animate()
                .translationXBy(dx)
                .translationYBy(dy)
                .scaleX(0.55f)
                .scaleY(0.55f)
                .alpha(0.2f)
                .setDuration(280)
                .withEndAction {
                    binding.root.removeView(ghost)
                    flyGhost = null
                    flyTargetId = 0L
                    tag.visibility = View.VISIBLE
                }
                .start()
        }
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
