package com.chen.mavenchen.freeDialog

import android.content.Context
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.activity.ComponentDialog
import androidx.core.content.ContextCompat.getSystemService

/**
 * 目的是为了点击空白区域将软键盘收起。
 *
 * @constructor
 *
 * @param context
 * @param themeResId
 */
class FoldInputTouchDialog(
    context: Context,
    themeResId: Int = 0,
) : ComponentDialog(context, themeResId) {

    // onTouchEvent只能接收到没有点击事件的view的点击事件，所以需要在dispatchTouchEvent去处理
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        // 这里只能
        if (ev.action == MotionEvent.ACTION_DOWN) {
            val v = currentFocus
            if (v != null && isShouldHideInput(v, ev)) {
                getSystemService(context, InputMethodManager::class.java)?.apply {
                    hideSoftInputFromWindow(v.windowToken, 0)
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    private fun isShouldHideInput(
        v: View,
        event: MotionEvent,
    ): Boolean {
        if (v is EditText) {
            val leftTop = IntArray(2)
            v.getLocationInWindow(leftTop)
            val left = leftTop[0]
            val top = leftTop[1]
            val bottom = top + v.height
            val right = left + v.width
            return if (event.x > left && event.x < right && event.y > top && event.y < bottom) {
                false
            } else {
                v.setFocusable(false)
                v.isFocusableInTouchMode = true
                true
            }
        }
        return false
    }
}
