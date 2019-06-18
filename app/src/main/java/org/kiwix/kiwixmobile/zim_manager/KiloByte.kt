package org.kiwix.kiwixmobile.zim_manager

import java.text.DecimalFormat

inline class KiloByte(val kilobyteString: String?) {
  val humanReadable
    get() = kilobyteString?.toLongOrNull()?.let {
      val units = arrayOf("KB", "MB", "GB", "TB")
      val conversion = (Math.log10(it.toDouble()) / Math.log10(1024.0)).toInt()
      (DecimalFormat("#,##0.#")
          .format(it / Math.pow(1024.0, conversion.toDouble()))
          + " "
          + units[conversion])
    } ?: ""

}
