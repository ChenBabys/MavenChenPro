package com.chen.freedialog

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.Point
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.Window
import android.view.WindowManager
import android.view.WindowManager.LayoutParams
import android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.PopupWindow
import androidx.annotation.RequiresApi
import androidx.fragment.app.DialogFragment
import androidx.viewbinding.ViewBinding
import com.chen.freedialog.config.AnchorGravity
import com.chen.freedialog.config.DialogAnim
import com.chen.freedialog.config.SwipeDirection
import com.chen.freedialog.utils.ScreenUtil

/**
 * 基类。可以定位到某个View类似[PopupWindow],也可以是正常dialog
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

    // private var softInputHelper: SoftInputHelper? = null

    /**
     * 滑动关闭动画
     */
    private var swipeDismissAnimator: ValueAnimator? = null

    private val windowFlagCompat by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowFlagCompat.Api30Impl()
        } else {
            WindowFlagCompat.BeforeApi30Impl()
        }
    }

    open fun getDialogStyle(): Int = R.style.BaseDialogFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, getDialogStyle())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        vBinding = getViewBinding(inflater, container)
        return vBinding!!.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        initView(binding)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        swipeDismissAnimator?.cancel()
        swipeDismissAnimator = null
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
            // 获取状态栏高度和底部栏高度
            dialogConfig.statusHeight = ScreenUtil.getStatusHeight(resources)
            dialogConfig.navBarHeight = ScreenUtil.getNavBarHeight(resources)

            window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            window.setDimAmount(dialogConfig.dimAmount)
            // 沉浸式状态栏（API 21+）,加上，避免dialogFragment被其他dialogFragment覆盖时，状态栏冒出来
            // window.setFlags(LayoutParams.FLAG_FULLSCREEN, LayoutParams.FLAG_FULLSCREEN)
            // View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN 允许内容延伸到状态栏后面
            // View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY 当用户交互后，状态栏会自动隐藏
            // window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            // window.decorView.fitsSystemWindows = false
            // (window.decorView as ViewGroup).getChildAt(0)?.fitsSystemWindows = false

            // WindowInsetsControllerCompat(window, window.decorView).apply {
            //     hide(WindowInsetsCompat.Type.systemBars())
            // }

            //  ScreenUtil.getCutoutGravity(requireActivity().window)

            // 跟随Activity的window，如果其状态栏是隐藏的，那么我们的window就设置为全屏
            if (ScreenUtil.isStatusBarHidden(requireActivity().window.decorView)) {
                // 全屏，隐藏状态栏和导航栏
                window.setFlags(LayoutParams.FLAG_FULLSCREEN, LayoutParams.FLAG_FULLSCREEN)
            }

            windowFlagCompat.setupFlag(window.attributes, dialogConfig)
            // 动画
            setAnimation(window)
            // 拖拽事件
            setDragTouchEvent(window)
            // 手势滑动关闭
            setSwipeToDismiss(window)
        }

        // 如果需要定位到某个View(这是固定的)
        val isNeedAttachedToAnchor = dialogConfig.isAttachedToAnchor && anchorView != null
        if (isNeedAttachedToAnchor) {
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

        val statusBarHeight = if (ScreenUtil.isStatusBarHidden(requireActivity().window.decorView)) 0 else dialogConfig.statusHeight
        val navBarHeight = dialogConfig.navBarHeight

        // 横屏时xy坐标是反过来的，原本的左右变为上下了，所以得处理一下
        val anchorRect = Rect(
            anchorLocation[0], // 关键：转换为窗口坐标系
            anchorLocation[1] - statusBarHeight,
            anchorLocation[0] + anchorView!!.width,
            anchorLocation[1] + anchorView!!.height - statusBarHeight,
        )

        // 先让其不可见，等测量到高度再让其可见
        window.decorView.visibility = View.INVISIBLE

        window.decorView.viewTreeObserver.addOnGlobalLayoutListener(
            object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    window.decorView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    window.decorView.visibility = View.VISIBLE
                    // 这里获取到真实的宽高，才去计算对话框的位置
                    val dialogWidth = window.decorView.measuredWidth
                    val dialogHeight = window.decorView.measuredHeight

                    // 计算对话框位置
                    val dialogPosition = calculateDialogCoordinates(anchorRect, dialogWidth, dialogHeight, statusBarHeight, navBarHeight)

                    // 设置对话框位置
                    val params = window.attributes
                    params.gravity = Gravity.TOP or Gravity.START // 必须
                    params.x = dialogPosition.x
                    params.y = dialogPosition.y
                    window.attributes = params
                }
            },
        )

        // 获取对话框的宽高， 一般测量到的都是warp宽度
        window.decorView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        val dialogWidth = getRealDialogWidth(window.decorView.measuredWidth)
        val dialogHeight = getRealDialogHeight(window.decorView.measuredHeight)
        // 设置对话框宽高及其他
        val params = window.attributes
        setWidthAndHeightAndOther(params, dialogWidth, dialogHeight)
        window.attributes = params
    }

    /**
     * 计算对话框位置,计算方式二，使用AnchorGravity入参
     */
    private fun calculateDialogCoordinates(
        anchorRect: Rect,
        dialogWidth: Int,
        dialogHeight: Int,
        statusBarHeight: Int,
        navBarHeight: Int,
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
        result.y = result.y.coerceIn(0, screenHeight - dialogHeight - statusBarHeight)

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

            // 如果有拖拽的viewId，则特殊处理,通过计算来居中，初始为中间，有预置的值就用预置的，无则计算居中
            if (dialogConfig.dragViewId != 0) {
                // 有拖拽view,则必须如此这句
                params.gravity = Gravity.TOP or Gravity.START

                params.x = if (dialogConfig.offsetX == 0) {
                    (screenWidth - dialogWidth) / 2
                } else {
                    dialogConfig.offsetX
                }

                params.y = if (dialogConfig.offsetY == 0) {
                    (screenHeight - dialogHeight) / 2
                } else {
                    dialogConfig.offsetY
                }
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
    private fun getRealDialogWidth(dialogWidth: Int): Int =
        if (dialogConfig.fixWidth != ViewGroup.LayoutParams.WRAP_CONTENT) {
            dialogConfig.fixWidth
        } else {
            // dialogWidth合理，一般也是warp，宽度这里合理
            if (dialogWidth > dialogConfig.minWidth) {
                dialogWidth
            } else {
                dialogConfig.minWidth
            }
        }

    /**
     * 通过测量的高度传入获取更准确真实的高度，避免有些测量不准确
     */
    private fun getRealDialogHeight(dialogHeight: Int): Int =
        if (dialogConfig.fixHeight != ViewGroup.LayoutParams.WRAP_CONTENT) {
            dialogConfig.fixHeight
        } else {
            if (dialogHeight > dialogConfig.minHeight) {
                // 高度要绝对warp,不适用dialogHeight，可能高于实际高度，这点不同于dialogWidth
                ViewGroup.LayoutParams.WRAP_CONTENT
            } else {
                dialogConfig.minHeight
            }
        }

    /**
     * 设置宽高 和一些其他共同配置
     * 如果没有设置固定宽度时，而设置了最小宽度，当弹框实际warp内容大于就是变为warp，同理高度一样
     * dialogRealWidth:真是宽度
     * dialogRealHeight：真实高度，外部都确定好了
     * 总结一下，浮动需要wrap宽高，非浮动需要match宽高，并且会受到主题中windowIsFloating属性的影响
     */
    private fun setWidthAndHeightAndOther(
        params: LayoutParams,
        dialogRealWidth: Int,
        dialogRealHeight: Int,
    ) {
        // 宽高需要重新处理，不然就是默认warp的,因为VB的缘故
        params.width = dialogRealWidth
        params.height = dialogRealHeight

        // 是否拦截外部触摸事件
        if (dialogConfig.isInterceptOutSideEvent) {
            params.flags = params.flags and LayoutParams.FLAG_NOT_TOUCH_MODAL.inv() // 拦截外部事件
        } else {
            params.flags = params.flags or LayoutParams.FLAG_NOT_TOUCH_MODAL // 不拦截外部事件
        }

        // 将窗口设置为不针对显示的输入法进行调整。窗口的尺寸不会改变,并且不会移动以显示其焦点。
        // 设置这个属性之后，还得确保主题那边的windowIsFloating和windowIsTranslucent（可选）都设置为false才可生效
        if (dialogConfig.softInputAdjustNothing) {
            params.softInputMode = LayoutParams.SOFT_INPUT_ADJUST_NOTHING
            // 确保内容可以延伸到状态栏后面
            params.flags = params.flags or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            // 强制窗口使用整个屏幕区域进行布局,这一句是关键，可以达到windowIsFloating = false的效果
            params.flags = params.flags or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        }

        if (params.width != ViewGroup.LayoutParams.MATCH_PARENT && params.height != ViewGroup.LayoutParams.MATCH_PARENT) {
            // 只要不是宽高都铺满，就设置将window放置在整个屏幕，忽略父窗口的任何约束
            dialog?.window?.addFlags(FLAG_LAYOUT_NO_LIMITS)
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
            val statusBarHeight = if (ScreenUtil.isStatusBarHidden(requireActivity().window.decorView)) 0 else dialogConfig.statusHeight

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

                        val dialogWidth = window.decorView.measuredWidth
                        val dialogHeight = window.decorView.measuredHeight

                        // 更新到窗口,有了dragViewId，上面已经处理了 params.gravity了，这里不用处理了
                        val params = window.attributes
                        // 最终边界修正（考虑安全区域）
                        params.x = x.coerceIn(0, screenWidth - dialogWidth)
                        params.y = y.coerceIn(0, screenHeight - dialogHeight - statusBarHeight)

                        window.attributes = params
                        true
                    }

                    else -> {
                        false
                    }
                }
            }
        }
    }

    /**
     * 设置滑动关闭弹框
     * 注意：setOnTouchListener 是替换操作，不是添加操作，注意别被覆盖了
     * 若dragViewId不空，则取消这个swipe事件，以drag偏业务类型的功能优先
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun setSwipeToDismiss(window: Window) {
        if (!dialogConfig.swipeToDismissEnabled || dialogConfig.dragViewId != 0) {
            return
        }

        /**
         * 手势滑动相关字段
         */
        var initialTouchY = 0f
        var initialTouchX = 0f
        var isDragging = false
        var dragPercent = 0f

        /**
         * 更新整个页面视图位置
         */
        fun updateViewPosition(percent: Float) {
            view?.let {
                when (dialogConfig.swipeDirection) {
                    SwipeDirection.SWIPE_DIRECTION_DOWN -> it.translationY = percent * it.height
                    SwipeDirection.SWIPE_DIRECTION_UP -> it.translationY = -percent * it.height
                    SwipeDirection.SWIPE_DIRECTION_LEFT -> it.translationX = -percent * it.width
                    SwipeDirection.SWIPE_DIRECTION_RIGHT -> it.translationX = percent * it.width
                    else -> {
                        // 对于任意方向，同时移动X和Y
                        it.translationX = 0f
                        it.translationY = percent * it.height
                    }
                }

                // 添加透明度变化效果
                if (dialogConfig.swipeDismissChangeAlpha) {
                    it.alpha = 1f - (percent * 0.5f)
                }
            }
        }

        /**
         * 处理拖动事件
         */
        fun handleDrag(event: MotionEvent) {
            val deltaY = event.rawY - initialTouchY
            val deltaX = event.rawX - initialTouchX

            // 根据方向计算拖动百分比
            dragPercent = when (dialogConfig.swipeDirection) {
                SwipeDirection.SWIPE_DIRECTION_DOWN -> deltaY / (view?.height ?: 1)
                SwipeDirection.SWIPE_DIRECTION_UP -> -deltaY / (view?.height ?: 1)
                SwipeDirection.SWIPE_DIRECTION_LEFT -> -deltaX / (view?.width ?: 1)
                SwipeDirection.SWIPE_DIRECTION_RIGHT -> deltaX / (view?.width ?: 1)
                else -> {
                    // 对于任意方向，使用最大的拖动百分比
                    Math.max(Math.abs(deltaY) / (view?.height ?: 1), Math.abs(deltaX) / (view?.width ?: 1))
                }
            }.coerceIn(0f, 1f)

            // 更新视图位置
            updateViewPosition(dragPercent)
        }

        /**
         * 计算滑动速度
         */
        fun calculateVelocity(event: MotionEvent): Float {
            val deltaTime = event.eventTime - event.downTime
            if (deltaTime <= 0) return 0f

            val deltaY = event.rawY - initialTouchY
            val deltaX = event.rawX - initialTouchX

            return when (dialogConfig.swipeDirection) {
                SwipeDirection.SWIPE_DIRECTION_DOWN,
                SwipeDirection.SWIPE_DIRECTION_UP -> Math.abs(deltaY / deltaTime) * 1000

                SwipeDirection.SWIPE_DIRECTION_LEFT,
                SwipeDirection.SWIPE_DIRECTION_RIGHT -> Math.abs(deltaX / deltaTime) * 1000

                else -> Math.max(
                    Math.abs(deltaY / deltaTime) * 1000,
                    Math.abs(deltaX / deltaTime) * 1000
                )
            }
        }

        /**
         * 判断是否应该关闭对话框
         */
        fun shouldDismissDialog(velocity: Float): Boolean {
            // 如果拖动超过阈值，或者速度超过阈值，则关闭
            return dragPercent > dialogConfig.swipeThreshold ||
                    velocity > dialogConfig.swipeVelocityThreshold
        }

        /**
         * 带动画关闭对话框
         */
        fun dismissWithAnimation() {
            //  val targetPosition = when (dialogConfig.swipeDirection) {
            //      SwipeDirection.SWIPE_DIRECTION_DOWN -> view?.height?.toFloat() ?: 0f
            //      SwipeDirection.SWIPE_DIRECTION_UP -> -(view?.height?.toFloat() ?: 0f)
            //      SwipeDirection.SWIPE_DIRECTION_LEFT -> -(view?.width?.toFloat() ?: 0f)
            //      SwipeDirection.SWIPE_DIRECTION_RIGHT -> view?.width?.toFloat() ?: 0f
            //      else -> view?.height?.toFloat() ?: 0f // 默认向下
            //  }

            swipeDismissAnimator = ValueAnimator.ofFloat(dragPercent, 1f).apply {
                duration = dialogConfig.swipeDismissAnimDuration
                interpolator = AccelerateInterpolator()
                addUpdateListener { animation ->
                    val value = animation.animatedValue as Float
                    updateViewPosition(value)
                }
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        dismiss()
                    }
                })
                start()
            }
        }

        /**
         * 重置位置动画
         */
        fun resetPositionWithAnimation() {
            swipeDismissAnimator = ValueAnimator.ofFloat(dragPercent, 0f).apply {
                duration = dialogConfig.swipeDismissAnimDuration
                interpolator = DecelerateInterpolator()
                addUpdateListener { animation ->
                    val value = animation.animatedValue as Float
                    updateViewPosition(value)
                }
                start()
            }
        }

        /**
         * 处理拖动结束
         */
        fun handleDragEnd(event: MotionEvent) {
            val velocity = calculateVelocity(event)
            val shouldDismiss = shouldDismissDialog(velocity)

            if (shouldDismiss) {
                dismissWithAnimation()
            } else {
                resetPositionWithAnimation()
            }
        }

        /**
         * 检查滑动方向是否有效
         */
        fun isSwipeDirectionValid(event: MotionEvent): Boolean {
            val deltaY = event.rawY - initialTouchY
            val deltaX = event.rawX - initialTouchX

            return when (dialogConfig.swipeDirection) {
                SwipeDirection.SWIPE_DIRECTION_DOWN -> deltaY > 0 && Math.abs(deltaY) > Math.abs(deltaX)
                SwipeDirection.SWIPE_DIRECTION_UP -> deltaY < 0 && Math.abs(deltaY) > Math.abs(deltaX)
                SwipeDirection.SWIPE_DIRECTION_LEFT -> deltaX < 0 && Math.abs(deltaX) > Math.abs(deltaY)
                SwipeDirection.SWIPE_DIRECTION_RIGHT -> deltaX > 0 && Math.abs(deltaX) > Math.abs(deltaY)
                SwipeDirection.SWIPE_DIRECTION_ANY -> true
                else -> deltaY > 0 && Math.abs(deltaY) > Math.abs(deltaX) // 默认向下
            }
        }

        /**
         * 正式处理监听swipe事件，若外部传入指定VIew,则用，否则就是整个页面都作用
         */
        val swipeView: View? = if (dialogConfig.swipeDismissViewId != 0) {
            view?.findViewById(dialogConfig.swipeDismissViewId)
        } else view

        swipeView?.setOnTouchListener { v, event ->
            if (swipeDismissAnimator != null && swipeDismissAnimator!!.isRunning) {
                // 动画正在进行中，不处理触摸事件
                return@setOnTouchListener true
            }

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialTouchY = event.rawY
                    initialTouchX = event.rawX
                    isDragging = false
                    dragPercent = 0f
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    if (!isDragging && isSwipeDirectionValid(event)) {
                        isDragging = true
                    }

                    if (isDragging) {
                        handleDrag(event)
                    }
                    true
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (isDragging) {
                        handleDragEnd(event)
                    }
                    isDragging = false
                    true
                }

                else -> false
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
    protected abstract fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
    ): VB

    protected abstract fun initView(binding: VB)

    /**
     * Window flag compat
     *
     * 刘海屏
     * - 设备默认设置
     * - 边角刘海屏
     * - 双刘海屏
     * - 打孔屏
     * - 长型刘海屏
     * - 瀑布刘海屏
     * - 隐藏
     * - 在刘海区域下方呈现应用
     *
     * Android 9.0系统中提供了3种layoutInDisplayCutoutMode属性来允许应用自主决定该如何对刘海屏设备进行适配。
     * LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT
     * 这是一种默认的属性，在不进行明确指定的情况下，系统会自动使用这种属性。这种属性允许应用程序的内容在竖屏模式下自动延伸到刘海区域，而在横屏模式下则不会延伸到刘海区域。
     * LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
     * 这种属性表示，不管手机处于横屏还是竖屏模式，都会允许应用程序的内容延伸到刘海区域。
     * LAYOUT_IN_DISPLAY_CUTOUT_MODE_NEVER
     * 这种属性表示，永远不允许应用程序的内容延伸到刘海区域。
     * LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS （Android 11,Api 30增加）
     * 允许应用内容始终延伸至凹口区域，实现真正的“边到边”显示效果
     */
    interface WindowFlagCompat {
        fun setupFlag(
            params: ViewGroup.LayoutParams,
            dialogConfig: DialogConfigs,
        )

        class BeforeApi30Impl : WindowFlagCompat {
            override fun setupFlag(
                params: ViewGroup.LayoutParams,
                dialogConfig: DialogConfigs,
            ) {
                if (params is LayoutParams) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        // 允许占用刘海
                        params.layoutInDisplayCutoutMode = LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                    }
                }
            }
        }

        class Api30Impl : WindowFlagCompat {
            @RequiresApi(Build.VERSION_CODES.R)
            override fun setupFlag(
                params: ViewGroup.LayoutParams,
                dialogConfig: DialogConfigs,
            ) {
                if (params is LayoutParams) {
                    // 允许占用刘海
                    params.layoutInDisplayCutoutMode = LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                }
            }
        }
    }
}
