package org.kiwix.kiwixmobile.views;

import android.content.Context;
import android.os.Build;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.CoordinatorLayout.Behavior;
import android.support.design.widget.Snackbar;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;

public class PageBottomTabLayoutSnackBarBehavior extends Behavior<View> {

  public PageBottomTabLayoutSnackBarBehavior() {
  }

  public PageBottomTabLayoutSnackBarBehavior(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  @Override
  public boolean layoutDependsOn(CoordinatorLayout parent, View child, View dependency) {
    if (dependency instanceof Snackbar.SnackbarLayout) {
      updateSnackbar(child, dependency);
    }
    return super.layoutDependsOn(parent, child, dependency);
  }

  @Override
  public boolean onDependentViewChanged(CoordinatorLayout parent, View child, View dependency) {
    float translationY = Math.min(0, dependency.getTranslationY() - dependency.getHeight());
    child.setTranslationY(translationY);
    return super.onDependentViewChanged(parent, child, dependency);
  }

  @Override
  public void onDependentViewRemoved(CoordinatorLayout parent, View child, View dependency) {
    ViewCompat.animate(child).translationY(0).start();
  }

  private void updateSnackbar(View child, View snackbarLayout) {
    if (snackbarLayout.getLayoutParams() instanceof CoordinatorLayout.LayoutParams
      && child.getVisibility() != View.GONE && child.getVisibility() != View.INVISIBLE) {
      CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) snackbarLayout.getLayoutParams();
      params.setAnchorId(child.getId());
      params.anchorGravity = Gravity.TOP;
      params.gravity = Gravity.TOP;
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        child.setZ(snackbarLayout.getZ() + 1);
      }
      snackbarLayout.setLayoutParams(params);
    } else {
      CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) snackbarLayout.getLayoutParams();
      params.gravity = Gravity.BOTTOM;
      snackbarLayout.setLayoutParams(params);
    }
  }
}
