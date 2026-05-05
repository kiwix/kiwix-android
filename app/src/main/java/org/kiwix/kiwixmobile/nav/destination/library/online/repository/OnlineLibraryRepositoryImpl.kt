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
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.kiwix.kiwixmobile.core.data.remote.KiwixService.Companion.ITEMS_PER_PAGE
import org.kiwix.kiwixmobile.core.di.IoDispatcher
import org.kiwix.kiwixmobile.core.di.modules.KIWIX_OPDS_LIBRARY_URL
import org.kiwix.kiwixmobile.core.ui.components.ONE
import org.kiwix.kiwixmobile.core.utils.FIVE
import org.kiwix.kiwixmobile.data.remote.AppProgressListenerProvider
import org.kiwix.kiwixmobile.data.remote.OnlineLibraryManager
import org.kiwix.kiwixmobile.data.remote.opds.KiwixOpdsServiceFactory
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.OnlineLibraryViewModel.OnlineLibraryRequest
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.OnlineLibraryViewModel.OnlineLibraryState
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.OnlineLibraryViewModel.OnlineLibraryState.Error
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.OnlineLibraryViewModel.OnlineLibraryState.Loading
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.OnlineLibraryViewModel.OnlineLibraryState.Parsing
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.OnlineLibraryViewModel.OnlineLibraryState.Success
import retrofit2.Response
import javax.inject.Inject

class OnlineLibraryRepositoryImpl @Inject constructor(
  private val onlineLibraryManager: OnlineLibraryManager,
  private val serviceFactory: KiwixOpdsServiceFactory,
  @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : OnlineLibraryRepository {
  override fun fetchOnlineLibrary(
    request: OnlineLibraryRequest,
    appProgressListener: AppProgressListenerProvider?
  ): Flow<OnlineLibraryState> = flow {
    emit(Loading(request.isLoadMoreItem))
    val baseUrl = KIWIX_OPDS_LIBRARY_URL
    val start = onlineLibraryManager.getStartOffset(request.page, ITEMS_PER_PAGE)

    val service = serviceFactory.create(
      baseUrl = baseUrl,
      start = start,
      count = ITEMS_PER_PAGE,
      query = request.query,
      lang = request.lang,
      category = request.category,
      shouldTrackProgress = !request.isLoadMoreItem,
      appProgressListener = appProgressListener
    )
    val maxRetries = FIVE
    repeat(maxRetries) { attempt ->
      try {
        val url = onlineLibraryManager.buildLibraryUrl(
          baseUrl,
          start,
          ITEMS_PER_PAGE,
          request.query,
          request.lang,
          request.category
        )

        val response = service.getLibraryPage(url)
        val base = response.getResolvedBaseUrl()
        emit(Parsing)
        val books = onlineLibraryManager
          .parseOPDSStreamAndGetBooks(response.body(), base)
          .orEmpty()
        val totalResult = onlineLibraryManager.totalResult
        val totalPages = onlineLibraryManager.calculateTotalPages(
          totalResult,
          ITEMS_PER_PAGE
        )
        emit(Success(request, books, totalPages))
        return@flow
      } catch (ignore: Exception) {
        if (attempt == maxRetries - ONE) {
          emit(Error(request, ignore))
        }
      }
    }
  }.flowOn(ioDispatcher)

  private fun Response<String>.getResolvedBaseUrl(): String {
    val url = raw().networkResponse?.request?.url ?: raw().request.url
    return "${url.scheme}://${url.host}"
  }
}
