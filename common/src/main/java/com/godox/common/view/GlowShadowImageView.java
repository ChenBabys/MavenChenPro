package com.godox.common.view;


import android.content.Context;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;

/**
 * Author: ChenBabys
 * Date: 2025/4/14
 * Description: 实现背景有光晕
 */
public class GlowShadowImageView extends AppCompatImageView {
    private final Paint mGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public GlowShadowImageView(Context context) {
        this(context, null);
    }

    public GlowShadowImageView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GlowShadowImageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mGlowPaint.setColor(Color.parseColor("#29FFFFFF")); // 光晕基础色
        mGlowPaint.setMaskFilter(new BlurMaskFilter(30, BlurMaskFilter.Blur.NORMAL));
        // 必须关闭硬件加速,指定mGlowPaint
        // setLayerType(LAYER_TYPE_SOFTWARE, mGlowPaint);
        // 必须关闭硬件加速。不需要指定paint，不然会导致有部分图被光晕覆盖的
        setLayerType(LAYER_TYPE_SOFTWARE, null);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // 先绘制光晕背景，radius半径getHeight()) / 3f
        canvas.drawCircle(getWidth() / 2f, getHeight() / 2f,
            Math.min(getWidth(), getHeight()) / 3f, mGlowPaint);
        // 再绘制原图
        super.onDraw(canvas);
    }

}
