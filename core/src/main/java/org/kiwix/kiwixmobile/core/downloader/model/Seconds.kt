package org.kiwix.kiwixmobile.core.downloader.model

import org.kiwix.kiwixmobile.core.KiwixApplication
import org.kiwix.kiwixmobile.core.R
import java.util.Locale
import kotlin.math.roundToLong

inline class Seconds(val seconds: Long) {
  fun toHumanReadableTime(): String {
    val minutes = 60.0
    val hours = 60 * minutes
    val days = 24 * hours

    val context = KiwixApplication.getInstance()
    return when {
      (seconds / days).roundToLong() > 0 -> String.format(
        Locale.getDefault(), "%d %s %s", (seconds / days).roundToLong(),
        context.getString(R.string.time_day),
        context.getString(R.string.time_left)
      )
      (seconds / hours).roundToLong() > 0 -> String.format(
        Locale.getDefault(), "%d %s %s", (seconds / hours).roundToLong(),
        context.getString(R.string.time_hour),
        context.getString(R.string.time_left)
      )
      ((seconds / minutes).roundToLong() > 0) -> String.format(
        Locale.getDefault(), "%d %s %s", (seconds / minutes).roundToLong(),
        context.getString(R.string.time_minute),
        context.getString(R.string.time_left)
      )
      else -> String.format(
        Locale.getDefault(), "%d %s %s", seconds,
        context.getString(R.string.time_second),
        context.getString(R.string.time_left)
      )
    }
  }
}
