package com.godox.common.view

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import com.godox.common.R

/**
 * Author: ChenBabys
 * Date: 2025/4/15
 * Description: 圆形进度条,如果不满足需求,可以使用使用 Material Components 的 CircularProgressIndicator
 */
class CircularProgressView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rectF = RectF()

    private var progress = 0f // 0-100
    private var maxProgress = 100f
    private var strokeWidth = 20f
    private var bgColor = Color.LTGRAY
    private var progressColor = Color.BLUE
    private var textColor = Color.BLACK
    private var textSize = 40f
    private var showText = true

    init {
        // 从XML属性获取自定义属性
        context.theme.obtainStyledAttributes(attrs, R.styleable.CircularProgressView, 0, 0).apply {
            try {
                progress = getFloat(R.styleable.CircularProgressView_circular_progress, 0f)
                maxProgress = getFloat(R.styleable.CircularProgressView_circular_maxProgress, 100f)
                strokeWidth = getDimension(R.styleable.CircularProgressView_circular_strokeWidth, 20f)
                bgColor = getColor(R.styleable.CircularProgressView_circular_bgColor, Color.LTGRAY)
                progressColor = getColor(R.styleable.CircularProgressView_circular_progressColor, Color.BLUE)
                textColor = getColor(R.styleable.CircularProgressView_circular_textColor, Color.BLACK)
                textSize = getDimension(R.styleable.CircularProgressView_circular_textSize, 40f)
                showText = getBoolean(R.styleable.CircularProgressView_circular_showText, true)
            } finally {
                recycle()
            }
        }

        setupPaints()
    }

    private fun setupPaints() {
        backgroundPaint.color = bgColor
        backgroundPaint.style = Paint.Style.STROKE
        backgroundPaint.strokeWidth = strokeWidth
        backgroundPaint.strokeCap = Paint.Cap.ROUND

        progressPaint.color = progressColor
        progressPaint.style = Paint.Style.STROKE
        progressPaint.strokeWidth = strokeWidth
        progressPaint.strokeCap = Paint.Cap.ROUND

        textPaint.color = textColor
        textPaint.textSize = textSize
        textPaint.textAlign = Paint.Align.CENTER
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val centerX = width / 2f
        val centerY = height / 2f
        val radius = (width.coerceAtMost(height) / 2f - strokeWidth / 2f)
        // 绘制背景圆
        canvas.drawCircle(centerX, centerY, radius, backgroundPaint)
        // 绘制进度弧
        rectF.set(
            centerX - radius,
            centerY - radius,
            centerX + radius,
            centerY + radius,
        )
        val sweepAngle = 360 * (progress / maxProgress)
        canvas.drawArc(rectF, -90f, sweepAngle, false, progressPaint)

        // 绘制进度文本
        if (showText) {
            val text = "${progress.toInt()}%"
            val textY = centerY - (textPaint.descent() + textPaint.ascent()) / 2f
            canvas.drawText(text, centerX, textY, textPaint)
        }
    }

    fun setProgress(progress: Float) {
        this.progress = progress.coerceIn(0f, maxProgress)
        invalidate()
    }

    fun getProgress(): Float {
        return this.progress
    }

    fun setMaxProgress(max: Float) {
        this.maxProgress = max
        invalidate()
    }

    fun setProgressColor(color: Int) {
        progressPaint.color = color
        invalidate()
    }

    fun setProgressBackgroundColor(color: Int) {
        backgroundPaint.color = color
        invalidate()
    }
}
