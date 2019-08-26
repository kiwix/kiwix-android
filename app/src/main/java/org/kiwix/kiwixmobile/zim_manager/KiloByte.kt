package org.kiwix.kiwixmobile.zim_manager

import java.text.DecimalFormat
import kotlin.math.log10
import kotlin.math.pow

inline class KiloByte(private val kilobyteString: String?) {
  val humanReadable
    get() = kilobyteString?.toLongOrNull()?.let {
      val units = arrayOf("KB", "MB", "GB", "TB")
      val conversion = (log10(it.toDouble()) / log10(1024.0)).toInt()
      (DecimalFormat("#,##0.#")
        .format(it / 1024.0.pow(conversion.toDouble())) +
        " " +
        units[conversion])
    } ?: ""
}
