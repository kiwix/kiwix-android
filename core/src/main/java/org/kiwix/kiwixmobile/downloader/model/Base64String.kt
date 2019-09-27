package org.kiwix.kiwixmobile.downloader.model

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64

inline class Base64String(private val encodedString: String?) {
  fun toBitmap(): Bitmap? = try {
    encodedString?.let { nonNullString ->
      Base64.decode(nonNullString, Base64.DEFAULT)
        .let {
          BitmapFactory.decodeByteArray(it, 0, it.size)
        }
    }
  } catch (illegalArgumentException: IllegalArgumentException) {
    null
  }
}
