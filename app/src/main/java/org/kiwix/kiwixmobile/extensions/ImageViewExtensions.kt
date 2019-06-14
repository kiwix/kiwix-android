package org.kiwix.kiwixmobile.extensions

import android.widget.ImageView
import org.kiwix.kiwixmobile.downloader.model.Base64String

public fun ImageView.setBitmap(base64String: Base64String) {
  if (tag != base64String) {
    base64String.toBitmap()
        ?.let {
          setImageBitmap(it)
          tag = base64String
        }
  }
}