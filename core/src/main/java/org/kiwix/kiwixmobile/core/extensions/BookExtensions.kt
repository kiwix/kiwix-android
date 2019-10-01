package org.kiwix.kiwixmobile.core.extensions

import org.kiwix.kiwixmobile.core.KiwixApplication
import org.kiwix.kiwixmobile.core.entity.LibraryNetworkEntity.Book
import org.kiwix.kiwixmobile.core.utils.BookUtils
import org.kiwix.kiwixmobile.core.utils.NetworkUtils

fun Book.calculateSearchMatches(
  filter: String,
  bookUtils: BookUtils
) {
  val searchableText = buildSearchableText(bookUtils)
  searchMatches = filter.split("\\s+")
    .foldRight(0,
      { filterWord, acc ->
        if (searchableText.contains(filterWord, true)) acc + 1
        else acc
      })
}

fun Book.buildSearchableText(bookUtils: BookUtils): String =
  StringBuilder().apply {
    append(title)
    append("|")
    append(description)
    append("|")
    append(NetworkUtils.parseURL(KiwixApplication.getInstance(), url))
    append("|")
    if (bookUtils.localeMap.containsKey(language)) {
      append(bookUtils.localeMap[language]!!.displayLanguage)
      append("|")
    }
  }.toString()
