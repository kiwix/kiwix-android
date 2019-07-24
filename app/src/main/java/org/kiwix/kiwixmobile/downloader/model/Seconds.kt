package org.kiwix.kiwixmobile.downloader.model

import org.kiwix.kiwixmobile.KiwixApplication
import java.util.Locale

inline class Seconds(private val seconds: Int) {
  fun toHumanReadableTime(): String {
    val MINUTES = 60.0
    val HOURS = 60 * MINUTES
    val DAYS = 24 * HOURS

    val context = KiwixApplication.getInstance()
    return when {
      Math.round(seconds / DAYS) > 0 -> String.format(
          Locale.getDefault(), "%d %s %s", Math.round(seconds / DAYS),
          context.getString(org.kiwix.kiwixmobile.R.string.time_day),
          context.getString(org.kiwix.kiwixmobile.R.string.time_left)
      )
      Math.round(seconds / HOURS) > 0 -> String.format(
          Locale.getDefault(), "%d %s %s", Math.round(seconds / HOURS),
          context.getString(org.kiwix.kiwixmobile.R.string.time_hour),
          context.getString(org.kiwix.kiwixmobile.R.string.time_left)
      )
      (Math.round(seconds / MINUTES) > 0) -> String.format(
          Locale.getDefault(), "%d %s %s", Math.round(seconds / MINUTES),
          context.getString(org.kiwix.kiwixmobile.R.string.time_minute),
          context.getString(org.kiwix.kiwixmobile.R.string.time_left)
      )
      else -> String.format(
          Locale.getDefault(), "%d %s %s", seconds,
          context.getString(org.kiwix.kiwixmobile.R.string.time_second),
          context.getString(org.kiwix.kiwixmobile.R.string.time_left)
      )
    }
  }
}
