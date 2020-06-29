package com.rubengees.introduction.view;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.animation.DecelerateInterpolator;
import android.widget.Scroller;

import java.lang.reflect.Field;

public class SmoothViewPager extends ViewPager {

	public SmoothViewPager(Context context, AttributeSet attrs) {
		super(context, attrs);
		setMyScroller();
	}

	private void setMyScroller() {
		try {
			Class<?> viewpager = ViewPager.class;
			Field scroller = viewpager.getDeclaredField("mScroller");
			scroller.setAccessible(true);
			scroller.set(this, new SmoothScroller(getContext()));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static class SmoothScroller extends Scroller {
		public SmoothScroller(Context context) {
			super(context, new DecelerateInterpolator());
		}

		@Override
		public void startScroll(int startX, int startY, int dx, int dy, int duration) {
			super.startScroll(startX, startY, dx, dy, 300);
		}
	}
}
