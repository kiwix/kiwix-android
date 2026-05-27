/*
 * Kiwix Android
 * Copyright (c) 2026 Kiwix <android.kiwix.org>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.kiwix.kiwixmobile.language.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.retry
import org.kiwix.kiwixmobile.core.data.remote.KiwixService
import org.kiwix.kiwixmobile.core.di.CategoryKiwixService
import org.kiwix.kiwixmobile.core.di.IoDispatcher
import org.kiwix.kiwixmobile.core.ui.components.ONE
import org.kiwix.kiwixmobile.core.utils.FIVE
import org.kiwix.kiwixmobile.core.utils.TAG_KIWIX
import org.kiwix.kiwixmobile.core.utils.ZERO
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.core.utils.files.Log
import org.kiwix.kiwixmobile.core.zim_manager.Language
import javax.inject.Inject

class LanguageRepositoryImpl @Inject constructor(
  @CategoryKiwixService private val kiwixService: KiwixService,
  private val kiwixDataStore: KiwixDataStore,
  @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : LanguageRepository {
  override fun fetchLanguages(): Flow<List<Language>> = flow {
    val feed = kiwixService.getLanguages()
    val selectedLanguagesSet = kiwixDataStore.selectedOnlineContentLanguage.first()
      .split(",")
      .asSequence()
      .filter { it.isNotEmpty() }
      .toSet()
    var allBooksCount = ZERO

    val languages = feed.entries.orEmpty().mapIndexedNotNull { index, entry ->
      allBooksCount += entry.count
      runCatching {
        Language(
          languageCode = entry.languageCode,
          active = entry.languageCode in selectedLanguagesSet,
          occurrencesOfLanguage = entry.count,
          id = (index + ONE).toLong()
        )
      }.onFailure {
        Log.w(TAG_KIWIX, "Unsupported locale code: ${entry.languageCode}", it)
      }.getOrNull()
    }

    val languageList =
      when {
        languages.isEmpty() -> emptyList()
        else -> buildList {
          add(
            Language(
              languageCode = "",
              active = selectedLanguagesSet.isEmpty(),
              occurrencesOfLanguage = allBooksCount,
              id = ZERO.toLong()
            )
          )
          addAll(languages)
        }
      }
    emit(languageList)
  }.retry(FIVE.toLong())
    .catch { e ->
      e.printStackTrace()
      emit(emptyList())
    }.flowOn(ioDispatcher)
}
