package com.godox.common.kdtablayout.widget

import android.graphics.Canvas

/**
 * Created By：XuQK
 * Created Date：3/1/20 2:56 PM
 * Description：
 */
abstract class KDBadge(protected val tab: KDTab) {
    protected var show: Boolean = true

    open fun dismiss() {
        if (!show) return
        show = false
    }

    open fun show() {
        show = true
        tab.invalidate()
    }

    abstract fun draw(canvas: Canvas)
}
