package com.chen.freedialog.utils

import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat


/**
 * Author: ChenBabys
 * Date: 2025/5/14
 * Description: 屏幕工具类，
 *
 *          鉴于使用：
 *               val decorView = window.decorView
 *               val windowInsets = ViewCompat.getRootWindowInsets(decorView)
 *               val insets = windowInsets?.getInsetsIgnoringVisibility(WindowInsetsCompat.Type.systemBars())
 *               // 获取状态栏和导航栏高度（兼容新旧API）
 *               val statusBarHeight = insets?.top ?: 0
 *               val navBarHeight = insets?.bottom ?: 0
 *
 *               以上代码获取不到格子高度，所以增设这个工具类吧，单独获取
 *
 */
object ScreenUtil {

    /**
     * 最保险的获取状态栏高度的方法
     * 传入context.resources
     */
    fun getStatusHeight(resources: Resources): Int {
        val resourceStatusId = resources.getIdentifier("status_bar_height", "dimen", "android")
        val statusBarHeight = resources.getDimensionPixelSize(resourceStatusId)
        return statusBarHeight
    }

    /**
     * 最保险的获取底部栏高度
     * 传入context.resources
     */
    fun getNavBarHeight(resources: Resources): Int {
        val resourceNavId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
        val navBarHeight = resources.getDimensionPixelSize(resourceNavId)
        return navBarHeight
    }

    /**
     * 检查状态栏有没有被隐藏
     */
    fun isStatusBarHidden(decorView: View): Boolean {
        val isHidden = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            ViewCompat.getRootWindowInsets(decorView)?.isVisible(WindowInsetsCompat.Type.statusBars()) == false
        } else {
            (decorView.systemUiVisibility and View.SYSTEM_UI_FLAG_FULLSCREEN) != 0
        }
        return isHidden
    }

    /**
     * 判断是否是横屏
     */
    fun isLandscape(resources: Resources): Boolean {
        return resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    }

    /**
     * dp转px
     *
     * @param dpValue
     * @return
     */
    fun dp2px(resources: Resources, dpValue: Float): Int {
        val scale: Float = resources.displayMetrics.density
        return (dpValue * scale + 0.5f).toInt()
    }

}
