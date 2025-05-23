package com.godox.common.kdtablayout

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.util.Log
import android.view.*
import android.widget.OverScroller
import androidx.interpolator.view.animation.LinearOutSlowInInterpolator
import androidx.viewpager.widget.ViewPager
import androidx.viewpager2.widget.ViewPager2
import com.godox.common.BuildConfig
import com.godox.common.kdtablayout.widget.KDTab
import com.godox.common.kdtablayout.widget.KDTabIndicator
import kotlin.math.abs
import kotlin.math.max


/**
 * Created By：XuQK
 * Created Date：2/15/20 7:39 PM
 * Description：高仿【MagicIndicator】这个库，原生支持vp2，不用额外写那么多东西，兼容性也好
 *              使用说明：https://github.com/ChenBabys/KDTabLayout/tree/master
 */
class KDTabLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0,
) : ViewGroup(context, attrs, defStyleAttr), KDViewPagerHelper.ViewPageStateListener {

    companion object {
        /**
         * Indicates that the pager is in an idle, settled state. The current page
         * is fully in view and no animation is in progress.
         */
        const val SCROLL_STATE_IDLE = 0

        /**
         * Indicates that the pager is currently being dragged by the user.
         */
        const val SCROLL_STATE_DRAGGING = 1

        /**
         * Indicates that the pager is in the process of settling to a final position.
         */
        const val SCROLL_STATE_SETTLING = 2

        private const val DEFAULT_DURATION: Long = 250

        /**
         * tab可滚动，此时tab的排列为按照tab本身宽度横向依序排列
         */
        const val TAB_MODE_SCROLLABLE = 0

        /**
         * tab不可滚动，此时tab的排列为按照tab本身宽度，以TabLayout的x中线对齐
         */
        const val TAB_MODE_PACK = 1

        /**
         * tab不可滚动，此时tab按比例均分整个TabLayout
         * 此时Tab中的weight参数生效
         */
        const val TAB_MODE_SPREAD = 2

        /**
         * 自适应模式
         * 所有tab原本总宽度如果没有超过TabLayout，就按MODE_TAB_SPREAD模式排列，此时Tab中的weight参数生效
         * 若超过TabLayout总宽度，则按照MODE_TAB_SCROLLABLE排列
         */
        const val TAB_MODE_FLEXIBLE = 3
    }

    private var savedBundle: Bundle? = null

    // 所有子Tab宽度之和
    private var totalWidth: Int = 0
    private var scrollable: Boolean = false

    // 由于Tab只管横向滑动，所以滑动只记录x坐标即可
    private var lastX: Float = 0f

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop / 2
    private val overScrollDistance = dpToPx(context, 16f)
    private val scroller = OverScroller(context)
    private var velocityTracker: VelocityTracker? = null
    private val minVelocity = ViewConfiguration.get(context).scaledMinimumFlingVelocity
    private var mKDTabLayoutStateListener: KDTabLayoutStateListener? = null

    /**
     * 滚动时，被选中Tab与控件中心x坐标的偏差
     */
    var scrollBiasX: Float = 0f
        set(value) {
            field = dpToPx(context, value).toFloat()
        }
    var tabMode: Int = TAB_MODE_FLEXIBLE

    /**平滑滚动动画时间*/
    var smoothScrollDuration: Long = DEFAULT_DURATION

    /**
     * 为true，从tab2直接滚动到tab10，滚动开始后，会瞬间切到tab2，然后动画滚动到tab10，中间的tab也会对滚动进行响应
     * 为false，从tab2直接滚动到tab10，滚动开始后，会从当前停留的点，直接动画滚动到tab10，中间的tab不会对滚动进行响应，变化只发生在tab2和tab10上
     * 如果Tab数量很多且可滚动，建议将此项设置为false体验稍好一些
     */
    var needCompleteScroll: Boolean = false
    var contentAdapter: KDTabAdapter? = null
        set(value) {
            field = value
            init()
        }

    var currentItem: Int = 0
        private set

    private var stopViewPagerAffect: Boolean = true
    var indicator: KDTabIndicator? = null
        private set
    private var tabChangeAnimator: ValueAnimator? = null

    private val vpHelper: KDViewPagerHelper by lazy { KDViewPagerHelper() }

    private var scrollState: Int = SCROLL_STATE_IDLE

    /**
     * 设置vp,不要在这里立即预选tab,留给vp的setCurrentItem去处理吧，避免一些同步问题
     * mKDTabLayoutStateListener:非必须实现
     */
    fun setViewPager(viewPager: ViewPager, mKDTabLayoutStateListener: KDTabLayoutStateListener? = null) {
        stopViewPagerAffect = false
        vpHelper.bindViewPager(viewPager)
        this.mKDTabLayoutStateListener = mKDTabLayoutStateListener
        vpHelper.stateListener = this
        // updateTabState(viewPager.currentItem) // 多余的
    }

    /**
     * 设置vp2,不要在这里立即预选tab,留给vp2的setCurrentItem去处理吧，避免一些同步问题
     */
    fun setViewPager2(viewPager2: ViewPager2, mKDTabLayoutStateListener: KDTabLayoutStateListener? = null) {
        stopViewPagerAffect = false
        vpHelper.bindViewPager2(viewPager2)
        this.mKDTabLayoutStateListener = mKDTabLayoutStateListener
        vpHelper.stateListener = this
        // 原作者这句没必要，只会增加初始设置又调用【setCurrentItem】后的闪烁，
        // 这句的功能其实在bindViewPager2中已处理，
        // 毕竟默认都是0，没必要多此一举，后续默认选中交给vp2的setCurrentItem去处理
        // updateTabState(viewPager2.currentItem)
    }

    fun setCurrentItem(position: Int, smooth: Boolean = true) {
        if (currentItem == position) return

        if (smooth) {
            smoothScrollToItem(position)
        } else {
            scrollToItem(position)
        }

        currentItem = position
    }

    private fun scrollToItem(position: Int) {
        // 在该方法作用域里用来记录startPosition
        val tempStartPosition = currentItem
        currentItem = position

        updateTabState(currentItem)
        syncIndicatorScrollState(tempStartPosition, position, 1f)
    }

    private fun smoothScrollToItem(position: Int) {
        if (tabChangeAnimator?.isRunning == true) {
            tabChangeAnimator?.cancel()
            updateTabState(currentItem)
        }

        // 在该方法作用域里用来记录startPosition
        val tempStartPosition = currentItem
        currentItem = position

        val startTab = getChildAt(tempStartPosition)
        val endTab = getChildAt(position)

        val startScrollX = scrollX

        tabChangeAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = this@KDTabLayout.smoothScrollDuration
            interpolator = LinearOutSlowInInterpolator()
            addListener(
                object : Animator.AnimatorListener {
                    override fun onAnimationRepeat(animation: Animator) {}

                    override fun onAnimationEnd(animation: Animator) {
                        scrollState = SCROLL_STATE_IDLE
                        updateTabState(currentItem)
                    }

                    override fun onAnimationCancel(animation: Animator) {
                        scrollState = SCROLL_STATE_IDLE
                    }

                    override fun onAnimationStart(animation: Animator) {
                        scrollState = SCROLL_STATE_SETTLING
                    }
                },
            )

            addUpdateListener {
                if (it.animatedFraction < 1f) {
                    // 同步tab状态
                    syncTabScrollState(startTab, endTab, it.animatedFraction)

                    // 同步tabLayout scrollX
                    val endScrollX = getTabScrollXInCenter(endTab)
                    scrollTo(startScrollX + ((endScrollX - startScrollX) * it.animatedFraction).toInt())

                    // 同步indicator状态
                    syncIndicatorScrollState(tempStartPosition, position, it.animatedFraction)
                }
            }
            start()
        }
    }

    fun init() {
        this.removeAllViews()
        // fix：当重新初始化tab数据之后，因为View没有重初始化，导致的位移没有恢复的问题
        this.scrollTo(0)
        contentAdapter?.let { adapter ->
            if (adapter.getTabCount() <= 0) {
                indicator = null
                return@let
            }

            if (currentItem > adapter.getTabCount() - 1) {
                currentItem = adapter.getTabCount() - 1
            }
            (0 until adapter.getTabCount()).forEach { i ->
                adapter.createTab(i)?.let {
                    if (i == currentItem) {
                        it.selectTab()
                    } else {
                        it.reset()
                    }
                    addView(it as View)
                }
            }
            indicator = adapter.createIndicator()
            if (indicator != null) {
                setWillNotDraw(false)
            }

            if (childCount > 0 && indicator != null) {
                // tab和indicator都有
            } else if (childCount == 0 && indicator != null) {
                // 只有indicator
            } else if (childCount > 0 && indicator == null) {
                // 只有tab
            } else {
                throw IllegalArgumentException("tab和indicator不能都不设置")
            }

            post {
                indicator?.init()
            }
        }
    }

    fun getTab(position: Int): KDTab? {
        return getChildAt(position) as? KDTab
    }

    override fun onDraw(canvas: Canvas) {
        indicator?.draw(canvas)
    }

    fun smoothScrollBy(x: Int) {
        if (!scrollable) return
        scroller.startScroll(scrollX, 0, x, 0, smoothScrollDuration.toInt())
        postInvalidateOnAnimation()
    }

    /**
     * 丝滑滑动到指定位置
     * 有个点要注意，当tabLayout重新初始化时数据时，若View没有被重新初始化，那么scroller的值需要回调圆点
     * 即：重新初始化数据前，调用本方法：smoothScrollTo（0）或者下面的scrollTo（0）可以让偏移恢复初始值
     * fix.这个不调用也可以了，当做bug修复在上面了
     */
    fun smoothScrollTo(destX: Int) {
        if (!scrollable) return
        val dx = when {
            destX < 0 -> -scrollX
            destX > totalWidth - width -> totalWidth - width - scrollX
            else -> destX - scrollX
        }
        scroller.startScroll(scrollX, 0, dx, 0, smoothScrollDuration.toInt())
        postInvalidateOnAnimation()
    }

    /**
     * 到指定位置，作用和smoothScrollTo类似
     */
    fun scrollTo(destX: Int) {
        if (!scrollable) return
        val dx = when {
            destX < 0 -> 0
            destX > totalWidth - width -> totalWidth - width
            else -> destX
        }

        scrollTo(dx, 0)
    }

    override fun computeScroll() {
        if (scroller.computeScrollOffset()) {
            scrollTo(scroller.currX, 0)
            postInvalidate()
        }
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                lastX = ev.rawX
                parent?.requestDisallowInterceptTouchEvent(true)
                if (!scroller.isFinished) {
                    scroller.abortAnimation()
                    return true
                }
            }

            MotionEvent.ACTION_MOVE -> {
                val evRawX = ev.rawX
                if (abs(evRawX - lastX) > touchSlop) {
                    return true
                }
                lastX = evRawX
            }
        }
        return super.onInterceptTouchEvent(ev)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!scrollable) return true

        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain()
        }
        velocityTracker!!.addMovement(event)

        when (event.action) {
            MotionEvent.ACTION_MOVE -> {
                val evRawX = event.rawX
                val deltaX = (lastX - evRawX).toInt()
                val resultX = deltaX + scrollX
                when {
                    resultX < 0 -> scrollTo(0, 0)
                    resultX > totalWidth - width -> scrollTo(totalWidth - width, 0)
                    else -> scrollBy(deltaX, 0)
                }
                lastX = evRawX
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (scrollX > 0 && scrollX < totalWidth - width) {
                    velocityTracker?.let {
                        it.computeCurrentVelocity(1000)
                        val xVelocity = it.xVelocity
                        if (abs(xVelocity) > minVelocity) {
                            scroller.fling(
                                scrollX,
                                0,
                                -xVelocity.toInt(),
                                0,
                                0,
                                totalWidth - width,
                                0,
                                0,
                                overScrollDistance,
                                0,
                            )
                            postInvalidateOnAnimation()
                        }
                    }
                }

                velocityTracker?.clear()
                velocityTracker?.recycle()
                velocityTracker = null
            }
        }
        return true
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        totalWidth = 0
        var totalHeight = 0
        if (childCount == 0) {
            // 没有tab的情况，TabLayout内容宽度与布局宽度一致
            totalWidth = indicator?.getWidth() ?: 0
            totalHeight = indicator?.getHeight() ?: 0
        } else {
            var totalWeight = 0
            for (i in 0 until childCount) {
                val child = getChildAt(i)
                measureChild(child, widthMeasureSpec, heightMeasureSpec)

                totalWidth += child.measuredWidth
                totalHeight = max(totalHeight, child.height)
                totalWeight += (child as KDTab).weight
            }

            when (tabMode) {
                // tab宽度保持既定不变的情况
                TAB_MODE_SCROLLABLE, TAB_MODE_PACK -> {
                    setMeasuredDimension(widthMeasureSpec, heightMeasureSpec)
                }
                // tab宽度要按比例占满整个TabLayout的情况
                TAB_MODE_SPREAD -> {
                    totalWidth = measuredWidth
                    for (i in 0 until childCount) {
                        val child = getChildAt(i)
                        child.measure(
                            MeasureSpec.makeMeasureSpec(
                                totalWidth * (child as KDTab).weight / totalWeight,
                                MeasureSpec.EXACTLY,
                            ),
                            MeasureSpec.makeMeasureSpec(measuredHeight, MeasureSpec.AT_MOST),
                        )
                    }
                }

                TAB_MODE_FLEXIBLE -> {
                    if (totalWidth <= measuredWidth) {
                        // 所有子tab宽度加起来都比parent宽度小，此时跟SPREAD模式一致
                        totalWidth = measuredWidth
                        for (i in 0 until childCount) {
                            val child = getChildAt(i)
                            child.measure(
                                MeasureSpec.makeMeasureSpec(
                                    totalWidth * (child as KDTab).weight / totalWeight,
                                    MeasureSpec.EXACTLY,
                                ),
                                MeasureSpec.makeMeasureSpec(measuredHeight, MeasureSpec.AT_MOST),
                            )
                        }
                    } else {
                        // 所有子tab宽度加起来比parent宽度大，此时跟SCROllABLE模式一致
                        setMeasuredDimension(widthMeasureSpec, heightMeasureSpec)
                    }
                }
            }
        }

        setMeasuredDimension(
            View.resolveSizeAndState(totalWidth, widthMeasureSpec, 0),
            View.resolveSizeAndState(totalHeight, heightMeasureSpec, 0),
        )
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        var width = 0
        if (tabMode == TAB_MODE_PACK) {
            width = (r - l - totalWidth) / 2
        }
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            child.layout(width, 0, width + child.measuredWidth, child.measuredHeight)
            width += child.measuredWidth
        }
        scrollable = r - l < width
        if (changed) {
            indicator?.init()
        }

        savedBundle?.let {
            updateTabState(it.getInt("currentItem"))
            post { scrollTo(it.getInt("scrollX")) }
        }
        savedBundle = null
    }

    override fun onScrolling(scrollFraction: Float, startItem: Int, endItem: Int) {
        if (stopViewPagerAffect) return
        val startTab: View? = getChildAt(startItem)
        val endTab: View? = getChildAt(endItem)

        if (startTab != null && endTab != null) {
            // 整个TabLayout同步滚动
            syncLayoutScrollX(startTab, endTab, scrollFraction)

            // 将当前滚动参数同步给关联到的两个tab
            syncTabScrollState(startTab, endTab, scrollFraction)

            // 将滚动状态同步给indicator
            syncIndicatorScrollState(startItem, endItem, scrollFraction)
            // 事件回调
            mKDTabLayoutStateListener?.onScrolling(scrollFraction, startItem, endItem)
        } else {
            if (BuildConfig.DEBUG)
                Log.d("KDView", "startTab空:${startTab == null},endTab空:${endTab == null}，不执行同步了")
        }

        // 将滚动状态同步给indicator,  2025.4.24: fix.移到上面括号去了，会下标异常的，放这里（原作者为啥放这？）
        // syncIndicatorScrollState(startItem, endItem, scrollFraction)
    }

    override fun onScrollStateChanged(state: Int) {
        when (state) {
            SCROLL_STATE_IDLE -> {
                if (stopViewPagerAffect) {
                    stopViewPagerAffect = false
                }
                updateTabState(currentItem)
            }

            SCROLL_STATE_DRAGGING -> tabChangeAnimator?.cancel()
            SCROLL_STATE_SETTLING -> {
                if (scrollState == SCROLL_STATE_DRAGGING) {
                    // 表明是用户拖动ViewPager导致的状态变化
                } else if (scrollState == SCROLL_STATE_IDLE) {
                    // 表明是直接调用ViewPager.setCurrentItem方法导致的状态变化
                    // 此时如果不需要完全滚动，就要禁止ViewPager滚动对Tab的影响，使用Tab自身的滚动方法来进行状态变化
                    if (!needCompleteScroll) {
                        stopViewPagerAffect = true
                    }
                }
            }
        }
        scrollState = state
        // 滑动不停止，都有效
        if (!stopViewPagerAffect) {
            mKDTabLayoutStateListener?.onScrollStateChanged(state)
        }
    }

    override fun onTabSelected(position: Int) {
        // 只有在禁止ViewPager滚动对Tab滚动的影响的时候，才需要调用Tab自身的滚动方法
        if (stopViewPagerAffect) {
            smoothScrollToItem(position)
        }
        // 放在条件之外，如果能不能滑动，都要执行它
        mKDTabLayoutStateListener?.onPageSelected(position)
    }

    private fun updateTabState(currentItem: Int) {
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (i == currentItem) {
                (child as KDTab).selectTab()
            } else {
                (child as KDTab).reset()
            }
            child.invalidate()
        }
        this.currentItem = currentItem
        syncIndicatorScrollState(currentItem, currentItem, 1f)
    }

    private fun syncTabScrollState(startTab: View, endTab: View, fraction: Float) {
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            when (child) {
                startTab -> (child as KDTab).onScrolling(1 - fraction, startTab.left > endTab.left)
                endTab -> (child as KDTab).onScrolling(fraction, startTab.left < endTab.left)
                else -> (child as KDTab).reset()
            }
            child.invalidate()
        }

        if (fraction == 1f) {
            currentItem = indexOfChild(endTab)
        }
    }

    private fun syncIndicatorScrollState(startItem: Int, endItem: Int, fraction: Float) {
        indicator?.onTabScrolled(startItem, endItem, fraction)
        invalidate()
    }

    /**
     * 根据滚动的两个tab和滚动进程，将tabLayout同步滚动
     */
    private fun syncLayoutScrollX(startTab: View, endTab: View, fraction: Float) {
        if (scrollable) {
            val startScrollX = getTabScrollXInCenter(startTab)
            val endScrollX = getTabScrollXInCenter(endTab)
            scrollTo(startScrollX + ((endScrollX - startScrollX) * fraction).toInt())
        }
    }

    /**
     * tab在正中间时，整个tabLayout的scrollX值
     */
    private fun getTabScrollXInCenter(tab: View?): Int {
        return if (tab == null) {
            0
        } else {
            tab.left - (width - tab.width) / 2 - scrollBiasX.toInt()
        }
    }

    override fun onSaveInstanceState(): Parcelable {
        savedBundle = null
        val bundle = Bundle()
        val superData = super.onSaveInstanceState()
        bundle.putParcelable("superData", superData)
        bundle.putInt("currentItem", currentItem)
        bundle.putInt("scrollX", scrollX)
        return bundle
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        val bundle = state as? Bundle
        if (bundle == null) {
            super.onRestoreInstanceState(state)
        } else {
            savedBundle = bundle
            super.onRestoreInstanceState(bundle.getParcelable("superData"))
        }
    }


    interface KDTabLayoutStateListener {
        /**
         * 从currentItem到nextItem的滚动过程中的fraction变化
         * @param scrollFraction 从一个tab到另一个tab，是0-1
         * @param startItem 从currentItem的tab出发
         * @param endItem 到nextItem的tab
         */
        fun onScrolling(scrollFraction: Float, startItem: Int, endItem: Int) {}

        /**
         * 滚动状态变化
         */
        fun onScrollStateChanged(state: Int) {}

        /**
         * tab选中，一般vp选中就会回调
         */
        fun onPageSelected(position: Int)
    }
}
