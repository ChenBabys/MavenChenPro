package com.jude.rollviewpager.adapter;

import android.view.View;
import android.view.ViewGroup;

import androidx.viewpager.widget.PagerAdapter;

import java.util.ArrayList;


public abstract class StaticPagerAdapter extends PagerAdapter {
    private ArrayList<View> mViewList = new ArrayList<>();

	@Override
	public boolean isViewFromObject(View arg0, Object arg1) {
		return arg0==arg1;
	}

	@Override
	public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((View) object);
    }

	@Override
	public void notifyDataSetChanged() {
        mViewList.clear();
        super.notifyDataSetChanged();
	}

	@Override
	public int getItemPosition(Object object) {
		return POSITION_NONE;
	}

	@Override
	public Object instantiateItem(ViewGroup container, int position) {
        View itemView = findViewByPosition(container,position);
        container.addView(itemView);
        onBind(itemView,position);
		return itemView;
	}

    private View findViewByPosition(ViewGroup container,int position){
        for (View view : mViewList) {
            if (((int)view.getTag()) == position&&view.getParent()==null){
                return view;
            }
        }
        View view = getView(container,position);
        view.setTag(position);
        mViewList.add(view);
        return view;
    }


    public void onBind(View view,int position){
    }

	public abstract View getView(ViewGroup container, int position);

}
