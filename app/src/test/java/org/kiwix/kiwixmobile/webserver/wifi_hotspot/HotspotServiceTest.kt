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

package org.kiwix.kiwixmobile.webserver.wifi_hotspot

import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.kiwix.kiwixmobile.core.data.DataSource
import org.kiwix.kiwixmobile.core.entity.LibkiwixBook
import org.kiwix.kiwixmobile.core.utils.ServerUtils
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.BooksOnDiskListItem.BookOnDisk
import org.kiwix.kiwixmobile.webserver.WebServerHelper
import org.kiwix.kiwixmobile.webserver.ZimHostCallbacks

/**
 * Covers HotspotService.resyncServerWithHostedBooks() - the handler that reacts to
 * LibkiwixBookOnDisk.bookRemovals (issue #4341): the *server* must restart with the
 * reduced book list when a hosted ZIM is deleted, not just the Hotspot screen's UI.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HotspotServiceTest {
  private val webServerHelper: WebServerHelper = mockk(relaxed = true)
  private val dataSource: DataSource = mockk()
  private val zimHostCallbacks: ZimHostCallbacks = mockk(relaxed = true)

  private lateinit var hotspotService: HotspotService

  private fun bookOnDisk(id: String, path: String) =
    BookOnDisk(LibkiwixBook(_id = id, _path = path))

  @Before
  fun setUp() {
    clearAllMocks()
    hotspotService = spyk(HotspotService())
    hotspotService.webServerHelper = webServerHelper
    hotspotService.dataSource = dataSource
    hotspotService.ioDispatcher = Dispatchers.Unconfined
    hotspotService.serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
    hotspotService.registerCallBack(zimHostCallbacks)
    every { hotspotService.startForeground(any(), any()) } returns Unit
    every { hotspotService.stopForeground(any<Int>()) } returns Unit
    every { hotspotService.stopSelf() } returns Unit
    ServerUtils.isServerStarted = false
  }

  @After
  fun tearDown() {
    ServerUtils.isServerStarted = false
    hotspotService.serviceScope.cancel()
  }

  @Test
  fun `resync when server not started does nothing`() = runTest {
    ServerUtils.isServerStarted = false

    hotspotService.resyncServerWithRemainingBooks()

    coVerify(exactly = 0) { webServerHelper.startServerHelper(any(), any()) }
    verify(exactly = 0) { webServerHelper.stopAndroidWebServer() }
  }

  @Test
  fun `resync when the removed book was not hosted leaves the server untouched`() = runTest {
    ServerUtils.isServerStarted = true
    hotspotService.currentlyHostedPaths = listOf("/path1")
    every { dataSource.getLanguageCategorizedBooks() } returns flowOf(
      listOf(bookOnDisk("id1", "/path1"))
    )

    hotspotService.resyncServerWithRemainingBooks()

    coVerify(exactly = 0) { webServerHelper.startServerHelper(any(), any()) }
    verify(exactly = 0) { webServerHelper.stopAndroidWebServer() }
  }

  @Test
  fun `resync when a hosted book was removed restarts with the remaining paths`() = runTest {
    ServerUtils.isServerStarted = true
    hotspotService.currentlyHostedPaths = listOf("/path1", "/path2")
    every { dataSource.getLanguageCategorizedBooks() } returns flowOf(
      listOf(bookOnDisk("id1", "/path1"))
    )
    coEvery { webServerHelper.startServerHelper(any(), true) } returns
      ServerStatus(isServerStarted = true, errorMessage = null)
    every { webServerHelper.getServerAddress() } returns "192.168.0.1:8080"

    hotspotService.resyncServerWithRemainingBooks()

    coVerify(exactly = 1) {
      webServerHelper.startServerHelper(match { it.size == 1 && it.contains("/path1") }, true)
    }
    verify(exactly = 1) { zimHostCallbacks.onServerStarted("192.168.0.1:8080") }
    verify(exactly = 0) { webServerHelper.stopAndroidWebServer() }
  }

  @Test
  fun `resync when the only hosted book was removed stops the server instead of restarting empty`() =
    runTest {
      ServerUtils.isServerStarted = true
      hotspotService.currentlyHostedPaths = listOf("/path1")
      every { dataSource.getLanguageCategorizedBooks() } returns flowOf(emptyList())

      hotspotService.resyncServerWithRemainingBooks()

      coVerify(exactly = 0) { webServerHelper.startServerHelper(any(), any()) }
      verify(exactly = 1) { webServerHelper.stopAndroidWebServer() }
      verify(exactly = 1) { zimHostCallbacks.onServerStopped() }
    }
}
