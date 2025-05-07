package com.godox.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.widget.FrameLayout

/**
 * Q shadow layout
 *
 * 适用于给 正圆、圆角矩形（四个圆角相等）图形 设置阴影
 *
 * 支持的属性
 *  阴影颜色
 *  阴影大小
 *  阴影x轴偏移大小
 *  阴影x轴正方向偏移（Boolean）
 *  阴影y轴偏移大小
 *  阴影y轴正方向偏移（Boolean）
 *  圆角大小
 *  是否为正圆
 *
 * @constructor
 *
 * @param context
 * @param attrs
 * @param defStyleAttr
 */
class QShadowLayout @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : FrameLayout(context, attrs, defStyleAttr) {
    private var centerXY: Float = 0f
    private val shadowPaint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.TRANSPARENT // Color.parseColor("#230000ff")
            style = Paint.Style.FILL
        }
    }
    private var shadowColor = -1
    private var shadowSize = 0f
    private var xAxisOffsetSize = 0f
    private var xAxisPositiveOffset = true
    private var yAxisOffsetSize = 0f
    private var yAxisPositiveOffset = true
    private var cornerRadius = 0f
    private var isOval = false

    private var topShadowSize = 0f
    private var leftShadowSize = 0f
    private var rightShadowSize = 0f
    private var bottomShadowSize = 0f

    private var xAxisOffsetSizeWithSymbol = 0f
    private var yAxisOffsetSizeWithSymbol = 0f

    private var isShowShadow = true

    // 阴影半径收缩大小
    private var shadowRadiusShrinkSize = 0f

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)

        val attr = getContext().obtainStyledAttributes(attrs, R.styleable.QShadowLayout)

        attr.apply {
            isShowShadow = attr.getBoolean(R.styleable.QShadowLayout_isShowShadow, true)
            shadowColor = attr.getColor(R.styleable.QShadowLayout_shadowColor, -1)
            shadowSize = attr.getDimension(R.styleable.QShadowLayout_shadowSize, 0f)
            xAxisOffsetSize = attr.getDimension(R.styleable.QShadowLayout_xAxisOffsetSize, 0f)
            yAxisOffsetSize = attr.getDimension(R.styleable.QShadowLayout_yAxisOffsetSize, 0f)
            xAxisPositiveOffset = attr.getBoolean(R.styleable.QShadowLayout_xAxisPositiveOffset, true)
            yAxisPositiveOffset = attr.getBoolean(R.styleable.QShadowLayout_yAxisPositiveOffset, true)
            cornerRadius = attr.getDimension(R.styleable.QShadowLayout_cornerRadius, 0f)
            isOval = attr.getBoolean(R.styleable.QShadowLayout_isOval, false)
            shadowRadiusShrinkSize = attr.getDimension(R.styleable.QShadowLayout_shadowRadiusShrinkSize, 0f)
            recycle()
        }

        xAxisOffsetSizeWithSymbol = if (xAxisPositiveOffset) xAxisOffsetSize else -xAxisOffsetSize
        yAxisOffsetSizeWithSymbol = if (yAxisPositiveOffset) yAxisOffsetSize else -yAxisOffsetSize

        leftShadowSize = shadowSize - xAxisOffsetSizeWithSymbol + paddingLeft
        rightShadowSize = shadowSize + xAxisOffsetSizeWithSymbol + paddingRight
        topShadowSize = shadowSize - yAxisOffsetSizeWithSymbol + paddingTop
        bottomShadowSize = shadowSize + yAxisOffsetSizeWithSymbol + paddingBottom

        setShadowLayer()
    }

    private fun setShadowLayer() {
        if (!isShowShadow) return

        setPadding(
            leftShadowSize.toInt(),
            topShadowSize.toInt(),
            rightShadowSize.toInt(),
            bottomShadowSize.toInt(),
        )

        shadowPaint.setShadowLayer(
            shadowSize,
            xAxisOffsetSizeWithSymbol,
            yAxisOffsetSizeWithSymbol,
            shadowColor,
        )
    }

    override fun onMeasure(
        widthMeasureSpec: Int,
        heightMeasureSpec: Int,
    ) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        centerXY = ((measuredWidth / 2).toFloat())
    }

    override fun dispatchDraw(canvas: Canvas) {
        if (isOval) {
            canvas.drawCircle(
                centerXY - xAxisOffsetSizeWithSymbol,
                centerXY - yAxisOffsetSizeWithSymbol,
                centerXY - shadowSize - shadowRadiusShrinkSize,
                shadowPaint,
            )
        } else {
            canvas.drawRoundRect(
                leftShadowSize,
                topShadowSize,
                width - rightShadowSize,
                height - bottomShadowSize,
                cornerRadius,
                cornerRadius,
                shadowPaint,
            )
        }

        super.dispatchDraw(canvas)
    }

    fun hideShadow() {
        isShowShadow = false
        shadowPaint.clearShadowLayer()
        postInvalidate()
    }

    fun showShadow() {
        isShowShadow = true
        setShadowLayer()
        postInvalidate()
    }

    fun isShowShadow(): Boolean = isShowShadow
}
