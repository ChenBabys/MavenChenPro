package com.godox.common.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.View
import com.godox.common.R

/**
 *  Author: ChenBabys
 *  Date: 2024/12/7
 *  Description:
 *    电池剩余电量view,全部依靠绘制完成，不用图
 *    包含两种样式：
 *    1.一节一节的电池内部
 *    2.内部直接一体式填充
 */
class BatteryCanvasView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet?, defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {
    private val TAG = "BatteryCanvasView"
    private var maxBatteryLevel = 100//电池最大值

    // 初始宽高，也就是宽高最小值，当宽高都设置为warp是会使用这个默认值
    // 绘制电池总的宽度和总高度,初始可以给0，也可以给对应初始值，但初始给0的话，若设置为warp会高度为0
    private var batteryFullWidth = dpToPx(15f).toFloat()
    private var batteryFullHeight = dpToPx(8f).toFloat()

    // 电池身设置为最大宽度-12%，留点空间给电池帽
    private var batteryBody = 0f

    //内部边距一边3px，根据设计图自己调整
    private val batteryDrawPadding = 3f

    // 绘制电池格数的宽度
    private var batteryFrameWidth = 0f

    // 每个电池格的高度
    private var batteryFrameHeight = 0f

    //最大拆分个数
    private var maxNumOfBars: Int = 5//默认是五格

    // 定义圆角半径
    private var bgStrokeCornerRadius: Float = 4f

    //边线的宽度，主要用于让边角绘制更加圆滑
    private var bgStrokeWidth: Float = 3f

    //当前拆分的个数，根据电量拆分，默认给0，
    //可能是0--最大值中的任意一个，它会根据反馈回来的电量数据做判断累计
    //电池样式，Style.Grid时使用
    private var numOfBars: Int = 0

    //电池当前值，Style.Fill时使用
    private var mCurBatteryLevel = 0

    private var defaultColor = Color.parseColor("#82809A")

    //背景方框的画笔（默认抗锯齿ANTI_ALIAS_FLAG）
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        // 设置画笔颜色
        color = defaultColor  // 线条颜色
        style = Paint.Style.STROKE // 边框线条
        strokeWidth = this@BatteryCanvasView.bgStrokeWidth
    }

    //背景方框前面的电池盖盖的画笔（默认抗锯齿ANTI_ALIAS_FLAG）
    private val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        // 设置画笔颜色
        color = defaultColor  // 和线条颜色一致
        style = Paint.Style.FILL // 实心
    }

    //填充（方块/一体式填充）的画笔（默认抗锯齿ANTI_ALIAS_FLAG）
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        // 设置画笔颜色
        color = defaultColor // 电池格颜色
        style = Paint.Style.FILL // 填充模式
    }

    //充电中的画笔（默认抗锯齿ANTI_ALIAS_FLAG）
    private val chargingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        // 设置画笔颜色
        color = defaultColor
        style = Paint.Style.STROKE
        strokeWidth = 2f //3已经很粗了对于小图标来说，足够
        setShadowLayer(10f, 0f, 0f, defaultColor) // 发光效果
    }


    //默认是填充类型
    private var curStyle = Style.Fill

    //充电中与否
    private var isCharging: Boolean = false

    /**
     * 两种方式，一种是格子（一共五个满格），一种是一体式填充
     */
    enum class Style {
        Grid, Fill
    }

    init {
        var newFillColor: Int? = null
        // 从XML属性获取自定义属性
        context.theme.obtainStyledAttributes(attrs, R.styleable.BatteryCanvasView, 0, 0).apply {
            try {
                curStyle = if (getInt(R.styleable.BatteryCanvasView_bcv_showStyle, 0) == 0) Style.Fill else Style.Grid
                maxBatteryLevel = getInt(R.styleable.BatteryCanvasView_bcv_maxBatteryLevel, 100)
                batteryFullWidth = getDimension(R.styleable.BatteryCanvasView_bcv_minWidth, dpToPx(15f).toFloat())
                batteryFullHeight = getDimension(R.styleable.BatteryCanvasView_bcv_minHeight, dpToPx(8f).toFloat())
                bgStrokeWidth = getDimension(R.styleable.BatteryCanvasView_bcv_bgStrokeWidth, 3f)
                bgStrokeCornerRadius = getDimension(R.styleable.BatteryCanvasView_bcv_bgStrokeCornerRadius, 4f)
                defaultColor = getColor(R.styleable.BatteryCanvasView_bcv_strokeColor, Color.parseColor("#82809A"))
                newFillColor = getColor(R.styleable.BatteryCanvasView_bcv_fillColor, 0)
            } finally {
                recycle()
            }
        }

        // 更新设置的颜色给这几个paint
        if (bgPaint.color != defaultColor) {
            bgPaint.color = defaultColor
        }
        if (bgPaint.strokeWidth != bgStrokeWidth) {
            bgPaint.strokeWidth = bgStrokeWidth
        }

        if (paint.color != defaultColor) {
            if (newFillColor != null && newFillColor != 0) {
                paint.color = newFillColor!!
            } else {
                paint.color = defaultColor
            }
        }

        if (cellPaint.color != defaultColor) {
            cellPaint.color = defaultColor
        }

        if (chargingPaint.color != defaultColor) {
            if (newFillColor != null && newFillColor != 0) {
                chargingPaint.color = newFillColor!!
                chargingPaint.setShadowLayer(10f, 0f, 0f, newFillColor!!)
            } else {
                chargingPaint.color = defaultColor
                chargingPaint.setShadowLayer(10f, 0f, 0f, defaultColor)
            }
        }

    }

    /**
     * 绘制warp时矫正高度，但当onSizeChanged处理后，依旧会被真实设定的高度改变
     */
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val desiredWidthSize = batteryFullWidth.toInt()
        val desiredHeightSize = batteryFullHeight.toInt()
        val width = resolveSize(desiredWidthSize, widthMeasureSpec)
        val height = resolveSize(desiredHeightSize, heightMeasureSpec)
        setMeasuredDimension(width, height)
    }

    // onSizeChanged中处理宽高的获取,是最佳，每次变化调用，在onLayout和都onMeasure不太合适，可能会不准确
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // 当布局那边都设置warp的时候，这里的宽高就会变成父布局的宽高了，要是放在根布局，就满屏大小了，所以给它设置一个固定的大小作为warp是的固定项
        batteryFullWidth = w.toFloat()
        batteryFullHeight = h.toFloat()
        initFrameWidthHeight(curStyle)//首次时给的默认样式
        // Log.d(TAG, "宽高发生了改变-->w:${w},h:${h},oldw:${oldw},oldh:${oldh}")
    }

    /**
     * 初始化一个格子或者是满填充的宽高度，当是Grid样式时就是每个格子的宽度，当是Fill样式时，就是这个一体式填充的宽高度
     */
    private fun initFrameWidthHeight(style: Style) {
        batteryBody = batteryFullWidth - (batteryFullWidth * 0.12f)
        // 单个方向两侧的线条宽度
        val bothDireStrokeWidth = bgStrokeWidth * 2
        // 单个方向设计图上的内边距两边+起来，根据设计图调整
        val bothDireDrawPadding = batteryDrawPadding * 2
        // 减去一个pad即可，如果上下都减去一个pad,那么内边距太大了，不好看，紧贴一些好看些，高度都是一样的，不用区分样式
        batteryFrameHeight = batteryFullHeight - bothDireStrokeWidth - bothDireDrawPadding
        if (style == Style.Fill) {
            batteryFrameWidth = batteryBody - bothDireStrokeWidth - bothDireDrawPadding//默认是最大值
        } else {
            // 计算需要绘制的电池格数，5格即可,以最大格数来计算
            maxNumOfBars = maxBatteryLevel / 20
            val eachShouldWidth =// 每项的应有理论宽度，但还不够，因为有边框线
                ((batteryBody - bothDireStrokeWidth - bothDireDrawPadding) / maxNumOfBars)
            batteryFrameWidth = eachShouldWidth // 真正的一项的宽度
        }
    }


    /**
     * 设置样式
     */
    fun setStyle(style: Style) {
        this.curStyle = style
        //选择样式后的参数改变初始化
        initFrameWidthHeight(curStyle)
    }

    /**
     * 设置是否充电中
     */
    fun setCharging(isCharging: Boolean) {
        this.isCharging = isCharging
        invalidate()// 重绘制
    }

    /**
     * 设置电池电量,0--100, Math.min避免数值越界
     * isCharging:是否是充电中
     */
    fun setBatteryLevel(level: Int, isCharging: Boolean) {
        this.isCharging = isCharging
        if (curStyle == Style.Fill) {
            mCurBatteryLevel = Math.min(level, maxBatteryLevel)
        } else {
            when (level) {
                in 1..20 -> {
                    numOfBars = 1
                }

                in 21..40 -> {
                    numOfBars = 2
                }

                in 41..60 -> {
                    numOfBars = 3
                }

                in 61..80 -> {
                    numOfBars = 4
                }

                in 81..100 -> {
                    numOfBars = 5
                }

                else -> {
                    numOfBars = 0
                }
            }
        }
        invalidate()//重绘
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        onDrawBatteryRoundRet(canvas)
        onDrawCell(canvas)
        // 非充电情况下正常填充，而充电情况下只有当满电的时候才填充，否则只展示电量样式
        when {
            this.isCharging -> {
                when (this.curStyle) {
                    Style.Grid -> {
                        if (numOfBars == maxNumOfBars) {
                            onDrawBatteryGrid(canvas)
                        } else {
                            // 充电样式
                            onDrawCharging(canvas)
                        }
                    }

                    else -> {
                        if (mCurBatteryLevel == maxBatteryLevel) {
                            onDrawBatteryFill(canvas)
                        } else {
                            // 充电样式
                            onDrawCharging(canvas)
                        }
                    }
                }
            }

            else -> {
                if (this.curStyle == Style.Grid) {
                    onDrawBatteryGrid(canvas)
                } else {
                    onDrawBatteryFill(canvas)
                }
            }
        }
    }

    /**
     * 绘制电池背景框，圆角的
     */
    private fun onDrawBatteryRoundRet(canvas: Canvas) {
        //起始坐标给0，0,绘制背景图标，绘制,但因为 要顾及变线，所以坐标稍微调整
        // 定义矩形的位置和大小
        // 注意：由于设置了描边宽度，我们需要从总宽度和高度中减去这个宽度的一半
        // 以确保线条不会超出View的边界,从而让边界更加清晰和圆滑
        val rectWidth = batteryBody - bgStrokeWidth
        val rectHeight = batteryFullHeight - bgStrokeWidth
        val strokeWidthCenter = bgStrokeWidth / 2//边线的宽度中心
        val ret = RectF(
            strokeWidthCenter,
            strokeWidthCenter,
            rectWidth + strokeWidthCenter,
            rectHeight + strokeWidthCenter,
        )
        canvas.drawRoundRect(ret, bgStrokeCornerRadius, bgStrokeCornerRadius, bgPaint)
    }

    /**
     * 绘制电池头盖
     */
    private fun onDrawCell(canvas: Canvas) {
        //在电池方块的右边10像素处中心点绘制,电池和帽之间的间距是5，固定了，就不拿出来了，想不固定就拿出去
        val cellPointCenterX = batteryBody + 5f//在宽度的右侧4像素处开始做，不能超过最大宽度，即+12%以内
        val cellPointCenterY = batteryFullHeight / 2//Y轴圆心
        //宽度是4px,高度是12px，电池帽给固定的大小了
        val cellRet = RectF(
            cellPointCenterX - 2,
            cellPointCenterY - 6,
            cellPointCenterX + 2,
            cellPointCenterY + 6,
        )
        val roundRadius = 20f//10以上即可
        // 创建Path并构建形状
        val path = Path()
        //根据path来绘制只有右边有圆角的图标
        val floatArray = floatArrayOf(
            0f, 0f,
            roundRadius,
            roundRadius,
            roundRadius,
            roundRadius,
            0f, 0f,
        )
        //绘制圆角矩形的其中两个圆角
        path.addRoundRect(
            cellRet, floatArray,
            Path.Direction.CW,//顺时针就可以了
        )
        // 闭合路径（可选，但推荐）
        path.close()
        // 使用画笔和路径绘制形状
        canvas.drawPath(path, cellPaint)
    }

    /**
     * 绘制电池的填充,一体式填充，Style.Fill时使用
     */
    private fun onDrawBatteryFill(canvas: Canvas) {
        val left = bgStrokeWidth + batteryDrawPadding//左边开始的位置
        val top = batteryFullHeight / 2 - (batteryFrameHeight / 2)  //以圆心为标准开始
        val bottom = batteryFullHeight / 2 + (batteryFrameHeight / 2)  //以圆心为标准结束
        //右边到达的位置,根据公式比如：76/100 = x/满宽，来计算
        val right = left + ((batteryFrameWidth * mCurBatteryLevel) / maxBatteryLevel)
        val rect = RectF(left, top, right, bottom)
        val roundRadius = bgStrokeCornerRadius - (bgStrokeCornerRadius / 3) // 左右两边的圆角,所以内部圆角小一点
        canvas.drawRoundRect(rect, roundRadius, roundRadius, paint)
    }


    /**
     * 绘制电池的所有格子,Style.Grid时使用
     */
    private fun onDrawBatteryGrid(canvas: Canvas) {
        for (i in 0 until numOfBars) {//绘制电池格
            //从边线的内中心点出发，除去固定pad,开始绘制
            val left = (i * batteryFrameWidth) + batteryDrawPadding + (bgStrokeWidth / 2)
            val top = batteryFullHeight / 2 - (batteryFrameHeight / 2)  //以圆心为标准开始
            val bottom = batteryFullHeight / 2 + (batteryFrameHeight / 2)  //以圆心为标准结束
            val spacingWidth = bgStrokeWidth//以线条宽度作为间隙去绘制,刚好也满足了首帧需要填充bgStrokeWidth的需求
            val rect = RectF(
                left + spacingWidth,//从左边开始，以线条宽度作为间隙去绘制
                top,
                left + batteryFrameWidth,//每一帧都减去了间距，所以直接写就可以了
                bottom,
            )
            val roundRadius = bgStrokeCornerRadius - (bgStrokeCornerRadius / 3) // 左右两边的圆角,所以内部圆角小一点
            if (i == 0 || ((i == numOfBars - 1) && numOfBars == maxNumOfBars)) {
                // 创建Path并构建形状
                val path = Path()
                val floatArray = if (i == 0) floatArrayOf(
                    roundRadius,
                    roundRadius,
                    0f,
                    0f,
                    0f,
                    0f,
                    roundRadius,
                    roundRadius,
                ) else floatArrayOf(
                    0f,
                    0f,
                    roundRadius,
                    roundRadius,
                    roundRadius,
                    roundRadius,
                    0f,
                    0f,
                )
                //绘制圆角矩形的其中两个圆角
                path.addRoundRect(
                    rect, floatArray,
                    Path.Direction.CW,//顺时针就可以了
                )
                // 闭合路径（可选，但推荐）
                path.close()
                // 使用画笔和路径绘制形状
                canvas.drawPath(path, paint)
            } else {
                canvas.drawRect(rect, paint)
            }
        }
    }

    /**
     * 绘制充电中样式
     */
    private fun onDrawCharging(canvas: Canvas) {
        // 获取 View 的有效电池样式满屏宽高
        val width = batteryBody// 给电池的body宽度即可
        val height = batteryFullHeight
        // 宽度的十分之一。作为偏移使用
        val tenthWidth = width / 10f

        // 绘制的触边内边距，留点缝隙，所以2倍
        val drawPaddingGap = batteryDrawPadding * 2

        val centerX = width / 2f
        // 起点坐标
        val startX = centerX + tenthWidth
        // 顶部开始,内边距线再溢出点，避免接触到边线
        val startY = 0f + drawPaddingGap

        // 定义转折点的坐标
        val firstTurnX = centerX - tenthWidth// 第一个转折点向左偏移，10分之一
        val firstTurnY = height / 2f // 第一个转折点在高度的1/2处

        val secondTurnX = centerX + tenthWidth // 第二个转折点向右偏移，10分之一
        val secondTurnY = height / 2f // 第er个转折点也在高度的1/2处

        val endX = centerX - tenthWidth // 终点回到，减六分之一偏移
        // 底部结束，内边距线再溢出点，避免接触到边线
        val endY = height - drawPaddingGap

        // 构建路径
        val path = Path()
        path.reset()
        path.moveTo(startX, startY) // 起点
        path.lineTo(firstTurnX, firstTurnY) // 到第一个转折点
        path.lineTo(secondTurnX, secondTurnY) // 到第二个转折点
        path.lineTo(endX, endY) // 到终点

        // 绘制路径
        canvas.drawPath(path, chargingPaint)
    }

    internal fun dpToPx(dp: Float): Int {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.displayMetrics).toInt()
    }
}
