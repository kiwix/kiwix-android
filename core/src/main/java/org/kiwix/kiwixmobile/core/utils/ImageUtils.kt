package org.kiwix.kiwixmobile.core.utils

import android.graphics.Bitmap
import android.graphics.Bitmap.createBitmap
import android.media.ThumbnailUtils
import android.view.View
import android.view.View.MeasureSpec

object ImageUtils {
  @JvmStatic
  fun getBitmapFromView(
    viewToDrawFrom: View,
    width: Int,
    height: Int
  ): Bitmap? {
    val previousDrawingCacheState = viewToDrawFrom.isDrawingCacheEnabled
    viewToDrawFrom.isDrawingCacheEnabled = true
    return getBitmapFromView(
      width,
      height,
      viewToDrawFrom
    )
      .also { viewToDrawFrom.isDrawingCacheEnabled = previousDrawingCacheState }
  }

  private fun getBitmapFromView(width: Int, height: Int, viewToDrawFrom: View): Bitmap? {
    if (width <= 0 || height <= 0) {
      if (viewToDrawFrom.width <= 0 || viewToDrawFrom.height <= 0) {
        return viewToDrawFrom.measure(0, 0, MeasureSpec.UNSPECIFIED).let {
          if (it.width <= 0 || it.height <= 0) viewToDrawFrom.createBitmap()
          else layoutAndCreateBitmap(
            it.width,
            it.height,
            viewToDrawFrom,
            it
          )
        }
      }
      return viewToDrawFrom.createBitmap()
    }
    return layoutAndCreateBitmap(
      width,
      height,
      viewToDrawFrom,
      viewToDrawFrom.measure(width, height, MeasureSpec.EXACTLY)
    )
  }

  private fun layoutAndCreateBitmap(
    width: Int,
    height: Int,
    viewToDrawFrom: View,
    measuredView: MeasuredView
  ): Bitmap? {
    viewToDrawFrom.layout(0, 0, measuredView.width, measuredView.height)
    return viewToDrawFrom.createBitmap(width, height)
  }

  private fun View.createBitmap(): Bitmap? = drawingCache?.let(::createBitmap)

  private fun View.createBitmap(
    width: Int,
    height: Int
  ): Bitmap? = ThumbnailUtils.extractThumbnail(drawingCache, width, height).let {
    if (it == null || it != drawingCache) it
    else createBitmap(it)
  }

  private fun View.measure(
    width: Int,
    height: Int,
    measureSpec: Int = MeasureSpec.EXACTLY
  ): MeasuredView {
    measure(
      MeasureSpec.makeMeasureSpec(width, measureSpec),
      MeasureSpec.makeMeasureSpec(height, measureSpec)
    )
    return MeasuredView(this)
  }
}

inline class MeasuredView(private val view: View) {
  val width: Int
    get() = view.measuredWidth
  val height: Int
    get() = view.measuredHeight
}
