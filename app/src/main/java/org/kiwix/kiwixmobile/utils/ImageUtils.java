package org.kiwix.kiwixmobile.utils;

import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class ImageUtils {
  @Nullable
  public static Bitmap getBitmapFromView(final @NonNull View viewToDrawFrom, int width,
    int height) {
    boolean wasDrawingCacheEnabled = viewToDrawFrom.isDrawingCacheEnabled();
    if (!wasDrawingCacheEnabled) {
      viewToDrawFrom.setDrawingCacheEnabled(true);
    }
    if (width <= 0 || height <= 0) {
      if (viewToDrawFrom.getWidth() <= 0 || viewToDrawFrom.getHeight() <= 0) {
        viewToDrawFrom.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
          View.MeasureSpec
            .makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        width = viewToDrawFrom.getMeasuredWidth();
        height = viewToDrawFrom.getMeasuredHeight();
      }
      if (width <= 0 || height <= 0) {
        final Bitmap bmp = viewToDrawFrom.getDrawingCache();
        final Bitmap bitmap = bmp == null ? null : Bitmap.createBitmap(bmp);
        if (!wasDrawingCacheEnabled) {
          viewToDrawFrom.setDrawingCacheEnabled(false);
        }
        return bitmap;
      }
      viewToDrawFrom.layout(0, 0, width, height);
    } else {
      viewToDrawFrom.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
        View.MeasureSpec
          .makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
      viewToDrawFrom.layout(0, 0, viewToDrawFrom.getMeasuredWidth(),
        viewToDrawFrom.getMeasuredHeight());
    }
    final Bitmap drawingCache = viewToDrawFrom.getDrawingCache();
    final Bitmap bmp = ThumbnailUtils.extractThumbnail(drawingCache, width, height);
    final Bitmap bitmap = bmp == null || bmp != drawingCache ? bmp : Bitmap.createBitmap(bmp);
    if (!wasDrawingCacheEnabled) {
      viewToDrawFrom.setDrawingCacheEnabled(false);
    }

    return bitmap;
  }
}
