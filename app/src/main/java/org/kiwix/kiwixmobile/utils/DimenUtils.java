package org.kiwix.kiwixmobile.utils;

import android.content.Context;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.util.TypedValue;

/**
 * Created by gmon on 1/13/17.
 */

public class DimenUtils {

  private static boolean isStatusBarTranslucent() {
    return Build.VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP;
  }

  /**
   * @return Height of status bar if translucency is enabled, zero otherwise.
   */
  public static int getTranslucentStatusBarHeight(Context context) {
    int id = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
    float statusBarHeight = id > 0 ? context.getResources().getDimensionPixelSize(id) : 0;
    return isStatusBarTranslucent() ? (int) statusBarHeight : 0;
  }

  public static int getToolbarHeight(Context context) {
    TypedValue t = new TypedValue();
    context.getTheme().resolveAttribute(android.support.v7.appcompat.R.attr.actionBarSize, t, true);
    return context.getResources().getDimensionPixelSize(t.resourceId);
  }

  public static int getToolbarAndStatusBarHeight(Context context) {
    return DimenUtils.getToolbarHeight(context) +
        DimenUtils.getTranslucentStatusBarHeight(context);
  }
}
