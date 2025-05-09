package com.godox.common.decoration

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.godox.common.util.dp

/**
 * 分割线，可以两端不满屏
 */
class SpaceDividerDecoration(
    private val color: Int = Color.GRAY,
    private val height: Float = 1f.dp, // 线条高度（默认1dp）
    private val marginStart: Float = 0f, // 左/上边距（根据方向）
    private val marginEnd: Float = 0f,  // 右/下边距
    private val isFullWidth: Boolean = true, // 是否两端满屏,满屏后margin值失效
    private val orientation: Int = LinearLayoutManager.VERTICAL, // 支持横竖方向
) : RecyclerView.ItemDecoration() {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = this@SpaceDividerDecoration.color
        style = Paint.Style.FILL
    }

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        // 为每个Item下方/右侧预留分割线空间
        when (orientation) {
            LinearLayoutManager.VERTICAL -> outRect.bottom = height.toInt()
            LinearLayoutManager.HORIZONTAL -> outRect.right = height.toInt()
        }
    }

    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        if (parent.layoutManager == null || parent.childCount <= 1) return

        when (orientation) {
            LinearLayoutManager.VERTICAL -> drawVerticalDividers(c, parent)
            LinearLayoutManager.HORIZONTAL -> drawHorizontalDividers(c, parent)
        }
    }

    private fun drawVerticalDividers(c: Canvas, parent: RecyclerView) {
        val left = if (isFullWidth) {
            parent.paddingLeft.toFloat()
        } else {
            parent.paddingLeft + marginStart.dp
        }

        val right = if (isFullWidth) {
            parent.width - parent.paddingRight.toFloat()
        } else {
            parent.width - parent.paddingRight - marginEnd.dp
        }

        for (i in 0 until parent.childCount - 1) { // 最后一个Item不画
            val child = parent.getChildAt(i)
            val params = child.layoutParams as RecyclerView.LayoutParams
            val top = child.bottom + params.bottomMargin
            val bottom = top + height
            c.drawRect(left, top.toFloat(), right, bottom, paint)
        }
    }

    private fun drawHorizontalDividers(c: Canvas, parent: RecyclerView) {
        val top = if (isFullWidth) {
            parent.paddingTop.toFloat()
        } else {
            parent.paddingTop + marginStart.dp
        }

        val bottom = if (isFullWidth) {
            parent.height - parent.paddingBottom.toFloat()
        } else {
            parent.height - parent.paddingBottom - marginEnd.dp
        }

        for (i in 0 until parent.childCount - 1) {
            val child = parent.getChildAt(i)
            val params = child.layoutParams as RecyclerView.LayoutParams
            val left = child.right + params.rightMargin
            val right = left + height
            c.drawRect(left.toFloat(), top, right, bottom, paint)
        }
    }
}

