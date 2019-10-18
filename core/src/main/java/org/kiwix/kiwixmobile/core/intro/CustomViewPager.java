/*
 * Kiwix Android
 * Copyright (c) 2019 Kiwix <android.kiwix.org>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.kiwix.kiwixmobile.core.intro;

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
