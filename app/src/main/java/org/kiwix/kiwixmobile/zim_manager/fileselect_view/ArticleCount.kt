package org.kiwix.kiwixmobile.zim_manager.fileselect_view

import android.content.Context
import android.util.Log
import org.kiwix.kiwixmobile.R.string
import java.text.DecimalFormat

inline class ArticleCount(val articleCount: String) {
  fun toHumanReadable(context: Context) = try {
    val size = Integer.parseInt(articleCount)
    if (size <= 0) {
      ""
    } else {
      val units = arrayOf("", "K", "M", "B", "T")
      val conversion = (Math.log10(size.toDouble()) / 3).toInt()
      context.getString(
          string.articleCount, DecimalFormat("#,##0.#")
          .format(size / Math.pow(1000.0, conversion.toDouble())) + units[conversion]
      )
    }
  } catch (e: NumberFormatException) {
    Log.d("BooksAdapter", e.toString())
    ""
  }
}
