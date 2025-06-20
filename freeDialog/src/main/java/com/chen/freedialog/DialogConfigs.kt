package com.chen.freedialog

import android.os.Parcel
import android.os.Parcelable
import android.os.Parcelable.Creator
import android.view.Gravity
import android.view.ViewGroup
import com.chen.freedialog.config.AnchorGravity
import com.chen.freedialog.config.DialogAnim

/**
 * 弹框的配置项，
 * 有些字段没实现的，先保留，后面逐渐丰富这个自定义组件
 */
class DialogConfigs : Parcelable {
    /**
     * 背景透明度，0透明，1完成不透明
     * 默认一半透明度
     */
    @JvmField
    var dimAmount: Float = 0.5f // 遮罩层透明度

    @JvmField
    var location: IntArray = IntArray(2)

    @JvmField
    var navBarHeight: Int = 0

    @JvmField
    var statusHeight: Int = 0 // 缓存状态栏

    @JvmField
    var offsetX: Int = 0

    @JvmField
    var offsetY: Int = 0 // 相对于view的x轴y轴偏移位置

    @JvmField
    var anchorViewId: Int = 0 // 依附的view

    @JvmField
    var anchorGravity: Int = AnchorGravity.BOTTOM

    /**
     * 是否可以取消，返回触发之类
     */
    @JvmField
    var isCancelable: Boolean = true

    /**
     * 键盘方式
     */
    @JvmField
    var softMode: Int = 0

    /**
     * 是否可以拖拽dragViewId之后都可以拖拽,具备了,当需要暂停一段时间拖拽时，可用
     * 默认可以拖拽的
     */
    @JvmField
    var canDragWhenHasDragViewId: Boolean = true

    /**
     * 可以拖拽的View的id
     */
    @JvmField
    var dragViewId: Int = 0

    /**
     * 长按触发时间，这个占时没用到
     */
    @JvmField
    var timeMillis: Int = 300

    @JvmField
    var isLongClickModule: Boolean = false

    @JvmField
    var lastX: Float = 0f

    @JvmField
    var lastY: Float = 0f

    @JvmField
    var xDown: Float = 0f

    @JvmField
    var yDown: Float = 0f

    @JvmField
    var style: Int = 0

    @JvmField
    var isAttachedToAnchor: Boolean = false

    /**
     * 默认的位置，当有anchorGravity去定位时，则优先那边的值
     * 如果有拖拽viewId,那么这个属性设置的值失效，则默认左上角了
     */
    @JvmField
    var defaultGravity: Int = Gravity.CENTER

    /**
     * 返回最小宽度
     * 因为使用了vb的缘故，所以宽高由外部代码处设置更好
     */
    @JvmField
    var minWidth: Int = ViewGroup.LayoutParams.WRAP_CONTENT

    /**
     * 返回最小高度
     * 因为使用了vb的缘故，所以宽高由外部代码处设置更好
     */
    @JvmField
    var minHeight: Int = ViewGroup.LayoutParams.WRAP_CONTENT

    /**
     * 设置固定宽度，设置了固定宽度，最小宽度就无效了
     * 因为使用了vb的缘故，所以宽高由外部代码处设置更好
     */
    @JvmField
    var fixWidth: Int = ViewGroup.LayoutParams.WRAP_CONTENT

    /**
     * 设置固定高度，设置了固定高度，最小高度就无效了
     * 因为使用了vb的缘故，所以宽高由外部代码处设置更好
     */
    @JvmField
    var fixHeight: Int = ViewGroup.LayoutParams.WRAP_CONTENT

    /**
     * 点击外部是否可以取消弹框,默认可以
     */
    @JvmField
    var isTouchOutSideCancelable: Boolean = true

    /**
     * 是否拦截外部的触摸事件,默认拦截
     */
    @JvmField
    var isInterceptOutSideEvent: Boolean = true

    /**
     * 动画,默认淡出淡入
     */
    @JvmField
    var showAnimation: Int = DialogAnim.DialogDefault

    /**
     * 使window的底部对齐输入法的顶部，否则使window内的焦点视图对齐输入法的顶部
     */
    var attachSoftInputByWindowBottom: Boolean = true

    /**
     * 是否适配输入法
     */
    var softInputAdaptive: Boolean = false

    constructor()

    private constructor(p: Parcel) {
        dimAmount = p.readFloat()
        location = p.createIntArray()!!
        navBarHeight = p.readInt()
        statusHeight = p.readInt()
        offsetX = p.readInt()
        offsetY = p.readInt()
        anchorViewId = p.readInt()
        anchorGravity = p.readInt()
        isCancelable = p.readByte().toInt() != 0
        softMode = p.readInt()
        canDragWhenHasDragViewId = p.readByte().toInt() != 0
        dragViewId = p.readInt()
        timeMillis = p.readInt()
        isLongClickModule = p.readByte().toInt() != 0
        lastX = p.readFloat()
        lastY = p.readFloat()
        xDown = p.readFloat()
        yDown = p.readFloat()
        style = p.readInt()
        isAttachedToAnchor = p.readByte().toInt() != 0
        defaultGravity = p.readInt()
        minWidth = p.readInt()
        minHeight = p.readInt()
        fixWidth = p.readInt()
        fixHeight = p.readInt()
        isTouchOutSideCancelable = p.readByte().toInt() != 0
        isInterceptOutSideEvent = p.readByte().toInt() != 0
        showAnimation = p.readInt()
        attachSoftInputByWindowBottom = p.readByte().toInt() != 0
        softInputAdaptive = p.readByte().toInt() != 0
    }

    override fun describeContents(): Int = 0

    override fun writeToParcel(
        dest: Parcel,
        flags: Int,
    ) {
        dest.writeFloat(dimAmount)
        dest.writeIntArray(location)
        dest.writeInt(navBarHeight)
        dest.writeInt(statusHeight)
        dest.writeInt(offsetX)
        dest.writeInt(offsetY)
        dest.writeInt(anchorViewId)
        dest.writeInt(anchorGravity)
        dest.writeByte((if (isCancelable) 1 else 0).toByte())
        dest.writeInt(softMode)
        dest.writeByte((if (canDragWhenHasDragViewId) 1 else 0).toByte())
        dest.writeInt(dragViewId)
        dest.writeInt(timeMillis)
        dest.writeByte((if (isLongClickModule) 1 else 0).toByte())
        dest.writeFloat(lastX)
        dest.writeFloat(lastY)
        dest.writeFloat(xDown)
        dest.writeFloat(yDown)
        dest.writeInt(style)
        dest.writeByte((if (isAttachedToAnchor) 1 else 0).toByte())
        dest.writeInt(defaultGravity)
        dest.writeInt(minWidth)
        dest.writeInt(minHeight)
        dest.writeInt(fixWidth)
        dest.writeInt(fixHeight)
        dest.writeByte((if (isTouchOutSideCancelable) 1 else 0).toByte())
        dest.writeByte((if (isInterceptOutSideEvent) 1 else 0).toByte())
        dest.writeInt(showAnimation)
        dest.writeByte((if (attachSoftInputByWindowBottom) 1 else 0).toByte())
        dest.writeByte((if (softInputAdaptive) 1 else 0).toByte())
    }

    companion object CREATOR : Creator<DialogConfigs> {
        override fun createFromParcel(parcel: Parcel): DialogConfigs = DialogConfigs(parcel)

        override fun newArray(size: Int): Array<DialogConfigs?> = arrayOfNulls(size)
    }
}
