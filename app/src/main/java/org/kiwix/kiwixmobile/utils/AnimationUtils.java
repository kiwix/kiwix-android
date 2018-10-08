package org.kiwix.kiwixmobile.utils;

import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Transformation;

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
    animation.setDuration((int) (targetHeight / view.getContext().getResources().getDisplayMetrics().density));
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
    animation.setDuration((int) (initialHeight / view.getContext().getResources().getDisplayMetrics().density));
    view.startAnimation(animation);
  }
}
