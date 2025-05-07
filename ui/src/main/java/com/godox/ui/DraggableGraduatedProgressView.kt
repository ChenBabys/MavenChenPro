package com.godox.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.max
import kotlin.math.min

/**
 * 自定义可拖动的有刻度的进度条
 *  有刻度可以保证得到的progress的唯一性，移动滑块时，滑块会一顿一顿的。
 */
@SuppressLint("CustomViewStyleable")
class DraggableGraduatedProgressView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : View(context, attrs, defStyleAttr) {
    // 灰色背景线段的画笔
    private val bgPaint by lazy {
        Paint().apply {
            style = Paint.Style.FILL_AND_STROKE
            strokeCap = Paint.Cap.ROUND
        }
    }

    // 实际进度绿色线段的画笔
    private val progressPaint by lazy {
        Paint().apply {
            style = Paint.Style.FILL_AND_STROKE
            strokeCap = Paint.Cap.ROUND
        }
    }

    // 圆点指示器的画笔
    private val circlePaint by lazy {
        Paint().apply {
            style = Paint.Style.FILL
        }
    }
    private val markCirclePaint by lazy {
        Paint().apply {
            style = Paint.Style.FILL
        }
    }

    // 每个刻度块的宽度大小
    private var mMarkBlockWidth = 0f

    // 刻度块的个数，没有设置的话使用maxProgress赋值
    private var mMarkCount = 0f

    // 当前View的宽度
    private var width = 0
    private var height = 0
    private var oldWidth = 0
    private var paddingLeft = 0 // 距离左边的内边距
    private var paddingRight = 0 // 距离右边的内边距
    private var isDrag = true // 是否可拖动
    private var showDotSlider = true // 是否显示圆点指示器

    // 圆点滑块
    private var dotSliderRadius = 0 // 圆点指示器的半径 默认10dp
    private var dotSliderColor = Color.WHITE // 圆形指示器颜色
    private var dotSliderShadowColor = Color.WHITE // 圆形指示器阴影颜色
    private var dotSliderShadowRadius = 0 // 圆形指示器阴影半径
    private var dotSliderColorN = 0 // 圆形指示器不可拖动时颜色
    private var dotSliderShadowColorN = 0 // 圆形指示器阴影不可拖动时颜色
    private var currentDotSliderProgress = 0f // 圆点指示器的水平位置
    private var maxDotSliderDistance = 0f // 圆点指示器最大移动距离

    // 进度条
    private var progressBackgroundColor = Color.BLACK // 进度条背景颜色
    private var progressStartBackgroundColor = 0
    private var progressCenterBackgroundColor = 0
    private var progressEndBackgroundColor = 0
    private var progressColor = Color.WHITE // 进度条颜色
    private var progressHeight = 0 // 进度条高度 默认10dp
    private var progressColorN = 0 // 进度条不可拖动时颜色
    private var currentProgress = 0f // 进度条当前的宽度
    private var maxProgressWidth = 0f // 进度条的最大宽度
    private var maxProgress = 100f // 最大进度值

    private val isCanTouch = true

    private var onProgressListener: OnProgressListener? = null

    // 设置进度时,View可能未可见,需view可见时执行
    private var setProgressRunnable: Runnable? = null

    init {
        // attrs不为null, 是在xml中静态创建，否则在代码中动态创建。
        if (attrs != null) {
            val mTypedArray = context.obtainStyledAttributes(attrs, R.styleable.DraggableGraduatedProgressView)
            isDrag = mTypedArray.getBoolean(R.styleable.DraggableGraduatedProgressView_dgp_isCanDrag, true) // 默认可拖动
            progressHeight = mTypedArray.getLayoutDimension(R.styleable.DraggableGraduatedProgressView_dgp_progressHeight, dip2px(context, 10f))
            progressBackgroundColor =
                mTypedArray.getColor(
                    R.styleable.DraggableGraduatedProgressView_dgp_progressBackgroundColor,
                    progressBackgroundColor,
                )
            progressStartBackgroundColor =
                mTypedArray.getColor(
                    R.styleable.DraggableGraduatedProgressView_dgp_progressStartBackgroundColor,
                    progressStartBackgroundColor,
                )
            progressCenterBackgroundColor =
                mTypedArray.getColor(
                    R.styleable.DraggableGraduatedProgressView_dgp_progressCenterBackgroundColor,
                    progressCenterBackgroundColor,
                )
            progressEndBackgroundColor =
                mTypedArray.getColor(
                    R.styleable.DraggableGraduatedProgressView_dgp_progressEndBackgroundColor,
                    progressEndBackgroundColor,
                )
            progressColor =
                mTypedArray.getColor(R.styleable.DraggableGraduatedProgressView_dgp_progressColor, progressColor)

            dotSliderRadius =
                mTypedArray.getLayoutDimension(
                    R.styleable.DraggableGraduatedProgressView_dgp_dotSliderRadius,
                    dip2px(context, 10f),
                )
            dotSliderShadowRadius =
                mTypedArray.getLayoutDimension(
                    R.styleable.DraggableGraduatedProgressView_dgp_dotSliderShadowRadius,
                    dip2px(context, 2f),
                )
            dotSliderColor =
                mTypedArray.getColor(R.styleable.DraggableGraduatedProgressView_dgp_dotSliderColor, dotSliderColor)
            dotSliderShadowColor =
                mTypedArray.getColor(
                    R.styleable.DraggableGraduatedProgressView_dgp_dotSliderShadowColor,
                    dotSliderShadowColor,
                )
            progressColorN =
                mTypedArray.getColor(R.styleable.DraggableGraduatedProgressView_dgp_progressColorN, progressColor)
            dotSliderColorN =
                mTypedArray.getColor(R.styleable.DraggableGraduatedProgressView_dgp_dotSliderColorN, dotSliderColor)
            dotSliderShadowColorN =
                mTypedArray.getColor(
                    R.styleable.DraggableGraduatedProgressView_dgp_dotSliderShadowColorN,
                    dotSliderShadowColor,
                )
            showDotSlider =
                mTypedArray.getBoolean(R.styleable.DraggableGraduatedProgressView_dgp_showDotSlider, showDotSlider)
            mTypedArray.recycle()
        } else {
            isDrag = true
            dotSliderRadius = dip2px(context, 10f)
            progressHeight = dip2px(context, 10f)
            progressColorN = progressColor
            dotSliderColorN = dotSliderColor
            dotSliderShadowColorN = dotSliderShadowColor
        }

        isClickable = true
        // 要支持阴影下过必须关闭硬件加速
        setLayerType(LAYER_TYPE_SOFTWARE, null) // 发光效果不支持硬件加速

        bgPaint.apply {
            color = progressBackgroundColor
            strokeWidth = progressHeight.toFloat()
        }
        progressPaint.apply {
            color = progressColor
            strokeWidth = progressHeight.toFloat()
        }
        circlePaint.apply {
            color = dotSliderColor
            // 圆点指示器阴影颜色
            setShadowLayer(dotSliderShadowRadius.toFloat(), 0f, 0f, dotSliderShadowColor)
        }
        markCirclePaint.apply {
            color = dotSliderColor
        }
    }

    override fun onMeasure(
        widthMeasureSpec: Int,
        heightMeasureSpec: Int,
    ) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val width = MeasureSpec.getSize(widthMeasureSpec)
        // View最小高度为圆点指示器（包括其阴影）的直径。
        val minHeight = dotSliderRadius * 2 + dotSliderShadowRadius * 2
        val height = resolveSize(minHeight, heightMeasureSpec)
        setMeasuredDimension(width, height)
    }

    override fun onLayout(
        changed: Boolean,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int,
    ) {
        super.onLayout(changed, left, top, right, bottom)
        // 初始化几个距离参数
        width = getWidth() // view的宽度
        height = getHeight() // view的高度

        // 让左边距至少为半个圆点指示器的距离
        paddingLeft = getPaddingLeft() + dotSliderShadowRadius // 距离左边的距离
        // 让右边距至少为半个圆点指示器的距离
        paddingRight = getPaddingRight() + dotSliderShadowRadius // 距离右边的距离

        // 最大进度长度等于View的宽度-(左边的内边距+右边的内边距)
        maxProgressWidth = (width - paddingLeft - paddingRight).toFloat()
        // 原点指示器最大移动距离
        maxDotSliderDistance =
            (width - paddingLeft - paddingRight - dotSliderRadius * 2).toFloat() // - dip2px(mContext, 2) * 2;

        // 计算出每个刻度块的宽度
        mMarkBlockWidth = maxProgressWidth / (if (mMarkCount > 0) mMarkCount else maxProgress)

        // 如果当前进度小于左边距
        calculateProgress()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (isDrag) {
            progressPaint.color = progressColor
            circlePaint.color = dotSliderColor
        } else {
            progressPaint.color = progressColorN
            circlePaint.color = dotSliderColorN
        }

        // 绘制进度条背景
        // 从（左边距，View高度的一半）开始，到（View宽度-右边距，View高度的一半）还将绘制灰色背景线段

        // 设置进度条渐变的背景色（如果有设置的话）
        // 保证只有宽度发生变化才执行，避免多次创建对象
        if (progressStartBackgroundColor != 0 && width != oldWidth) {
            oldWidth = width
            bgPaint.setShader(
                LinearGradient(
                    0f,
                    0f,
                    (width - paddingRight).toFloat(),
                    0f,
                    intArrayOf(
                        progressStartBackgroundColor,
                        progressCenterBackgroundColor,
                        progressEndBackgroundColor,
                    ),
                    floatArrayOf(0f, 0.5f, 1.0f),
                    Shader.TileMode.CLAMP,
                ),
            )
        }

        // 线帽的宽度
        val lineCapSize = progressHeight / 2f
        val viewCenterY = height / 2f

        // 绘制线条起终点是没包含线帽，所以需要手动算上线帽的宽度
        canvas.drawLine(
            paddingLeft + lineCapSize,
            viewCenterY,
            width - paddingRight - lineCapSize,
            viewCenterY,
            bgPaint,
        )

        // 绘制进度条进度
        // 从（左边距+线帽）开始，到（现在的触摸到的进度宽度-线帽）还将绘制灰色背景线段
        if ((currentProgress - lineCapSize) >= paddingLeft + lineCapSize) {
            canvas.drawLine(
                paddingLeft + lineCapSize,
                viewCenterY,
                currentProgress - lineCapSize,
                viewCenterY,
                progressPaint,
            )
        } else { // 最小显示的进度，一个小圆点
            canvas.drawLine(
                paddingLeft + lineCapSize,
                viewCenterY,
                paddingLeft + lineCapSize,
                viewCenterY,
                progressPaint,
            )
        }

        // 测试：
        // 绘制出刻度线，看指示器移动的刻度块是否准确
        // int progressTmp = 0;
        // while (progressTmp < maxProgress) {
        //     canvas.drawLine(paddingLeft + progressTmp * mMarkBlockWidth, 0, paddingLeft + progressTmp * mMarkBlockWidth, progressHeight, tmpPaint);
        //     progressTmp++;
        // }
        if (showDotSlider) {
            // 绘制圆点指示器
            canvas.drawCircle(
                currentDotSliderProgress,
                viewCenterY,
                viewCenterY - dotSliderShadowRadius,
                circlePaint,
            )
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        when (event!!.action) {
            MotionEvent.ACTION_DOWN -> {
                parent.requestDisallowInterceptTouchEvent(true)
                setMotionProgress(event)
            }

            MotionEvent.ACTION_MOVE -> {
                setMotionProgress(event)
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                setMotionProgress(event)
                parent.requestDisallowInterceptTouchEvent(false)
            }
        }
        return super.onTouchEvent(event)
    }

    /**
     * 根据用户手势计算进度值
     *
     * @param event 用户手势操作事件
     */
    private fun setMotionProgress(event: MotionEvent) {
        if (!isDrag || !isCanTouch) {
            return
        }
        // 获取当前触摸点，赋值给当前进度
        currentProgress = calculateTouchX(event.x)
        // 对范围进行限制（当手指移动范围超出该view，需要限制便捷）
        calculateProgress()
        // 实际百分比进度数值
        val progress = ((currentProgress - paddingLeft) * maxProgress) / maxProgressWidth
        //        LogUtils.e("paddingLeft: " + paddingLeft + ", currentProgress: " + currentProgress + ", maxProgressWidth: " + maxProgressWidth + ", progress: " + result);
        onProgressListener?.onProgressChanged(
            progress,
            true,
            event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL,
        )
        invalidate()
    }

    /**
     * 根据触摸点计算出应该落在的哪个刻度块，返回该刻度块的x坐标
     *
     * @param touchX
     * @return
     */
    private fun calculateTouchX(touchX: Float): Float = mMarkBlockWidth * Math.round((touchX - paddingLeft) / mMarkBlockWidth) + paddingLeft

    private fun calculateProgress() {
        // 限制左右边界，如果当前进度超出边界，将当前进度赋值为边界极值
        currentProgress =
            min(max(currentProgress, paddingLeft.toFloat()), (width - paddingRight).toFloat())

        currentDotSliderProgress =
            (currentProgress - paddingLeft) * maxDotSliderDistance / maxProgressWidth + paddingLeft + dotSliderRadius
    }

    /**
     * 需要在layout()之前调用
     *
     * @param maxProgress
     */
    fun setMaxProgress(maxProgress: Float) {
        if (maxProgress <= 0) return // 只允许设置大于0的范围
        this.maxProgress = maxProgress
    }

    /**
     * 需要在layout()之前调用
     *
     * @param count
     */
    fun setMarkCount(count: Int) {
        this.mMarkCount = count.toFloat()
        mMarkBlockWidth = maxProgressWidth / (if (mMarkCount > 0) mMarkCount else maxProgress)
    }

    fun setProgress(
        progress: Float,
        notify: Boolean = true,
        isFromUser: Boolean = false,
    ) {
        if (progress > maxProgress || progress < 0) {
            return
        }
        setProgressRunnable =
            Runnable {
                setProgressRunnable = null
                calculateProgress()
                // 设置当前进度的宽度
                currentProgress = ((progress * maxProgressWidth) / maxProgress) + paddingLeft
                currentDotSliderProgress =
                    ((progress * maxDotSliderDistance) / maxProgress) + paddingLeft + dotSliderRadius

                if (onProgressListener != null && notify) {
                    onProgressListener!!.onProgressChanged(progress, isFromUser, true)
                }
                invalidate()
            }
        post(setProgressRunnable)
    }

    fun getProgress(): Float = ((currentProgress - paddingLeft) * maxProgress) / maxProgressWidth

    fun setProgressColorAndBGColor(
        color: Int,
        bgColor: Int,
    ) {
        this.progressColor = color
        this.progressStartBackgroundColor = bgColor
        this.progressCenterBackgroundColor = bgColor
        this.progressEndBackgroundColor = bgColor
        oldWidth = 0
        invalidate()
    }

    fun setProgressColorAndBGColor(
        color: Int,
        start: Int,
        center: Int,
        end: Int,
    ) {
        this.progressColor = color
        this.progressStartBackgroundColor = start
        this.progressCenterBackgroundColor = center
        this.progressEndBackgroundColor = end
        oldWidth = 0
        invalidate()
    }

    // 设置拖动进度监听
    fun setOnProgressListener(listener: OnProgressListener?) {
        this.onProgressListener = listener
    }

    /**
     * 根据手机的分辨率从 dp 的单位 转成为 px(像素)
     */
    private fun dip2px(
        context: Context,
        dpValue: Float,
    ): Int {
        val scale = context.resources.displayMetrics.density
        return (dpValue * scale + 0.5f).toInt()
    }

    interface OnProgressListener {
        /**
         * @param progress   当前进度
         * @param isFromUser 是否由用户触发
         * @param isTouchEnd 手指是否离开该View
         */
        fun onProgressChanged(
            progress: Float,
            isFromUser: Boolean,
            isTouchEnd: Boolean,
        )
    }
}
