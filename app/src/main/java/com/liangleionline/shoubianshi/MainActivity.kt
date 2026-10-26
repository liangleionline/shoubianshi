package com.liangleionline.shoubianshi

import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
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

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val vm: MainViewModel by viewModels()

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
            // 单击：胶囊飞向另一区域（上区 <-> 下区）
            tag.setOnClickListener { onTagTap(it, task.id, fromNow) }
            flow.addView(tag)
            // 若是单击飞行移动过来的胶囊：先隐藏真实胶囊，让幽灵飞向它的真实落点，到达后再显示
            if (task.id == flyTargetId && flyTargetId != 0L) {
                tag.visibility = View.INVISIBLE
                startFlyTo(tag)
            }
        }
        empty.visibility = if (tasks.isEmpty()) View.VISIBLE else View.GONE
    }

    // ---------------- 单击：胶囊飞向目标区域 ----------------

    private fun onTagTap(v: View, id: Long, fromNow: Boolean) {
        flyTargetId = id

        // 起飞点 = 胶囊自身当前所在的位置，与点击坐标无关。
        // 注意：root 有 fitsSystemWindows，会被系统加上顶部 padding，幽灵作为 root 的子 view
        // 布局时会再叠加一次该 padding，因此这里把 padding 一并扣掉，确保 ghost 落点 = 胶囊位置。
        val srcPos = IntArray(2)
        v.getLocationOnScreen(srcPos)
        val rootOffset = IntArray(2)
        binding.root.getLocationOnScreen(rootOffset)
        flySrcX = (srcPos[0] - rootOffset[0] - binding.root.paddingLeft).toFloat()
        flySrcY = (srcPos[1] - rootOffset[1] - binding.root.paddingTop).toFloat()

        v.visibility = View.INVISIBLE

        // 先移动数据，触发目标区重渲染；fillZone 会在渲染出该胶囊后接管飞行终点
        if (fromNow) vm.moveToPool(id) else vm.moveToNow(id)
    }

    /** 目标区渲染出飞行胶囊后：等布局完成才创建幽灵，从胶囊当前位置直线飞向落点 */
    private fun startFlyTo(tag: View) {
        tag.post {
            // 此刻才创建幽灵：用 LayoutParams margin 布局定位（布局位置 = 胶囊位置），不存在中间帧
            val ghost = layoutInflater.inflate(R.layout.item_task, binding.root, false) as TextView
            val t = tag as TextView
            ghost.text = t.text
            ghost.background = t.background
            ghost.setTextColor(t.currentTextColor)
            ghost.alpha = 1f
            ghost.elevation = 12f
            ghost.isClickable = false
            val spec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            ghost.measure(spec, spec)
            ghost.layout(0, 0, ghost.measuredWidth, ghost.measuredHeight)
            // 用 margin 定位：FrameLayout 布局 child = padding + margin，这里 margin 已含 padding 补偿
            val lp = FrameLayout.LayoutParams(ghost.measuredWidth, ghost.measuredHeight)
            lp.leftMargin = flySrcX.toInt()
            lp.topMargin = flySrcY.toInt()
            binding.root.addView(ghost, lp)
            flyGhost = ghost

            // 落点：目标胶囊位置（同样扣掉 root padding，保持同一坐标系）
            val pos = IntArray(2)
            tag.getLocationOnScreen(pos)
            val rootOffset = IntArray(2)
            binding.root.getLocationOnScreen(rootOffset)
            val targetX = (pos[0] - rootOffset[0] - binding.root.paddingLeft).toFloat()
            val targetY = (pos[1] - rootOffset[1] - binding.root.paddingTop).toFloat()

            val dx = targetX - flySrcX
            val dy = targetY - flySrcY
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
