package com.godox.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ComposeShader
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.SweepGradient
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.Size
import androidx.core.content.ContextCompat
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * HSI 色盘
 *
 */
class ColorPieView : View {
    private val gradColorArr =
        intArrayOf(
            Color.RED,
            Color.MAGENTA,
            Color.BLUE,
            Color.CYAN,
            Color.GREEN,
            Color.YELLOW,
            Color.RED,
        )
    private val gradAlphaArr = intArrayOf(Color.WHITE, Color.TRANSPARENT)
    private val mHSV = floatArrayOf(180f, 1f, 1f)

    private var mPaint: Paint
    private var mShader: Shader? = null

    // 选取颜色的滑块
    private var mPointerDrawable: Drawable? = null
    private var mPointerRadius = 0f
    private var mLastX = 0
    private var mLastY = 0
    private var mPointerTx = 0f
    private var mPointerTy = 0f

    // 五彩圆的半径
    private var mColorfulCircleRadius = 0
    private var mPointerBgCirclePaint: Paint

    /**
     * 当前选中的颜色值
     */
    var mSelectedColor = 0
        get() = field
        private set(value) {
            field = value
        }

    private var mOnColorChangedListener: OnColorChangedListener? = null

    interface OnColorChangedListener {
        fun onColorChanged(
            gradientView: ColorPieView,
            hsv: FloatArray,
            @ColorInt color: Int,
            r: Int,
            g: Int,
            b: Int,
            w: Float,
            isFromUser: Boolean,
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

    constructor(
        context: Context?,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int,
    ) : super(context, attrs, defStyleAttr, defStyleRes)

    init {
        isClickable = true
        mPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mPointerBgCirclePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        setLayerType(LAYER_TYPE_SOFTWARE, if (isInEditMode) null else mPaint)
    }

    override fun onMeasure(
        widthMeasureSpec: Int,
        heightMeasureSpec: Int,
    ) {
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
        // width: 573, height: 573
//        logg("width: $width, height: $height")

        mColorfulCircleRadius = (width - paddingLeft - paddingRight) / 2

        if (changed) {
            buildShader()
        }

        mPointerDrawable?.apply {
            // 指的是drawable将在被绘制在canvas的哪个矩形区域内
            setBounds(0, 0, (`mPointerRadius` * 2).toInt(), (mPointerRadius * 2).toInt())
            hsvToXY()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawCircle(
            mColorfulCircleRadius.toFloat() + paddingLeft,
            mColorfulCircleRadius.toFloat() + paddingTop,
            mColorfulCircleRadius.toFloat(),
            mPaint,
        )

        mPointerDrawable?.apply {
            // left: 253, top: 351, right:826, bottom:924
//            logg("left: $left, top: $top, right:$right, bottom:$bottom")

//            // 控制不超过左右上下边界
//            tx = max(paddingLeft - mPointerRadius, min(tx, width - paddingRight - mPointerRadius))
//            ty = max(paddingTop - mPointerRadius, min(ty, height - paddingBottom - mPointerRadius))
//            Log.e("","draw..mPointerTx:${mPointerTx},mPointerTy:${mPointerTy}")
            canvas.translate(mPointerTx.toFloat(), mPointerTy.toFloat())
            draw(canvas)
            canvas.translate(-mPointerTx.toFloat(), -mPointerTy.toFloat())
        }

        canvas.drawCircle(
            mPointerTx + mPointerRadius.toFloat(),
            mPointerTy + mPointerRadius.toFloat(),
            mPointerRadius * 0.5f,
            mPointerBgCirclePaint,
        )
    }

    private fun buildShader() {
        // 横向的纯色线性渐变,可以认为是hsv中h（色相）的渐变
        val gradientShader =
            SweepGradient(
                mColorfulCircleRadius.toFloat() + paddingLeft,
                mColorfulCircleRadius.toFloat() + paddingTop,
                gradColorArr,
                null,
            )
        val alphaShader =
            RadialGradient(
                mColorfulCircleRadius.toFloat() + paddingLeft,
                mColorfulCircleRadius.toFloat() + paddingTop,
                mColorfulCircleRadius.toFloat(),
                gradAlphaArr,
                null,
                Shader.TileMode.CLAMP,
            )
        mShader = ComposeShader(alphaShader, gradientShader, PorterDuff.Mode.OVERLAY)
        mPaint.shader = mShader
    }

    private var isCanTouch = true

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (isCanTouch) {
            mLastX = event.x.toInt()
            mLastY = event.y.toInt()
            xyToHsv(event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL)
            invalidate()
        }
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                parent.requestDisallowInterceptTouchEvent(true)
                if (isCanTouch) {
                    touchListener.invoke(true)
                }
            }

            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL,
            -> {
                parent.requestDisallowInterceptTouchEvent(false)
                touchListener.invoke(false)
            }
        }
        return super.onTouchEvent(event)
    }

    fun setCanTouch(isOk: Boolean) {
        isCanTouch = isOk
    }

    private var touchListener: (isTouching: Boolean) -> Unit = {}

    fun addTouchListener(block: (isTouching: Boolean) -> Unit) {
        touchListener = block
    }

    private fun xyToHsv(isTouchEnd: Boolean) {
        // 将drawable中心点移动到手指位置
        mPointerTx = mLastX - mPointerRadius
        mPointerTy = mLastY - mPointerRadius

        val dx = abs(mColorfulCircleRadius + paddingLeft - mLastX)
        val dy = abs(mColorfulCircleRadius + paddingTop - mLastY)
        // 手指触摸位置与圆心的直线距离（直角三角形的斜边），这个距离大于圆半径就得限制在圆圈上
        val radius = sqrt((dx * dx + dy * dy).toDouble())
        if (radius >= mColorfulCircleRadius) {
            logg("在圆外，把它规在圆圈上")
//                val x1 = Math.toDegrees(atan(abs(dy / dx).toDouble()))
            // 斜线与x轴的夹角
            val radian = atan2(dy.toDouble(), dx.toDouble())

            logg("斜线与x轴的夹角: ${Math.toDegrees(radian)}, 弧度：$radian")

            if (mLastX > mColorfulCircleRadius + paddingLeft && mLastY < mColorfulCircleRadius + paddingTop) {
                // 第一象限
                logg("第一象限")
                val sin = sin(radian) // 参数是弧度，不是角度
                val cos = cos(radian)
                logg("圆半径：$mColorfulCircleRadius - x坐标：${cos * mColorfulCircleRadius} - y坐标：${sin * mColorfulCircleRadius}")
                val nDownX =
                    (mColorfulCircleRadius + paddingLeft + cos * mColorfulCircleRadius).toInt()
                val nDownY =
                    (mColorfulCircleRadius + paddingTop - sin * mColorfulCircleRadius).toInt()
                mPointerTy = nDownY - mPointerRadius
                mPointerTx = nDownX - mPointerRadius
                mHSV[0] = degreesToHue(Math.toDegrees(radian).toFloat())
            } else if (mLastX < mColorfulCircleRadius + paddingLeft && mLastY < mColorfulCircleRadius + paddingTop) {
                // 第二象限
                logg("第二象限")
                val sin = sin(radian)
                val cos = cos(radian)
                logg("圆半径：$mColorfulCircleRadius - x坐标：${cos * mColorfulCircleRadius} - y坐标：${sin * mColorfulCircleRadius}")
                val nDownX =
                    (mColorfulCircleRadius + paddingLeft - cos * mColorfulCircleRadius).toInt()
                val nDownY =
                    (mColorfulCircleRadius + paddingTop - sin * mColorfulCircleRadius).toInt()
                mPointerTy = nDownY - mPointerRadius
                mPointerTx = nDownX - mPointerRadius
                mHSV[0] = degreesToHue(180 - Math.toDegrees(radian).toFloat())
            } else if (mLastX < mColorfulCircleRadius + paddingLeft && mLastY > mColorfulCircleRadius + paddingTop) {
                // 第三象限
                logg("第三象限")
                val sin = sin(radian)
                val cos = cos(radian)
                logg("圆半径：$mColorfulCircleRadius - x坐标：${cos * mColorfulCircleRadius} - y坐标：${sin * mColorfulCircleRadius}")
                val nDownX =
                    (mColorfulCircleRadius + paddingLeft - cos * mColorfulCircleRadius).toInt()
                val nDownY =
                    (mColorfulCircleRadius + paddingTop + sin * mColorfulCircleRadius).toInt()
                mPointerTy = nDownY - mPointerRadius
                mPointerTx = nDownX - mPointerRadius
                mHSV[0] = degreesToHue(180 + Math.toDegrees(radian).toFloat())
            } else if (mLastX > mColorfulCircleRadius + paddingLeft && mLastY > mColorfulCircleRadius + paddingTop) {
                // 第四象限
                logg("第四象限")
                val sin = sin(radian)
                val cos = cos(radian)
                logg("圆半径：$mColorfulCircleRadius - x坐标：${cos * mColorfulCircleRadius} - y坐标：${sin * mColorfulCircleRadius}")
                val nDownX =
                    (mColorfulCircleRadius + paddingLeft + cos * mColorfulCircleRadius).toInt()
                val nDownY =
                    (mColorfulCircleRadius + paddingTop + sin * mColorfulCircleRadius).toInt()
                mPointerTy = nDownY - mPointerRadius
                mPointerTx = nDownX - mPointerRadius
                logg("nDownY:$nDownY, mPointerTy:$mPointerTy, mPointerRadius:$mPointerRadius")
                mHSV[0] = degreesToHue(360 - Math.toDegrees(radian).toFloat())
//                LogUtils.e("h: ${mHSV[0]}, radian：${Math.toDegrees(radian).toFloat()}")
            } else if (mLastX == mColorfulCircleRadius + paddingLeft) {
                // y轴上
                logg("y轴上")
                mPointerTy =
                    if (mLastY < mColorfulCircleRadius + paddingTop) {
                        mHSV[0] = 90f
                        -mPointerRadius + paddingTop
                    } else {
                        mHSV[0] = 270f
                        mColorfulCircleRadius * 2 + paddingTop - mPointerRadius
                    }
                mPointerTx = mColorfulCircleRadius + paddingLeft - mPointerRadius
            } else if (mLastY == mColorfulCircleRadius + paddingTop) {
                // x轴上
                mPointerTx =
                    if (mLastX < mColorfulCircleRadius + paddingLeft) {
                        mHSV[0] = 180f
                        -mPointerRadius + paddingLeft
                    } else {
                        mHSV[0] = 360f
                        mColorfulCircleRadius * 2 + paddingLeft - mPointerRadius
                    }
                mPointerTy = mColorfulCircleRadius + paddingTop - mPointerRadius
//                LogUtils.e("x轴上, h:${mHSV[0]}")
            } else {
                // 未知
                logg("未知")
            }
            mHSV[1] = 1f
        } else {
            logg("在圆内，放行")
            // 斜线与x轴的夹角
            val radian = atan2(dy.toDouble(), dx.toDouble())

            logg("斜线与x轴的夹角: ${Math.toDegrees(radian)}, 弧度：$radian")
            if (mLastX > mColorfulCircleRadius + paddingLeft && mLastY < mColorfulCircleRadius + paddingTop) {
                // 第一象限
                logg("第一象限")
                mHSV[0] = degreesToHue(Math.toDegrees(radian).toFloat())
            } else if (mLastX < mColorfulCircleRadius + paddingLeft && mLastY < mColorfulCircleRadius + paddingTop) {
                // 第二象限
                logg("第二象限")
                mHSV[0] = degreesToHue(180 - Math.toDegrees(radian).toFloat())
            } else if (mLastX < mColorfulCircleRadius + paddingLeft && mLastY > mColorfulCircleRadius + paddingTop) {
                // 第三象限
                logg("第三象限")
                mHSV[0] = degreesToHue(180 + Math.toDegrees(radian).toFloat())
            } else if (mLastX > mColorfulCircleRadius + paddingLeft && mLastY > mColorfulCircleRadius + paddingTop) {
                // 第四象限
                logg("第四象限")
                mHSV[0] = degreesToHue(360 - Math.toDegrees(radian).toFloat())
            } else if (mLastX == mColorfulCircleRadius + paddingLeft) {
                // y轴上
                logg("y轴上")
                if (mLastY in 0 until mColorfulCircleRadius + paddingTop) {
                    mHSV[0] = 90f
                } else {
                    mHSV[0] = 270f
                }
            } else if (mLastY == mColorfulCircleRadius + paddingTop) {
                // x轴上
                logg("x轴上")
                if (mLastX in 0 until mColorfulCircleRadius + paddingLeft) {
                    mHSV[0] = 180f
                } else {
                    mHSV[0] = 360f
                }
            } else {
                // 未知
                logg("未知")
            }
            mHSV[1] = radiusToSaturation(radius)
//            logg("mHSV[1]: ${mHSV[1]}, radius:$radius, mColorfulCircleRadius:$mColorfulCircleRadius")
        }
        mHSV[0] = mHSV[0].toInt().toFloat()
        mHSV[1] = (mHSV[1] * 100f).toInt() / 100f
        mSelectedColor = Color.HSVToColor(mHSV)
        mPointerBgCirclePaint.color = mSelectedColor
        dispatchColorChanged(mHSV, mSelectedColor, true, isTouchEnd)
    }

    private fun hsvToXY() {
        val degrees = hueToDegrees(mHSV[0])
        val radius = saturationToRadius(mHSV[1])
        mSelectedColor = Color.HSVToColor(mHSV)
        mPointerBgCirclePaint.color = mSelectedColor
        val radian = Math.toRadians(degrees.toDouble())

        when {
            degrees == 0f -> {
                mLastX = mColorfulCircleRadius + paddingLeft + radius
                mLastY = mColorfulCircleRadius + paddingTop
            }

            degrees > 0f && degrees < 90f -> {
                // 第一象限
                val sin = sin(radian) * radius
                val cos = cos(radian) * radius
                mLastX = (mColorfulCircleRadius + paddingLeft + cos).toInt()
                mLastY = (mColorfulCircleRadius + paddingTop - sin).toInt()
            }

            degrees == 90f -> {
                // y轴正方向
                mLastX = mColorfulCircleRadius + paddingLeft
                mLastY = mColorfulCircleRadius + paddingTop - radius
            }

            degrees > 90f && degrees < 180 -> {
                // 第二象限
                val sin = sin(Math.toRadians((180 - degrees).toDouble())) * radius
                val cos = cos(Math.toRadians((180 - degrees).toDouble())) * radius
                mLastX = (mColorfulCircleRadius + paddingLeft - cos).toInt()
                mLastY = (mColorfulCircleRadius + paddingTop - sin).toInt()
            }

            degrees == 180f -> {
                // x轴负方向
                mLastX = mColorfulCircleRadius + paddingLeft - radius
                mLastY = mColorfulCircleRadius + paddingTop
            }

            degrees > 180f && degrees < 270f -> {
                // 第三象限
                val sin = sin(Math.toRadians((degrees - 180).toDouble())) * radius
                val cos = cos(Math.toRadians((degrees - 180).toDouble())) * radius
                mLastX = (mColorfulCircleRadius + paddingLeft - cos).toInt()
                mLastY = (mColorfulCircleRadius + paddingTop + sin).toInt()
            }

            degrees == 270f -> {
                // y轴负方向
                mLastX = mColorfulCircleRadius + paddingLeft
                mLastY = mColorfulCircleRadius + paddingTop + radius
            }

            degrees > 270f && degrees < 360f -> {
                // 第四象限
                val sin = sin(Math.toRadians((360 - degrees).toDouble())) * radius
                val cos = cos(Math.toRadians((360 - degrees).toDouble())) * radius
                mLastX = (mColorfulCircleRadius + paddingLeft + cos).toInt()
                mLastY = (mColorfulCircleRadius + paddingTop + sin).toInt()
            }

            degrees == 360f -> {
                // x轴正方向
                mLastX = mColorfulCircleRadius + paddingLeft + radius
                mLastY = mColorfulCircleRadius + paddingTop
            }
        }
        mPointerTy = mLastY - mPointerRadius
        mPointerTx = mLastX - mPointerRadius
//        Log.e("","mPointerTx:${mPointerTx},mPointerTy:${mPointerTy}")
    }

    private fun saturationToRadius(sat: Float): Int = ((width - paddingLeft - paddingRight) / 2 * sat).toInt()

    private fun radiusToSaturation(radius: Double): Float {
        return (radius / ((width - paddingLeft - paddingRight) / 2)).toFloat() // 注意radius / (width / 2) 和 radius / width / 2的区别哦！！！
    }

    private fun hueToDegrees(hue: Float): Float = hue

    private fun degreesToHue(degrees: Float): Float = degrees

    /**
     * 设置指针drawable
     *
     * @param pointerDrawable
     */
    fun setPointerDrawable(
        @DrawableRes pointerDrawable: Int,
        radius: Float,
    ) {
        setPointerDrawable(ContextCompat.getDrawable(context, pointerDrawable), radius)
    }

    fun setPointerDrawable(
        pointerDrawable: Drawable?,
        radius: Float,
    ) {
        if (pointerDrawable == null) {
            return
        }
        if (mPointerDrawable !== pointerDrawable) {
            mPointerDrawable = pointerDrawable
            mPointerRadius =
                if (radius <= 0) {
                    mPointerDrawable!!.intrinsicWidth.toFloat()
                } else {
                    radius
                }
            requestLayout()
        }
    }

    fun setColor(
        @ColorInt selectedColor: Int,
        dispatchEvent: Boolean = true,
    ) {
        mSelectedColor = selectedColor
        Color.colorToHSV(selectedColor, mHSV)
        hsvToXY()
        invalidate()
        if (dispatchEvent) {
            dispatchColorChanged(mHSV, mSelectedColor, false, true)
        }
    }

    fun setColor(
        @Size(3) hsv: FloatArray,
        dispatchEvent: Boolean = true,
        isTouchEnd: Boolean,
    ) {
        hsv.copyInto(mHSV)
        mSelectedColor = Color.HSVToColor(hsv)
        hsvToXY()
        invalidate()
        if (dispatchEvent) {
            dispatchColorChanged(mHSV, mSelectedColor, false, isTouchEnd)
        }
    }

    /**
     * 通知调用方选中颜色改变
     *
     * @param color
     */
    private fun dispatchColorChanged(
        hsv: FloatArray,
        color: Int,
        isFromUser: Boolean,
        isTouchEnd: Boolean,
    ) {
        mOnColorChangedListener?.onColorChanged(
            this,
            hsv,
            color,
            Color.red(color),
            Color.green(color),
            Color.blue(color),
            0f,
            isFromUser,
            isTouchEnd,
        )
    }

    private fun logg(str: String) {
//        if (BuildConfig.DEBUG)
//            logg(str)
    }
}
