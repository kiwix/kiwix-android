package org.kiwix.kiwixmobile.intro;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.animation.Interpolator;
import android.widget.Scroller;
import androidx.viewpager.widget.ViewPager;
import java.lang.reflect.Field;

/**
 * A custom implementation of {@link ViewPager} to decrease the speed of auto-scroll animation
 * of {@link ViewPager}.
 */

public class CustomViewPager extends ViewPager {

  public CustomViewPager(Context context) {
    super(context);
    postInitViewPager();
  }

  public CustomViewPager(Context context, AttributeSet attrs) {
    super(context, attrs);
    postInitViewPager();
  }

  /**
   * Override the {@link Scroller} instance with our own class so we can change the
   * duration
   */
  private void postInitViewPager() {
    try {
      Field scroller = ViewPager.class.getDeclaredField("mScroller");
      scroller.setAccessible(true);
      Field interpolator = ViewPager.class.getDeclaredField("sInterpolator");
      interpolator.setAccessible(true);

      CustomScroller customScroller = new CustomScroller(getContext(),
          (Interpolator) interpolator.get(null));
      scroller.set(this, customScroller);
    } catch (Exception e) {
      Log.e("CustomViewPager", e.toString());
    }
  }

  class CustomScroller extends Scroller {

    CustomScroller(Context context, Interpolator interpolator) {
      super(context, interpolator);
    }

    @Override
    public void startScroll(int startX, int startY, int dx, int dy, int duration) {
      super.startScroll(startX, startY, dx, dy, duration * 3);
    }
  }
}
