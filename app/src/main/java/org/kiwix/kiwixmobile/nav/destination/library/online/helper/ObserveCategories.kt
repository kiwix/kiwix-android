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

package org.kiwix.kiwixmobile.nav.destination.library.online.helper

import kotlinx.coroutines.flow.first
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.core.zim_manager.Category
import org.kiwix.kiwixmobile.core.zim_manager.ConnectivityBroadcastReceiver
import org.kiwix.kiwixmobile.core.zim_manager.NetworkState
import org.kiwix.kiwixmobile.nav.destination.library.online.repository.CategoryRepository
import javax.inject.Inject

class ObserveCategories @Inject constructor(
  private val repository: CategoryRepository,
  private val kiwixDataStore: KiwixDataStore,
  private val connectivityBroadcastReceiver: ConnectivityBroadcastReceiver
) {
  sealed class Result {
    data class Success(val categories: List<Category>) : Result()
    data class Error(val message: String) : Result()
  }

  var hasFetched: Boolean = false

  suspend operator fun invoke(
    errorNoCategory: String,
    errorNoNetwork: String
  ): Result {
    val cachedCategoryList = kiwixDataStore.cachedOnlineCategoryList.first()
    val isOnline =
      connectivityBroadcastReceiver.networkStates.value == NetworkState.CONNECTED

    return when {
      hasFetched && !cachedCategoryList.isNullOrEmpty() -> {
        Result.Success(cachedCategoryList)
      }
      isOnline -> {
        var result: Result? = null
        repository.fetchCategories().collect { categories ->
          if (categories.isNotEmpty()) {
            kiwixDataStore.saveOnlineCategoryList(categories)
            hasFetched = true
            result = Result.Success(categories)
          } else {
            result = resolveCache(cachedCategoryList, errorNoCategory)
          }
        }
        result ?: resolveCache(cachedCategoryList, errorNoCategory)
      }
      else -> {
        resolveCache(cachedCategoryList, errorNoNetwork)
      }
    }
  }

  private fun resolveCache(
    cachedCategoryList: List<Category>?,
    errorMessage: String
  ): Result =
    if (!cachedCategoryList.isNullOrEmpty()) {
      Result.Success(cachedCategoryList)
    } else {
      Result.Error(errorMessage)
    }
}
