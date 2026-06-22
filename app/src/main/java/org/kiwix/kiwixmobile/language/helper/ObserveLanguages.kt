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

package org.kiwix.kiwixmobile.language.helper

import kotlinx.coroutines.flow.first
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.core.zim_manager.ConnectivityBroadcastReceiver
import org.kiwix.kiwixmobile.core.zim_manager.Language
import org.kiwix.kiwixmobile.core.zim_manager.NetworkState
import org.kiwix.kiwixmobile.language.repository.LanguageRepository
import javax.inject.Inject

class ObserveLanguages @Inject constructor(
  private val repository: LanguageRepository,
  private val kiwixDataStore: KiwixDataStore,
  private val connectivityBroadcastReceiver: ConnectivityBroadcastReceiver
) {
  sealed class Result {
    data class Success(val languages: List<Language>) : Result()
    data class Error(val message: String) : Result()
  }

  var hasFetched: Boolean = false

  suspend operator fun invoke(
    errorNoLanguage: String,
    errorNoNetwork: String
  ): Result {
    val cachedLanguageList = kiwixDataStore.cachedLanguageList.first()
    val isOnline =
      connectivityBroadcastReceiver.networkStates.value == NetworkState.CONNECTED

    return when {
      hasFetched && !cachedLanguageList.isNullOrEmpty() -> {
        Result.Success(cachedLanguageList)
      }

      isOnline -> {
        var result: Result? = null
        repository.fetchLanguages().collect { languages ->
          if (languages.isNotEmpty()) {
            kiwixDataStore.saveLanguageList(languages)
            hasFetched = true
            result = Result.Success(languages)
          } else {
            result = resolveCache(cachedLanguageList, errorNoLanguage)
          }
        }
        result ?: resolveCache(cachedLanguageList, errorNoLanguage)
      }

      else -> {
        resolveCache(cachedLanguageList, errorNoNetwork)
      }
    }
  }

  private fun resolveCache(
    cachedLanguageList: List<Language>?,
    errorMessage: String
  ): Result =
    if (!cachedLanguageList.isNullOrEmpty()) {
      Result.Success(cachedLanguageList)
    } else {
      Result.Error(errorMessage)
    }
}
