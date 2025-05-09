package com.godox.common.decoration

import android.graphics.Canvas
import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

/**
 * 将第0项的样式，copy一份出来，然后置顶在最顶部或者左边悬浮
 */
class FixedFirstItemDecoration(
    private val fixedWidth: Int, // 固定项的宽度（需与 position=0 的 Item 宽度一致）
    private val adapter: RecyclerView.Adapter<*> // 用于判断 position
) : RecyclerView.ItemDecoration() {

    private var fixedView: View? = null

    override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        super.onDrawOver(c, parent, state)

        // 只在 position=0 时绘制固定项
        if (adapter.itemCount == 0) return

        // 获取或创建固定 View
        val view = getFixedView(parent) ?: return

        // 计算固定 View 的绘制位置（始终左侧）
        val left = maxOf(0, parent.paddingLeft)
        c.save()
        c.translate(left.toFloat(), 0f)
        view.draw(c)
        c.restore()
    }

    private fun getFixedView(parent: RecyclerView): View? {
        if (fixedView == null) {
            // 从 Adapter 获取 position=0 的 ViewHolder（关键！）
            val holder = parent.adapter?.createViewHolder(parent, adapter.getItemViewType(0))
            if (holder != null) {
                parent.adapter?.bindViewHolder(holder, 0)
                fixedView = holder.itemView.apply {
                    // 手动测量和布局
                    measure(
                        View.MeasureSpec.makeMeasureSpec(fixedWidth, View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(parent.height, View.MeasureSpec.EXACTLY)
                    )
                    layout(0, 0, fixedWidth, parent.height)
                }
            }
        }
        return fixedView
    }

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        super.getItemOffsets(outRect, view, parent, state)
        // 为 position=0 的 Item 预留空间（避免被固定项遮盖）
        if (parent.getChildAdapterPosition(view) == 0) {
            outRect.left = fixedWidth
        }
    }
}
