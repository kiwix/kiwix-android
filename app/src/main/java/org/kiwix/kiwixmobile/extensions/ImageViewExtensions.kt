package org.kiwix.kiwixmobile.extensions

import android.widget.ImageView
import org.kiwix.kiwixmobile.downloader.model.Base64String

fun ImageView.setBitmap(base64String: Base64String) {
  if (tag != base64String) {
    base64String.toBitmap()
      ?.let {
        setImageBitmap(it)
        tag = base64String
      }
  }
}

// methods that accept inline classes as parameters are not allowed to be called from java
// hence this facade
fun ImageView.setBitmapFromString(string: String) {
  setBitmap(Base64String(string))
}
