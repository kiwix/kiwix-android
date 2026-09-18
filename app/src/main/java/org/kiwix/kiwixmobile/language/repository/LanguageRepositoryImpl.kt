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
import org.kiwix.kiwixmobile.core.compat.CompatHelper.Companion.convertToLocal
import org.kiwix.kiwixmobile.core.data.remote.KiwixService
import org.kiwix.kiwixmobile.core.di.IoDispatcher
import org.kiwix.kiwixmobile.core.di.OPDSKiwixService
import org.kiwix.kiwixmobile.core.ui.components.ONE
import org.kiwix.kiwixmobile.core.utils.FIVE
import org.kiwix.kiwixmobile.core.utils.TAG_KIWIX
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.core.utils.files.Log
import org.kiwix.kiwixmobile.core.zim_manager.Language
import java.util.Locale
import javax.inject.Inject

class LanguageRepositoryImpl @Inject constructor(
  @param:OPDSKiwixService private val kiwixService: KiwixService,
  private val kiwixDataStore: KiwixDataStore,
  @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : LanguageRepository {
  private suspend fun getAppChosenLanguageCode(): String {
    val pref = runCatching { kiwixDataStore.prefLanguage.first() }.getOrDefault("")
    val locale = if (pref.isNotBlank() && pref != Locale.ROOT.toString() && pref != Locale.ROOT.language) {
      runCatching { pref.convertToLocal() }.getOrNull()
    } else {
      null
    } ?: Locale.getDefault()
    return runCatching {
      locale.isO3Language.ifEmpty { locale.language }
    }.getOrDefault("")
  }

  override fun fetchLanguages(): Flow<List<Language>> = flow {
    val feed = kiwixService.getLanguages()
    val savedLangPref = kiwixDataStore.selectedOnlineContentLanguage.first()
    val defaultAppLang = if (savedLangPref.isEmpty() || savedLangPref.equals("all", ignoreCase = true)) {
      getAppChosenLanguageCode()
    } else {
      ""
    }
    val selectedLanguagesSet = when {
      savedLangPref.isNotEmpty() && !savedLangPref.equals("all", ignoreCase = true) ->
        savedLangPref
          .split(",")
          .asSequence()
          .filter { it.isNotEmpty() }
          .toSet()
      defaultAppLang.isNotEmpty() -> setOf(defaultAppLang)
      else -> emptySet()
    }

    val languages = feed.entries.orEmpty().mapIndexedNotNull { index, entry ->
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

    emit(languages)
  }.retry(FIVE.toLong())
    .catch { e ->
      e.printStackTrace()
      emit(emptyList())
    }.flowOn(ioDispatcher)
}
