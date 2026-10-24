package com.liangleionline.shoubianshi

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup

/** 自动换行的流式布局，用于多列排布椭圆标签。 */
class FlowLayout(context: Context, attrs: AttributeSet? = null) : ViewGroup(context, attrs) {

    override fun generateLayoutParams(attrs: AttributeSet?): LayoutParams =
        MarginLayoutParams(context, attrs)

    override fun generateLayoutParams(p: LayoutParams): LayoutParams = MarginLayoutParams(p)

    override fun generateDefaultLayoutParams(): LayoutParams =
        MarginLayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val hMode = MeasureSpec.getMode(heightMeasureSpec)
        val hSize = MeasureSpec.getSize(heightMeasureSpec)

        var x = paddingLeft
        var y = paddingTop
        var lineHeight = 0

        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.visibility == View.GONE) continue
            val lp = child.layoutParams as MarginLayoutParams
            measureChildWithMargins(child, widthMeasureSpec, 0, heightMeasureSpec, 0)
            val cw = child.measuredWidth + lp.leftMargin + lp.rightMargin
            val ch = child.measuredHeight + lp.topMargin + lp.bottomMargin
            if (x + cw > widthSize - paddingRight) {
                x = paddingLeft
                y += lineHeight
                lineHeight = 0
            }
            lineHeight = maxOf(lineHeight, ch)
            x += cw
        }

        val totalHeight = y + lineHeight + paddingBottom
        when (hMode) {
            MeasureSpec.EXACTLY -> setMeasuredDimension(widthSize, hSize)
            MeasureSpec.AT_MOST -> setMeasuredDimension(widthSize, minOf(totalHeight, hSize))
            else -> setMeasuredDimension(widthSize, totalHeight)
        }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val width = right - left
        var x = paddingLeft
        var y = paddingTop
        var lineHeight = 0

        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.visibility == View.GONE) continue
            val lp = child.layoutParams as MarginLayoutParams
            val cw = child.measuredWidth
            val ch = child.measuredHeight
            if (x + lp.leftMargin + cw + lp.rightMargin > width - paddingRight) {
                x = paddingLeft
                y += lineHeight
                lineHeight = 0
            }
            child.layout(
                x + lp.leftMargin, y + lp.topMargin,
                x + lp.leftMargin + cw, y + lp.topMargin + ch
            )
            x += lp.leftMargin + cw + lp.rightMargin
            lineHeight = maxOf(lineHeight, lp.topMargin + ch + lp.bottomMargin)
        }
    }
}
