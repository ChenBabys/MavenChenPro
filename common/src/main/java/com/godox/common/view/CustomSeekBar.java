package com.godox.common.view;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.godox.common.R;
import com.godox.common.util.KtxKt;


/**
 * Author: ChenBabys
 * Date: 2025/4/17
 * Description: 自定义的SeekBar
 */
public class CustomSeekBar extends View {
    private Paint bgPaint, progressPaint, thumbPaint, thumbStrokePaint, thumbInnerStrokePaint, thumbInnerPaint;
    // 默认的粗细，横向时是高度，纵向时是宽度
    private float lineWeight = 20f;
    /**
     * 背景颜色，当传入bgColorList时，当前参数会失效，替代了
     */
    private int bgColor = Color.GRAY;
    /**
     * 背景颜色数组，优先级高，传入后，bgColor的值失效
     */
    private int[] bgColorList;

    /**
     * 进度色默认为0，当为0时不绘制
     */
    private int progressColor = 0;
    private int thumbColor = Color.WHITE;
    private int thumbStrokeAndInnerColor = Color.BLACK;
    private float cornerRadius = 10f;
    private boolean isHorizontal = true;
    private float strokeWidth = 5f;
    private int maxProgress = 100;
    private int progress = 0;
    private RectF bgRect = new RectF();
    private RectF progressRect = new RectF();
    private float thumbRadius = 20f; // 指示器半径
    private ValueAnimator colorAnimator;
    /**
     * 是否只有按住了才可以改变进度条
     */
    private boolean isLongTouchToAvailable = false;

    /**
     * 按下加宽数值,按下开始逐步加大，最大5，实现多帧动画加大的效果
     */
    private float touchWiden = 1f;

    /**
     * 鉴别长按事件
     */
    private GestureDetector mGestureDetector;
    /**
     * 是否处于按下状态，不一定在滑动
     */
    private boolean isTouchMode = false;

    /**
     * 是否在滑动
     */
    private boolean isSeeking = false;

    /**
     * 特殊使用，得外部主动恢复，用于处理特殊情况的
     * 表明外部点击了进度条
     */
    private boolean isTouchSeekbarView = false;


    public CustomSeekBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CustomSeekBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    private void init(AttributeSet attrs) {
        // 初始化属性
        TypedArray ta = getContext().obtainStyledAttributes(attrs, R.styleable.CustomSeekBar);
        lineWeight = ta.getDimension(R.styleable.CustomSeekBar_csb_line_weight, lineWeight);
        maxProgress = ta.getColor(R.styleable.CustomSeekBar_csb_max_progress, maxProgress);
        bgColor = ta.getColor(R.styleable.CustomSeekBar_csb_background_color, bgColor);
        progressColor = ta.getColor(R.styleable.CustomSeekBar_csb_progress_color, progressColor);
        thumbColor = ta.getColor(R.styleable.CustomSeekBar_csb_thumb_color, thumbColor);
        thumbStrokeAndInnerColor = ta.getColor(R.styleable.CustomSeekBar_csb_thumb_stroke_and_inner_color, thumbStrokeAndInnerColor);
        cornerRadius = ta.getDimension(R.styleable.CustomSeekBar_csb_corner_radius, cornerRadius);
        thumbRadius = ta.getDimension(R.styleable.CustomSeekBar_csb_thumb_radius, thumbRadius);
        isHorizontal = ta.getInt(R.styleable.CustomSeekBar_csb_orientation, 0) == 0;
        strokeWidth = ta.getDimension(R.styleable.CustomSeekBar_csb_stroke_width, strokeWidth);
        isLongTouchToAvailable = ta.getBoolean(R.styleable.CustomSeekBar_csb_is_long_touch_to_available, false);
        ta.recycle();

        // 初始化背景画笔
        bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setColor(bgColor);
        bgPaint.setAntiAlias(true);
        bgPaint.setStyle(Paint.Style.FILL);

        // 初始化进度颜色画笔
        initProgressPaint(progressColor);

        // 以下指示器
        // 覆盖圆
        thumbPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        thumbPaint.setColor(thumbColor);
        thumbPaint.setAntiAlias(true);
        thumbPaint.setStyle(Paint.Style.FILL);

        // 外边圆STROKE
        thumbStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        thumbStrokePaint.setColor(thumbStrokeAndInnerColor);
        thumbStrokePaint.setStyle(Paint.Style.STROKE);
        thumbStrokePaint.setAntiAlias(true);
        thumbStrokePaint.setStrokeWidth(strokeWidth);

        // 内圆边FILL_AND_STROKE
        thumbInnerStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        thumbInnerStrokePaint.setColor(Color.LTGRAY); // 固定浅灰色吧
        thumbInnerStrokePaint.setStyle(Paint.Style.FILL_AND_STROKE);
        thumbInnerStrokePaint.setAntiAlias(true);
        thumbInnerStrokePaint.setStrokeWidth(strokeWidth);

        // 内圆
        thumbInnerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        thumbInnerPaint.setColor(thumbStrokeAndInnerColor);
        thumbInnerPaint.setAntiAlias(true);
        thumbInnerPaint.setStyle(Paint.Style.FILL);

        // 创建长按才触发的初始逻辑
        initLongTouchEvent();
    }

    /**
     * 创建长按才触发的初始逻辑
     */
    private void initLongTouchEvent() {
        if (!isLongTouchToAvailable) {
            return;
        }
        mGestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public void onLongPress(@NonNull MotionEvent e) {
                super.onLongPress(e);
                KtxKt.vibrate(getContext());
                isSeeking = true;
                touchWiden = 5;
                postInvalidate();
            }

        });

    }

    /**
     * 初始化进度颜色画笔
     */
    private void initProgressPaint(int progressColor) {
        if (progressColor == 0) {
            return;
        }
        progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        progressPaint.setColor(progressColor);
        progressPaint.setAntiAlias(true);
        progressPaint.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // warp,高度足够显示圆形指示
        if (isHorizontal) {
            int width = MeasureSpec.getSize(widthMeasureSpec);
            int minHeight = (int) (thumbRadius * 2 + strokeWidth * 2);
            int height = resolveSize(minHeight, heightMeasureSpec);
            setMeasuredDimension(width, height);
        } else {
            int height = MeasureSpec.getSize(heightMeasureSpec);
            int width = (int) (thumbRadius * 2 + strokeWidth * 2);
            setMeasuredDimension(width, height);// 不用resolveSize了
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        // 如果有线性渐变色，则绘制
        if (bgColorList != null && bgColorList.length > 0) {
            // positions的参数可以对应颜色下标，但现在不需要
            LinearGradient gradient = new LinearGradient(
                0, 0, getWidth(), 0,
                bgColorList, null, Shader.TileMode.CLAMP);
            bgPaint.setShader(gradient);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // 绘制背景（增加安全边距）
        float safeInset = thumbRadius / 2;
        // 计算垂直居中位置
        float centerY = (getHeight() - lineWeight) / 2;
        // 方块的上下
        float rectTop = centerY;
        float rectBottom = centerY + lineWeight;

        if (isLongTouchToAvailable) {
            rectTop = isSeeking ? centerY - touchWiden : centerY;
            rectBottom = isSeeking ? centerY + lineWeight + touchWiden : centerY + lineWeight;
            // 补值，让其每次都大一点，最大5
            // touchWiden = Math.min(touchWiden + 1, 5);
        }

        // bgRect.set(safeInset, centerY, getWidth() - safeInset, centerY + lineWeight);
        bgRect.set(safeInset, rectTop, getWidth() - safeInset, rectBottom);
        canvas.drawRoundRect(bgRect, cornerRadius, cornerRadius, bgPaint);

        // 绘制进度（动态计算最小可见进度）,需判断
        if (isShowProgressColor()) {
            float minVisibleProgress = safeInset * maxProgress / (isHorizontal ? getWidth() : lineWeight);
            float drawProgress = Math.max(progress, minVisibleProgress);
            float progressRatio = drawProgress / (float) maxProgress;
            if (isHorizontal) {
                // progressRect.set(safeInset, centerY,safeInset + (getWidth() - 2 * safeInset) * progressRatio, centerY + lineWeight);

                progressRect.set(safeInset, rectTop, safeInset + (getWidth() - 2 * safeInset) * progressRatio, rectBottom);
            } else {
                progressRect.set(safeInset,
                    centerY + lineWeight - (lineWeight - 2 * safeInset) * progressRatio,
                    getWidth() - safeInset,
                    centerY + lineWeight);
            }
            // 如果是按住才可以用的，用这个方式
            if (isLongTouchToAvailable) {
                // Alpha:0-255
                if (isSeeking) {
                    progressPaint.setAlpha(255);
                } else {
                    progressPaint.setAlpha(128);
                }
            }

            canvas.drawRoundRect(progressRect, cornerRadius, cornerRadius, progressPaint);
        }

        // 绘制指示器（位置计算同步调整）
        float thumbX = calculateThumbX();
        float thumbY = calculateThumbY();
        // 先绘制覆盖圈
        canvas.drawCircle(thumbX, thumbY, thumbRadius, thumbPaint);
        // 再绘制边圈
        canvas.drawCircle(thumbX, thumbY, thumbRadius, thumbStrokePaint);
        // 绘制内圈和实心两个
        float innerThumbRadius = thumbRadius - strokeWidth * 2;
        canvas.drawCircle(thumbX, thumbY, innerThumbRadius, thumbInnerStrokePaint);
        canvas.drawCircle(thumbX, thumbY, innerThumbRadius - thumbRadius / 4, thumbInnerPaint);
    }


    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // 禁止父容器拦截事件,独享
                getParent().requestDisallowInterceptTouchEvent(true);
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                getParent().requestDisallowInterceptTouchEvent(false);
                break;
        }
        return super.dispatchTouchEvent(event);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled()) {
            return false;
        }
        // 如果是长按触发的，在这里注册它的事件，注意：不可以放到ACTION_DOWN中传入注册，会让其无法完整接管事件
        if (isLongTouchToAvailable) {
            // 它也可以鉴别到ViewConfiguration.getLongPressTimeout()长按事件
            mGestureDetector.onTouchEvent(event);
        }

        float pos = isHorizontal ? event.getX() : getHeight() - event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                isTouchMode = true;
                // 只处理非长按情况即可，长按留给mGestureDetector处理
                if (!isLongTouchToAvailable) {
                    isSeeking = true;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (isSeeking) {
                    updateProgressWithDelta(pos, false);
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (isSeeking) {
                    updateProgressWithDelta(pos, true);
                }
                isSeeking = false;
                if (isLongTouchToAvailable) {
                    touchWiden = 1f;
                }
                isTouchMode = true;
                // 这个是外部使用的字段，不用管
                isTouchSeekbarView = true;
                break;
            default:
                break;
        }
        return true;
    }

    /**
     * 更新进度
     */
    private void updateProgressWithDelta(float pos, boolean isTouchEnd) {
        pos = Math.max(thumbRadius, Math.min(pos, (isHorizontal ? getWidth() : getHeight()) - thumbRadius));
        int newProgress = (int) (maxProgress * (pos - thumbRadius) /
            ((isHorizontal ? getWidth() : getHeight()) - thumbRadius * 2));
        // 避免不必要的重复更新,但是要保留最后多一帧touchEnd
        if (isTouchEnd || newProgress != progress) {
            changeProgress(newProgress, true, isTouchEnd, false);
        }
    }

    /**
     * @param progress         进度
     * @param fromUser         是否来自用户滑动
     * @param isTouchEnd       是否滑动完毕
     * @param notCallback 是否不回调，默认false
     */
    private void changeProgress(int progress, boolean fromUser, boolean isTouchEnd, boolean notCallback) {
        this.progress = Math.max(0, Math.min(progress, maxProgress));
        // animateProgressColor(); 不变色了
        invalidate();
        if (listener != null && !notCallback) {
            listener.onProgressChanged(this, this.progress, fromUser, isTouchEnd);
        }
    }

    /**
     * 外部设置进度
     *
     * @param progress         进度值
     * @param notCallback 是否不回调
     */
    public void setProgress(int progress, boolean notCallback) {
        changeProgress(progress, false, true, notCallback);
    }

    /**
     * 外部设置进度
     *
     * @param progress 进度值
     */
    public void setProgress(int progress) {
        changeProgress(progress, false, true, false);
    }

    private float calculateThumbX() {
        if (!isHorizontal) return getWidth() / 2f;
        float progressRatio = progress / (float) maxProgress;
        float safeInset = thumbRadius / 2;
        return safeInset + (getWidth() - 2 * safeInset) * progressRatio;
    }

    private float calculateThumbY() {
        if (isHorizontal) return getHeight() / 2f;
        float progressRatio = progress / (float) maxProgress;
        float safeInset = thumbRadius / 2;
        return getHeight() - safeInset - (getHeight() - 2 * safeInset) * progressRatio;
    }

    /**
     * 设置进度条的颜色，重新绘制
     * 画笔可能没有初始化，要重新初始化
     */
    public void setProgressColor(int progressColor) {
        this.progressColor = progressColor;
        // 进度的画笔可能没有初始化，要重新初始化
        initProgressPaint(progressColor);
        invalidate();
    }

    /**
     * 设置指示器的内部实心的颜色
     */
    public void setThumbStrokeAndInnerColor(int color) {
        this.thumbStrokeAndInnerColor = color;
        thumbStrokePaint.setColor(thumbStrokeAndInnerColor);
        thumbInnerPaint.setColor(thumbStrokeAndInnerColor);
        invalidate();
    }

    /**
     * 设置背景条单颜色，重新绘制
     */
    public void setBgColor(int bgColor) {
        this.bgColor = bgColor;
        invalidate();
    }

    /**
     * 设置背景条颜色数组，重新绘制
     * 设置线性渐变
     */
    public void setBgColorList(int[] bgColorList) {
        this.bgColorList = bgColorList;
        invalidate();
    }

    /**
     * 动态改变进度条颜色，随机的
     */
    @Deprecated // 不使用这个了
    private void animateProgressColor() {
        if (!isShowProgressColor()) {
            return;
        }
        if (colorAnimator != null) colorAnimator.cancel();

        float fraction = progress / (float) maxProgress;
        int newColor = interpolateColor(Color.GREEN, Color.RED, fraction);

        colorAnimator = ValueAnimator.ofArgb(progressPaint.getColor(), newColor);
        colorAnimator.addUpdateListener(anim -> {
            int color = (int) anim.getAnimatedValue();
            progressPaint.setColor(color);
            // 关键同步：指示器颜色随进度变化
            thumbInnerPaint.setColor(color);
            invalidate();
        });
        colorAnimator.setDuration(300);
        colorAnimator.start();
    }

    private int interpolateColor(int color1, int color2, float fraction) {
        int a = (int) (Color.alpha(color1) + (Color.alpha(color2) - Color.alpha(color1)) * fraction);
        int r = (int) (Color.red(color1) + (Color.red(color2) - Color.red(color1)) * fraction);
        int g = (int) (Color.green(color1) + (Color.green(color2) - Color.green(color1)) * fraction);
        int b = (int) (Color.blue(color1) + (Color.blue(color2) - Color.blue(color1)) * fraction);
        return Color.argb(a, r, g, b);
    }

    // 进度变化监听器
    public interface OnSeekBarChangeListener {
        void onProgressChanged(CustomSeekBar seekBar, int progress, boolean fromUser, boolean isTouchEnd);
    }

    private OnSeekBarChangeListener listener;

    public void setOnSeekBarChangeListener(OnSeekBarChangeListener listener) {
        this.listener = listener;
    }

    public void removeOnSeekBarChangeListener() {
        this.listener = null;
    }

    /**
     * 是否展示进度色，这个由progressColor不为0控制
     */
    private boolean isShowProgressColor() {
        return progressPaint != null && progressColor != 0;
    }

    // Getter/Setter方法
    public void setOrientation(boolean horizontal) {
        isHorizontal = horizontal;
        requestLayout();
        invalidate();
    }

    /**
     * 默认的粗细，横向时是高度，纵向时是宽度
     *
     * @param lineWeight 粗细，单位是dp
     */
    public void setLineWeight(float lineWeight) {
        this.lineWeight = lineWeight;
        invalidate();
    }

    /**
     * 设置最大值，避免重复设置
     *
     * @param maxProgress 最大值
     */
    public void setMaxProgress(int maxProgress) {
        if (this.maxProgress == maxProgress) {
            return;
        }
        this.maxProgress = maxProgress;
        invalidate();
    }

    /**
     * 获取进度
     */
    public int getProgress() {
        return progress;
    }

    /**
     * 设置是否由长按才触发变大并且可以滑动进度条
     */
    public void setIsLongTouchToAvailable(boolean isLongTouchToAvailable) {
        this.isLongTouchToAvailable = isLongTouchToAvailable;
    }

    /**
     * 判断是否正在滑动
     */
    public boolean isSeeking() {
        return isSeeking;
    }

    /**
     * 判断是否正在按住
     */
    public boolean isTouchMode() {
        return isTouchMode;
    }

    /**
     * 判断是否触摸了进度条的
     */
    public boolean isTouchSeekbarView() {
        return isTouchSeekbarView;
    }

    /**
     * 外部恢复
     */
    public void setCancelTouchSeekbarView() {
        this.isTouchSeekbarView = false;
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}
