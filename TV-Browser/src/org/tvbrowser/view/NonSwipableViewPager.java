package org.tvbrowser.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

/**
 * A {@link ViewPager} that supports disabling swipe gestures and related transitions.
 *
 * Used together with a {@link android.support.design.widget.BottomNavigationView} this
 * allows material specific cross-fade (dissolve) animations by still using a view pager to
 * handle fragments.
 */
public class NonSwipableViewPager extends ViewPager {
  private PageTransformer pageTransformer;
  private boolean pagingEnabled = true;

  public NonSwipableViewPager(final Context context, final AttributeSet attrs) {
    super(context, attrs);
    pageTransformer = new FadePageTransformer();
    setPageTransformer(false, pageTransformer);
  }

  @Override
  public boolean canScrollHorizontally(final int direction) {
    return pagingEnabled && super.canScrollHorizontally(direction);
  }

  @Override
  public boolean executeKeyEvent(@NonNull final KeyEvent event)
  {
    return pagingEnabled && super.executeKeyEvent(event);
  }

  public boolean isPagingEnabled() {
    return pagingEnabled;
  }

  @Override
  public boolean onInterceptTouchEvent(final MotionEvent event) {
    return pagingEnabled && super.onInterceptTouchEvent(event);
  }

  @SuppressLint("ClickableViewAccessibility")
  @Override
  public boolean onTouchEvent(final MotionEvent event) {
    return pagingEnabled && super.onTouchEvent(event);
  }

  public void setPagingEnabled(final boolean pagingEnabled) {
    this.pagingEnabled = pagingEnabled;
  }

  private class FadePageTransformer implements ViewPager.PageTransformer {
    public void transformPage(@NonNull final View view, final float position) {
      if (!isPagingEnabled()) {
        view.setAlpha(1 - Math.abs(position));
        view.setScrollX(position == 0 ? 0 : (int) (Math.signum(position) * ((float) (view.getWidth()) * position)));
      }
    }
  }
}