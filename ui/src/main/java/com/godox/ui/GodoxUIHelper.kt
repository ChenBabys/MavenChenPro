package com.godox.ui

import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.method.MovementMethod
import android.text.method.Touch
import android.view.KeyEvent
import android.view.MotionEvent
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import com.godox.base.ktx.dp2px

object GodoxUIHelper {
    /**
     * 自定义MovementMethod，用于处理TextView的文本交互
     * 主要功能：
     * 1. 去掉文本选中效果
     * 2. 保持点击事件响应
     * 3. 禁用键盘和轨迹球事件
     */
    private class NoSelectionMovementMethod : MovementMethod {
        /**
         * 初始化方法，在TextView设置文本时调用
         * @param widget 目标TextView
         * @param text 文本内容
         */
        override fun initialize(
            widget: TextView,
            text: Spannable,
        ) {}

        /**
         * 处理TextView获得焦点时的回调
         * @param view 目标TextView
         * @param text 文本内容
         * @param dir 焦点方向
         */
        override fun onTakeFocus(
            view: TextView,
            text: Spannable,
            dir: Int,
        ) {}

        /**
         * 处理轨迹球事件
         * @param widget 目标TextView
         * @param text 文本内容
         * @param event 轨迹球事件
         * @return 是否处理了该事件
         */
        override fun onTrackballEvent(
            widget: TextView?,
            text: Spannable?,
            event: MotionEvent?,
        ): Boolean = false

        /**
         * 处理触摸事件
         * 使用Touch.onTouchEvent处理点击事件，但不显示选中效果
         * @param widget 目标TextView
         * @param text 文本内容
         * @param event 触摸事件
         * @return 是否处理了该事件
         */
        override fun onTouchEvent(
            widget: TextView,
            text: Spannable,
            event: MotionEvent,
        ): Boolean {
            if (event.action == MotionEvent.ACTION_UP) {
                val x = event.x.toInt()
                val y = event.y.toInt()
                val layout = widget.layout
                val line = layout.getLineForVertical(y)
                val offset = layout.getOffsetForHorizontal(line, x.toFloat())
                val spans = text.getSpans(offset, offset, ClickSpan::class.java)
                if (spans.isNotEmpty()) {
                    spans[0].onClick(widget)
                    return true
                }
            }
            return false
        }

        /**
         * 处理通用运动事件（如鼠标移动等）
         * @param widget 目标TextView
         * @param text 文本内容
         * @param event 运动事件
         * @return 是否处理了该事件
         */
        override fun onGenericMotionEvent(
            widget: TextView?,
            text: Spannable?,
            event: MotionEvent?,
        ): Boolean = false

        /**
         * 是否允许任意选择文本
         * @return false 表示不允许选择文本
         */
        override fun canSelectArbitrarily(): Boolean = false

        /**
         * 处理按键按下事件
         * @param widget 目标TextView
         * @param text 文本内容
         * @param keyCode 按键码
         * @param event 按键事件
         * @return 是否处理了该事件
         */
        override fun onKeyDown(
            widget: TextView,
            text: Spannable,
            keyCode: Int,
            event: KeyEvent,
        ): Boolean = true

        /**
         * 处理按键释放事件
         * @param widget 目标TextView
         * @param text 文本内容
         * @param keyCode 按键码
         * @param event 按键事件
         * @return 是否处理了该事件
         */
        override fun onKeyUp(
            widget: TextView,
            text: Spannable,
            keyCode: Int,
            event: KeyEvent,
        ): Boolean = true

        /**
         * 处理其他按键事件
         * @param view 目标TextView
         * @param text 文本内容
         * @param event 按键事件
         * @return 是否处理了该事件
         */
        override fun onKeyOther(
            view: TextView,
            text: Spannable,
            event: KeyEvent,
        ): Boolean = true
    }

    /**
     * 将TextView的内容修改为部分可高亮可点击
     * 应用场景：
     *   同意条款的UI，[clickableTextArray]包含要高亮的文本和可跳转的链接，点击后通过[clickCallBack]回调。
     *
     * @param tv
     * @param text
     * @param textSize
     * @param textColor
     * @param clickableTextColor
     * @param showUnderline
     * @param clickableTextArray
     * @param clickCallBack
     */
    fun makePartiallyClickableTextView(
        tv: TextView,
        text: String,
        textSize: Float = 14.dp2px().toFloat(),
        @ColorRes textColor: Int,
        @ColorRes clickableTextColor: Int,
        showUnderline: Boolean = false,
        clickableTextArray: Map<String, String>,
        clickCallBack: (String) -> Unit,
    ) {
        tv.apply {
            this.textSize = textSize
            this.setTextColor(ContextCompat.getColor(tv.context, textColor))
            movementMethod = NoSelectionMovementMethod()
            this.text =
                SpannableStringBuilder(text).apply {
                    clickableTextArray.entries.forEach { clickableTermEntry ->
                        val startHighlightIndex = text.indexOf(clickableTermEntry.key)
                        if (startHighlightIndex > 0) {
                            setSpan(
                                ClickSpan(
                                    ContextCompat.getColor(tv.context, clickableTextColor),
                                    showUnderline,
                                ) {
                                    clickCallBack.invoke(clickableTermEntry.value)
                                },
                                startHighlightIndex,
                                startHighlightIndex + clickableTermEntry.key.length,
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
                            )
                        }
                    }
                }
        }
    }
}
