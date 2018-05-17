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
package org.kiwix.kiwixmobile.common.utils;

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
