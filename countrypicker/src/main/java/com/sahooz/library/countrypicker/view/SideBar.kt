package com.sahooz.library.countrypicker.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.sahooz.library.countrypicker.R
import kotlin.math.ceil

class SideBar @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : View(context, attrs, defStyleAttr) {
    private val paint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = letterSize.toFloat()
        }
    }
    private var textHeight = 0f
    private var cellWidth = 0
    private var cellHeight = 0
    private var currentIndex = -1
    private var letterColor = 0
    private var selectColor = 0
    private var letterSize = 0

    private var letters: Array<String> =
        arrayOf(
            "A", "B", "C", "D", "E", "F", "G",
            "H", "I", "J", "K", "L", "M", "N",
            "O", "P", "Q", "R", "S", "T",
            "U", "V", "W", "X", "Y", "Z",
        )
    val indexes: ArrayList<String> = ArrayList()

    interface OnLetterChangeListener {
        fun onLetterChange(letter: String?)

        // 手指抬起
        fun onReset()
    }

    var onLetterChangeListener: OnLetterChangeListener? = null

    init {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.SideBar, defStyleAttr, 0)
        letterColor = ta.getColor(R.styleable.SideBar_letterColor, Color.BLACK)
        selectColor = ta.getColor(R.styleable.SideBar_selectColor, Color.CYAN)
        letterSize = ta.getDimensionPixelSize(R.styleable.SideBar_letterSize, 24)
        ta.recycle()

        val fontMetrics = paint.fontMetrics
        textHeight = ceil((fontMetrics.descent - fontMetrics.ascent).toDouble()).toFloat()

        indexes.addAll(listOf<String>(*letters))
    }

    override fun onSizeChanged(
        w: Int,
        h: Int,
        oldw: Int,
        oldh: Int,
    ) {
        super.onSizeChanged(w, h, oldw, oldh)
        cellWidth = measuredWidth
        cellHeight = measuredHeight / indexes.size
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        paint.textSize = letterSize.toFloat()
        for (i in indexes.indices) {
            val letter = indexes[i]
            val textWidth = paint.measureText(letter)
            val x = (cellWidth - textWidth) * 0.5f
            val y = (cellHeight + textHeight) * 0.5f + cellHeight * i

            if (i == currentIndex) {
                paint.color = selectColor
            } else {
                paint.color = letterColor
            }

            canvas.drawText(letter, x, y, paint)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        when (event!!.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                val downY = event.y.toInt()
                // 获取当前索引
                currentIndex = downY / cellHeight
                if (currentIndex < 0 || currentIndex > indexes.size - 1) {
                    //
                } else {
                    onLetterChangeListener?.onLetterChange(indexes[currentIndex])
                }
                // 重新绘制
                invalidate()
            }

            MotionEvent.ACTION_UP -> {
                currentIndex = -1
                // 手动刷新
                invalidate()
                // 表示手指抬起了
                onLetterChangeListener?.onReset()
            }
        }

        // 为了 能够接受  move+up事件
        return true
    }

    fun addIndex(
        indexStr: String,
        position: Int,
    ) {
        indexes.add(position, indexStr)
        invalidate()
    }

    fun removeIndex(indexStr: String?) {
        indexes.remove(indexStr)
        invalidate()
    }

    fun setLetterSize(letterSize: Int) {
        if (this.letterSize == letterSize) return
        this.letterSize = letterSize
        invalidate()
    }

    fun getLetter(position: Int): String {
        if (position < 0 || position >= indexes.size) return ""
        return indexes[position]
    }
}
