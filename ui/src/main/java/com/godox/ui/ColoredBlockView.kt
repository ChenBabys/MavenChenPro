package com.godox.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.SweepGradient
import android.util.AttributeSet
import android.view.View

/**
 * 七彩圆角色块
 *
 * @constructor
 *
 * @param context
 * @param attrs
 * @param defStyleAttr
 */
class ColoredBlockView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : View(context, attrs, defStyleAttr) {
    private val GRAD_COLORS =
        intArrayOf(
            Color.RED,
            Color.MAGENTA,
            Color.BLUE,
            Color.CYAN,
            Color.GREEN,
            Color.YELLOW,
            Color.RED,
        )

    private val mPaint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader =
                SweepGradient(
                    width / 2f + paddingLeft,
                    height / 2f + paddingTop,
                    GRAD_COLORS,
                    null,
                )
        }
    }

    private val bPaint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG)
    }

    private val strokeWith by lazy {
        context.resources.getDimension(R.dimen.size1)
    }

    // 圆角半径
    private val radius by lazy {
        context.resources.getDimension(R.dimen.size3)
    }

    override fun onMeasure(
        widthMeasureSpec: Int,
        heightMeasureSpec: Int,
    ) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        setMeasuredDimension(
            getDefaultSize(suggestedMinimumWidth, widthMeasureSpec),
            getDefaultSize(suggestedMinimumHeight, heightMeasureSpec),
        )
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        if (colorMode == ColorMode.Multi) {
            canvas.drawRoundRect(0f, 0f, width.toFloat(), height.toFloat(), radius, radius, mPaint)
        } else {
            bPaint.color = Color.WHITE
            canvas.drawRoundRect(0f, 0f, width.toFloat(), height.toFloat(), radius, radius, bPaint)
            bPaint.color = singleColor
            canvas.drawRoundRect(
                0f + strokeWith,
                0f + strokeWith,
                width.toFloat() - strokeWith,
                height.toFloat() - strokeWith,
                radius,
                radius,
                bPaint,
            )
        }
    }

    private var colorMode: ColorMode = ColorMode.Multi
    private var singleColor: Int = 0

    fun transform(
        mode: ColorMode,
        color: Int = 0,
    ) {
        colorMode = mode
        singleColor = color
        invalidate()
    }

    enum class ColorMode {
        Single,
        Multi,
    }
}
