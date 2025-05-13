package com.chen.freedialog;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;
import androidx.viewbinding.ViewBinding;


/**
 * 偷懒使用的方法  横竖屏切换场景  不建议使用  会不显示
 */
public class FreeDialog<VB extends ViewBinding> extends FreeCusDialog<VB> {
    private int layoutId;
    private Builder builder;

    @Override
    public VB getViewBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return null;
    }

    @NonNull
    @Override
    public LayoutInflater onGetLayoutInflater(@Nullable Bundle savedInstanceState) {
        return super.onGetLayoutInflater(savedInstanceState);
    }

    @Override
    protected void initView(Bundle savedInstanceState) {

    }

    public void setLayoutId(int layoutId, Builder builder) {
        this.layoutId = layoutId;
        this.builder = builder;
    }

    public abstract static class Builder {
        private FreeDialog freeDialog;

        public Builder(int layoutId) {
            freeDialog = new FreeDialog();
            freeDialog.setLayoutId(layoutId, this);
        }

        public abstract void onCreateView(Bundle savedInstanceState);

        public Builder setGravity(int gravity) {
            freeDialog.setGravity(gravity);
            return this;
        }

        public Builder setListener(ViewClick listener) {
            freeDialog.setListener(listener);
            return this;
        }

        protected void addViewListener(int ids) {
            freeDialog.addViewListener(ids);
        }

        public Builder setElevation(int elevation) {
            freeDialog.setElevation(elevation);
            return this;
        }

        public Builder setDimAmount(float dimAmount) {
            freeDialog.setDimAmount(dimAmount);
            return this;
        }

        public Builder setCancel(boolean cancel) {
            freeDialog.setCancel(cancel);
            return this;
        }

        public Builder setAnchor(View anchorView, int xOffset, int yOffset) {
            freeDialog.setAnchor(anchorView, xOffset, yOffset);
            return this;
        }

        public Builder setTrend(boolean trend) {
            freeDialog.setTrend(trend);
            return this;
        }

        public Builder setDrag(Boolean canDrag) {
            freeDialog.setCanDrag(canDrag);
            return this;
        }

        public Builder setTimeMills(int timeMills) {
            freeDialog.setTimeMillis(timeMills);
            return this;
        }

        public Builder setStyle(int style) {
            freeDialog.setStyle(style);
            return this;
        }

        public Builder setExitAnimation(Animation animation) {
            freeDialog.setExitAnimation(animation);
            return this;
        }

        public FreeDialog show(FragmentManager fragmentManager, String tag) {
            freeDialog.show(fragmentManager.beginTransaction(), tag);
            return freeDialog;
        }

        public void dismiss() {
            freeDialog.dismiss();
        }
    }

}