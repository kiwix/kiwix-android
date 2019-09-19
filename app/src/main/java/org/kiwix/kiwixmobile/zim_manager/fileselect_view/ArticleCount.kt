package org.kiwix.kiwixmobile.zim_manager.fileselect_view

import android.content.Context
import android.util.Log
import org.kiwix.kiwixmobile.R.string
import java.text.DecimalFormat
import kotlin.math.log10
import kotlin.math.pow

inline class ArticleCount(val articleCount: String) {
  fun toHumanReadable(context: Context): String = try {
    val size = Integer.parseInt(articleCount)
    if (size <= 0) {
      ""
    } else {
      val units = arrayOf("", "K", "M", "B", "T")
      val conversion = (log10(size.toDouble()) / 3).toInt()
      context.getString(
        string.articleCount, DecimalFormat("#,##0.#")
          .format(size / 1000.0.pow(conversion.toDouble())) + units[conversion]
      )
    }
  } catch (e: NumberFormatException) {
    Log.d("BooksAdapter", "$e")
    ""
  }
}
