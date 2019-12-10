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

package org.kiwix.kiwixmobile.core.utils;

import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.view.animation.Transformation;
import androidx.core.content.ContextCompat;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import org.kiwix.kiwixmobile.core.R;

public class AnimationUtils {
  public static void expand(final View view) {
    view.measure(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    final int targetHeight = view.getMeasuredHeight();

    // Older versions of android (pre API 21) cancel animations for views with animation height of 0.
    view.getLayoutParams().height = 1;
    Animation animation = new Animation() {
      @Override
      protected void applyTransformation(float interpolatedTime, Transformation t) {
        if (interpolatedTime == 1) {
          view.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
        } else {
          view.getLayoutParams().height = (int) (targetHeight * interpolatedTime);
        }
        view.requestLayout();
      }

      @Override
      public boolean willChangeBounds() {
        return true;
      }
    };

    // 1dp/ms
    animation.setDuration(
      (int) (targetHeight / view.getContext().getResources().getDisplayMetrics().density));
    view.startAnimation(animation);
    view.setVisibility(View.VISIBLE);
  }

  public static void collapse(final View view) {
    final int initialHeight = view.getMeasuredHeight();

    Animation animation = new Animation() {
      @Override
      protected void applyTransformation(float interpolatedTime, Transformation t) {
        if (interpolatedTime == 1) {
          view.setVisibility(View.GONE);
        } else {
          view.getLayoutParams().height = initialHeight - (int) (initialHeight * interpolatedTime);
          view.requestLayout();
        }
      }

      @Override
      public boolean willChangeBounds() {
        return true;
      }
    };

    // 1dp/ms
    animation.setDuration(
      (int) (initialHeight / view.getContext().getResources().getDisplayMetrics().density));
    view.startAnimation(animation);
  }

  //rotate animation for closeAllTabs FAB
  public static void rotate(FloatingActionButton v) {
    RotateAnimation wheelRotation =
      new RotateAnimation(0.0f, 360f, v.getWidth() / 2.0f, v.getHeight() / 2.0f);
    wheelRotation.setDuration(200);
    wheelRotation.setRepeatCount(0);
    wheelRotation.setInterpolator(v.getContext(), android.R.interpolator.cycle);
    v.startAnimation(wheelRotation);
    wheelRotation.setAnimationListener(new Animation.AnimationListener() {

      public void onAnimationEnd(Animation animation) {
        v.setImageDrawable(
          ContextCompat.getDrawable(v.getContext(), R.drawable.ic_done_white_24dp));
      }

      public void onAnimationRepeat(Animation animation) {
      }

      public void onAnimationStart(Animation animation) {
        v.setImageDrawable(
          ContextCompat.getDrawable(v.getContext(), R.drawable.ic_close_black_24dp));
      }
    });
  }
}
