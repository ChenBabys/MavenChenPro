package com.godox.common.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.godox.common.R;

import androidx.annotation.Nullable;


/**
 * Author: ChenBabys
 * Date: 2025/4/14
 * Description: 自定义的进度条组件
 */
@Deprecated // 稍微有点不好用，暂时先不启用了
public class SeekBarWidget extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final float circleRadius;//指示器半径
    private final float circleStrokeWidth;//指示器外边框宽度
    private final float lineHeight;//整体高度
    private float percentage;
    private int progress = 0;
    private int beforeProgress;
    private int maxProgress;
    private int minProgress;
    private ColorTransition colorTransition;
    private int backgroundColor;
    private int circleStrokeColor;
    private final float horizontalPadding;
    private final float mPaddingLeft;
    private OnProgressChangeListener mListener;
    private boolean isSeeking = false;
    private boolean isClickChange = false;

    public SeekBarWidget(Context context) {
        this(context, null);
    }

    public SeekBarWidget(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SeekBarWidget(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        if (null != attrs) {
            TypedArray typedArray = getResources().obtainAttributes(attrs, R.styleable.SeekBarWidget);
            minProgress = typedArray.getInt(R.styleable.SeekBarWidget_seek_minProgress, 0);
            maxProgress = typedArray.getInt(R.styleable.SeekBarWidget_seek_maxProgress, 100) - minProgress;
            progress = typedArray.getInt(R.styleable.SeekBarWidget_seek_progress, 0) - minProgress;
            if (progress < 0) progress = minProgress;
            circleRadius = typedArray.getDimension(R.styleable.SeekBarWidget_seek_circleRadius, 16f);
            circleStrokeWidth = typedArray.getDimension(R.styleable.SeekBarWidget_seek_circleStrokeWidth, 5f);
            lineHeight = typedArray.getDimension(R.styleable.SeekBarWidget_seek_lineHeight, 5f);
            backgroundColor = typedArray.getColor(R.styleable.SeekBarWidget_seek_backgroundColor, Color.parseColor("#F0F0F0"));
            circleStrokeColor = typedArray.getColor(R.styleable.SeekBarWidget_seek_circleStrokeColor, Color.WHITE);
            int maxColor = typedArray.getColor(R.styleable.SeekBarWidget_seek_maxColor, Color.RED);
            int startColor = typedArray.getColor(R.styleable.SeekBarWidget_seek_startColor, maxColor);
            colorTransition = new ColorTransition(startColor, maxColor);
            typedArray.recycle();
            percentage = progress * 1.0f / maxProgress;
            horizontalPadding = circleRadius * 2 + circleStrokeWidth * 2 + getPaddingStart() + getPaddingEnd();
            mPaddingLeft = horizontalPadding - getPaddingEnd() - circleRadius - circleStrokeWidth;
            return;
        }

        // 这些是默认参数
        maxProgress = 100;
        minProgress = 0;
        circleRadius = 16;
        circleStrokeWidth = 5;
        lineHeight = 5;
        backgroundColor = Color.parseColor("#F0F0F0");
        colorTransition = new ColorTransition(Color.WHITE, Color.RED);
        circleStrokeColor = Color.WHITE;

        horizontalPadding = circleRadius * 2 + circleStrokeWidth * 2 + getPaddingStart() + getPaddingEnd();
        mPaddingLeft = horizontalPadding - getPaddingEnd() - circleRadius - circleStrokeWidth;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        // warp,高度足够显示圆形指示
        int minHeight = (int) (circleRadius * 3 + circleStrokeWidth * 3);
        int height = resolveSize(minHeight, heightMeasureSpec);
        setMeasuredDimension(width, height);
    }


    @SuppressWarnings("ALL")
    @Override
    protected void onDraw(Canvas canvas) {
        final int width = (int) (getMeasuredWidth() - horizontalPadding);
        final int height = getMeasuredHeight();
        // final float centerY = height >> 1;
        final float centerY = height / 2f;

        // 绘制背景线
        paint.setColor(backgroundColor);
        paint.setStrokeWidth(lineHeight);
        paint.setStyle(Paint.Style.FILL_AND_STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        canvas.drawLine(mPaddingLeft, centerY, mPaddingLeft + width, centerY, paint);

        // 绘制进度线
        float circleX = mPaddingLeft + width * percentage;
        int currColor = colorTransition.getValue(percentage);
        paint.setColor(currColor);
        canvas.drawLine(mPaddingLeft, centerY, circleX, centerY, paint);

        // 绘制圆形指示器,突出的外边圆
        float circleX2 = mPaddingLeft + width * percentage;
        // 开始绘制外边圆
        paint.setColor(circleStrokeColor);
        canvas.drawCircle(circleX2, centerY, circleRadius + (circleStrokeWidth / 2f), paint);

        // 绘制圆形指示器内圆
        paint.setColor(currColor);
        canvas.drawCircle(circleX2, centerY, circleRadius - (circleStrokeWidth / 2f), paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float eventX = event.getX();
        float eventY = event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                isSeeking = true;
                // 点击直接跳转进度
                calculateProgress(eventX, eventY);
                return true;
            case MotionEvent.ACTION_MOVE:
                if (isSeeking) {
                    calculateProgress(eventX, eventY);
                    return true;
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                isSeeking = false;
                isClickChange = true;
                if (mListener != null) {
                    safeNotifyProgressChange(true, true);
                }
                break;
        }
        // 返回true也无法解决被ItemTouchHelper抢事件,那边优先级最高，所以另解决了
        return super.onTouchEvent(event);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        getParent().requestDisallowInterceptTouchEvent(true);
        return super.dispatchTouchEvent(event);
    }

    /**
     * 计算进度条
     */
    private void calculateProgress(float eventX, float eventY) {
        float width = getMeasuredWidth() - horizontalPadding;
        // float centerY = getMeasuredHeight() >> 1;
        final float centerY = getMeasuredHeight() / 2f;
        // 计算触摸点是否在进度条范围内
        float touchX = eventX - mPaddingLeft;
        touchX = Math.max(0, Math.min(touchX, width)); // 限制在进度条范围内
        // 更新进度百分比
        percentage = touchX / width;
        percentage = Math.max(0, Math.min(1, percentage)); // 限制在 0~1
        // 更新进度值
        progress = (int) (percentage * maxProgress);
        postInvalidate();
        // 回调监听器
        if (mListener != null && (beforeProgress != getProgress())) {
            beforeProgress = getProgress();
            safeNotifyProgressChange(true, false);
        }
    }

    /**
     * 安全通知进度变化,避免外部更新进度时，潜在的UI更新问题，【虽然目前没问题】
     * isFromUser:是否来自用户滑动的，当用户滑动的不用延时，直接回调即可
     */
    private void safeNotifyProgressChange(boolean isFromUser, boolean isSeekEnd) {
        if (mListener != null) {
            if (isFromUser) {
                mListener.onProgress(getProgress(), true, isSeekEnd);
            } else {
                // 使用postDelayed避免在滚动过程中回调
                postDelayed(() -> {
                    mListener.onProgress(getProgress(), false, isSeekEnd);
                }, 32); // 延迟2帧（约16ms一帧）
            }
        }
    }

    /**
     * 判断是否正在滑动
     */
    public boolean isSeeking() {
        return isSeeking;
    }

    /**
     * 判断是否点击了改变进度
     */
    public boolean isClickChange() {
        return isClickChange;
    }

    /**
     * 外部恢复
     */
    public void setCancelClickChange() {
        this.isClickChange = false;
    }

    public int getMaxProgress() {
        return this.maxProgress - minProgress;
    }

    public int getProgress() {
        return this.progress + minProgress;
    }

    public void setProgress(int progress) {
        this.progress = Math.min(progress, getMaxProgress()) - minProgress;
        this.percentage = this.progress * 1.0f / maxProgress;
        postInvalidate();
        // 回调监听器
        if (mListener != null && (beforeProgress != getProgress())) {
            beforeProgress = getProgress();
            safeNotifyProgressChange(false, true);
        }
    }

    public void setOnProgressChangListener(OnProgressChangeListener listener) {
        this.mListener = listener;
    }

    public void removeOnProgressListener() {
        this.mListener = null;
    }

    public interface OnProgressChangeListener {
        void onProgress(int progress, boolean isFromUser, boolean isSeekEnd);
    }

    public void setMaxProgress(int maxProgress) {
        int newMaxProgress = (maxProgress - minProgress);
        if (newMaxProgress != this.maxProgress) {
            this.maxProgress = newMaxProgress;
            invalidate(); // 重绘制，否则不准确
        }
    }

    public void setProgressBackgroundColor(int color) {
        this.backgroundColor = color;
        invalidate();
    }

    public void setProgressForegroundColor(int beginColor, int endColor) {
        this.colorTransition = new ColorTransition(beginColor, endColor);
        invalidate();
    }

    public void setProgressStrokeColor(int color) {
        this.circleStrokeColor = color;
        invalidate();
    }

    public static class ColorTransition {
        private final int fromColor;
        private final int toColor;

        public ColorTransition(int beginColor, int endColor) {
            this.fromColor = beginColor;
            this.toColor = endColor;
        }

        public int getValue(float percentage) {
            int fromA = Color.alpha(fromColor);
            int fromR = Color.red(fromColor);
            int fromG = Color.green(fromColor);
            int fromB = Color.blue(fromColor);

            int toA = Color.alpha(toColor);
            int toR = Color.red(toColor);
            int toG = Color.green(toColor);
            int toB = Color.blue(toColor);

            return Color.argb(
                (int) (fromA + ((toA - fromA) * percentage)),
                (int) (fromR + ((toR - fromR) * percentage)),
                (int) (fromG + ((toG - fromG) * percentage)),
                (int) (fromB + ((toB - fromB) * percentage))
            );
        }
    }
}
