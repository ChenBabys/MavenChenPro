package com.godox.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.RectF
import android.graphics.Region
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.content.res.ResourcesCompat

/**
 * XY 色域图
 *
 * @constructor
 *
 * @param context
 * @param attrs
 */
class CIE1931ColorPickerView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : FrameLayout(context, attrs) {
    companion object {
        val ILLUMINANT_E = PointF(0.3127f, 0.329f)
    }

    private var selectedPoint: PointF? = null
    private var selectedColor = 0
    private var mColor = 0
    private var mDrawMode: DrawMode = DrawMode.ALL
    private var imageViewDrawable: Drawable
    private var imageView: ImageView
    private var mColorListener: ColorListener? = null

    private val triangle2020 by lazy {
        TriangleInfo(
            topVertex = PointF(0.1725f, 0.7058f),
            leftVertex = PointF(0.1442f, 0.0566f),
            rightVertex = PointF(0.6879f, 0.3076f),
            leftSide = TriangleSideInfo(k = 22.94f, d = -3.2513f),
            rightSide = TriangleSideInfo(k = -0.7726f, d = 0.8391f),
            bottomSide = TriangleSideInfo(k = 0.4617f, d = -0.01f),
        )
    }
    private val triangle709 by lazy {
        TriangleInfo(
            topVertex = PointF(0.3f, 0.6f),
            leftVertex = PointF(0.15f, 0.06f),
            rightVertex = PointF(0.64f, 0.33f),
            leftSide = TriangleSideInfo(k = 3.6f, d = -0.48f),
            rightSide = TriangleSideInfo(k = -0.7941f, d = 0.8382f),
            bottomSide = TriangleSideInfo(k = 0.551f, d = -0.022f),
        )
    }

    private val triangleP3 by lazy {
        TriangleInfo(
            topVertex = PointF(0.265f, 0.69f),
            leftVertex = PointF(0.15f, 0.06f),
            rightVertex = PointF(0.68f, 0.32f),
            leftSide = TriangleSideInfo(k = 5.4783f, d = -0.7617f),
            rightSide = TriangleSideInfo(k = -0.8916f, d = 0.9263f),
            bottomSide = TriangleSideInfo(k = 0.4906f, d = -0.0136f),
        )
    }

    private val selectorPaint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }
    }
    private val mmPaint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            strokeWidth = 10f
        }
    }
    private var selectorRadiusPx = 128.0f

    private val triangleMaskPaint by lazy {
        Paint(5).apply {
//            strokeCap = Paint.Cap.ROUND
            style = Paint.Style.FILL_AND_STROKE
            setARGB(200, 35, 35, 35)
        }
    }

    interface ColorListener {
        fun onColorSelected(value: Int)

        fun onColorXY(
            x: Float,
            y: Float,
            color: Int,
            isTouchEnd: Boolean,
        )
    }

    fun setColorListener(listener: ColorListener) {
        mColorListener = listener
    }

    // 马蹄图轨迹坐标
    private val originalPointFs =
        mutableListOf(
            PointF(0.1669f, 0.0086f),
            PointF(0.1566f, 0.0177f),
            PointF(0.1440f, 0.0297f),
            PointF(0.1355f, 0.0399f),
            PointF(0.1241f, 0.0578f),
            PointF(0.1096f, 0.0868f),
            PointF(0.0913f, 0.1327f),
            PointF(0.0687f, 0.2007f),
            PointF(0.0454f, 0.2950f),
            PointF(0.0255f, 0.4127f),
            PointF(0.0092f, 0.5384f),
            PointF(0.0059f, 0.6548f),
            PointF(0.0139f, 0.7502f),
            PointF(0.0389f, 0.8120f),
            PointF(0.0743f, 0.8338f),
            PointF(0.1142f, 0.8262f),
            PointF(0.1547f, 0.8059f),
            PointF(0.1929f, 0.7816f),
            PointF(0.2296f, 0.7543f),
            PointF(0.2658f, 0.7243f),
            PointF(0.3016f, 0.6923f),
            PointF(0.3373f, 0.6589f),
            PointF(0.3731f, 0.6245f),
            PointF(0.4087f, 0.5896f),
            PointF(0.4441f, 0.5547f),
            PointF(0.4788f, 0.5202f),
            PointF(0.5125f, 0.4866f),
            PointF(0.5448f, 0.4544f),
            PointF(0.5752f, 0.4242f),
            PointF(0.6029f, 0.3965f),
            PointF(0.6270f, 0.3725f),
            PointF(0.6482f, 0.3514f),
            PointF(0.6658f, 0.3340f),
            PointF(0.6915f, 0.3083f),
            PointF(0.7140f, 0.2859f),
            PointF(0.7311f, 0.2689f),
        )

    private var mPoints = mutableListOf<PointF>()
    val mPointPath = Path()

    init {
        viewTreeObserver.addOnGlobalLayoutListener(
            object : OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    if (measuredWidth > 0) { // 为了让View完成测量，有了宽高.再去计算点的坐标才是正确的。
                        this@CIE1931ColorPickerView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                        this@CIE1931ColorPickerView.onFirstLayout()
                    }
                }
            },
        )
        imageViewDrawable = ResourcesCompat.getDrawable(resources, R.mipmap.xy_coordinate, null)!!
        imageView = ImageView(context)
        imageView.scaleType = ImageView.ScaleType.FIT_START
        imageView.setImageDrawable(imageViewDrawable)
        addView(imageView, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))

        mPoints.addAll(generateDenseCoordinates(originalPointFs, 5))
    }

    private fun onFirstLayout() {
        // 初始化马蹄图轨迹Path
        mPoints.forEachIndexed { index, it ->
            if (index == 0) {
                mPointPath.moveTo(getX(it.x).toFloat(), getY(it.y).toFloat())
            } else {
                mPointPath.lineTo(getX(it.x).toFloat(), getY(it.y).toFloat())
            }
        }
        mPointPath.close()
        // 另外对马蹄图底部直线进行更细粒度的线性插值
        mPoints.addAll(generateDenseCoordinates(mutableListOf(PointF(0.7311f, 0.2689f), PointF(0.1669f, 0.0086f)), 100))
        loadListeners()
    }

    override fun dispatchTouchEvent(paramMotionEvent: MotionEvent?): Boolean {
        parent.requestDisallowInterceptTouchEvent(true)
        return super.dispatchTouchEvent(paramMotionEvent)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun loadListeners() {
        setOnTouchListener { _, event ->
            onTouchReceived(event, true)
            invalidate()
            true
        }
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        drawView(canvas)
        selectedPoint?.apply {
            drawCircle(canvas, this.x, this.y, getColor())
        }
//        drawDots(canvas)
    }

    /**
     * 线性插值
     *
     * @param ops
     * @param density 往中间插入几个点
     * @return
     */
    private fun generateDenseCoordinates(
        ops: MutableList<PointF>,
        density: Int,
    ): MutableList<PointF> {
        val denseCoordinates: MutableList<PointF> = mutableListOf()
        for (i in 0 until ops.size - 1) {
            val startPoint = ops[i]
            val endPoint = ops[i + 1]
            val deltaX = (endPoint.x - startPoint.x) / density
            val deltaY = (endPoint.y - startPoint.y) / density

            denseCoordinates.add(startPoint)
            for (j in 1 until density) {
                val newX = startPoint.x + deltaX * j
                val newY = startPoint.y + deltaY * j
                denseCoordinates.add(PointF(newX, newY))
            }
        }
        denseCoordinates.add(ops[ops.size - 1])
        return denseCoordinates
    }

    private fun drawDots(canvas: Canvas) {
        canvas.drawPath(
            mPointPath,
            Paint().apply {
                color = Color.WHITE
                style = Paint.Style.STROKE
                strokeWidth = 2f
            },
        )
//        val points = arrayListOf<Float>()
//        var count = 0
//        while (count < mPoints.size) {
//            points.add(getX(mPoints[count].x).toFloat())
//            points.add(getY(mPoints[count].y).toFloat())
//            count++
//        }
//        canvas.drawPoints(points.toFloatArray(), 0, points.size, Paint().apply {
//            color = Color.WHITE
//            strokeWidth = 2.5f
//        })
    }

    private fun drawCircle(
        canvas: Canvas,
        x: Float,
        y: Float,
        color: Int,
    ) {
        mmPaint.color = color
        this.selectorRadiusPx = resources.displayMetrics.density * 10.0f
        canvas.drawCircle(x, y, selectorRadiusPx, selectorPaint)
        canvas.drawCircle(x, y, selectorRadiusPx, mmPaint)
    }

    private fun drawView(canvas: Canvas) {
        when (mDrawMode) {
            DrawMode.REC_2020 -> {
                val path = Path()
                path.moveTo(0.0f, measuredHeight.toFloat())
                path.lineTo(
                    getX(triangle2020.leftVertex.x).toFloat(),
                    getY(triangle2020.leftVertex.y).toFloat(),
                )
                path.lineTo(
                    getX(triangle2020.rightVertex.x).toFloat(),
                    getY(triangle2020.rightVertex.y).toFloat(),
                )
                path.lineTo(measuredWidth.toFloat(), measuredHeight.toFloat())
                path.close()
                canvas.drawPath(path, triangleMaskPaint)

                path.reset()
                path.moveTo(0.0f, measuredHeight.toFloat())
                path.lineTo(
                    getX(triangle2020.leftVertex.x).toFloat(),
                    getY(triangle2020.leftVertex.y).toFloat(),
                )
                path.lineTo(
                    getX(triangle2020.topVertex.x).toFloat(),
                    getY(triangle2020.topVertex.y).toFloat(),
                )
                path.lineTo(
                    getX(triangle2020.rightVertex.x).toFloat(),
                    getY(triangle2020.rightVertex.y).toFloat(),
                )
                path.lineTo(measuredWidth.toFloat(), measuredHeight.toFloat())
                path.lineTo(measuredWidth.toFloat(), 0.0f)
                path.lineTo(0.0f, 0.0f)
                path.moveTo(0.0f, measuredHeight.toFloat())
                path.close()
                canvas.drawPath(path, triangleMaskPaint)
            }

            DrawMode.REC_709 -> {
                var path = Path()
                path.moveTo(0.0f, measuredHeight.toFloat())
                path.lineTo(
                    getX(triangle709.leftVertex.x).toFloat(),
                    getY(triangle709.leftVertex.y).toFloat(),
                )
                path.lineTo(
                    getX(triangle709.rightVertex.x).toFloat(),
                    getY(triangle709.rightVertex.y).toFloat(),
                )
                path.lineTo(measuredWidth.toFloat(), measuredHeight.toFloat())
                path.close()
                canvas.drawPath(path, triangleMaskPaint)

                path.reset()
                path.moveTo(0.0f, measuredHeight.toFloat())
                path.lineTo(
                    getX(triangle709.leftVertex.x).toFloat(),
                    getY(triangle709.leftVertex.y).toFloat(),
                )
                path.lineTo(
                    getX(triangle709.topVertex.x).toFloat(),
                    getY(triangle709.topVertex.y).toFloat(),
                )
                path.lineTo(
                    getX(triangle709.rightVertex.x).toFloat(),
                    getY(triangle709.rightVertex.y).toFloat(),
                )
                path.lineTo(measuredWidth.toFloat(), measuredHeight.toFloat())
                path.lineTo(measuredWidth.toFloat(), 0.0f)
                path.lineTo(0.0f, 0.0f)
                path.moveTo(0.0f, measuredHeight.toFloat())
                path.close()
                canvas.drawPath(path, triangleMaskPaint)
            }

            DrawMode.DCI_P3 -> {
                var path = Path()
                path.moveTo(0.0f, measuredHeight.toFloat())
                path.lineTo(
                    getX(triangleP3.leftVertex.x).toFloat(),
                    getY(triangleP3.leftVertex.y).toFloat(),
                )
                path.lineTo(
                    getX(triangleP3.rightVertex.x).toFloat(),
                    getY(triangleP3.rightVertex.y).toFloat(),
                )
                path.lineTo(measuredWidth.toFloat(), measuredHeight.toFloat())
                path.close()
                canvas.drawPath(path, triangleMaskPaint)

                path.reset()
                path.moveTo(0.0f, measuredHeight.toFloat())
                path.lineTo(
                    getX(triangleP3.leftVertex.x).toFloat(),
                    getY(triangleP3.leftVertex.y).toFloat(),
                )
                path.lineTo(
                    getX(triangleP3.topVertex.x).toFloat(),
                    getY(triangleP3.topVertex.y).toFloat(),
                )
                path.lineTo(
                    getX(triangleP3.rightVertex.x).toFloat(),
                    getY(triangleP3.rightVertex.y).toFloat(),
                )
                path.lineTo(measuredWidth.toFloat(), measuredHeight.toFloat())
                path.lineTo(measuredWidth.toFloat(), 0.0f)
                path.lineTo(0.0f, 0.0f)
                path.moveTo(0.0f, measuredHeight.toFloat())
                path.close()
                canvas.drawPath(path, triangleMaskPaint)
            }

            DrawMode.ALL -> {
            }
        }
    }

    // y= 0.4617D * x - 0.01D 三角形2020底边
    // y = 22.94D * x - 3.2513D 三角形2020左边
    // y = -0.7726D * x + 0.8391D 三角形2020右边
    private fun getTriangle2020LeftSideYByX(x: Float): Float = triangle2020.leftSide.k * x + triangle2020.leftSide.d

    private fun getTriangle2020RightSideYByX(x: Float): Float = triangle2020.rightSide.k * x + triangle2020.rightSide.d

    private fun getTriangle2020BottomSideYByX(x: Float): Float = triangle2020.bottomSide.k * x + triangle2020.bottomSide.d

    private fun getTriangle709LeftSideYByX(x: Float): Float = triangle709.leftSide.k * x + triangle709.leftSide.d

    private fun getTriangle709RightSideYByX(x: Float): Float = triangle709.rightSide.k * x + triangle709.rightSide.d

    private fun getTriangle709BottomSideYByX(x: Float): Float = triangle709.bottomSide.k * x + triangle709.bottomSide.d

    private fun getTriangleP3LeftSideYByX(x: Float): Float = triangleP3.leftSide.k * x + triangleP3.leftSide.d

    private fun getTriangleP3RightSideYByX(x: Float): Float = triangleP3.rightSide.k * x + triangleP3.rightSide.d

    private fun getTriangleP3BottomSideYByX(x: Float): Float = triangleP3.bottomSide.k * x + triangleP3.bottomSide.d

    private fun onTouchReceived(
        event: MotionEvent,
        isUserTouch: Boolean,
    ): Boolean {
        val isTouchEnd = event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL
        when (mDrawMode) {
            DrawMode.REC_2020 -> {
                val x = getmX(event.x)
                val y = getmY(event.y)
                when {
                    // 在三角形2020的内部区域
                    y >= getTriangle2020BottomSideYByX(x) &&
                        y <= getTriangle2020LeftSideYByX(x) &&
                        y <=
                        getTriangle2020RightSideYByX(
                            x,
                        )
                    -> {
                        selectedPoint = PointF(event.x, event.y)
                        selectedColor = getColorFromBitmap(selectedPoint!!.x, selectedPoint!!.y)
                        mColorListener?.onColorSelected(getColor())
                    }

                    // 在三角形2020的右边区域 （大于底边，小于左边，大于右边）
                    y > getTriangle2020BottomSideYByX(x) &&
                        y < getTriangle2020LeftSideYByX(x) &&
                        y >
                        getTriangle2020RightSideYByX(
                            x,
                        )
                    -> {
                        val array =
                            getSuitablePointForTriangle2020(
                                TriangleOutsideArea.A,
                                event.x,
                                event.y,
                                triangle2020.leftVertex.x,
                                triangle2020.leftVertex.y,
                                triangle2020.rightSide.k,
                                triangle2020.rightSide.d,
                            )
                        selectedPoint = PointF(array[0], array[1])
                        selectedColor = getColorFromBitmap(selectedPoint!!.x, selectedPoint!!.y)
                        mColorListener?.onColorSelected(getColor())
                    }

                    // 在三角形2020的左右边夹角区域（三角形顶部）
                    y > getTriangle2020LeftSideYByX(x) && y > getTriangle2020RightSideYByX(x) -> {
                        val array =
                            getSuitablePointForTriangle2020(
                                TriangleOutsideArea.B,
                            )
                        selectedPoint = PointF(array[0], array[1])
                        selectedColor = getColorFromBitmap(selectedPoint!!.x, selectedPoint!!.y)
                        mColorListener?.onColorSelected(getColor())
                    }

                    // 在三角形2020的左边区域
                    y > getTriangle2020BottomSideYByX(x) &&
                        y > getTriangle2020LeftSideYByX(x) &&
                        y <
                        getTriangle2020RightSideYByX(
                            x,
                        )
                    -> {
                        val array =
                            getSuitablePointForTriangle2020(
                                TriangleOutsideArea.C,
                                event.x,
                                event.y,
                                triangle2020.rightVertex.x,
                                triangle2020.rightVertex.y,
                                triangle2020.leftSide.k,
                                triangle2020.leftSide.d,
                            )
                        selectedPoint = PointF(array[0], array[1])
                        selectedColor = getColorFromBitmap(selectedPoint!!.x, selectedPoint!!.y)
                        mColorListener?.onColorSelected(getColor())
                    }

                    // 在三角形2020的底边和左边夹角区域
                    y < getTriangle2020BottomSideYByX(x) && y > getTriangle2020LeftSideYByX(x) -> {
                        val array =
                            getSuitablePointForTriangle2020(
                                TriangleOutsideArea.D,
                            )
                        selectedPoint = PointF(array[0], array[1])
                        selectedColor = getColorFromBitmap(selectedPoint!!.x, selectedPoint!!.y)
                        mColorListener?.onColorSelected(getColor())
                    }

                    // 在三角形2020的下边区域
                    y < getTriangle2020BottomSideYByX(x) &&
                        y < getTriangle2020LeftSideYByX(x) &&
                        y <
                        getTriangle2020RightSideYByX(
                            x,
                        )
                    -> {
                        val array =
                            getSuitablePointForTriangle2020(
                                TriangleOutsideArea.E,
                                event.x,
                                event.y,
                                triangle2020.topVertex.x,
                                triangle2020.topVertex.y,
                                triangle2020.bottomSide.k,
                                triangle2020.bottomSide.d,
                            )
                        selectedPoint = PointF(array[0], array[1])
                        selectedColor = getColorFromBitmap(selectedPoint!!.x, selectedPoint!!.y)
                        mColorListener?.onColorSelected(getColor())
                    }

                    // 在三角形2020的底边和右边夹角区域
                    y < getTriangle2020BottomSideYByX(x) && y > getTriangle2020RightSideYByX(x) -> {
                        val array =
                            getSuitablePointForTriangle2020(
                                TriangleOutsideArea.F,
                            )
                        selectedPoint = PointF(array[0], array[1])
                        selectedColor = getColorFromBitmap(selectedPoint!!.x, selectedPoint!!.y)
                        mColorListener?.onColorSelected(getColor())
                    }
                }
                mColorListener?.onColorXY(getmX(selectedPoint!!.x), getmY(selectedPoint!!.y), getColor(), isTouchEnd)
                return true
            }

            DrawMode.REC_709 -> {
                val x = getmX(event.x)
                val y = getmY(event.y)
                when {
                    // 在三角形709的内部区域
                    y >= getTriangle709BottomSideYByX(x) &&
                        y <= getTriangle709LeftSideYByX(x) &&
                        y <=
                        getTriangle709RightSideYByX(
                            x,
                        )
                    -> {
                        selectedPoint = PointF(event.x, event.y)
                        selectedColor = getColorFromBitmap(selectedPoint!!.x, selectedPoint!!.y)
                        mColorListener?.onColorSelected(getColor())
                    }

                    // 在三角形709的右边区域 （大于底边，小于左边，大于右边）
                    y > getTriangle709BottomSideYByX(x) &&
                        y < getTriangle709LeftSideYByX(x) &&
                        y >
                        getTriangle709RightSideYByX(
                            x,
                        )
                    -> {
                        val array =
                            getSuitablePointForTriangle2020(
                                TriangleOutsideArea.A,
                                event.x,
                                event.y,
                                triangle709.leftVertex.x,
                                triangle709.leftVertex.y,
                                triangle709.rightSide.k,
                                triangle709.rightSide.d,
                            )
                        selectedPoint = PointF(array[0], array[1])
                        selectedColor = getColorFromBitmap(selectedPoint!!.x, selectedPoint!!.y)
                        mColorListener?.onColorSelected(getColor())
                    }

                    // 在三角形709的左右边夹角区域（三角形顶部）
                    y > getTriangle709LeftSideYByX(x) && y > getTriangle709RightSideYByX(x) -> {
                        val array =
                            getSuitablePointForTriangle2020(
                                TriangleOutsideArea.B,
                            )
                        selectedPoint = PointF(array[0], array[1])
                        selectedColor = getColorFromBitmap(selectedPoint!!.x, selectedPoint!!.y)
                        mColorListener?.onColorSelected(getColor())
                    }

                    // 在三角形709的左边区域
                    y > getTriangle709BottomSideYByX(x) &&
                        y > getTriangle709LeftSideYByX(x) &&
                        y <
                        getTriangle709RightSideYByX(
                            x,
                        )
                    -> {
                        val array =
                            getSuitablePointForTriangle2020(
                                TriangleOutsideArea.C,
                                event.x,
                                event.y,
                                triangle709.rightVertex.x,
                                triangle709.rightVertex.y,
                                triangle709.leftSide.k,
                                triangle709.leftSide.d,
                            )
                        selectedPoint = PointF(array[0], array[1])
                        selectedColor = getColorFromBitmap(selectedPoint!!.x, selectedPoint!!.y)
                        mColorListener?.onColorSelected(getColor())
                    }

                    // 在三角形709的底边和左边夹角区域
                    y < getTriangle709BottomSideYByX(x) && y > getTriangle709LeftSideYByX(x) -> {
                        val array =
                            getSuitablePointForTriangle2020(
                                TriangleOutsideArea.D,
                            )
                        selectedPoint = PointF(array[0], array[1])
                        selectedColor = getColorFromBitmap(selectedPoint!!.x, selectedPoint!!.y)
                        mColorListener?.onColorSelected(getColor())
                    }

                    // 在三角形709的下边区域
                    y < getTriangle709BottomSideYByX(x) &&
                        y < getTriangle709LeftSideYByX(x) &&
                        y <
                        getTriangle709RightSideYByX(
                            x,
                        )
                    -> {
                        val array =
                            getSuitablePointForTriangle2020(
                                TriangleOutsideArea.E,
                                event.x,
                                event.y,
                                triangle709.topVertex.x,
                                triangle709.topVertex.y,
                                triangle709.bottomSide.k,
                                triangle709.bottomSide.d,
                            )
                        selectedPoint = PointF(array[0], array[1])
                        selectedColor = getColorFromBitmap(selectedPoint!!.x, selectedPoint!!.y)
                        mColorListener?.onColorSelected(getColor())
                    }

                    // 在三角形709的底边和右边夹角区域
                    y < getTriangle709BottomSideYByX(x) && y > getTriangle709RightSideYByX(x) -> {
                        val array =
                            getSuitablePointForTriangle2020(
                                TriangleOutsideArea.F,
                            )
                        selectedPoint = PointF(array[0], array[1])
                        selectedColor = getColorFromBitmap(selectedPoint!!.x, selectedPoint!!.y)
                        mColorListener?.onColorSelected(getColor())
                    }
                }
                mColorListener?.onColorXY(getmX(selectedPoint!!.x), getmY(selectedPoint!!.y), getColor(), isTouchEnd)
                return true
            }

            DrawMode.DCI_P3 -> {
                val x = getmX(event.x)
                val y = getmY(event.y)
                when {
                    // 在三角形P3的内部区域
                    y >= getTriangleP3BottomSideYByX(x) &&
                        y <= getTriangleP3LeftSideYByX(x) &&
                        y <=
                        getTriangleP3RightSideYByX(
                            x,
                        )
                    -> {
                        selectedPoint = PointF(event.x, event.y)
                        selectedColor = getColorFromBitmap(selectedPoint!!.x, selectedPoint!!.y)
                        mColorListener?.onColorSelected(getColor())
                    }

                    // 在三角形P3的右边区域 （大于底边，小于左边，大于右边）
                    y > getTriangleP3BottomSideYByX(x) &&
                        y < getTriangleP3LeftSideYByX(x) &&
                        y >
                        getTriangleP3RightSideYByX(
                            x,
                        )
                    -> {
                        val array =
                            getSuitablePointForTriangle2020(
                                TriangleOutsideArea.A,
                                event.x,
                                event.y,
                                triangleP3.leftVertex.x,
                                triangleP3.leftVertex.y,
                                triangleP3.rightSide.k,
                                triangleP3.rightSide.d,
                            )
                        selectedPoint = PointF(array[0], array[1])
                        selectedColor = getColorFromBitmap(selectedPoint!!.x, selectedPoint!!.y)
                        mColorListener?.onColorSelected(getColor())
                    }

                    // 在三角形P3的左右边夹角区域（三角形顶部）
                    y > getTriangleP3LeftSideYByX(x) && y > getTriangleP3RightSideYByX(x) -> {
                        val array =
                            getSuitablePointForTriangle2020(
                                TriangleOutsideArea.B,
                            )
                        selectedPoint = PointF(array[0], array[1])
                        selectedColor = getColorFromBitmap(selectedPoint!!.x, selectedPoint!!.y)
                        mColorListener?.onColorSelected(getColor())
                    }

                    // 在三角形P3的左边区域
                    y > getTriangleP3BottomSideYByX(x) &&
                        y > getTriangleP3LeftSideYByX(x) &&
                        y <
                        getTriangleP3RightSideYByX(
                            x,
                        )
                    -> {
                        val array =
                            getSuitablePointForTriangle2020(
                                TriangleOutsideArea.C,
                                event.x,
                                event.y,
                                triangleP3.rightVertex.x,
                                triangleP3.rightVertex.y,
                                triangleP3.leftSide.k,
                                triangleP3.leftSide.d,
                            )
                        selectedPoint = PointF(array[0], array[1])
                        selectedColor = getColorFromBitmap(selectedPoint!!.x, selectedPoint!!.y)
                        mColorListener?.onColorSelected(getColor())
                    }

                    // 在三角形P3的底边和左边夹角区域
                    y < getTriangleP3BottomSideYByX(x) && y > getTriangleP3LeftSideYByX(x) -> {
                        val array =
                            getSuitablePointForTriangle2020(
                                TriangleOutsideArea.D,
                            )
                        selectedPoint = PointF(array[0], array[1])
                        selectedColor = getColorFromBitmap(selectedPoint!!.x, selectedPoint!!.y)
                        mColorListener?.onColorSelected(getColor())
                    }

                    // 在三角形P3的下边区域
                    y < getTriangleP3BottomSideYByX(x) &&
                        y < getTriangleP3LeftSideYByX(x) &&
                        y <
                        getTriangleP3RightSideYByX(
                            x,
                        )
                    -> {
                        val array =
                            getSuitablePointForTriangle2020(
                                TriangleOutsideArea.E,
                                event.x,
                                event.y,
                                triangleP3.topVertex.x,
                                triangleP3.topVertex.y,
                                triangleP3.bottomSide.k,
                                triangleP3.bottomSide.d,
                            )
                        selectedPoint = PointF(array[0], array[1])
                        selectedColor = getColorFromBitmap(selectedPoint!!.x, selectedPoint!!.y)
                        mColorListener?.onColorSelected(getColor())
                    }

                    // 在三角形P3的底边和右边夹角区域
                    y < getTriangleP3BottomSideYByX(x) && y > getTriangleP3RightSideYByX(x) -> {
                        val array =
                            getSuitablePointForTriangle2020(
                                TriangleOutsideArea.F,
                            )
                        selectedPoint = PointF(array[0], array[1])
                        selectedColor = getColorFromBitmap(selectedPoint!!.x, selectedPoint!!.y)
                        mColorListener?.onColorSelected(getColor())
                    }
                }
                mColorListener?.onColorXY(getmX(selectedPoint!!.x), getmY(selectedPoint!!.y), getColor(), isTouchEnd)
                return true
            }

            DrawMode.ALL -> {
                if (event.x < 0 || event.y < 0 || !isPointInPath(event.x, event.y, mPointPath)) {
                    val nearestPoint = findNearestPoint(getmX(Math.max(0f, event.x)), getmY(Math.max(0f, event.y)))
                    if (isUserTouch) {
                        // 如果是用户触摸选择的，需要限制不能超过马蹄图边界
                        selectedPoint = PointF(getX(nearestPoint.x).toFloat(), getY(nearestPoint.y).toFloat())
                        selectedColor = getColorFromBitmap(selectedPoint!!.x, selectedPoint!!.y)
                    } else {
                        // 如果不是用户触摸选择，而是通过设置数值，需要让滑块显示在实际的位置上，而颜色还是取就近的颜色。
                        selectedColor = getColorFromBitmap(getX(nearestPoint.x).toFloat(), getY(nearestPoint.y).toFloat())
                        selectedPoint = PointF(event.x, event.y)
                    }
                } else {
                    selectedPoint = PointF(event.x, event.y)
                    selectedColor = getColorFromBitmap(selectedPoint!!.x, selectedPoint!!.y)
                }
//                LOGUtils.e("selectedColor：${selectedColor}, x=${selectedPoint!!.x}, y=${selectedPoint!!.y}, x2=${event.x}")
                mColorListener?.onColorSelected(getColor())
                mColorListener?.onColorXY(getmX(selectedPoint!!.x), getmY(selectedPoint!!.y), getColor(), isTouchEnd)
                return true
            }
        }
    }

    fun setXAndY(
        x: Float,
        y: Float,
        notify: Boolean = true,
        invoke: ((Int) -> Unit)? = null,
    ) {
        post {
            val actualX = getX(x).toFloat()
            val actualY = getY(y).toFloat()
            if (actualX < 0 || actualY < 0 || !isPointInPath(actualX, actualY, mPointPath)) {
                val nearestPoint = findNearestPoint(x, y)
                selectedColor = getColorFromBitmap(getX(nearestPoint.x).toFloat(), getY(nearestPoint.y).toFloat())
                selectedPoint = PointF(actualX, actualY)
            } else {
                selectedPoint = PointF(actualX, actualY)
                selectedColor = getColorFromBitmap(selectedPoint!!.x, selectedPoint!!.y)
            }
            invalidate()
            invoke?.invoke(selectedColor)
            if (notify) {
                mColorListener?.onColorSelected(getColor())
                mColorListener?.onColorXY(x, y, getColor(), true)
            }
        }
    }

    private fun findNearestPoint(
        x: Float,
        y: Float,
    ): PointF {
        var nearestPoint: PointF = mPoints[0]
        var minDistance = Float.MAX_VALUE
        for (point in mPoints) {
            val distance = calculateDistance(x, y, point.x, point.y)
            if (distance < minDistance) {
                minDistance = distance
                nearestPoint = point
            }
        }
        return nearestPoint
    }

    private fun calculateDistance(
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
    ): Float = Math.sqrt(Math.pow((x2 - x1).toDouble(), 2.0) + Math.pow((y2 - y1).toDouble(), 2.0)).toFloat()

    private fun isPointInPath(
        x: Float,
        y: Float,
        path: Path,
    ): Boolean {
        val region = Region()
        val bounds = RectF()
        path.computeBounds(bounds, true)
        region.setPath(path, Region(bounds.left.toInt(), bounds.top.toInt(), bounds.right.toInt(), bounds.bottom.toInt()))
        return region.contains(x.toInt(), y.toInt())
    }

    // 根据自定义坐标系上点横坐标，返回该点的实际水平距离
    private fun getX(value: Float): Double = measuredWidth * 10.0 * value / 9.0

    private fun getY(value: Float): Double = measuredHeight - measuredHeight * 10.0 * value / 9.0

    private fun getmX(paramFloat: Float): Float = (9.0f * paramFloat / measuredWidth).toFloat() / 10.0f

    private fun getmY(paramFloat: Float): Float = 9.0f * (measuredHeight - paramFloat) / measuredHeight / 10.0f

    private fun getColorFromBitmap(
        x: Float,
        y: Float,
    ): Int {
        var colorInt = 0
        val matrix = Matrix()
        imageView.imageMatrix.invert(matrix)
        val arrayOfFloat =
            FloatArray(2).apply {
                this[0] = x
                this[1] = y
            }
        matrix.mapPoints(arrayOfFloat)
        if (imageView.drawable is BitmapDrawable) {
            if (arrayOfFloat[0] > 0f &&
                arrayOfFloat[0] <= imageView.drawable.intrinsicWidth &&
                arrayOfFloat[1] > 0f &&
                arrayOfFloat[1] <= imageView.drawable.intrinsicHeight
            ) {
                val bitmap = (imageView.drawable as BitmapDrawable).bitmap
                val scale = imageView.drawable.intrinsicWidth / bitmap.width.toFloat()
                colorInt =
                    bitmap.getPixel(
                        (arrayOfFloat[0] / scale).toInt(),
                        (arrayOfFloat[1] / scale).toInt(),
                    )
            }
        }
        return colorInt
    }

    private fun getColor(): Int {
        if (selectedColor != 0) this.mColor = selectedColor
//        return this.mColor and 0xFFFFFF or -0x1000000
        return Color.rgb(Color.red(mColor), Color.green(mColor), Color.blue(mColor))
    }

    private fun calculateTwoLineCrossPoint(
        line1Point1X: Float,
        line1Point1Y: Float,
        line1Point2X: Float,
        line1Point2Y: Float,
        line2K: Float,
        line2D: Float,
    ): FloatArray {
//        LogUtils.e("计算两直线的交点坐标：")
        val line1K = (line1Point1Y - line1Point2Y) / (line1Point1X - line1Point2X)
        val line1D = line1Point1Y - line1K * line1Point1X
        val crossPoint = FloatArray(2)
        crossPoint[0] = (line2D - line1D) / (line1K - line2K)
        crossPoint[1] = crossPoint[0] * line1K + line1D
        // 转实际坐标
        crossPoint[0] = getX(crossPoint[0]).toFloat()
        crossPoint[1] = getY(crossPoint[1]).toFloat()
        return crossPoint
    }

    private fun getSuitablePointForTriangle2020(
        areaType: TriangleOutsideArea,
        getX: Float = 0f,
        getY: Float = 0f,
        vertexX: Float = 0f,
        vertexY: Float = 0f,
        line2K: Float = 0f,
        line2D: Float = 0f,
    ): FloatArray {
        // 计算的区域类型：$areaType"
        when (areaType) {
            TriangleOutsideArea.A -> {
                // 右边
                return calculateTwoLineCrossPoint(
                    getmX(getX),
                    getmY(getY),
                    vertexX,
                    vertexY,
                    line2K,
                    line2D,
                )
            }

            TriangleOutsideArea.B -> {
                // 三角上方顶点
                when (mDrawMode) {
                    DrawMode.REC_2020 -> {
                        return floatArrayOf(
                            getX(triangle2020.topVertex.x).toFloat(),
                            getY(triangle2020.topVertex.y).toFloat(),
                        )
                    }

                    DrawMode.REC_709 -> {
                        return floatArrayOf(
                            getX(triangle709.topVertex.x).toFloat(),
                            getY(triangle709.topVertex.y).toFloat(),
                        )
                    }

                    DrawMode.DCI_P3 -> {
                        return floatArrayOf(
                            getX(triangleP3.topVertex.x).toFloat(),
                            getY(triangleP3.topVertex.y).toFloat(),
                        )
                    }

                    else -> {}
                }
            }

            TriangleOutsideArea.C -> {
                // 左边
                return calculateTwoLineCrossPoint(
                    getmX(getX),
                    getmY(getY),
                    vertexX,
                    vertexY,
                    line2K,
                    line2D,
                )
            }

            TriangleOutsideArea.D -> {
                // 三角左边顶点
                when (mDrawMode) {
                    DrawMode.REC_2020 -> {
                        return floatArrayOf(
                            getX(triangle2020.leftVertex.x).toFloat(),
                            getY(triangle2020.leftVertex.y).toFloat(),
                        )
                    }

                    DrawMode.REC_709 -> {
                        return floatArrayOf(
                            getX(triangle709.leftVertex.x).toFloat(),
                            getY(triangle709.leftVertex.y).toFloat(),
                        )
                    }

                    DrawMode.DCI_P3 -> {
                        return floatArrayOf(
                            getX(triangleP3.leftVertex.x).toFloat(),
                            getY(triangleP3.leftVertex.y).toFloat(),
                        )
                    }

                    else -> {}
                }
            }

            TriangleOutsideArea.E -> {
                // 下边
                return calculateTwoLineCrossPoint(
                    getmX(getX),
                    getmY(getY),
                    vertexX,
                    vertexY,
                    line2K,
                    line2D,
                )
            }

            TriangleOutsideArea.F -> {
                // 三角右边顶点
                when (mDrawMode) {
                    DrawMode.REC_2020 -> {
                        return floatArrayOf(
                            getX(triangle2020.rightVertex.x).toFloat(),
                            getY(triangle2020.rightVertex.y).toFloat(),
                        )
                    }

                    DrawMode.REC_709 -> {
                        return floatArrayOf(
                            getX(triangle709.rightVertex.x).toFloat(),
                            getY(triangle709.rightVertex.y).toFloat(),
                        )
                    }

                    DrawMode.DCI_P3 -> {
                        return floatArrayOf(
                            getX(triangleP3.rightVertex.x).toFloat(),
                            getY(triangleP3.rightVertex.y).toFloat(),
                        )
                    }

                    else -> {}
                }
            }
        }
        return floatArrayOf()
    }

    fun setMode(mode: DrawMode) {
        mDrawMode = mode
        selectedPoint = PointF(getX(ILLUMINANT_E.x).toFloat(), getY(ILLUMINANT_E.y).toFloat())
        selectedColor = getColorFromBitmap(selectedPoint!!.x, selectedPoint!!.y)
        mColorListener?.onColorSelected(getColor())
        mColorListener?.onColorXY(ILLUMINANT_E.x, ILLUMINANT_E.y, getColor(), true)
        invalidate()
    }
}

enum class DrawMode {
    DCI_P3,
    REC_2020,
    REC_709,
    ALL,
}

/**
 * Triangle outside area
 *
 *     \      /
 *      \ B /
 *       \ /
 *   C   /\   A
 *      /  \
 *     /    \
 * ___/______\_____
 * D /    E   \ F
 *  /          \
 * @constructor Create empty Triangle outside area
 */
enum class TriangleOutsideArea {
    A,
    B,
    C,
    D,
    E,
    F,
}

data class TriangleInfo(
    val topVertex: PointF,
    val leftVertex: PointF,
    val rightVertex: PointF,
    val leftSide: TriangleSideInfo,
    val rightSide: TriangleSideInfo,
    val bottomSide: TriangleSideInfo,
)

/**
 * Triangle side info
 *
 * @property k 斜率
 * @property d 截距
 * @constructor Create empty Triangle side info
 */
data class TriangleSideInfo(val k: Float, val d: Float)
