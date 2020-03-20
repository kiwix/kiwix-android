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

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import org.kiwix.kiwixmobile.core.R;

public class AnimationUtils {
  public static void expand(final View v) {
    v.measure(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    final int targetHeight = v.getMeasuredHeight();

    // Older versions of android (pre API 21) cancel animations for views with a height of 0.
    v.getLayoutParams().height = 1;
    v.setVisibility(View.VISIBLE);

    ValueAnimator va = ValueAnimator.ofInt(1, targetHeight);
    va.addUpdateListener(animation -> {
      v.getLayoutParams().height = (Integer) animation.getAnimatedValue();
      v.requestLayout();
    });
    va.addListener(new Animator.AnimatorListener() {
      @Override
      public void onAnimationEnd(Animator animation) {
        v.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
      }

      @Override public void onAnimationStart(Animator animation) {}
      @Override public void onAnimationCancel(Animator animation) {}
      @Override public void onAnimationRepeat(Animator animation) {}
    });
    va.setDuration(100);
    va.setInterpolator(new LinearInterpolator());
    va.start();
  }

  public static void collapse(final @NonNull View v) {
    final int initialHeight = v.getMeasuredHeight();

    ValueAnimator va = ValueAnimator.ofInt(initialHeight, 0);
    va.addUpdateListener(animation -> {
      v.getLayoutParams().height = (Integer) animation.getAnimatedValue();
      v.requestLayout();
    });
    va.addListener(new Animator.AnimatorListener() {
      @Override
      public void onAnimationEnd(Animator animation) {
        v.setVisibility(View.GONE);
      }

      @Override public void onAnimationStart(Animator animation) {}
      @Override public void onAnimationCancel(Animator animation) {}
      @Override public void onAnimationRepeat(Animator animation) {}
    });
    va.setDuration(100);
    va.setInterpolator(new LinearInterpolator());
    va.start();
  }


  //rotate animation for closeAllTabs FAB
  public static void rotate(@NonNull FloatingActionButton v) {
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
