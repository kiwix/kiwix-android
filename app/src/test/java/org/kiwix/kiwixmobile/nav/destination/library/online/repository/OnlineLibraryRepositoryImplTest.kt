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

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.kiwix.kiwixmobile.core.data.remote.KiwixService
import org.kiwix.kiwixmobile.core.utils.FIVE
import org.kiwix.kiwixmobile.data.remote.OnlineLibraryManager
import org.kiwix.kiwixmobile.data.remote.opds.KiwixOpdsServiceFactory
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.OnlineLibraryViewModel.OnlineLibraryRequest
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.OnlineLibraryViewModel.OnlineLibraryState.Error
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.OnlineLibraryViewModel.OnlineLibraryState.Loading
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.OnlineLibraryViewModel.OnlineLibraryState.Parsing
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.OnlineLibraryViewModel.OnlineLibraryState.Success
import org.kiwix.sharedFunctions.MainDispatcherRule
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class OnlineLibraryRepositoryImplTest {
  @RegisterExtension
  @JvmField
  val dispatcherRule = MainDispatcherRule()
  private val onlineLibraryManager: OnlineLibraryManager = mockk()
  private val kiwixService: KiwixService = mockk()
  private val factory: KiwixOpdsServiceFactory = mockk()

  private lateinit var repository: OnlineLibraryRepositoryImpl

  @BeforeEach
  fun setup() {
    repository = OnlineLibraryRepositoryImpl(
      onlineLibraryManager,
      factory,
      dispatcherRule.dispatcher
    )
    every {
      factory.create(any(), any(), any(), any(), any(), any(), any(), any())
    } returns kiwixService
  }

  @Test
  fun `emits Loading, Parsing, Success`() = runTest {
    val request = OnlineLibraryRequest(page = 0, isLoadMoreItem = false)

    val response = mockk<Response<String>>(relaxed = true)
    every { onlineLibraryManager.getStartOffset(any(), any()) } returns 0
    every {
      onlineLibraryManager.buildLibraryUrl(
        any(),
        any(),
        any(),
        any(),
        any(),
        any()
      )
    } returns "url"
    coEvery { kiwixService.getLibraryPage(any()) } returns response
    every { response.body() } returns "xml"
    coEvery {
      onlineLibraryManager.parseOPDSStreamAndGetBooks(
        any(),
        any()
      )
    } returns arrayListOf()
    every { onlineLibraryManager.totalResult } returns 10
    every { onlineLibraryManager.calculateTotalPages(any(), any()) } returns 1

    val result = repository.fetchOnlineLibrary(request, null).toList()
    advanceUntilIdle()
    assertTrue(result[0] is Loading)
    assertTrue(result[1] is Parsing)
    assertTrue(result[2] is Success)
  }

  @Test
  fun `emits Error after max retries`() = runTest {
    val request = OnlineLibraryRequest(page = 0, isLoadMoreItem = false)

    every { onlineLibraryManager.getStartOffset(any(), any()) } returns 0
    every {
      onlineLibraryManager.buildLibraryUrl(
        any(),
        any(),
        any(),
        any(),
        any(),
        any()
      )
    } returns "url"

    coEvery { kiwixService.getLibraryPage(any()) } throws RuntimeException("network error")

    val result = repository.fetchOnlineLibrary(request, null).toList()
    advanceUntilIdle()
    assertTrue(result.size == 2)
    assertTrue(result[0] is Loading)
    assertTrue(result[1] is Error)
  }

  @Test
  fun `retries and succeeds before max attempts`() = runTest {
    val request = OnlineLibraryRequest(page = 0, isLoadMoreItem = false)

    val response = mockk<Response<String>>(relaxed = true)

    every { onlineLibraryManager.getStartOffset(any(), any()) } returns 0
    every {
      onlineLibraryManager.buildLibraryUrl(
        any(),
        any(),
        any(),
        any(),
        any(),
        any()
      )
    } returns "url"

    coEvery { kiwixService.getLibraryPage(any()) }
      .throws(RuntimeException())
      .andThenThrows(RuntimeException())
      .andThen(response)

    every { response.body() } returns "xml"
    coEvery { onlineLibraryManager.parseOPDSStreamAndGetBooks(any(), any()) } returns arrayListOf()
    every { onlineLibraryManager.totalResult } returns 10
    every { onlineLibraryManager.calculateTotalPages(any(), any()) } returns 1

    val result = repository.fetchOnlineLibrary(request, null).toList()

    assertTrue(result.any { it is Success })
  }

  @Test
  fun `emits Loading with loadMore true`() = runTest {
    val request = OnlineLibraryRequest(page = 0, isLoadMoreItem = true)

    val response = mockk<Response<String>>(relaxed = true)

    every { onlineLibraryManager.getStartOffset(any(), any()) } returns 0
    every {
      onlineLibraryManager.buildLibraryUrl(
        any(),
        any(),
        any(),
        any(),
        any(),
        any()
      )
    } returns "url"
    coEvery { kiwixService.getLibraryPage(any()) } returns response
    every { response.body() } returns "xml"
    coEvery { onlineLibraryManager.parseOPDSStreamAndGetBooks(any(), any()) } returns arrayListOf()
    every { onlineLibraryManager.totalResult } returns 10
    every { onlineLibraryManager.calculateTotalPages(any(), any()) } returns 1

    val result = repository.fetchOnlineLibrary(request, null).toList()

    assertTrue((result[0] as Loading).isLoadMore)
  }

  @Test
  fun `handles null response body`() = runTest {
    val request = OnlineLibraryRequest(page = 0, isLoadMoreItem = false)

    val response = mockk<Response<String>>(relaxed = true)

    every { onlineLibraryManager.getStartOffset(any(), any()) } returns 0
    every {
      onlineLibraryManager.buildLibraryUrl(
        any(),
        any(),
        any(),
        any(),
        any(),
        any()
      )
    } returns "url"
    coEvery { kiwixService.getLibraryPage(any()) } returns response
    every { response.body() } returns null
    coEvery { onlineLibraryManager.parseOPDSStreamAndGetBooks(any(), any()) } returns null
    every { onlineLibraryManager.totalResult } returns 0
    every { onlineLibraryManager.calculateTotalPages(any(), any()) } returns 0

    val result = repository.fetchOnlineLibrary(request, null).toList()

    assertTrue(result.any { it is Success })
  }

  @Test
  fun `retries exact number of times`() = runTest {
    val request = OnlineLibraryRequest(page = 0, isLoadMoreItem = false)

    every { onlineLibraryManager.getStartOffset(any(), any()) } returns 0
    every {
      onlineLibraryManager.buildLibraryUrl(
        any(),
        any(),
        any(),
        any(),
        any(),
        any()
      )
    } returns "url"

    coEvery { kiwixService.getLibraryPage(any()) } throws RuntimeException()

    repository.fetchOnlineLibrary(request, null).toList()

    coVerify(exactly = FIVE * 2) { kiwixService.getLibraryPage(any()) }
  }
}
