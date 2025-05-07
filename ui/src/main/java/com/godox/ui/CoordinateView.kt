package com.godox.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.widget.FrameLayout

class CoordinateView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : FrameLayout(context, attrs) {
    private val mGraduatePaint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL_AND_STROKE
            color = resources.getColor(R.color.color_text_dark)
            strokeWidth = 2f
        }
    }

    private val mLabelPaint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = resources.getColor(R.color.color_text_dark)
            textSize = resources.getDimension(R.dimen.textSize12)
        }
    }

    private var textBoundRect = Rect()
    private var yAxisSpace = 0f
    private var xAxisSpace = 0f
    private val graduateWidth = resources.getDimension(R.dimen.size8)

    init {
        setWillNotDraw(false)
        yAxisSpace = mLabelPaint.measureText("0.80") + graduateWidth

        mLabelPaint.getTextBounds("0", 0, 1, textBoundRect)

        xAxisSpace = graduateWidth + (textBoundRect.height() * 3).toFloat()
        setPadding(
            (paddingLeft + yAxisSpace).toInt(),
            (paddingTop + mLabelPaint.textSize).toInt(),
            paddingRight,
            (paddingBottom + xAxisSpace + +mLabelPaint.textSize).toInt(),
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        mLabelPaint.textAlign = Paint.Align.LEFT

        val coordinateHeight = getChildAt(0).measuredHeight
        val coordinateWidth = getChildAt(0).measuredWidth

        // 绘制刻度
        val vGraduateGap = (coordinateHeight - mGraduatePaint.strokeWidth) / 9f
        val hGraduateGap = (coordinateWidth - mGraduatePaint.strokeWidth) / 9f

        repeat(10) {
            canvas.drawLine(
                yAxisSpace - graduateWidth,
                it * vGraduateGap + mLabelPaint.textSize + mGraduatePaint.strokeWidth,
                yAxisSpace,
                it * vGraduateGap + mLabelPaint.textSize + mGraduatePaint.strokeWidth,
                mGraduatePaint,
            )
        }

        repeat(10) {
            canvas.drawLine(
                yAxisSpace + hGraduateGap * it,
                coordinateHeight.toFloat() + mLabelPaint.textSize,
                yAxisSpace + hGraduateGap * it,
                coordinateHeight.toFloat() + mLabelPaint.textSize + graduateWidth,
                mGraduatePaint,
            )
        }

        // 绘制刻度标签
        repeat(10) {
            if (it == 0) {
                mLabelPaint.textAlign = Paint.Align.RIGHT
            } else {
                mLabelPaint.textAlign = Paint.Align.LEFT
            }
            canvas.drawText(
                if (it == 0) {
                    "Y"
                } else {
                    "0.${9 - it}"
                },
                if (it == 0) {
                    mLabelPaint.measureText("0.8")
                } else {
                    0f
                },
                mLabelPaint.textSize * 1.5f + vGraduateGap * it,
                mLabelPaint,
            )
        }

        mLabelPaint.textAlign = Paint.Align.CENTER

        repeat(10) {
            canvas.drawText(
                if (it < 9) {
                    "0.$it"
                } else {
                    "X"
                },
                yAxisSpace + hGraduateGap * it,
                coordinateHeight.toFloat() + graduateWidth + textBoundRect.height() * 2 + +mLabelPaint.textSize,
                mLabelPaint,
            )
        }
    }
}
