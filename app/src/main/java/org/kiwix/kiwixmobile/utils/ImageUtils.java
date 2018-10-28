package org.kiwix.kiwixmobile.utils;

import android.graphics.Bitmap;
import android.util.Log;
import android.view.View;

public class ImageUtils {
  public static Bitmap getBitmapFromView(View view) {
    view.clearFocus();
    view.setPressed(false);

    boolean willNotCache = view.willNotCacheDrawing();
    view.setWillNotCacheDrawing(false);

    /*
     * Reset the drawing cache background color to fully transparent
     * for the duration of this operation
     */
    int color = view.getDrawingCacheBackgroundColor();
    view.setDrawingCacheBackgroundColor(0);

    if (color != 0) {
      view.destroyDrawingCache();
    }
    view.buildDrawingCache();
    Bitmap cacheBitmap = view.getDrawingCache();
    if (cacheBitmap == null) {
      Log.e("ImageUtils", "Failed getViewBitmap(" + view + ")");
      return null;
    }
    Bitmap bitmap = Bitmap.createBitmap(cacheBitmap);

    // Restore the view
    view.destroyDrawingCache();
    view.setWillNotCacheDrawing(willNotCache);
    view.setDrawingCacheBackgroundColor(color);

    return bitmap;
  }
}
