package com.chen.freedialog.utils

import android.animation.ValueAnimator
import android.app.Activity
import android.graphics.Rect
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewTreeObserver
import android.view.Window
import android.view.animation.AccelerateDecelerateInterpolator

/**
 * 移动[dialogWindow]，使其内的焦点视图的底部 或者直接整个[dialogWindow]的底部 对齐输入法顶部
 * @property activity
 * @property dialogWindow
 * @property attachByWindowBottom 是否直接将整个[dialogWindow]的底部对齐输入法顶部
 */
@Deprecated("由于刘海屏、系统栏等存在，此方式兼容性不太好，暂时用Dialog原生的方式去适配输入法")
class SoftInputHelper(val activity: Activity, private val dialogWindow: Window, private val attachByWindowBottom: Boolean) :
    ViewTreeObserver.OnGlobalLayoutListener,
    ViewTreeObserver.OnGlobalFocusChangeListener {
    private var activityWindow: Window? = null
    private var activityDecorView: View? = null
    private val windowVisibleDisplayFrame = Rect()
    private var keyboardMinHeight = 0
    private var keyBoardHeight = 0

    private var windowYPositionAnimator: ValueAnimator? = null
    private val moveRunnable = Runnable { startMove() }
    private val viewInWindowLocation = IntArray(2)

    /** dialogWindow初始的Gravity。当输入法弹出后会将dialogWindow改成Gravity.Top or Gravity.Start，然后去偏移它的y*/
    private var initGravity: Int

    /** dialogWindow初始位置*/
    private val windowInScreenLocation = IntArray(2)

    /** dialogWindow初始位置左上角y坐标，相对于整个屏幕*/
    private var initDialogWindowTopY = 0

    init {
        activityWindow = activity.window
        activityDecorView = activityWindow!!.decorView
        keyboardMinHeight =
            TypedValue
                .applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    200f,
                    activity.resources.displayMetrics,
                ).toInt()

        if (activityDecorView!!.viewTreeObserver.isAlive) {
            activityDecorView!!.viewTreeObserver.addOnGlobalLayoutListener(this)
        }

        if (dialogWindow.decorView.viewTreeObserver.isAlive) {
            dialogWindow.decorView.viewTreeObserver.addOnGlobalFocusChangeListener(this)
        }

        initGravity = dialogWindow.attributes.gravity
    }

    /**
     * 监听布局变化（一般是输入法显示和隐藏触发）
     */
    override fun onGlobalLayout() {
        val keyboardHeight = calcKeyboardHeight()
        val lastHeight: Int = keyBoardHeight
        val lastOpened: Boolean = keyBoardHeight > 0
        val nowOpened = keyboardHeight > 0
        keyBoardHeight = keyboardHeight
        if (lastOpened != nowOpened) {
            // 软键盘打开或关闭
            notifyOpenOrClose()
            startMove()
        } else {
            // 软键盘打开中或关闭中的高度变化。这种情况暂时没测到什么情况下会发生。
            if (lastHeight != keyboardHeight) {
                notifyHeightChanged()
                startMove()
            }
        }
    }

    /**
     * 监听DialogFragment window的焦点变化
     * @param oldFocus
     * @param newFocus
     */
    override fun onGlobalFocusChanged(
        oldFocus: View?,
        newFocus: View?,
    ) {
        Logger.log("oldFocus: $oldFocus, newFocus: $newFocus")
        if (isOpened()) {
            activityDecorView!!.postDelayed(moveRunnable, 100)
        }
    }

    /**
     * 计算软键盘高度
     * @return
     */
    private fun calcKeyboardHeight(): Int {
        if (initDialogWindowTopY == 0) {
            dialogWindow.decorView.getLocationOnScreen(windowInScreenLocation)
            initDialogWindowTopY = windowInScreenLocation[1]
        }
        val rect = getWindowVisibleDisplayFrame()
        val usableHeightNow = rect.height()
        val usableHeightSansKeyboard: Int = activityDecorView!!.height
        val heightDifference = usableHeightSansKeyboard - usableHeightNow
        return if (heightDifference > (usableHeightSansKeyboard / 4) || heightDifference > keyboardMinHeight) {
            heightDifference
        } else {
            0
        }
    }

    /**
     * 获取Activity Window可视区域
     */
    private fun getWindowVisibleDisplayFrame(): Rect {
        activityDecorView!!.getWindowVisibleDisplayFrame(windowVisibleDisplayFrame)
//        Logger.log("windowVisibleDisplayFrame: $windowVisibleDisplayFrame") // Rect(0, 51 - 2560, 754)  Rect(0, 51 - 2560, 1568)
        return windowVisibleDisplayFrame
    }

    /**
     * 软键盘是否打开
     * @return
     */
    private fun isOpened(): Boolean = keyBoardHeight > 0

    private fun notifyOpenOrClose() {
    }

    private fun notifyHeightChanged() {
    }

    private fun startMove() {
        activityDecorView!!.removeCallbacks(moveRunnable)
        if (isOpened()) {
            val windowBottomY: Int

            val focusView = dialogWindow.decorView.findFocus()
            if (focusView != null && !attachByWindowBottom) {
                focusView.getLocationInWindow(viewInWindowLocation)
                // 对齐焦点视图底部
                windowBottomY = initDialogWindowTopY + viewInWindowLocation[1] + focusView.height
            } else {
                // 对齐Dialog底部
                windowBottomY = initDialogWindowTopY + dialogWindow.decorView.measuredHeight
            }
            // 键盘顶部在屏幕的Y坐标（这里Activity可能是包含或不包含systemBar、延伸或未延伸到DisplayCutout）
            val keyBoardTopY = activity.resources.displayMetrics.heightPixels - keyBoardHeight
            // activityDecorView!!.height - keyBoardHeight + if (ScreenUtil.isStatusBarHidden(activityDecorView!!)) 0 else ScreenUtil.getStatusHeight(activity.resources)
//            Logger.log("initDialogWindowY:${initDialogWindowTopY}, windowBottomY:${windowBottomY}, keyBoardTopY:${keyBoardTopY}")
            Logger.log(
                "键盘高度: $keyBoardHeight, 屏幕高度: ${activity.resources.displayMetrics.heightPixels}，activty高度:${activityDecorView!!.height}," +
                        "dialogWindow高度：${dialogWindow.decorView.height}," +
                        " windowInScreenLocation:${
                            windowInScreenLocation.joinToString(
                                ",",
                            )
                        } ",
            )
            if (windowBottomY > keyBoardTopY) { // 当弹窗底部在键盘顶部的下方，才需要去移动dialogWindow

//                animateWindowYPosition(dialogWindow, initDialogWindowTopY) // - (windowBottomY - keyBoardTopY))
            }
        } else {
//            animateWindowYPosition(dialogWindow, windowInScreenLocation[1])
        }
    }

    private fun animateWindowYPosition(
        window: Window,
        targetY: Int,
        duration: Long = 200,
    ) {
        // 获取当前窗口的LayoutParams
        val params = window.attributes

        if (params.gravity == initGravity) {
            // 第一次的时候，锚点改成以左上角
            params.gravity = Gravity.TOP or Gravity.START
            // WindowManager.LayoutParams的x和y都是根据锚点去做偏移。比如gravity是Gravity.Bottom，y=-100，表示从底部上移100px
            params.x = windowInScreenLocation[0]
            params.y = windowInScreenLocation[1]
            window.attributes = params
        }

        // 保存当前的y值
        val startY = params.y
        if (startY == targetY) {
            return
        }

        windowYPositionAnimator?.cancel()
        // 创建值动画
        windowYPositionAnimator =
            ValueAnimator.ofInt(startY, targetY).apply {
                this.duration = duration
                interpolator = AccelerateDecelerateInterpolator()
                addUpdateListener { animation ->
                    // 在动画更新时，更新y值并设置回窗口
                    val animatedValue = animation.animatedValue as Int
                    params.gravity = Gravity.TOP or Gravity.START
                    params.y = animatedValue
                    window.attributes = params
                }
            }
        windowYPositionAnimator!!.start()
    }

    fun detach() {
        activityDecorView!!.removeCallbacks(moveRunnable)
        if (activityDecorView!!.getViewTreeObserver().isAlive) {
            activityDecorView!!.getViewTreeObserver().removeOnGlobalLayoutListener(this)
        }
        if (activityDecorView!!.viewTreeObserver.isAlive) {
            activityDecorView!!.viewTreeObserver.removeOnGlobalFocusChangeListener(this)
        }
    }
}
