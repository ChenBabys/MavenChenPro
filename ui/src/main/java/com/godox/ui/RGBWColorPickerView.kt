package com.godox.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import kotlin.math.max
import kotlin.math.min

/**
 * RGBW的颜色选择视图
 *
 */
class RGBWColorPickerView : View {
    private val GRAD_COLORS =
        intArrayOf(
            Color.RED,
            Color.YELLOW,
            Color.GREEN,
            Color.CYAN,
            Color.BLUE,
            Color.MAGENTA,
            Color.RED,
        )
    private val GRAD_ALPHA = intArrayOf(Color.WHITE, Color.TRANSPARENT)

    private val mHSV = floatArrayOf(180f, 0.5f, 1f)

    /**
     * 选取颜色的指针样式
     */
    private var mPointerDrawable: Drawable? = null

    /**
     * 选取颜色的指针的大小半径
     */
    private var mPointerRadius = 0

    /**
     * 颜色矩形的画笔
     */
    private var mPaint: Paint

    private var mPointerBgPaint: Paint

    /**
     * 颜色矩形的画笔的Shader
     */
    private var mShader: Shader? = null

    /**
     * 颜色矩形的圆角半径
     */
    private val mRadius = 5f

    /**
     *  左上右下是相对当前view的位置
     */
    private val mGradientRect = RectF()

    /**
     * 当前触摸坐标
     */
    private var mLastX = 0
    private var mLastY = 0

    /**
     * 当前选中的颜色值
     */
    private var mSelectedColor = 0

    private var mOnColorChangedListener: OnColorChangedListener? = null

    interface OnColorChangedListener {
        fun onColorChanged(
            gradientView: RGBWColorPickerView,
            @ColorInt color: Int,
            r: Int,
            g: Int,
            b: Int,
            w: Float,
            isTouchEnd: Boolean,
        )
    }

    fun setOnColorChangedListener(onColorChangedListener: OnColorChangedListener) {
        mOnColorChangedListener = onColorChangedListener
    }

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr,
    )

    init {
        isClickable = true
        mPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mPointerBgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        setLayerType(LAYER_TYPE_SOFTWARE, if (isInEditMode) null else mPaint)
    }

    override fun onMeasure(
        widthMeasureSpec: Int,
        heightMeasureSpec: Int,
    ) {
//        LogUtils.e(
//            "width:${
//                getDefaultSize(
//                    suggestedMinimumWidth,
//                    widthMeasureSpec
//                )
//            }, height:${getDefaultSize(suggestedMinimumHeight, heightMeasureSpec)}"
//        )
//        setMeasuredDimension(width, height)
        setMeasuredDimension(
            getDefaultSize(suggestedMinimumWidth, widthMeasureSpec),
            getDefaultSize(suggestedMinimumHeight, heightMeasureSpec),
        )
    }

    override fun onLayout(
        changed: Boolean,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int,
    ) {
        super.onLayout(changed, left, top, right, bottom)
//        LogUtils.e("changed $changed, left $left, top $top, right $right, bottom $bottom")
        mGradientRect.set(
            paddingLeft.toFloat(),
            paddingTop.toFloat(),
            (right - left - paddingRight).toFloat(),
            (bottom - top - paddingBottom).toFloat(),
        )
        mGradientRect.inset(mPointerRadius.toFloat(), mPointerRadius.toFloat())
//        LogUtils.e(mGradientRect)
        if (changed) {
            buildShader()
        }

        mPointerDrawable?.apply {
            // 指的是drawable将在被绘制在canvas的哪个矩形区域内
            setBounds(0, 0, mPointerRadius * 2, mPointerRadius * 2)
            updatePointerPosition()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        mShader?.apply {
            canvas.drawRoundRect(mGradientRect, mRadius, mRadius, mPaint)
        }

//       用于测试，显示当前的mSelectedColor的颜色
//        canvas.drawCircle(
//            width / 2f,
//            height / 2f,
//            mPointerRadius / 4f,
//            mPointerBgPaint
//        )

        // 如果RGB颜色值不在I为1的HSI颜色空间中则不显示滑块
        val hsi = floatArrayOf(0f, 0f, 1f)
        Color.colorToHSV(mSelectedColor, hsi)
        if (hsi[2] != 1f) {
            return
        }

        var tx = 0f
        var ty = 0f

        // 将drawable中心点移动到手指位置
        tx = (mLastX - mPointerRadius).toFloat()
        ty = (mLastY - mPointerRadius).toFloat()

        // 控制不超过左右上下边界
        tx =
            max(
                mGradientRect.left - mPointerRadius,
                min(tx, mGradientRect.right - mPointerRadius),
            )
        ty =
            max(
                mGradientRect.top - mPointerRadius,
                min(ty, mGradientRect.bottom - mPointerRadius),
            )
        mPointerDrawable?.apply {
            canvas.translate(tx, ty)
            draw(canvas)
            canvas.translate(-tx, -ty)
        }

        canvas.drawCircle(
            tx + mPointerRadius.toFloat(),
            ty + mPointerRadius.toFloat(),
            mPointerRadius / 2f,
            mPointerBgPaint,
        )
    }

    private fun buildShader() {
        // 横向的纯色线性渐变,可以认为是hsv中h（色相）的渐变
        val gradientShader =
            LinearGradient(
                mGradientRect.left,
                mGradientRect.top,
                mGradientRect.right,
                mGradientRect.top,
                GRAD_COLORS,
                null,
                Shader.TileMode.CLAMP,
            )
        // 竖向的白色透明度渐变，可以认为是hsv中s（饱和度）的渐变
        val alphaShader =
            LinearGradient(
                0f,
                mGradientRect.top,
                0f,
                mGradientRect.bottom,
                GRAD_ALPHA,
                null,
                Shader.TileMode.CLAMP,
            )
        // 将h和s结合，可以认为是rgbw
        mShader = ComposeShader(alphaShader, gradientShader, PorterDuff.Mode.OVERLAY)
        mPaint.shader = mShader
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        mLastX = event.x.toInt()
        mLastY = event.y.toInt()
        onUpdateColorSelection(mLastX, mLastY, event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL)
        invalidate()
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                parent.requestDisallowInterceptTouchEvent(true)
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                parent.requestDisallowInterceptTouchEvent(false)
            }
        }
        return super.onTouchEvent(event)
    }

    /**
     * 将手指触摸的坐标位置转换为hsv
     *
     * @param x
     * @param y
     */
    private fun onUpdateColorSelection(
        x: Int,
        y: Int,
        isTouchEnd: Boolean,
    ) {
        val xt = max(mGradientRect.left, min(x.toFloat(), mGradientRect.right)).toInt()
        val yt = max(mGradientRect.top, min(y.toFloat(), mGradientRect.bottom)).toInt()

        val hue: Float = pointToHue(xt.toFloat())
        val sat = pointToSaturation(yt.toFloat())
        mHSV[0] = hue
        mHSV[1] = sat
        mHSV[2] = 1f // 明亮度固定为1
        mSelectedColor = Color.HSVToColor(mHSV)
        mPointerBgPaint.color =
            Color.rgb(
                Color.red(mSelectedColor),
                Color.green(mSelectedColor),
                Color.blue(mSelectedColor),
            )
        dispatchColorChanged(mSelectedColor, isTouchEnd)
    }

    /**
     * 将hsv转换为指针坐标
     */
    private fun updatePointerPosition() {
        if (mGradientRect.width() != 0f && mGradientRect.height() != 0f) {
            mLastX = hueToPoint(mHSV[0])
            mLastY = saturationToPoint(mHSV[1])
            mSelectedColor = Color.HSVToColor(mHSV)
            mPointerBgPaint.color =
                Color.rgb(
                    Color.red(mSelectedColor),
                    Color.green(mSelectedColor),
                    Color.blue(mSelectedColor),
                )
//            LogUtils.e("mLastX: $mLastX, mLastY: $mLastY")
        }
    }

    /**
     * 通知调用方选中颜色改变
     *
     * @param color
     */
    private fun dispatchColorChanged(
        color: Int,
        isTouchEnd: Boolean,
    ) {
        mOnColorChangedListener?.onColorChanged(
            this,
            color,
            Color.red(color),
            Color.green(color),
            Color.blue(color),
            mHSV[2],
            isTouchEnd,
        )
    }

    /**
     * 设置指针drawable
     *
     * @param pointerDrawable
     */
    fun setPointerDrawable(
        @DrawableRes pointerDrawable: Int,
        radius: Int,
    ) {
        setPointerDrawable(ContextCompat.getDrawable(context, pointerDrawable), radius)
    }

    fun setPointerDrawable(
        pointerDrawable: Drawable?,
        radius: Int,
    ) {
        if (pointerDrawable == null) {
            return
        }
        if (mPointerDrawable !== pointerDrawable) {
            mPointerDrawable = pointerDrawable
            mPointerRadius =
                if (radius <= 0) {
                    mPointerDrawable!!.intrinsicWidth / 2
                } else {
                    radius
                }
            requestLayout()
        }
    }

    /**
     * 设置当前选中的颜色
     *
     * @param selectedColor
     * @param updatePointers
     */
    fun setColor(
        @ColorInt selectedColor: Int,
        updatePointers: Boolean,
        dispatchEvent: Boolean = true,
        isTouchEnd: Boolean,
    ) {
        mSelectedColor = selectedColor
        Color.colorToHSV(selectedColor, mHSV)
        mPointerBgPaint.color =
            Color.rgb(
                Color.red(mSelectedColor),
                Color.green(mSelectedColor),
                Color.blue(mSelectedColor),
            )
        if (updatePointers) {
            updatePointerPosition()
        }
        invalidate()
        if (dispatchEvent) {
            dispatchColorChanged(mSelectedColor, isTouchEnd)
        }
    }

    /**
     * 指针x坐标转色相值
     *
     * @param x
     * @return
     */
    private fun pointToHue(x: Float): Float = (x - mGradientRect.left) * 360f / mGradientRect.width()

    /**
     * 色相值转指针x坐标
     *
     * @param hue
     * @return
     */
    private fun hueToPoint(hue: Float): Int = (mGradientRect.left + hue * mGradientRect.width() / 360).toInt()

    /**
     * 指针y坐标转饱和度
     *
     * @param y
     * @return
     */
    private fun pointToSaturation(y: Float): Float = 1f / mGradientRect.height() * (y - mGradientRect.top)

    private fun saturationToPoint(sat: Float): Int = (mGradientRect.top + mGradientRect.height() * sat).toInt()
}
