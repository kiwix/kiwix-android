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

package org.kiwix.kiwixmobile.nav.destination.library.online.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.retry
import org.kiwix.kiwixmobile.core.data.remote.KiwixService
import org.kiwix.kiwixmobile.core.di.OPDSKiwixService
import org.kiwix.kiwixmobile.core.di.IoDispatcher
import org.kiwix.kiwixmobile.core.ui.components.ONE
import org.kiwix.kiwixmobile.core.utils.FIVE
import org.kiwix.kiwixmobile.core.utils.ZERO
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.core.zim_manager.Category
import javax.inject.Inject

class CategoryRepositoryImpl @Inject constructor(
  @OPDSKiwixService private val kiwixService: KiwixService,
  private val kiwixDataStore: KiwixDataStore,
  @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : CategoryRepository {
  @Suppress("MagicNumber")
  override fun fetchCategories(): Flow<List<Category>> = flow {
    val feed = kiwixService.getCategories()
    val selectedCategoriesRaw = kiwixDataStore.selectedOnlineContentCategory.first()
    val selectedCategories = selectedCategoriesRaw
      .split(",")
      .asSequence()
      .filter { it.isNotEmpty() }
      .toSet()

    val categories = feed.entries.orEmpty().mapIndexed { index, entry ->
      Category(
        category = entry.title,
        active = entry.title in selectedCategories,
        id = (index + ONE).toLong()
      )
    }

    val categoryList =
      when {
        categories.isEmpty() -> emptyList()
        else -> buildList {
          add(
            Category(
              category = "",
              active = selectedCategoriesRaw.isEmpty(),
              id = ZERO.toLong()
            )
          )
          addAll(categories)
        }
      }
    emit(categoryList)
  }.retry(FIVE.toLong())
    .catch { e ->
      e.printStackTrace()
      emit(emptyList())
    }.flowOn(ioDispatcher)
}
