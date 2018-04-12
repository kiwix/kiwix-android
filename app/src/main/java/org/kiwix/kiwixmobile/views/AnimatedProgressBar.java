/*
 * Kiwix Android
 * Copyright (C) 2018  Kiwix <android.kiwix.org>
 *
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
 */
package org.kiwix.kiwixmobile.views;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Transformation;
import android.widget.LinearLayout;

import org.kiwix.kiwixmobile.R;

public class AnimatedProgressBar extends LinearLayout {

  private final Paint mPaint = new Paint();

  private final Rect mRect = new Rect();

  private int mProgress = 0;

  private boolean mBidirectionalAnimate = true;

  private int mDrawWidth = 0;

  private int mProgressColor;

  public AnimatedProgressBar(Context context, AttributeSet attrs) {
    super(context, attrs);
    init(context, attrs);
  }

  public AnimatedProgressBar(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init(context, attrs);
  }

  /**
   * Initialize the AnimatedProgressBar
   *
   * @param context is the context passed by the constructor
   * @param attrs is the attribute set passed by the constructor
   */
  private void init(final Context context, AttributeSet attrs) {
    TypedArray array = context.getTheme()
        .obtainStyledAttributes(attrs, R.styleable.AnimatedProgressBar, 0, 0);
    int backgroundColor;
    try {   // Retrieve the style of the progress bar that the user hopefully set
      int DEFAULT_BACKGROUND_COLOR = 0x424242;
      int DEFAULT_PROGRESS_COLOR = 0x2196f3;

      backgroundColor = array.getColor(R.styleable.AnimatedProgressBar_backgroundColor,
          DEFAULT_BACKGROUND_COLOR);
      mProgressColor = array.getColor(R.styleable.AnimatedProgressBar_progressColor,
          DEFAULT_PROGRESS_COLOR);
      mBidirectionalAnimate = array
          .getBoolean(R.styleable.AnimatedProgressBar_bidirectionalAnimate, false);
    } finally {
      array.recycle();
    }

    LayoutInflater inflater = (LayoutInflater) context
        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    inflater.inflate(R.layout.progress_bar, this, true);

    // set the background color for this view
    this.setBackgroundColor(backgroundColor);
  }

  /**
   * Returns the current progress value between 0 and 100
   *
   * @return progress of the view
   */
  public int getProgress() {
    return mProgress;
  }

  /**
   * sets the progress as an integer value between 0 and 100. Values above or below that interval
   * will be adjusted to their nearest value within the interval, i.e. setting a value of 150 will
   * have the effect of setting the progress to 100. You cannot trick us.
   *
   * @param progress an integer between 0 and 100
   */
  public void setProgress(int progress) {

    if (progress > 100) {       // progress cannot be greater than 100
      progress = 100;
    } else if (progress < 0) {  // progress cannot be less than 0
      progress = 0;
    }

    if (this.getAlpha() < 1.0f) {
      fadeIn();
    }

    int mWidth = this.getMeasuredWidth();
    // Set the drawing bounds for the ProgressBar
    mRect.left = 0;
    mRect.top = 0;
    mRect.bottom = this.getBottom() - this.getTop();
    if (progress < mProgress
        && !mBidirectionalAnimate) {   // if the we only animate the view in one direction
      // then reset the view width if it is less than the
      // previous progress
      mDrawWidth = 0;
    } else if (progress
        == mProgress) {     // we don't need to go any farther if the progress is unchanged
      if (progress == 100) {
        fadeOut();
      }
      return;
    }

    mProgress = progress;       // save the progress

    final int deltaWidth = (mWidth * mProgress / 100)
        - mDrawWidth;     // calculate amount the width has to change

    animateView(mDrawWidth, mWidth, deltaWidth);    // animate the width change
  }

  @Override
  protected void onDraw(Canvas canvas) {
    mPaint.setColor(mProgressColor);
    mPaint.setStrokeWidth(10);
    mRect.right = mRect.left + mDrawWidth;
    canvas.drawRect(mRect, mPaint);
  }

  /**
   * private method used to create and run the animation used to change the progress
   *
   * @param initialWidth is the width at which the progress starts at
   * @param maxWidth is the maximum width (total width of the view)
   * @param deltaWidth is the amount by which the width of the progress view will change
   */
  private void animateView(final int initialWidth, final int maxWidth, final int deltaWidth) {
    Animation fill = new Animation() {

      @Override
      protected void applyTransformation(float interpolatedTime, Transformation t) {
        int width = initialWidth + (int) (deltaWidth * interpolatedTime);
        if (width <= maxWidth) {
          mDrawWidth = width;
          invalidate();
        }
        if ((1.0f - interpolatedTime) < 0.0005) {
          if (mProgress >= 100) {
            fadeOut();
          }
        }
      }

      @Override
      public boolean willChangeBounds() {
        return false;
      }
    };

    fill.setDuration(500);
    fill.setInterpolator(new DecelerateInterpolator());
    this.startAnimation(fill);
  }

  /**
   * fades in the progress bar
   */
  private void fadeIn() {
    ObjectAnimator fadeIn = ObjectAnimator.ofFloat(this, "alpha", 1.0f);
    fadeIn.setDuration(200);
    fadeIn.setInterpolator(new DecelerateInterpolator());
    fadeIn.start();
  }

  /**
   * fades out the progress bar
   */
  private void fadeOut() {
    ObjectAnimator fadeOut = ObjectAnimator.ofFloat(this, "alpha", 0.0f);
    fadeOut.setDuration(200);
    fadeOut.setInterpolator(new DecelerateInterpolator());
    fadeOut.start();
  }

  @Override
  protected void onRestoreInstanceState(Parcelable state) {

    if (state instanceof Bundle) {
      Bundle bundle = (Bundle) state;
      this.mProgress = bundle.getInt("progressState");
      state = bundle.getParcelable("instanceState");
    }

    super.onRestoreInstanceState(state);
  }

  @Override
  protected Parcelable onSaveInstanceState() {

    Bundle bundle = new Bundle();
    bundle.putParcelable("instanceState", super.onSaveInstanceState());
    bundle.putInt("progressState", mProgress);
    return bundle;
  }
}
