package org.kiwix.kiwixmobile.zimManager

import android.content.Context
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.compat.CompatHelper.Companion.convertToLocal
import org.kiwix.kiwixmobile.core.ui.components.ONE

object OnlineLibraryHeaderHelper {
  fun getOnlineLibrarySectionTitle(
    selectedLanguage: String,
    context: Context
  ): String {
    return if (selectedLanguage.isBlank()) {
      context.getString(R.string.all_languages)
    } else {
      val languages = selectedLanguage.split(",").filter { it.isNotEmpty() }
      val languageCount = languages.size
      when {
        languageCount == ONE -> {
          context.getString(
            R.string.your_language,
            languages.first().convertToLocal().displayLanguage
          )
        }

        else -> {
          val joinedLanguages =
            languages.joinToString(", ") { it.convertToLocal().displayLanguage }
          "${context.getString(R.string.your_languages)} $joinedLanguages"
        }
      }
    }
  }
}
