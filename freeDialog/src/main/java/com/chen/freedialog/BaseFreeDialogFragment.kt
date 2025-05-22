package com.chen.freedialog

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.Point
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import androidx.viewbinding.ViewBinding
import com.chen.freedialog.config.AnchorGravity
import com.chen.freedialog.config.DialogAnim
import com.chen.freedialog.utils.ScreenUtil

/**
 * 基类。可以定位到某个View[类似PopupWindow],也可以是正常dialog
 * 为什么不用popupWindow和其他自定义dialog呢？是因为他们都只能在activity层面展示，当在DialogFragment内部时，则会被覆盖了，不好用
 */
abstract class BaseFreeDialogFragment<VB : ViewBinding?> : DialogFragment() {
    /**
     * 获取ViewBinding实例
     */
    private var vBinding: VB? = null
    protected val binding: VB get() = vBinding!!

    /**
     * 基本配置表
     */
    protected val dialogConfig = DialogConfigs()

    /**
     * 依赖于某个View时使用，有View时会修改configs中的配置
     */
    private var anchorView: View? = null

    /**
     * 外部传入的dialog内部viewId触发的获取的可拖拽整个dialog的touchView
     */
    private var dragView: View? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.BaseDialogFragment)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        vBinding = getViewBinding(inflater, container)
        return vBinding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView(binding)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        vBinding = null
    }

    /**
     * onStart 的情况下，dialogFragment才算真正准备好，
     * 所以在这里处理定位刚刚好，尺寸啥的就可以在onViewCreated中设置了
     */
    override fun onStart() {
        super.onStart()
        val window = dialog!!.window
        if (window != null) {
            //获取状态栏高度和底部栏高度
            dialogConfig.statusHeight = ScreenUtil.getStatusHeight(resources)
            dialogConfig.navBarHeight = ScreenUtil.getNavBarHeight(resources)

            window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            window.setDimAmount(dialogConfig.dimAmount)
            // 沉浸式状态栏（API 21+）,加上，避免dialogFragment被其他dialogFragment覆盖时，状态栏冒出来
            window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            // 动画
            setAnimation(window)
            // 拖拽事件
            setDragTouchEvent(window)
        }
        // 如果需要定位到某个View
        if (dialogConfig.isAttachedToAnchor && anchorView != null) {
            positionDialogRelativeToAnchor(window)
        } else {
            // 否则使用默认的Dialog布局参数
            setupDefaultDialogWindow(window)
        }
    }

    /**
     * 设置对话框相对于某个View的位置
     *
     * @param anchorView 锚点View
     * @param anchorGravity  详细看：AnchorGravity
     * @param offsetX    水平偏移
     * @param offsetY    垂直偏移
     */
    fun setAnchor(anchorView: View?) {
        this.anchorView = anchorView
        dialogConfig.anchorViewId = anchorView?.id ?: 0
        dialogConfig.isAttachedToAnchor = true
    }

    /**
     * 定位对话框相对于锚点View
     * 只要确保anchorView是早就在布局中的视图，那么这里位置的计算就不用延时处理了
     */
    private fun positionDialogRelativeToAnchor(window: Window?) {
        if (window == null) return

        // 获取锚点View的左上位置
        val anchorLocation = IntArray(2)
        // 不使用getLocationInWindow，可能因为窗口原因导致位置不准的
        anchorView!!.getLocationOnScreen(anchorLocation)

        // 存起来，外部需要调用可以用
        dialogConfig.location[0] = anchorLocation[0]
        dialogConfig.location[1] = anchorLocation[1]

        // 横屏
        val isLandscape = ScreenUtil.isLandscape(resources)

        val statusBarHeight = dialogConfig.statusHeight
        val navBarHeight = dialogConfig.navBarHeight

        // 横屏时xy坐标是反过来的，原本的左右变为上下了，所以得处理一下
        val anchorRect = if (isLandscape) {
            Rect(
                anchorLocation[0] - statusBarHeight, // 关键：转换为窗口坐标系
                anchorLocation[1],
                anchorLocation[0] + anchorView!!.width - statusBarHeight,
                anchorLocation[1] + anchorView!!.height,
            )
        } else {
            Rect(
                anchorLocation[0],
                anchorLocation[1] - statusBarHeight, // 关键：转换为窗口坐标系
                anchorLocation[0] + anchorView!!.width,
                anchorLocation[1] + anchorView!!.height - statusBarHeight,
            )
        }

        // 如果getLocationOnScreen有不准确的地方或者需要再滚动中获取的话，
        // 考虑getGlobalVisibleRect 自动包含所有变换和滚动偏移,看看是否需要吧，再说
        // 似乎在dialogFragment包裹着dialogFragment中横屏时不太准确，这个方式
        // val anchorLocation = Rect().apply {
        //     anchorView!!.getGlobalVisibleRect(this)
        //     // 转换为窗口坐标系（扣除状态栏）
        //     top -= statusBarHeight
        //     bottom -= statusBarHeight
        // }

        // 获取对话框的宽高， 一般测量到的都是warp宽度
        window.decorView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)

        // 这里一定要是measured的值，否则不准确,测量到的好像都是warp的值,测量不是很准，所以如果外部有固定值，则使用
        val dialogWidth = getRealDialogWidth(window.decorView.measuredWidth)
        val dialogHeight = getRealDialogHeight(window.decorView.measuredHeight)

        // 计算对话框位置
        val dialogPosition = calculateDialogCoordinates(anchorRect, dialogWidth, dialogHeight, statusBarHeight, navBarHeight)

        // 设置对话框位置
        val params = window.attributes
        params.gravity = Gravity.TOP or Gravity.START // 必须
        params.x = dialogPosition.x
        params.y = dialogPosition.y

        setWidthAndHeightAndOther(params, dialogWidth, dialogHeight)
        window.attributes = params
    }

    /**
     * 计算对话框位置,计算方式二，使用AnchorGravity入参
     */
    private fun calculateDialogCoordinates(
        anchorRect: Rect,
        dialogWidth: Int, dialogHeight: Int,
        statusBarHeight: Int, navBarHeight: Int,
    ): Point {
        val result = Point()
        // 屏幕宽高
        val screenWidth = resources.displayMetrics.widthPixels
        val screenHeight = resources.displayMetrics.heightPixels

        val yGravity: Int = dialogConfig.anchorGravity and 0xf0 // 获取前4位 得到y轴
        val xGravity: Int = dialogConfig.anchorGravity and 0x0f // 获取后4位 得到x轴

        // 计算实际可用屏幕区域（排除系统UI），不用了
        // val safeScreenHeight = screenHeight - statusBarHeight - navBarHeight

        // anchorRect中包含了去除statusBarHeight，所以这里不用处理了
        // 处理y轴
        when (yGravity) {
            AnchorGravity.TOP -> {
                result.y = Math.max(0, anchorRect.top + dialogConfig.offsetY - dialogHeight)

                //  // 如果上方空间不足，自动切换到下方，不准确，放弃了先
                //  if (result.y < statusBarHeight) {
                //      result.y = Math.max(0, anchorRect.bottom + dialogConfig.offsetY)
                //  }

            }

            AnchorGravity.CENTER_VERTICAL -> {
                result.y = Math.max(0, anchorRect.top + dialogConfig.offsetY - (dialogHeight - anchorRect.height() / 2))
            }

            else -> { // BOTTOM
                result.y = Math.max(0, anchorRect.bottom + dialogConfig.offsetY)

                // 如果下方空间不足，自动切换到上方,不准确，放弃了先
                //  if (result.y + dialogHeight > screenHeight) {
                //      result.y = Math.max(0, anchorRect.top - anchorRect.height() + dialogConfig.offsetY - dialogHeight)
                //  }

            }
        }

        // 处理x轴
        when (xGravity) {
            AnchorGravity.ALIGN_LEFT -> {
                result.x = Math.max(0, anchorRect.left + dialogConfig.offsetX)
            }

            AnchorGravity.ALIGN_RIGHT -> {
                result.x = Math.max(0, anchorRect.right + dialogConfig.offsetX - dialogWidth)
            }

            AnchorGravity.LEFT -> {
                result.x = Math.max(0, anchorRect.left + dialogConfig.offsetX - dialogWidth)
            }

            AnchorGravity.RIGHT -> {
                result.x = Math.max(0, anchorRect.right + dialogConfig.offsetX)
            }

            else -> { // center_horizontal
                val centerX = anchorRect.left + anchorRect.width() / 2
                result.x = Math.max(0, centerX - dialogWidth / 2 + dialogConfig.offsetX)
            }
        }

        // 最终边界修正（考虑安全区域）
        result.x = result.x.coerceIn(0, screenWidth - dialogWidth)
        result.y = result.y.coerceIn(statusBarHeight, screenHeight - dialogHeight)

        return result
    }

    /**
     * 设置默认的对话框窗口参数,如果有拖拽的viewId，则特殊处理
     * 如果有拖拽的viewId，则特殊处理,通过计算来居中
     */
    private fun setupDefaultDialogWindow(window: Window?) {
        if (window != null) {
            // 屏幕宽高
            val screenWidth = resources.displayMetrics.widthPixels
            val screenHeight = resources.displayMetrics.heightPixels

            val params = window.attributes
            // 获取对话框的宽高
            window.decorView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
            // 一般测量到的都是warp宽度
            val dialogWidth = getRealDialogWidth(window.decorView.measuredWidth)
            val dialogHeight = getRealDialogHeight(window.decorView.measuredHeight)
            /**
             * 如果有拖拽的viewId，则特殊处理,通过计算来居中，初始为中间，有预置的值就用预置的，无则计算居中
             */
            if (dialogConfig.dragViewId != 0) {
                // 有拖拽view,则必须如此这句
                params.gravity = Gravity.TOP or Gravity.START

                params.x = if (dialogConfig.offsetX == 0) {
                    (screenWidth - dialogWidth) / 2
                } else dialogConfig.offsetX

                params.y = if (dialogConfig.offsetY == 0) {
                    (screenHeight - dialogHeight) / 2
                } else dialogConfig.offsetY

            } else {
                params.gravity = dialogConfig.defaultGravity
            }
            setWidthAndHeightAndOther(params, dialogWidth, dialogHeight)
            window.attributes = params
        }
    }

    /**
     * 通过测量的宽度传入获取更准确真实的宽度，避免有些测量不准确
     */
    private fun getRealDialogWidth(dialogWidth: Int): Int {
        return if (dialogConfig.fixWidth != ViewGroup.LayoutParams.WRAP_CONTENT) {
            dialogConfig.fixWidth
        } else {
            // dialogWidth合理，一般也是warp，宽度这里合理
            if (dialogWidth > dialogConfig.minWidth) {
                dialogWidth
            } else {
                dialogConfig.minWidth
            }
        }
    }

    /**
     * 通过测量的高度传入获取更准确真实的高度，避免有些测量不准确
     */
    private fun getRealDialogHeight(dialogHeight: Int): Int {
        return if (dialogConfig.fixHeight != ViewGroup.LayoutParams.WRAP_CONTENT) {
            dialogConfig.fixHeight
        } else {
            if (dialogHeight > dialogConfig.minHeight) {
                // 高度要绝对warp,不适用dialogHeight，可能高于实际高度，这点不同于dialogWidth
                ViewGroup.LayoutParams.WRAP_CONTENT
            } else {
                dialogConfig.minHeight
            }
        }
    }

    /**
     * 设置宽高 和一些其他共同配置
     * 如果没有设置固定宽度时，而设置了最小宽度，当弹框实际warp内容大于就是变为warp，同理高度一样
     * dialogRealWidth:真是宽度
     * dialogRealHeight：真实高度，外部都确定好了
     */
    private fun setWidthAndHeightAndOther(params: WindowManager.LayoutParams, dialogRealWidth: Int, dialogRealHeight: Int) {
        // 宽高需要重新处理，不然就是默认warp的,因为VB的缘故
        params.width = dialogRealWidth
        params.height = dialogRealHeight

        // 是否拦截外部触摸事件
        if (dialogConfig.isInterceptOutSideEvent) {
            params.flags = params.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL.inv() // 拦截外部事件
        } else {
            params.flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL // 不拦截外部事件
        }

        // 附加多一个判断
        requireDialog().setCanceledOnTouchOutside(dialogConfig.isTouchOutSideCancelable)

        // 是否可以点击外部取消
        requireDialog().setCancelable(dialogConfig.isCancelable)

    }

    /**
     * 拖拽事件的实现
     * 不建议用requireView(),不要崩溃了
     *
     * 若控件左上角在屏幕中的坐标为(left, top)，则：
     * rawX=left+X
     * rawY=top+Y
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun setDragTouchEvent(window: Window) {
        dragView = view?.findViewById(dialogConfig.dragViewId)
        dragView?.apply {
            // 屏幕宽高
            val screenWidth = resources.displayMetrics.widthPixels
            val screenHeight = resources.displayMetrics.heightPixels

            val dialogWidth = window.decorView.measuredWidth
            val dialogHeight = window.decorView.measuredHeight

            var initialX = 0
            var initialY = 0

            var initialTouchX = 0f
            var initialTouchY = 0f

            this.setOnTouchListener { v, event ->
                if (!dialogConfig.canDragWhenHasDragViewId) {
                    return@setOnTouchListener false
                }
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        // 记录初始位置
                        initialX = window.attributes.x
                        initialY = window.attributes.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        true
                    }

                    MotionEvent.ACTION_MOVE -> {
                        // 计算移动距离并更新窗口位置
                        val x = initialX + (event.rawX - initialTouchX).toInt()
                        val y = initialY + (event.rawY - initialTouchY).toInt()

                        // 更新到窗口,有了dragViewId，上面已经处理了 params.gravity了，这里不用处理了
                        val params = window.attributes
                        params.x = x
                        params.y = y

                        // 最终边界修正（考虑安全区域）
                        params.x = x.coerceIn(0, screenWidth - dialogWidth)
                        params.y = y.coerceIn(dialogConfig.statusHeight, screenHeight - dialogHeight)

                        window.attributes = params
                        true
                    }

                    else -> false
                }
            }
        }
    }

    /**
     * 设置展示动画
     */
    private fun setAnimation(window: Window) {
        when (dialogConfig.showAnimation) {
            DialogAnim.DialogTopToBottom -> {
                window.setWindowAnimations(R.style.DialogPositionTopToBottom)
            }

            DialogAnim.DialogBottomToTop -> {
                window.setWindowAnimations(R.style.DialogPositionBottomToTop)
            }

            DialogAnim.DialogLeftToRight -> {
                window.setWindowAnimations(R.style.DialogPositionLeftToRight)
            }

            DialogAnim.DialogRightToLeft -> {
                window.setWindowAnimations(R.style.DialogPositionRightToLeft)
            }

            DialogAnim.DialogDefault -> {
                // 默认不用赋值，系统默认即可
            }
        }
    }

    /**
     * 抽象方法，子类必须实现
     */
    protected abstract fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?): VB

    protected abstract fun initView(binding: VB)

}
