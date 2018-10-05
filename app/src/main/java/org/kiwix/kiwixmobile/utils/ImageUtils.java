package org.kiwix.kiwixmobile.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;
import android.view.View;

import org.kiwix.kiwixmobile.R;

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

  /**
   * Decode and create a Bitmap from the 64-Bit encoded favicon string
   *
   * @param context       {@link Context} for getting the app icon if string is not decoded properly
   * @param encodedString 64 bit encoded string
   * @return {@link Bitmap}
   */
  public static Bitmap createBitmapFromEncodedString(Context context, String encodedString) {

    try {
      byte[] decodedString = Base64.decode(encodedString, Base64.DEFAULT);
      return BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
    } catch (Exception e) {
      e.printStackTrace();
    }

    return BitmapFactory.decodeResource(context.getResources(), R.mipmap.kiwix_icon);
  }
}
